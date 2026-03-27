import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PessoaResumo, PessoaService } from '../../../core/pessoa/pessoa';

@Component({
  selector: 'app-pessoa-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputTextModule, RouterLink],
  templateUrl: './pessoa-list.html',
  styleUrl: './pessoa-list.css',
})
export class PessoaListComponent implements OnInit {
  private readonly pessoaService = inject(PessoaService);
  private readonly router = inject(Router);

  pessoas = signal<PessoaResumo[]>([]);
  loading = signal(false);
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
}
