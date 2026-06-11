import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { CategoriaResumo, CategoriaService } from '../../../core/categoria/categoria';
import { mensagemErroHttp } from '../../../core/http-error';
import { ItemMestreService } from '../../../core/item-mestre/item-mestre';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-item-mestre-form',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, TextareaModule, ButtonModule, CardModule, CheckboxModule, RouterLink, TranslatePipe],
  templateUrl: './item-mestre-form.html',
  styleUrl: './item-mestre-form.css',
})
export class ItemMestreFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);
  private readonly itemMestreService = inject(ItemMestreService);
  private readonly categoriaService = inject(CategoriaService);

  id = signal<string | null>(null);
  categorias = signal<CategoriaResumo[]>([]);
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
  gerandoImagemIa = signal(false);

  readonly edicao = computed(() => !!this.id());

  form = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    categoriaId: [''],
    descricao: ['', [Validators.maxLength(500)]],
    observacoes: ['', [Validators.maxLength(1000)]],
    ativa: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.id.set(id);
    this.carregarCategorias();

    if (!id) {
      return;
    }

    this.loading.set(true);

    this.itemMestreService.buscarPorId(id).subscribe({
      next: (item) => {
        this.form.patchValue({
          nome: item.nome ?? '',
          categoriaId: item.categoriaId ?? '',
          descricao: item.descricao ?? '',
          observacoes: item.observacoes ?? '',
          ativa: item.ativa,
        });
        this.imagemAtualUrl.set(item.imagemUrl);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('masterItems.form.loadError'));
        this.loading.set(false);
      },
    });
  }

  salvar(): void {
    this.errorMessage.set('');
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.errorMessage.set(this.i18n.translate('masterItems.form.invalidFields'));
      return;
    }

    this.salvando.set(true);

    const valor = this.form.getRawValue();
    const payload = {
      nome: valor.nome?.trim() ?? '',
      categoriaId: this.nullIfBlank(valor.categoriaId),
      descricao: this.nullIfBlank(valor.descricao),
      observacoes: this.nullIfBlank(valor.observacoes),
      ativa: !!valor.ativa,
    };

    if (this.edicao()) {
      this.itemMestreService.atualizar(this.id()!, payload).subscribe({
        next: (item) => this.enviarImagemSeNecessario(item.id),
        error: (err) => {
          this.salvando.set(false);
          this.errorMessage.set(this.extractError(err, this.i18n.translate('masterItems.form.updateError')));
        },
      });

      return;
    }

    this.itemMestreService.criar(payload).subscribe({
      next: (item) => this.enviarImagemSeNecessario(item.id),
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('masterItems.form.createError')));
      },
    });
  }

  selecionarImagem(event: Event): void {
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

    if (!['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(arquivo.type)) {
      this.errorMessage.set(this.i18n.translate('masterItems.form.imageInvalidType'));
      input.value = '';
      return;
    }

    if (arquivo.size > 5 * 1024 * 1024) {
      this.errorMessage.set(this.i18n.translate('masterItems.form.imageTooLarge'));
      input.value = '';
      return;
    }

    this.imagemSelecionada.set(arquivo);
    this.imagemSelecionadaGeradaPorIa.set(false);
    this.imagemSelecionadaProvider.set(null);
    this.imagemPreviewUrl.set(URL.createObjectURL(arquivo));
    this.limparImagemIaGerada();
  }

  campoInvalido(nome: keyof typeof this.form.controls): boolean {
    const campo = this.form.controls[nome];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  gerarImagemIa(): void {
    this.errorMessage.set('');
    const valor = this.form.getRawValue();
    const nome = this.nullIfBlank(valor.nome);

    if (!nome) {
      this.errorMessage.set(this.i18n.translate('masterItems.form.aiNameRequired'));
      return;
    }

    this.gerandoImagemIa.set(true);
    this.itemMestreService.gerarImagemIa({
      nome,
      categoria: this.categoriaSelecionadaNome(valor.categoriaId),
      descricao: this.nullIfBlank(valor.descricao),
    }).subscribe({
      next: (imagem) => {
        this.imagemIaPreviewUrl.set(imagem.dataUrl);
        this.imagemIaProvider.set(imagem.provider);
        this.gerandoImagemIa.set(false);
      },
      error: (err) => {
        this.gerandoImagemIa.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('masterItems.form.aiImageError')));
      },
    });
  }

  aceitarImagemIa(): void {
    const dataUrl = this.imagemIaPreviewUrl();
    if (!dataUrl) {
      return;
    }

    const arquivo = this.dataUrlToFile(dataUrl, 'imagem-ia.png');
    this.imagemSelecionada.set(arquivo);
    this.imagemSelecionadaGeradaPorIa.set(true);
    this.imagemSelecionadaProvider.set(this.imagemIaProvider());
    this.imagemPreviewUrl.set(dataUrl);
    this.limparImagemIaGerada();
  }

  cancelarImagemIa(): void {
    this.limparImagemIaGerada();
  }

  private carregarCategorias(): void {
    this.categoriaService.listar().subscribe({
      next: (categorias) => this.categorias.set(categorias),
      error: () => this.errorMessage.set(this.i18n.translate('masterItems.form.categoryLoadError')),
    });
  }

  private enviarImagemSeNecessario(itemId: string): void {
    const imagem = this.imagemSelecionada();

    if (!imagem) {
      this.salvando.set(false);
      this.router.navigate(['/itens-mestre']);
      return;
    }

    this.itemMestreService.atualizarImagemPrincipal(
      itemId,
      imagem,
      this.imagemSelecionadaGeradaPorIa(),
      this.imagemSelecionadaGeradaPorIa() ? this.imagemSelecionadaProvider() : null,
    ).subscribe({
      next: () => {
        this.salvando.set(false);
        this.router.navigate(['/itens-mestre']);
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

  private categoriaSelecionadaNome(categoriaId: string | null | undefined): string | null {
    if (!categoriaId) {
      return null;
    }

    return this.categorias().find((categoria) => categoria.id === categoriaId)?.nome ?? null;
  }

  private limparImagemIaGerada(): void {
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
