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
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-categoria-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputTextModule, TagModule, ConfirmDialogModule, RouterLink, TranslatePipe],
  providers: [ConfirmationService],
  templateUrl: './categoria-list.html',
})
export class CategoriaListComponent implements OnInit {
  private readonly categoriaService = inject(CategoriaService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly i18n = inject(I18nService);

  categorias = signal<CategoriaResumo[]>([]);
  loading = signal(false);
  deletingId = signal<string | null>(null);
  errorMessage = signal('');
  filtroNome = '';

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
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

  pesquisar(): void {
    const nome = this.filtroNome.trim();

    if (!nome) {
      this.carregar();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.categoriaService.buscarPorNome(nome).subscribe({
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

  novo(): void {
    this.router.navigate(['/categorias/nova']);
  }

  confirmarExclusao(categoria: CategoriaResumo): void {
    this.errorMessage.set('');

    this.confirmationService.confirm({
      header: this.i18n.translate('categories.deleteConfirmTitle'),
      message: this.i18n.translate('categories.deleteConfirmMessage', { name: categoria.nome }),
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: this.i18n.translate('common.cancel'),
      acceptLabel: this.i18n.translate('categories.delete'),
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.excluir(categoria),
    });
  }

  statusLabel(categoria: CategoriaResumo): string {
    return categoria.ativa ? this.i18n.translate('categories.active') : this.i18n.translate('categories.inactive');
  }

  iconClass(categoria: CategoriaResumo): string {
    return categoriaIconClass(categoria.icone);
  }

  private excluir(categoria: CategoriaResumo): void {
    this.deletingId.set(categoria.id);

    this.categoriaService.excluir(categoria.id).subscribe({
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
