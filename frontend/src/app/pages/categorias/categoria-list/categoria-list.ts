import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CategorySummary, CategoryService, categoriaIconClass } from '../../../core/categoria/categoria';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-categoria-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputTextModule, TagModule, ConfirmDialogModule, RouterLink, TranslatePipe],
  providers: [ConfirmationService],
  templateUrl: './categoria-list.html',
})
export class CategoryListComponent implements OnInit {
  private readonly categoriaService = inject(CategoryService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly i18n = inject(I18nService);

  categorias = signal<CategorySummary[]>([]);
  loading = signal(false);
  deletingId = signal<string | null>(null);
  errorMessage = signal('');
  nameFilter = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.categoriaService.listar().subscribe({
      next: (dados) => {
        this.categorias.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('categories.loadError'));
        this.loading.set(false);
      },
    });
  }

  search(): void {
    const name = this.nameFilter.trim();

    if (!name) {
      this.load();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.categoriaService.buscarPorNome(name).subscribe({
      next: (dados) => {
        this.categorias.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('categories.searchError'));
        this.loading.set(false);
      },
    });
  }

  create(): void {
    this.router.navigate(['/categorias/nova']);
  }

  confirmDelete(categoria: CategorySummary): void {
    this.errorMessage.set('');

    this.confirmationService.confirm({
      header: this.i18n.translate('categories.deleteConfirmTitle'),
      message: this.i18n.translate('categories.deleteConfirmMessage', { name: categoria.name }),
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: this.i18n.translate('common.cancel'),
      acceptLabel: this.i18n.translate('categories.delete'),
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.delete(categoria),
    });
  }

  statusLabel(categoria: CategorySummary): string {
    return categoria.active ? this.i18n.translate('categories.active') : this.i18n.translate('categories.inactive');
  }

  iconClass(categoria: CategorySummary): string {
    return categoriaIconClass(categoria.icone);
  }

  private delete(categoria: CategorySummary): void {
    this.deletingId.set(categoria.id);

    this.categoriaService.delete(categoria.id).subscribe({
      next: () => {
        this.categorias.update((categorias) => categorias.filter((item) => item.id !== categoria.id));
        this.deletingId.set(null);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('categories.deleteError'));
        this.deletingId.set(null);
      },
    });
  }
}
