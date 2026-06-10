import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { forkJoin, of, switchMap } from 'rxjs';
import { mensagemErroHttp } from '../../core/http-error';
import { CadastroFotoIaService, CadastroFotoItemSugestao } from '../../core/ia/cadastro-foto';
import { CategoriaResumo, CategoriaService } from '../../core/categoria/categoria';
import { InstanciaItemService } from '../../core/instancia-item/instancia-item';
import { ItemMestreResponse, ItemMestreService } from '../../core/item-mestre/item-mestre';
import { I18nService, TranslatePipe } from '../../core/i18n/i18n';
import { LocalResumo, LocalService } from '../../core/local/local';

const ORIGEM_CADASTRO_IA = 'CADASTRO_IA_FOTO';

type InstanciaRevisao = {
  aprovada: boolean;
  identificador: string;
  patrimonio: string;
  numeroSerie: string;
  estadoConservacao: string;
  observacoes: string;
  confianca: number | null;
};

type ItemRevisao = {
  aprovada: boolean;
  nome: string;
  descricao: string;
  categoriaId: string;
  categoriaSugerida: string;
  marca: string;
  modelo: string;
  autor: string;
  editora: string;
  anoPublicacao: string;
  isbn: string;
  fontePesquisa: string;
  identificacaoVerificada: boolean | null;
  quantidade: number;
  estadoConservacao: string;
  observacoes: string;
  confianca: number | null;
  instancias: InstanciaRevisao[];
};

@Component({
  selector: 'app-cadastro-foto',
  standalone: true,
  imports: [FormsModule, ButtonModule, CardModule, CheckboxModule, InputTextModule, TagModule, TranslatePipe],
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

    if (this.previewUrl()) {
      URL.revokeObjectURL(this.previewUrl()!);
    }

    this.arquivo.set(file);
    this.previewUrl.set(file ? URL.createObjectURL(file) : null);
    this.itens.set([]);
    this.errorMessage.set('');
    this.successMessage.set('');
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
    const selecionados = this.itens().filter((item) => item.aprovada && item.nome.trim());
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

  private cadastrarItem(item: ItemRevisao) {
    return this.itemMestreService.criar({
      nome: item.nome.trim(),
      descricao: this.nullIfBlank(item.descricao),
      observacoes: this.observacoesItem(item),
      origemCadastro: ORIGEM_CADASTRO_IA,
      categoriaId: item.categoriaId || null,
      ativa: true,
    }).pipe(
      switchMap((salvo) => {
        const instancias = this.instanciasAprovadas(item, salvo);
        if (!instancias.length) {
          return of([]);
        }
        return forkJoin(instancias.map((instancia) => this.instanciaService.criar(instancia)));
      }),
    );
  }

  private instanciasAprovadas(item: ItemRevisao, salvo: ItemMestreResponse) {
    return item.instancias
      .filter((instancia) => instancia.aprovada)
      .map((instancia, index) => ({
        itemMestreId: salvo.id,
        localAtualId: this.localPadraoId || null,
        identificador: this.nullIfBlank(instancia.identificador) ?? `${salvo.nome} ${index + 1}`,
        patrimonio: this.nullIfBlank(instancia.patrimonio),
        numeroSerie: this.nullIfBlank(instancia.numeroSerie),
        statusOperacional: 'DISPONIVEL' as const,
        observacoes: this.observacoesInstancia(instancia),
        origemCadastro: ORIGEM_CADASTRO_IA,
        ativa: true,
      }));
  }

  private toRevisao(item: CadastroFotoItemSugestao): ItemRevisao {
    const quantidade = Math.max(1, item.quantidade ?? item.instancias?.length ?? 1);
    const instanciasFonte = item.instancias?.length ? item.instancias : Array.from({ length: quantidade }, () => ({
      identificador: null,
      patrimonio: null,
      numeroSerie: null,
      estadoConservacao: item.estadoConservacao,
      observacoes: null,
      confianca: item.confianca,
    }));

    return {
      aprovada: true,
      nome: item.nome ?? '',
      descricao: item.descricao ?? '',
      categoriaId: this.categoriaPorNome(item.categoriaSugerida),
      categoriaSugerida: item.categoriaSugerida ?? '',
      marca: item.marca ?? '',
      modelo: item.modelo ?? '',
      autor: item.autor ?? '',
      editora: item.editora ?? '',
      anoPublicacao: item.anoPublicacao ?? '',
      isbn: item.isbn ?? '',
      fontePesquisa: item.fontePesquisa ?? '',
      identificacaoVerificada: item.identificacaoVerificada,
      quantidade,
      estadoConservacao: item.estadoConservacao ?? '',
      observacoes: item.observacoes ?? '',
      confianca: item.confianca,
      instancias: instanciasFonte.map((instancia, index) => ({
        aprovada: true,
        identificador: instancia.identificador ?? `${item.nome ?? 'Item'} ${index + 1}`,
        patrimonio: instancia.patrimonio ?? '',
        numeroSerie: instancia.numeroSerie ?? '',
        estadoConservacao: instancia.estadoConservacao ?? '',
        observacoes: instancia.observacoes ?? '',
        confianca: instancia.confianca,
      })),
    };
  }

  private categoriaPorNome(nome: string | null): string {
    const normalizado = (nome ?? '').trim().toLocaleLowerCase();
    if (!normalizado) {
      return '';
    }

    return this.categorias().find((categoria) => categoria.nome.toLocaleLowerCase() === normalizado)?.id ?? '';
  }

  private observacoesItem(item: ItemRevisao): string | null {
    return this.joinObservacoes([
      item.observacoes,
      item.marca ? `Marca sugerida: ${item.marca}` : '',
      item.modelo ? `Modelo sugerido: ${item.modelo}` : '',
      item.autor ? `Autor identificado: ${item.autor}` : '',
      item.editora ? `Editora identificada: ${item.editora}` : '',
      item.anoPublicacao ? `Ano de publicação identificado: ${item.anoPublicacao}` : '',
      item.isbn ? `ISBN identificado: ${item.isbn}` : '',
      item.fontePesquisa ? `Fonte da identificação: ${item.fontePesquisa}` : '',
      item.identificacaoVerificada != null ? `Identificação verificada: ${item.identificacaoVerificada ? 'sim' : 'não'}` : '',
      item.estadoConservacao ? `Estado sugerido: ${item.estadoConservacao}` : '',
      item.categoriaSugerida ? `Categoria sugerida pela IA: ${item.categoriaSugerida}` : '',
    ]);
  }

  private observacoesInstancia(instancia: InstanciaRevisao): string | null {
    return this.joinObservacoes([
      instancia.observacoes,
      instancia.estadoConservacao ? `Estado sugerido: ${instancia.estadoConservacao}` : '',
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
