import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CategoriaResumo, CategoriaService, categoriaIconClass } from '../../../core/categoria/categoria';
import { ConsultaSemanticaItem, ItemMestreResumo, ItemMestreService } from '../../../core/item-mestre/item-mestre';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-item-mestre-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputTextModule, TagModule, ConfirmDialogModule, RouterLink, TranslatePipe],
  providers: [ConfirmationService],
  templateUrl: './item-mestre-list.html',
  styleUrl: './item-mestre-list.css',
})
export class ItemMestreListComponent implements OnInit {
  private readonly itemMestreService = inject(ItemMestreService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly i18n = inject(I18nService);

  itens = signal<ItemMestreResumo[]>([]);
  resultadosSemanticos = signal<ConsultaSemanticaItem[]>([]);
  categorias = signal<CategoriaResumo[]>([]);
  loading = signal(false);
  loadingSemantica = signal(false);
  deletingId = signal<string | null>(null);
  errorMessage = signal('');
  semanticErrorMessage = signal('');
  filtroNome = '';
  filtroCategoriaId = '';
  consultaSemantica = '';

  ngOnInit(): void {
    this.carregarCategorias();
    this.carregar();
  }

  carregar(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.itemMestreService.listar().subscribe({
      next: (dados) => {
        this.itens.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('masterItems.loadError'));
        this.loading.set(false);
      },
    });
  }

  pesquisar(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.itemMestreService.filtrar(this.filtroNome, this.filtroCategoriaId).subscribe({
      next: (dados) => {
        this.itens.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('masterItems.searchError'));
        this.loading.set(false);
      },
    });
  }

  pesquisarSemanticamente(): void {
    const consulta = this.consultaSemantica.trim();
    if (!consulta) {
      this.resultadosSemanticos.set([]);
      return;
    }

    this.loadingSemantica.set(true);
    this.semanticErrorMessage.set('');

    this.itemMestreService.buscarSemanticamente(consulta).subscribe({
      next: (dados) => {
        this.resultadosSemanticos.set(dados);
        this.loadingSemantica.set(false);
      },
      error: () => {
        this.semanticErrorMessage.set(this.i18n.translate('masterItems.semanticSearchError'));
        this.loadingSemantica.set(false);
      },
    });
  }

  limparBuscaSemantica(): void {
    this.consultaSemantica = '';
    this.resultadosSemanticos.set([]);
    this.semanticErrorMessage.set('');
  }

  limparFiltros(): void {
    this.filtroNome = '';
    this.filtroCategoriaId = '';
    this.carregar();
  }

  novo(): void {
    this.router.navigate(['/itens-mestre/novo']);
  }

  confirmarExclusao(item: ItemMestreResumo): void {
    this.errorMessage.set('');

    this.confirmationService.confirm({
      header: this.i18n.translate('masterItems.deleteConfirmTitle'),
      message: this.i18n.translate('masterItems.deleteConfirmMessage', { name: item.nome }),
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: this.i18n.translate('common.cancel'),
      acceptLabel: this.i18n.translate('masterItems.delete'),
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.excluir(item),
    });
  }

  statusLabel(item: ItemMestreResumo): string {
    return item.ativa ? this.i18n.translate('masterItems.active') : this.i18n.translate('masterItems.inactive');
  }

  iconClass(item: ItemMestreResumo): string {
    return categoriaIconClass(item.categoriaIcone);
  }

  semanticIconClass(item: ConsultaSemanticaItem): string {
    return categoriaIconClass(item.categoriaIcone);
  }

  relevancia(item: ConsultaSemanticaItem): string {
    return `${Math.round(item.similaridade * 100)}%`;
  }

  instanciasResumo(item: ConsultaSemanticaItem): string {
    if (!item.instancias.length) {
      return this.i18n.translate('masterItems.semanticNoInstances');
    }

    return item.instancias
      .slice(0, 3)
      .map((instancia) => instancia.identificador || instancia.patrimonio || instancia.numeroSerie || this.i18n.translate('masterItems.semanticUnnamedInstance'))
      .join(', ');
  }

  locaisResumo(item: ConsultaSemanticaItem): string {
    if (!item.locaisProvaveis.length) {
      return this.i18n.translate('masterItems.semanticNoLocations');
    }

    return item.locaisProvaveis
      .map((local) => `${local.nome} (${local.quantidade})`)
      .join(', ');
  }

  private carregarCategorias(): void {
    this.categoriaService.listar().subscribe({
      next: (categorias) => this.categorias.set(categorias),
      error: () => this.errorMessage.set(this.i18n.translate('masterItems.categoryLoadError')),
    });
  }

  private excluir(item: ItemMestreResumo): void {
    this.deletingId.set(item.id);

    this.itemMestreService.excluir(item.id).subscribe({
      next: () => {
        this.itens.update((itens) => itens.filter((atual) => atual.id !== item.id));
        this.deletingId.set(null);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('masterItems.deleteError'));
        this.deletingId.set(null);
      },
    });
  }
}
