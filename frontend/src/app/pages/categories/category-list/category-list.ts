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
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputTextModule, TagModule, ConfirmDialogModule, RouterLink, TranslatePipe],
  providers: [ConfirmationService],
  templateUrl: './category-list.html',
})
export class CategoryListComponent implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly i18n = inject(I18nService);

  categories = signal<CategorySummary[]>([]);
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

    this.categoryService.listar().subscribe({
      next: (dados) => {
        this.categories.set(dados);
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

    this.categoryService.buscarPorNome(name).subscribe({
      next: (dados) => {
        this.categories.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('categories.searchError'));
        this.loading.set(false);
      },
    });
  }

  create(): void {
    this.router.navigate(['/categories/nova']);
  }

  confirmDelete(category: CategorySummary): void {
    this.errorMessage.set('');

    this.confirmationService.confirm({
      header: this.i18n.translate('categories.deleteConfirmTitle'),
      message: this.i18n.translate('categories.deleteConfirmMessage', { name: category.name }),
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: this.i18n.translate('common.cancel'),
      acceptLabel: this.i18n.translate('categories.delete'),
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.delete(category),
    });
  }

  statusLabel(category: CategorySummary): string {
    return category.active ? this.i18n.translate('categories.active') : this.i18n.translate('categories.inactive');
  }

  iconClass(category: CategorySummary): string {
    return categoryIconClass(category.icon);
  }

  private delete(category: CategorySummary): void {
    this.deletingId.set(category.id);

    this.categoryService.delete(category.id).subscribe({
      next: () => {
        this.categories.update((categories) => categories.filter((item) => item.id !== category.id));
        this.deletingId.set(null);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('categories.deleteError'));
        this.deletingId.set(null);
      },
    });
  }
}
