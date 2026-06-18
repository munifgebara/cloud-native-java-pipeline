import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CategorySummary, CategoryService, categoryIconClass } from '../../../core/category/category';
import { SemanticSearchItem, MainItemSummary, MainItemService } from '../../../core/main-item/main-item';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';
import { StellaAiPanelComponent } from '../../../shared/design-system/stella-ai-panel/stella-ai-panel';
import { StellaEmptyStateComponent } from '../../../shared/design-system/stella-empty-state/stella-empty-state';
import { StellaPageHeaderComponent } from '../../../shared/design-system/stella-page-header/stella-page-header';
import { StellaStateMessageComponent } from '../../../shared/design-system/stella-state-message/stella-state-message';

@Component({
  selector: 'app-main-item-list',
  standalone: true,
  imports: [
    FormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    TagModule,
    ConfirmDialogModule,
    RouterLink,
    TranslatePipe,
    StellaAiPanelComponent,
    StellaEmptyStateComponent,
    StellaPageHeaderComponent,
    StellaStateMessageComponent,
  ],
  providers: [ConfirmationService],
  templateUrl: './main-item-list.html',
  styleUrl: './main-item-list.css',
})
export class MainItemListComponent implements OnInit {
  private readonly itemMestreService = inject(MainItemService);
  private readonly categoryService = inject(CategoryService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly i18n = inject(I18nService);

  itens = signal<MainItemSummary[]>([]);
  resultadosSemanticos = signal<SemanticSearchItem[]>([]);
  categories = signal<CategorySummary[]>([]);
  loading = signal(false);
  loadingSemantica = signal(false);
  deletingId = signal<string | null>(null);
  errorMessage = signal('');
  semanticErrorMessage = signal('');
  nameFilter = '';
  filtroCategoryId = '';
  consultaSemantica = '';

  ngOnInit(): void {
    this.carregarCategories();
    this.load();
  }

  load(): void {
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

  search(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.itemMestreService.filtrar(this.nameFilter, this.filtroCategoryId).subscribe({
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
    this.nameFilter = '';
    this.filtroCategoryId = '';
    this.load();
  }

  create(): void {
    this.router.navigate(['/main-items/create']);
  }

  confirmDelete(item: MainItemSummary): void {
    this.errorMessage.set('');

    this.confirmationService.confirm({
      header: this.i18n.translate('masterItems.deleteConfirmTitle'),
      message: this.i18n.translate('masterItems.deleteConfirmMessage', { name: item.name }),
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: this.i18n.translate('common.cancel'),
      acceptLabel: this.i18n.translate('masterItems.delete'),
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.delete(item),
    });
  }

  statusLabel(item: MainItemSummary): string {
    return item.active ? this.i18n.translate('masterItems.active') : this.i18n.translate('masterItems.inactive');
  }

  iconClass(item: MainItemSummary): string {
    return categoryIconClass(item.categoryIcon);
  }

  semanticIconClass(item: SemanticSearchItem): string {
    return categoryIconClass(item.categoryIcon);
  }

  relevancia(item: SemanticSearchItem): string {
    return `${Math.round(item.similaridade * 100)}%`;
  }

  instanciasSummary(item: SemanticSearchItem): string {
    if (!item.instancias.length) {
      return this.i18n.translate('masterItems.semanticNoInstances');
    }

    return item.instancias
      .slice(0, 3)
      .map((instancia) => instancia.identifier || instancia.assetTag || instancia.serialNumber || this.i18n.translate('masterItems.semanticUnnamedInstance'))
      .join(', ');
  }

  locationsSummary(item: SemanticSearchItem): string {
    if (!item.probableLocations.length) {
      return this.i18n.translate('masterItems.semanticNoLocations');
    }

    return item.probableLocations
      .map((local) => `${local.name} (${local.quantidade})`)
      .join(', ');
  }

  private carregarCategories(): void {
    this.categoryService.listar().subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => this.errorMessage.set(this.i18n.translate('masterItems.categoryLoadError')),
    });
  }

  private delete(item: MainItemSummary): void {
    this.deletingId.set(item.id);

    this.itemMestreService.delete(item.id).subscribe({
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
