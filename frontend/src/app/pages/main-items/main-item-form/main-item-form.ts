import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { CategorySummary, CategoryService } from '../../../core/category/category';
import { IMAGE_CONTENT_TYPES, imageFileFromPaste } from '../../../core/image/image-clipboard';
import { mensagemErroHttp } from '../../../core/http-error';
import { MainItemService } from '../../../core/main-item/main-item';
import { ItemInstanceSummary, ItemInstanceService, StatusOperacionalInstancia } from '../../../core/item-instance/item-instance';
import { LocationSummary, LocationService } from '../../../core/location/location';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-main-item-form',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, InputTextModule, TextareaModule, ButtonModule, CardModule, CheckboxModule, DialogModule, TableModule, TagModule, RouterLink, TranslatePipe],
  templateUrl: './main-item-form.html',
  styleUrl: './main-item-form.css',
})
export class MainItemFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);
  private readonly itemMestreService = inject(MainItemService);
  private readonly categoryService = inject(CategoryService);
  private readonly instanciaItemService = inject(ItemInstanceService);
  private readonly localService = inject(LocationService);

  id = signal<string | null>(null);
  categories = signal<CategorySummary[]>([]);
  loading = signal(false);
  salvando = signal(false);
  errorMessage = signal('');
  imagemAtualUrl = signal<string | null>(null);
  imagemPreviewUrl = signal<string | null>(null);
  imagemSelecionada = signal<File | null>(null);
  imagemSelecionadaGeradaPorIa = signal(false);
  imagemSelecionadaProvider = signal<string | null>(null);
  imagemIaPreviewUrl = signal<string | null>(null);
  imagemIaProvider = signal<string | null>(null);
  gerandoImageIa = signal(false);

  // Instâncias do item
  instancias = signal<ItemInstanceSummary[]>([]);
  carregandoInstancias = signal(false);
  locations = signal<LocationSummary[]>([]);
  dialogInstancia = signal(false);
  salvandoInstancia = signal(false);
  instanciaError = signal('');
  novaInstanciaLocalId = '';
  novaInstanciaIdentificador = '';
  novaInstanciaPatrimonio = '';
  novaInstanciaNumeroSerie = '';
  novaInstanciaObservacoes = '';

  readonly edicao = computed(() => !!this.id());

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    categoryId: [''],
    description: ['', [Validators.maxLength(500)]],
    notes: ['', [Validators.maxLength(1000)]],
    active: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.id.set(id);
    this.carregarCategories();

    if (!id) {
      return;
    }

    this.loading.set(true);

    this.itemMestreService.buscarPorId(id).subscribe({
      next: (item) => {
        this.form.patchValue({
          name: item.name ?? '',
          categoryId: item.categoryId ?? '',
          description: item.description ?? '',
          notes: item.notes ?? '',
          active: item.active,
        });
        this.imagemAtualUrl.set(item.imageUrl);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('masterItems.form.loadError'));
        this.loading.set(false);
      },
    });

    this.carregarInstancias();
    this.carregarLocais();
  }

  save(): void {
    this.errorMessage.set('');
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.errorMessage.set(this.i18n.translate('masterItems.form.invalidFields'));
      return;
    }

    this.salvando.set(true);

    const valor = this.form.getRawValue();
    const payload = {
      name: valor.name?.trim() ?? '',
      categoryId: this.nullIfBlank(valor.categoryId),
      description: this.nullIfBlank(valor.description),
      notes: this.nullIfBlank(valor.notes),
      active: !!valor.active,
    };

    if (this.edicao()) {
      this.itemMestreService.update(this.id()!, payload).subscribe({
        next: (item) => this.enviarImageSeNecessario(item.id),
        error: (err) => {
          this.salvando.set(false);
          this.errorMessage.set(this.extractError(err, this.i18n.translate('masterItems.form.updateError')));
        },
      });

      return;
    }

    this.itemMestreService.criar(payload).subscribe({
      next: (item) => this.enviarImageSeNecessario(item.id),
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('masterItems.form.createError')));
      },
    });
  }

  // ── Gestão de instâncias ────────────────────────────────────────────────────

  abrirDialogInstancia(): void {
    this.instanciaError.set('');
    this.novaInstanciaLocalId = '';
    this.novaInstanciaIdentificador = '';
    this.novaInstanciaPatrimonio = '';
    this.novaInstanciaNumeroSerie = '';
    this.novaInstanciaObservacoes = '';
    this.dialogInstancia.set(true);
  }

  fecharDialogInstancia(): void {
    if (this.salvandoInstancia()) return;
    this.dialogInstancia.set(false);
  }

  salvarInstancia(): void {
    this.instanciaError.set('');
    const localId = this.novaInstanciaLocalId;
    const identifier = this.nullIfBlank(this.novaInstanciaIdentificador);
    const assetTag = this.nullIfBlank(this.novaInstanciaPatrimonio);
    const serialNumber = this.nullIfBlank(this.novaInstanciaNumeroSerie);

    if (!localId) {
      this.instanciaError.set(this.i18n.translate('itemInstances.form.locationInvalid'));
      return;
    }

    if (!identifier && !assetTag && !serialNumber) {
      this.instanciaError.set(this.i18n.translate('itemInstances.form.identificationRequired'));
      return;
    }

    this.salvandoInstancia.set(true);

    this.instanciaItemService.registrarEntrada({
      mainItemId: this.id()!,
      destinationLocationId: localId,
      identifier,
      assetTag,
      serialNumber,
      notes: this.nullIfBlank(this.novaInstanciaObservacoes),
    }).subscribe({
      next: () => {
        this.salvandoInstancia.set(false);
        this.dialogInstancia.set(false);
        this.carregarInstancias();
      },
      error: (err) => {
        this.salvandoInstancia.set(false);
        this.instanciaError.set(this.extractError(err, this.i18n.translate('masterItems.form.instanceCreateError')));
      },
    });
  }

  instanciaLabel(instancia: ItemInstanceSummary): string {
    return instancia.identifier || instancia.assetTag || instancia.serialNumber || '-';
  }

  instanciaStatusLabel(status: StatusOperacionalInstancia): string {
    return this.i18n.translate(`itemInstances.status.${status}`);
  }

  instanciaStatusSeverity(status: StatusOperacionalInstancia): 'success' | 'info' | 'warn' | 'secondary' {
    const map: Record<StatusOperacionalInstancia, 'success' | 'info' | 'warn' | 'secondary'> = {
      DISPONIVEL: 'success', EM_MOVIMENTACAO: 'info', EMPRESTADO: 'warn', INATIVO: 'secondary',
    };
    return map[status];
  }

  locationsAtivos(): LocationSummary[] {
    return this.locations().filter(l => l.active);
  }

  // ── Image ──────────────────────────────────────────────────────────────────

  selecionarImage(event: Event): void {
    this.errorMessage.set('');
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0] ?? null;

    if (!arquivo) {
      this.imagemSelecionada.set(null);
      this.imagemSelecionadaGeradaPorIa.set(false);
      this.imagemSelecionadaProvider.set(null);
      this.imagemPreviewUrl.set(null);
      return;
    }

    if (!this.aplicarImageSelecionada(arquivo)) {
      input.value = '';
      return;
    }
  }

  colarImage(event: ClipboardEvent): void {
    this.errorMessage.set('');
    const result = imageFileFromPaste(event, MAX_IMAGE_SIZE_BYTES);

    if (!result.ok) {
      if (result.reason === 'missing') {
        this.errorMessage.set(this.i18n.translate('masterItems.form.imagePasteMissing'));
      } else if (result.reason === 'invalid-type') {
        this.errorMessage.set(this.i18n.translate('masterItems.form.imageInvalidType'));
      } else {
        this.errorMessage.set(this.i18n.translate('masterItems.form.imageTooLarge'));
      }
      return;
    }

    this.aplicarImageSelecionada(result.file);
  }

  private aplicarImageSelecionada(arquivo: File): boolean {
    if (!IMAGE_CONTENT_TYPES.includes(arquivo.type as (typeof IMAGE_CONTENT_TYPES)[number])) {
      this.errorMessage.set(this.i18n.translate('masterItems.form.imageInvalidType'));
      return false;
    }

    if (arquivo.size > MAX_IMAGE_SIZE_BYTES) {
      this.errorMessage.set(this.i18n.translate('masterItems.form.imageTooLarge'));
      return false;
    }

    if (this.imagemPreviewUrl()) {
      URL.revokeObjectURL(this.imagemPreviewUrl()!);
    }

    this.imagemSelecionada.set(arquivo);
    this.imagemSelecionadaGeradaPorIa.set(false);
    this.imagemSelecionadaProvider.set(null);
    this.imagemPreviewUrl.set(URL.createObjectURL(arquivo));
    this.limparImageIaGerada();
    return true;
  }

  campoInvalido(name: keyof typeof this.form.controls): boolean {
    const campo = this.form.controls[name];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  gerarImageIa(): void {
    this.errorMessage.set('');
    const valor = this.form.getRawValue();
    const name = this.nullIfBlank(valor.name);

    if (!name) {
      this.errorMessage.set(this.i18n.translate('masterItems.form.aiNameRequired'));
      return;
    }

    this.gerandoImageIa.set(true);
    this.itemMestreService.gerarImageIa({
      name,
      category: this.categorySelecionadaNome(valor.categoryId),
      description: this.nullIfBlank(valor.description),
    }).subscribe({
      next: (imagem) => {
        this.imagemIaPreviewUrl.set(imagem.dataUrl);
        this.imagemIaProvider.set(imagem.provider);
        this.gerandoImageIa.set(false);
      },
      error: (err) => {
        this.gerandoImageIa.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('masterItems.form.aiImageError')));
      },
    });
  }

  aceitarImageIa(): void {
    const dataUrl = this.imagemIaPreviewUrl();
    if (!dataUrl) {
      return;
    }

    const arquivo = this.dataUrlToFile(dataUrl, 'imagem-ia.png');
    this.imagemSelecionada.set(arquivo);
    this.imagemSelecionadaGeradaPorIa.set(true);
    this.imagemSelecionadaProvider.set(this.imagemIaProvider());
    this.imagemPreviewUrl.set(dataUrl);
    this.limparImageIaGerada();
  }

  cancelarImageIa(): void {
    this.limparImageIaGerada();
  }

  // ── Privados ────────────────────────────────────────────────────────────────

  private carregarCategories(): void {
    this.categoryService.listar().subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => this.errorMessage.set(this.i18n.translate('masterItems.form.categoryLoadError')),
    });
  }

  private carregarInstancias(): void {
    const id = this.id();
    if (!id) return;
    this.carregandoInstancias.set(true);
    this.instanciaItemService.listar().subscribe({
      next: (todas) => {
        this.instancias.set(todas.filter(i => i.mainItemId === id));
        this.carregandoInstancias.set(false);
      },
      error: () => {
        this.instanciaError.set(this.i18n.translate('masterItems.form.instancesLoadError'));
        this.carregandoInstancias.set(false);
      },
    });
  }

  private carregarLocais(): void {
    this.localService.listar().subscribe({
      next: (locations) => this.locations.set(locations),
      error: () => {},
    });
  }

  private enviarImageSeNecessario(itemId: string): void {
    const imagem = this.imagemSelecionada();

    if (!imagem) {
      this.salvando.set(false);
      this.router.navigate(['/main-items']);
      return;
    }

    this.itemMestreService.atualizarImagePrincipal(
      itemId,
      imagem,
      this.imagemSelecionadaGeradaPorIa(),
      this.imagemSelecionadaGeradaPorIa() ? this.imagemSelecionadaProvider() : null,
    ).subscribe({
      next: () => {
        this.salvando.set(false);
        this.router.navigate(['/main-items']);
      },
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('masterItems.form.imageUploadError')));
      },
    });
  }

  private nullIfBlank(value: string | null | undefined): string | null {
    const v = (value ?? '').trim();
    return v ? v : null;
  }

  private categorySelecionadaNome(categoryId: string | null | undefined): string | null {
    if (!categoryId) {
      return null;
    }

    return this.categories().find((category) => category.id === categoryId)?.name ?? null;
  }

  private limparImageIaGerada(): void {
    this.imagemIaPreviewUrl.set(null);
    this.imagemIaProvider.set(null);
  }

  private dataUrlToFile(dataUrl: string, fileName: string): File {
    const [header, base64] = dataUrl.split(',');
    const contentType = header.match(/data:(.*);base64/)?.[1] ?? 'image/png';
    const bytes = atob(base64);
    const buffer = new Uint8Array(bytes.length);

    for (let i = 0; i < bytes.length; i++) {
      buffer[i] = bytes.charCodeAt(i);
    }

    return new File([buffer], fileName, { type: contentType });
  }

  private extractError(err: any, fallback: string): string {
    return mensagemErroHttp(err, fallback);
  }
}
