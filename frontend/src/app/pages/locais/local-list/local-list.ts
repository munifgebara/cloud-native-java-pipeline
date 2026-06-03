import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { LocalResumo, LocalService } from '../../../core/local/local';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-local-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputTextModule, TagModule, ConfirmDialogModule, RouterLink, TranslatePipe],
  providers: [ConfirmationService],
  templateUrl: './local-list.html',
  styleUrl: './local-list.css',
})
export class LocalListComponent implements OnInit {
  private readonly localService = inject(LocalService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly i18n = inject(I18nService);

  locais = signal<LocalResumo[]>([]);
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

    this.localService.listar().subscribe({
      next: (dados) => {
        this.locais.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('locations.loadError'));
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

    this.localService.buscarPorNome(nome).subscribe({
      next: (dados) => {
        this.locais.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('locations.searchError'));
        this.loading.set(false);
      },
    });
  }

  novo(): void {
    this.router.navigate(['/locais/novo']);
  }

  confirmarExclusao(local: LocalResumo): void {
    this.errorMessage.set('');

    this.confirmationService.confirm({
      header: this.i18n.translate('locations.deleteConfirmTitle'),
      message: this.i18n.translate('locations.deleteConfirmMessage', { name: local.nome }),
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: this.i18n.translate('common.cancel'),
      acceptLabel: this.i18n.translate('locations.delete'),
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.excluir(local),
    });
  }

  statusLabel(local: LocalResumo): string {
    return local.ativa ? this.i18n.translate('locations.active') : this.i18n.translate('locations.inactive');
  }

  indent(local: LocalResumo): string {
    return `${local.nivel * 1.25}rem`;
  }

  private excluir(local: LocalResumo): void {
    this.deletingId.set(local.id);

    this.localService.excluir(local.id).subscribe({
      next: () => {
        this.locais.update((locais) => locais.filter((item) => item.id !== local.id));
        this.deletingId.set(null);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('locations.deleteError'));
        this.deletingId.set(null);
      },
    });
  }
}
