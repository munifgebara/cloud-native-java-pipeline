import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { PessoaResumo, PessoaService } from '../../../core/pessoa/pessoa';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-pessoa-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputTextModule, ConfirmDialogModule, RouterLink, TranslatePipe],
  providers: [ConfirmationService],
  templateUrl: './pessoa-list.html',
})
export class PessoaListComponent implements OnInit {
  private readonly pessoaService = inject(PessoaService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly i18n = inject(I18nService);

  pessoas = signal<PessoaResumo[]>([]);
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

    this.pessoaService.listar().subscribe({
      next: (dados) => {
        this.pessoas.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('people.loadError'));
        this.loading.set(false);
      },
    });
  }

  pesquisar(): void {
    const name = this.filtroNome.trim();

    if (!name) {
      this.carregar();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.pessoaService.buscarPorNome(name).subscribe({
      next: (dados) => {
        this.pessoas.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('people.searchError'));
        this.loading.set(false);
      },
    });
  }

  novo(): void {
    this.router.navigate(['/pessoas/nova']);
  }

  confirmarExclusao(pessoa: PessoaResumo): void {
    this.errorMessage.set('');

    this.confirmationService.confirm({
      header: this.i18n.translate('people.deleteConfirmTitle'),
      message: this.i18n.translate('people.deleteConfirmMessage', { name: pessoa.name }),
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: this.i18n.translate('common.cancel'),
      acceptLabel: this.i18n.translate('people.delete'),
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.excluir(pessoa),
    });
  }

  private excluir(pessoa: PessoaResumo): void {
    this.deletingId.set(pessoa.id);

    this.pessoaService.excluir(pessoa.id).subscribe({
      next: () => {
        this.pessoas.update((pessoas) => pessoas.filter((item) => item.id !== pessoa.id));
        this.deletingId.set(null);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('people.deleteError'));
        this.deletingId.set(null);
      },
    });
  }
}
