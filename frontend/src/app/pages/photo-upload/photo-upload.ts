import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { forkJoin, of, switchMap } from 'rxjs';
import { IMAGE_CONTENT_TYPES, imageFileFromPaste } from '../../core/image/image-clipboard';
import { mensagemErroHttp } from '../../core/http-error';
import { PhotoUploadAiService, PhotoUploadItemSuggestion } from '../../core/ia/photo-upload';
import { CategorySummary, CategoryService } from '../../core/category/category';
import { ItemInstanceService } from '../../core/item-instance/item-instance';
import { MainItemResponse, MainItemService } from '../../core/main-item/main-item';
import { I18nService, TranslatePipe } from '../../core/i18n/i18n';
import { LocationSummary, LocationService } from '../../core/location/location';
import { StellaAiPanelComponent } from '../../shared/design-system/stella-ai-panel/stella-ai-panel';
import { StellaEmptyStateComponent } from '../../shared/design-system/stella-empty-state/stella-empty-state';
import { StellaPageHeaderComponent } from '../../shared/design-system/stella-page-header/stella-page-header';
import { StellaStateMessageComponent } from '../../shared/design-system/stella-state-message/stella-state-message';

const ORIGEM_CADASTRO_IA = 'CADASTRO_IA_FOTO';
const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;

type InstanceReview = {
  approved: boolean;
  identifier: string;
  assetTag: string;
  serialNumber: string;
  condition: string;
  notes: string;
  confidence: number | null;
};

type ItemRevisao = {
  approved: boolean;
  name: string;
  description: string;
  categoryId: string;
  suggestedCategory: string;
  brand: string;
  model: string;
  author: string;
  publisher: string;
  publicationYear: string;
  isbn: string;
  source: string;
  identificationVerified: boolean | null;
  quantity: number;
  condition: string;
  notes: string;
  confidence: number | null;
  instances: InstanceReview[];
};

@Component({
  selector: 'app-photo-upload',
  standalone: true,
  imports: [
    FormsModule,
    ButtonModule,
    CardModule,
    CheckboxModule,
    InputTextModule,
    TagModule,
    TranslatePipe,
    StellaAiPanelComponent,
    StellaEmptyStateComponent,
    StellaPageHeaderComponent,
    StellaStateMessageComponent,
  ],
  templateUrl: './photo-upload.html',
  styleUrl: './photo-upload.css',
})
export class PhotoUploadComponent implements OnInit {
  private readonly iaService = inject(PhotoUploadAiService);
  private readonly categoryService = inject(CategoryService);
  private readonly localService = inject(LocationService);
  private readonly itemMestreService = inject(MainItemService);
  private readonly instanceService = inject(ItemInstanceService);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);

  arquivo = signal<File | null>(null);
  previewUrl = signal<string | null>(null);
  items = signal<ItemRevisao[]>([]);
  categories = signal<CategorySummary[]>([]);
  locations = signal<LocationSummary[]>([]);
  localPadraoId = '';
  analisando = signal(false);
  cadastrando = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  ngOnInit(): void {
    this.categoryService.listar().subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => this.errorMessage.set(this.i18n.translate('photoRegistration.categoryLoadError')),
    });

    this.localService.listar().subscribe({
      next: (locations) => this.locations.set(locations),
      error: () => this.errorMessage.set(this.i18n.translate('photoRegistration.locationLoadError')),
    });
  }

  selecionarArquivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (!file) {
      this.limparImageSelecionada();
      this.errorMessage.set('');
      this.successMessage.set('');
      return;
    }

    if (!this.aplicarArquivo(file)) {
      input.value = '';
    }
  }

  colarArquivo(event: ClipboardEvent): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    const result = imageFileFromPaste(event, MAX_IMAGE_SIZE_BYTES);

    if (!result.ok) {
      if (result.reason === 'missing') {
        this.errorMessage.set(this.i18n.translate('photoRegistration.filePasteMissing'));
      } else if (result.reason === 'invalid-type') {
        this.errorMessage.set(this.i18n.translate('photoRegistration.fileInvalidType'));
      } else {
        this.errorMessage.set(this.i18n.translate('photoRegistration.fileTooLarge'));
      }
      return;
    }

    this.aplicarArquivo(result.file);
  }

  analisar(): void {
    const arquivo = this.arquivo();
    if (!arquivo) {
      this.errorMessage.set(this.i18n.translate('photoRegistration.fileRequired'));
      return;
    }

    this.analisando.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.iaService.sugerirCadastro(arquivo).subscribe({
      next: (response) => {
        this.items.set((response.items ?? []).map((item) => this.toRevisao(item)));
        this.successMessage.set(response.message ?? '');
        this.analisando.set(false);
      },
      error: (error) => {
        this.errorMessage.set(mensagemErroHttp(error, this.i18n.translate('photoRegistration.analysisError')));
        this.analisando.set(false);
      },
    });
  }

  cadastrarSelecionados(): void {
    const selecionados = this.items().filter((item) => item.approved && item.name.trim());
    if (!selecionados.length) {
      this.errorMessage.set(this.i18n.translate('photoRegistration.selectAtLeastOne'));
      return;
    }

    if (!this.localPadraoId) {
      this.errorMessage.set(this.i18n.translate('photoRegistration.locationRequired'));
      return;
    }

    this.cadastrando.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    forkJoin(selecionados.map((item) => this.cadastrarItem(item))).subscribe({
      next: () => {
        this.successMessage.set(this.i18n.translate('photoRegistration.saved'));
        this.cadastrando.set(false);
        this.router.navigate(['/main-items']);
      },
      error: (error) => {
        this.errorMessage.set(mensagemErroHttp(error, this.i18n.translate('photoRegistration.saveError')));
        this.cadastrando.set(false);
      },
    });
  }

  toggleItem(item: ItemRevisao): void {
    item.approved = !item.approved;
  }

  toggleInstancia(instance: InstanceReview): void {
    instance.approved = !instance.approved;
  }

  confiancaLabel(valor: number | null): string {
    return valor == null ? '-' : `${Math.round(valor * 100)}%`;
  }

  etapaAtual(): number {
    if (this.cadastrando()) {
      return 4;
    }

    if (this.items().length) {
      return this.itensAprovados() ? 4 : 3;
    }

    if (this.arquivo() || this.analisando()) {
      return 2;
    }

    return 1;
  }

  itensAprovados(): number {
    return this.items().filter((item) => item.approved && item.name.trim()).length;
  }

  approvedInstancesTotal(): number {
    return this.items()
      .filter((item) => item.approved)
      .reduce((total, item) => total + this.approvedInstancesForItem(item), 0);
  }

  approvedInstancesForItem(item: ItemRevisao): number {
    return item.instances.filter((instance) => instance.approved).length;
  }

  localPadraoSummary(): string {
    if (!this.localPadraoId) {
      return this.i18n.translate('photoRegistration.noDefaultLocation');
    }

    const local = this.locations().find((item) => item.id === this.localPadraoId);
    return local?.path || local?.name || this.i18n.translate('photoRegistration.noDefaultLocation');
  }

  private cadastrarItem(item: ItemRevisao) {
    const arquivo = this.arquivo();

    return this.itemMestreService.criar({
      name: item.name.trim(),
      description: this.nullIfBlank(item.description),
      notes: this.observacoesItem(item),
      registrationOrigin: ORIGEM_CADASTRO_IA,
      categoryId: item.categoryId || null,
      active: true,
    }).pipe(
      switchMap((salvo) => {
        const itemComImage$ = arquivo
          ? this.itemMestreService.atualizarImagePrincipal(salvo.id, arquivo)
          : of(salvo);

        return itemComImage$.pipe(
          switchMap((itemSalvo) => {
            const instances = this.approvedInstances(item, itemSalvo);
            if (!instances.length) {
              return of([]);
            }
            return forkJoin(instances.map((instance) => this.instanceService.criar(instance)));
          }),
        );
      }),
    );
  }

  private aplicarArquivo(file: File): boolean {
    this.errorMessage.set('');
    this.successMessage.set('');

    if (!IMAGE_CONTENT_TYPES.includes(file.type as (typeof IMAGE_CONTENT_TYPES)[number])) {
      this.errorMessage.set(this.i18n.translate('photoRegistration.fileInvalidType'));
      return false;
    }

    if (file.size > MAX_IMAGE_SIZE_BYTES) {
      this.errorMessage.set(this.i18n.translate('photoRegistration.fileTooLarge'));
      return false;
    }

    this.limparImageSelecionada();
    this.arquivo.set(file);
    this.previewUrl.set(URL.createObjectURL(file));
    this.items.set([]);
    return true;
  }

  private limparImageSelecionada(): void {
    if (this.previewUrl()) {
      URL.revokeObjectURL(this.previewUrl()!);
    }

    this.arquivo.set(null);
    this.previewUrl.set(null);
    this.items.set([]);
  }

  private approvedInstances(item: ItemRevisao, salvo: MainItemResponse) {
    return item.instances
      .filter((instance) => instance.approved)
      .map((instance, index) => ({
        mainItemId: salvo.id,
        currentLocationId: this.localPadraoId,
        identifier: this.nullIfBlank(instance.identifier) ?? `${salvo.name} ${index + 1}`,
        assetTag: this.nullIfBlank(instance.assetTag),
        serialNumber: this.nullIfBlank(instance.serialNumber),
        operationalStatus: 'DISPONIVEL' as const,
        notes: this.observacoesInstancia(instance),
        registrationOrigin: ORIGEM_CADASTRO_IA,
        active: true,
      }));
  }

  private toRevisao(item: PhotoUploadItemSuggestion): ItemRevisao {
    const quantity = Math.max(1, item.quantity ?? item.instances?.length ?? 1);
    const instancesSource = item.instances?.length ? item.instances : Array.from({ length: quantity }, () => ({
      identifier: null,
      assetTag: null,
      serialNumber: null,
      condition: item.condition,
      notes: null,
      confidence: item.confidence,
    }));

    return {
      approved: true,
      name: item.name ?? '',
      description: item.description ?? '',
      categoryId: this.categoryPorNome(item.suggestedCategory),
      suggestedCategory: item.suggestedCategory ?? '',
      brand: item.brand ?? '',
      model: item.model ?? '',
      author: item.author ?? '',
      publisher: item.publisher ?? '',
      publicationYear: item.publicationYear ?? '',
      isbn: item.isbn ?? '',
      source: item.source ?? '',
      identificationVerified: item.identificationVerified,
      quantity,
      condition: item.condition ?? '',
      notes: item.notes ?? '',
      confidence: item.confidence,
      instances: instancesSource.map((instance, index) => ({
        approved: true,
        identifier: instance.identifier ?? `${item.name ?? 'Item'} ${index + 1}`,
        assetTag: instance.assetTag ?? '',
        serialNumber: instance.serialNumber ?? '',
        condition: instance.condition ?? '',
        notes: instance.notes ?? '',
        confidence: instance.confidence,
      })),
    };
  }

  private categoryPorNome(name: string | null): string {
    const normalizado = (name ?? '').trim().toLocaleLowerCase();
    if (!normalizado) {
      return '';
    }

    return this.categories().find((category) => category.name.toLocaleLowerCase() === normalizado)?.id ?? '';
  }

  private observacoesItem(item: ItemRevisao): string | null {
    return this.joinObservacoes([
      item.notes,
      item.brand ? `Marca sugerida: ${item.brand}` : '',
      item.model ? `Modelo sugerido: ${item.model}` : '',
      item.author ? `Autor identificado: ${item.author}` : '',
      item.publisher ? `Editora identificada: ${item.publisher}` : '',
      item.publicationYear ? `Ano de publicação identificado: ${item.publicationYear}` : '',
      item.isbn ? `ISBN identificado: ${item.isbn}` : '',
      item.source ? `Fonte da identificação: ${item.source}` : '',
      item.identificationVerified != null ? `Identificação verificada: ${item.identificationVerified ? 'sim' : 'não'}` : '',
      item.condition ? `Estado sugerido: ${item.condition}` : '',
      item.suggestedCategory ? `Categoria sugerida pela IA: ${item.suggestedCategory}` : '',
    ]);
  }

  private observacoesInstancia(instance: InstanceReview): string | null {
    return this.joinObservacoes([
      instance.notes,
      instance.condition ? `Estado sugerido: ${instance.condition}` : '',
    ]);
  }

  private joinObservacoes(valores: string[]): string | null {
    const texto = valores.map((valor) => valor.trim()).filter(Boolean).join('\n');
    return texto || null;
  }

  private nullIfBlank(valor: string): string | null {
    const tratado = valor.trim();
    return tratado || null;
  }
}
