import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { PessoaResumo, PessoaService } from '../../../core/pessoa/pessoa';

@Component({
  selector: 'app-pessoa-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputTextModule, ConfirmDialogModule, RouterLink],
  providers: [ConfirmationService],
  templateUrl: './pessoa-list.html',
  styleUrl: './pessoa-list.css',
})
export class PessoaListComponent implements OnInit {
  private readonly pessoaService = inject(PessoaService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);

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
        this.errorMessage.set('Não foi possível carregar as pessoas.');
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

    this.pessoaService.buscarPorNome(nome).subscribe({
      next: (dados) => {
        this.pessoas.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível pesquisar as pessoas.');
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
      header: 'Confirmar exclusão',
      message: `Deseja excluir ${pessoa.nome}?`,
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: 'Cancelar',
      acceptLabel: 'Excluir',
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
        this.errorMessage.set('Não foi possível excluir a pessoa.');
        this.deletingId.set(null);
      },
    });
  }
}
