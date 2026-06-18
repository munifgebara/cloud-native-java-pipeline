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
import { CadastroFotoIaService, CadastroFotoItemSugestao } from '../../core/ia/cadastro-foto';
import { CategoriaResumo, CategoriaService } from '../../core/categoria/categoria';
import { InstanciaItemService } from '../../core/instancia-item/instancia-item';
import { ItemMestreResponse, ItemMestreService } from '../../core/item-mestre/item-mestre';
import { I18nService, TranslatePipe } from '../../core/i18n/i18n';
import { LocalResumo, LocalService } from '../../core/local/local';
import { StellaAiPanelComponent } from '../../shared/design-system/stella-ai-panel/stella-ai-panel';
import { StellaEmptyStateComponent } from '../../shared/design-system/stella-empty-state/stella-empty-state';
import { StellaPageHeaderComponent } from '../../shared/design-system/stella-page-header/stella-page-header';
import { StellaStateMessageComponent } from '../../shared/design-system/stella-state-message/stella-state-message';

const ORIGEM_CADASTRO_IA = 'CADASTRO_IA_FOTO';
const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;

type InstanciaRevisao = {
  aprovada: boolean;
  identifier: string;
  assetTag: string;
  serialNumber: string;
  condition: string;
  notes: string;
  confidence: number | null;
};

type ItemRevisao = {
  aprovada: boolean;
  name: string;
  description: string;
  categoryId: string;
  categoriaSugerida: string;
  brand: string;
  model: string;
  author: string;
  publisher: string;
  publicationYear: string;
  isbn: string;
  source: string;
  identificationVerified: boolean | null;
  quantidade: number;
  condition: string;
  notes: string;
  confidence: number | null;
  instancias: InstanciaRevisao[];
};

@Component({
  selector: 'app-cadastro-foto',
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
  templateUrl: './cadastro-foto.html',
  styleUrl: './cadastro-foto.css',
})
export class CadastroFotoComponent implements OnInit {
  private readonly iaService = inject(CadastroFotoIaService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly localService = inject(LocalService);
  private readonly itemMestreService = inject(ItemMestreService);
  private readonly instanciaService = inject(InstanciaItemService);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);

  arquivo = signal<File | null>(null);
  previewUrl = signal<string | null>(null);
  itens = signal<ItemRevisao[]>([]);
  categorias = signal<CategoriaResumo[]>([]);
  locais = signal<LocalResumo[]>([]);
  localPadraoId = '';
  analisando = signal(false);
  cadastrando = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  ngOnInit(): void {
    this.categoriaService.listar().subscribe({
      next: (categorias) => this.categorias.set(categorias),
      error: () => this.errorMessage.set(this.i18n.translate('photoRegistration.categoryLoadError')),
    });

    this.localService.listar().subscribe({
      next: (locais) => this.locais.set(locais),
      error: () => this.errorMessage.set(this.i18n.translate('photoRegistration.locationLoadError')),
    });
  }

  selecionarArquivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (!file) {
      this.limparImagemSelecionada();
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
        this.itens.set((response.itens ?? []).map((item) => this.toRevisao(item)));
        this.successMessage.set(response.mensagem ?? '');
        this.analisando.set(false);
      },
      error: (error) => {
        this.errorMessage.set(mensagemErroHttp(error, this.i18n.translate('photoRegistration.analysisError')));
        this.analisando.set(false);
      },
    });
  }

  cadastrarSelecionados(): void {
    const selecionados = this.itens().filter((item) => item.aprovada && item.name.trim());
    if (!selecionados.length) {
      this.errorMessage.set(this.i18n.translate('photoRegistration.selectAtLeastOne'));
      return;
    }

    this.cadastrando.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    forkJoin(selecionados.map((item) => this.cadastrarItem(item))).subscribe({
      next: () => {
        this.successMessage.set(this.i18n.translate('photoRegistration.saved'));
        this.cadastrando.set(false);
        this.router.navigate(['/itens-mestre']);
      },
      error: (error) => {
        this.errorMessage.set(mensagemErroHttp(error, this.i18n.translate('photoRegistration.saveError')));
        this.cadastrando.set(false);
      },
    });
  }

  toggleItem(item: ItemRevisao): void {
    item.aprovada = !item.aprovada;
  }

  toggleInstancia(instancia: InstanciaRevisao): void {
    instancia.aprovada = !instancia.aprovada;
  }

  confiancaLabel(valor: number | null): string {
    return valor == null ? '-' : `${Math.round(valor * 100)}%`;
  }

  etapaAtual(): number {
    if (this.cadastrando()) {
      return 4;
    }

    if (this.itens().length) {
      return this.itensAprovados() ? 4 : 3;
    }

    if (this.arquivo() || this.analisando()) {
      return 2;
    }

    return 1;
  }

  itensAprovados(): number {
    return this.itens().filter((item) => item.aprovada && item.name.trim()).length;
  }

  instanciasAprovadasTotal(): number {
    return this.itens()
      .filter((item) => item.aprovada)
      .reduce((total, item) => total + this.instanciasAprovadasItem(item), 0);
  }

  instanciasAprovadasItem(item: ItemRevisao): number {
    return item.instancias.filter((instancia) => instancia.aprovada).length;
  }

  localPadraoResumo(): string {
    if (!this.localPadraoId) {
      return this.i18n.translate('photoRegistration.noDefaultLocation');
    }

    const local = this.locais().find((item) => item.id === this.localPadraoId);
    return local?.caminho || local?.name || this.i18n.translate('photoRegistration.noDefaultLocation');
  }

  private cadastrarItem(item: ItemRevisao) {
    const arquivo = this.arquivo();

    return this.itemMestreService.criar({
      name: item.name.trim(),
      description: this.nullIfBlank(item.description),
      notes: this.observacoesItem(item),
      origemCadastro: ORIGEM_CADASTRO_IA,
      categoryId: item.categoryId || null,
      active: true,
    }).pipe(
      switchMap((salvo) => {
        const itemComImagem$ = arquivo
          ? this.itemMestreService.atualizarImagemPrincipal(salvo.id, arquivo)
          : of(salvo);

        return itemComImagem$.pipe(
          switchMap((itemSalvo) => {
            const instancias = this.instanciasAprovadas(item, itemSalvo);
            if (!instancias.length) {
              return of([]);
            }
            return forkJoin(instancias.map((instancia) => this.instanciaService.criar(instancia)));
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

    this.limparImagemSelecionada();
    this.arquivo.set(file);
    this.previewUrl.set(URL.createObjectURL(file));
    this.itens.set([]);
    return true;
  }

  private limparImagemSelecionada(): void {
    if (this.previewUrl()) {
      URL.revokeObjectURL(this.previewUrl()!);
    }

    this.arquivo.set(null);
    this.previewUrl.set(null);
    this.itens.set([]);
  }

  private instanciasAprovadas(item: ItemRevisao, salvo: ItemMestreResponse) {
    return item.instancias
      .filter((instancia) => instancia.aprovada)
      .map((instancia, index) => ({
        mainItemId: salvo.id,
        currentLocationId: this.localPadraoId || null,
        identifier: this.nullIfBlank(instancia.identifier) ?? `${salvo.name} ${index + 1}`,
        assetTag: this.nullIfBlank(instancia.assetTag),
        serialNumber: this.nullIfBlank(instancia.serialNumber),
        operationalStatus: 'DISPONIVEL' as const,
        notes: this.observacoesInstancia(instancia),
        origemCadastro: ORIGEM_CADASTRO_IA,
        active: true,
      }));
  }

  private toRevisao(item: CadastroFotoItemSugestao): ItemRevisao {
    const quantidade = Math.max(1, item.quantidade ?? item.instancias?.length ?? 1);
    const instanciasFonte = item.instancias?.length ? item.instancias : Array.from({ length: quantidade }, () => ({
      identifier: null,
      assetTag: null,
      serialNumber: null,
      condition: item.condition,
      notes: null,
      confidence: item.confidence,
    }));

    return {
      aprovada: true,
      name: item.name ?? '',
      description: item.description ?? '',
      categoryId: this.categoriaPorNome(item.categoriaSugerida),
      categoriaSugerida: item.categoriaSugerida ?? '',
      brand: item.brand ?? '',
      model: item.model ?? '',
      author: item.author ?? '',
      publisher: item.publisher ?? '',
      publicationYear: item.publicationYear ?? '',
      isbn: item.isbn ?? '',
      source: item.source ?? '',
      identificationVerified: item.identificationVerified,
      quantidade,
      condition: item.condition ?? '',
      notes: item.notes ?? '',
      confidence: item.confidence,
      instancias: instanciasFonte.map((instancia, index) => ({
        aprovada: true,
        identifier: instancia.identifier ?? `${item.name ?? 'Item'} ${index + 1}`,
        assetTag: instancia.assetTag ?? '',
        serialNumber: instancia.serialNumber ?? '',
        condition: instancia.condition ?? '',
        notes: instancia.notes ?? '',
        confidence: instancia.confidence,
      })),
    };
  }

  private categoriaPorNome(name: string | null): string {
    const normalizado = (name ?? '').trim().toLocaleLowerCase();
    if (!normalizado) {
      return '';
    }

    return this.categorias().find((categoria) => categoria.name.toLocaleLowerCase() === normalizado)?.id ?? '';
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
      item.categoriaSugerida ? `Categoria sugerida pela IA: ${item.categoriaSugerida}` : '',
    ]);
  }

  private observacoesInstancia(instancia: InstanciaRevisao): string | null {
    return this.joinObservacoes([
      instancia.notes,
      instancia.condition ? `Estado sugerido: ${instancia.condition}` : '',
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
