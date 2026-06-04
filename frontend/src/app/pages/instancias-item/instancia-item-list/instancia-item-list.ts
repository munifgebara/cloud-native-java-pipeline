import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CategoriaResumo, CategoriaService, categoriaIconClass } from '../../../core/categoria/categoria';
import { InstanciaItemResumo, InstanciaItemService, StatusOperacionalInstancia } from '../../../core/instancia-item/instancia-item';
import { LocalResumo, LocalService } from '../../../core/local/local';
import { PessoaResumo, PessoaService } from '../../../core/pessoa/pessoa';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-instancia-item-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputTextModule, TextareaModule, TagModule, DialogModule, ConfirmDialogModule, RouterLink, TranslatePipe],
  providers: [ConfirmationService],
  templateUrl: './instancia-item-list.html',
  styleUrl: './instancia-item-list.css',
})
export class InstanciaItemListComponent implements OnInit {
  private readonly instanciaItemService = inject(InstanciaItemService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly localService = inject(LocalService);
  private readonly pessoaService = inject(PessoaService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly i18n = inject(I18nService);

  instancias = signal<InstanciaItemResumo[]>([]);
  categorias = signal<CategoriaResumo[]>([]);
  locais = signal<LocalResumo[]>([]);
  pessoas = signal<PessoaResumo[]>([]);
  loading = signal(false);
  deletingId = signal<string | null>(null);
  saidaId = signal<string | null>(null);
  instanciaSaida = signal<InstanciaItemResumo | null>(null);
  transferenciaId = signal<string | null>(null);
  instanciaTransferencia = signal<InstanciaItemResumo | null>(null);
  emprestimoId = signal<string | null>(null);
  instanciaEmprestimo = signal<InstanciaItemResumo | null>(null);
  errorMessage = signal('');
  saidaMotivo = '';
  saidaObservacao = '';
  transferenciaLocalDestinoId = '';
  transferenciaObservacao = '';
  emprestimoPessoaId = '';
  emprestimoPrevisaoDevolucao = '';
  emprestimoObservacao = '';
  filtroIdentificacao = '';
  filtroItemMestre = '';
  filtroCategoriaId = '';
  filtroStatus: StatusOperacionalInstancia | '' = '';
  statusOptions: StatusOperacionalInstancia[] = ['DISPONIVEL', 'EM_MOVIMENTACAO', 'EMPRESTADO', 'INATIVO'];

  ngOnInit(): void {
    this.carregarCategorias();
    this.carregarLocais();
    this.carregarPessoas();
    this.carregar();
  }

  carregar(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.instanciaItemService.listar().subscribe({
      next: (dados) => {
        this.instancias.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('itemInstances.loadError'));
        this.loading.set(false);
      },
    });
  }

  pesquisar(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.instanciaItemService.filtrar(
      this.filtroIdentificacao,
      this.filtroItemMestre,
      this.filtroCategoriaId,
      this.filtroStatus || null
    ).subscribe({
      next: (dados) => {
        this.instancias.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('itemInstances.searchError'));
        this.loading.set(false);
      },
    });
  }

  limparFiltros(): void {
    this.filtroIdentificacao = '';
    this.filtroItemMestre = '';
    this.filtroCategoriaId = '';
    this.filtroStatus = '';
    this.carregar();
  }

  novo(): void {
    this.router.navigate(['/instancias-item/nova']);
  }

  confirmarExclusao(instancia: InstanciaItemResumo): void {
    this.errorMessage.set('');

    this.confirmationService.confirm({
      header: this.i18n.translate('itemInstances.deleteConfirmTitle'),
      message: this.i18n.translate('itemInstances.deleteConfirmMessage', { name: this.nomeInstancia(instancia) }),
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: this.i18n.translate('common.cancel'),
      acceptLabel: this.i18n.translate('itemInstances.delete'),
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.excluir(instancia),
    });
  }

  abrirSaida(instancia: InstanciaItemResumo): void {
    this.errorMessage.set('');
    this.instanciaSaida.set(instancia);
    this.saidaMotivo = '';
    this.saidaObservacao = '';
  }

  cancelarSaida(): void {
    if (this.saidaId()) {
      return;
    }
    this.instanciaSaida.set(null);
    this.saidaMotivo = '';
    this.saidaObservacao = '';
  }

  registrarSaida(): void {
    const instancia = this.instanciaSaida();
    const motivo = this.saidaMotivo.trim();

    if (!instancia || !motivo) {
      this.errorMessage.set(this.i18n.translate('itemInstances.exitReasonRequired'));
      return;
    }

    this.saidaId.set(instancia.id);
    this.instanciaItemService.registrarSaida({
      instanciaItemId: instancia.id,
      motivo,
      observacao: this.nullIfBlank(this.saidaObservacao),
    }).subscribe({
      next: () => {
        this.saidaId.set(null);
        this.cancelarSaida();
        this.pesquisar();
      },
      error: (err) => {
        this.errorMessage.set(this.extractError(err, this.i18n.translate('itemInstances.exitError')));
        this.saidaId.set(null);
      },
    });
  }

  abrirTransferencia(instancia: InstanciaItemResumo): void {
    this.errorMessage.set('');
    this.instanciaTransferencia.set(instancia);
    this.transferenciaLocalDestinoId = '';
    this.transferenciaObservacao = '';
  }

  cancelarTransferencia(): void {
    if (this.transferenciaId()) {
      return;
    }
    this.instanciaTransferencia.set(null);
    this.transferenciaLocalDestinoId = '';
    this.transferenciaObservacao = '';
  }

  registrarTransferencia(): void {
    const instancia = this.instanciaTransferencia();

    if (!instancia || !this.transferenciaLocalDestinoId) {
      this.errorMessage.set(this.i18n.translate('itemInstances.transferDestinationRequired'));
      return;
    }

    this.transferenciaId.set(instancia.id);
    this.instanciaItemService.registrarTransferencia({
      instanciaItemId: instancia.id,
      localDestinoId: this.transferenciaLocalDestinoId,
      observacao: this.nullIfBlank(this.transferenciaObservacao),
    }).subscribe({
      next: () => {
        this.transferenciaId.set(null);
        this.cancelarTransferencia();
        this.pesquisar();
      },
      error: (err) => {
        this.errorMessage.set(this.extractError(err, this.i18n.translate('itemInstances.transferError')));
        this.transferenciaId.set(null);
      },
    });
  }

  abrirEmprestimo(instancia: InstanciaItemResumo): void {
    this.errorMessage.set('');
    this.instanciaEmprestimo.set(instancia);
    this.emprestimoPessoaId = '';
    this.emprestimoPrevisaoDevolucao = '';
    this.emprestimoObservacao = '';
  }

  cancelarEmprestimo(): void {
    if (this.emprestimoId()) {
      return;
    }
    this.instanciaEmprestimo.set(null);
    this.emprestimoPessoaId = '';
    this.emprestimoPrevisaoDevolucao = '';
    this.emprestimoObservacao = '';
  }

  registrarEmprestimo(): void {
    const instancia = this.instanciaEmprestimo();

    if (!instancia || !this.emprestimoPessoaId) {
      this.errorMessage.set(this.i18n.translate('itemInstances.loanPersonRequired'));
      return;
    }

    this.emprestimoId.set(instancia.id);
    this.instanciaItemService.registrarEmprestimo({
      instanciaItemId: instancia.id,
      pessoaId: this.emprestimoPessoaId,
      previsaoDevolucao: this.nullIfBlank(this.emprestimoPrevisaoDevolucao),
      observacao: this.nullIfBlank(this.emprestimoObservacao),
    }).subscribe({
      next: () => {
        this.emprestimoId.set(null);
        this.cancelarEmprestimo();
        this.pesquisar();
      },
      error: (err) => {
        this.errorMessage.set(this.extractError(err, this.i18n.translate('itemInstances.loanError')));
        this.emprestimoId.set(null);
      },
    });
  }

  statusLabel(instancia: InstanciaItemResumo): string {
    return this.i18n.translate(`itemInstances.status.${instancia.statusOperacional}`);
  }

  statusSeverity(status: StatusOperacionalInstancia): 'success' | 'info' | 'warn' | 'secondary' {
    const severities: Record<StatusOperacionalInstancia, 'success' | 'info' | 'warn' | 'secondary'> = {
      DISPONIVEL: 'success',
      EM_MOVIMENTACAO: 'info',
      EMPRESTADO: 'warn',
      INATIVO: 'secondary',
    };
    return severities[status];
  }

  iconClass(instancia: InstanciaItemResumo): string {
    return categoriaIconClass(instancia.categoriaIcone);
  }

  nomeInstancia(instancia: InstanciaItemResumo): string {
    return instancia.identificador || instancia.patrimonio || instancia.numeroSerie || instancia.itemMestreNome;
  }

  podeRegistrarSaida(instancia: InstanciaItemResumo): boolean {
    return instancia.ativa && instancia.statusOperacional === 'DISPONIVEL' && !!instancia.localAtualId;
  }

  podeTransferir(instancia: InstanciaItemResumo): boolean {
    return instancia.ativa && instancia.statusOperacional === 'DISPONIVEL' && !!instancia.localAtualId;
  }

  podeEmprestar(instancia: InstanciaItemResumo): boolean {
    return instancia.ativa && instancia.statusOperacional === 'DISPONIVEL' && !!instancia.localAtualId;
  }

  locaisDestino(instancia: InstanciaItemResumo): LocalResumo[] {
    return this.locais().filter((local) => local.ativa && local.id !== instancia.localAtualId);
  }

  statusOptionLabel(status: StatusOperacionalInstancia): string {
    return this.i18n.translate(`itemInstances.status.${status}`);
  }

  private carregarCategorias(): void {
    this.categoriaService.listar().subscribe({
      next: (categorias) => this.categorias.set(categorias),
      error: () => this.errorMessage.set(this.i18n.translate('itemInstances.categoryLoadError')),
    });
  }

  private carregarLocais(): void {
    this.localService.listar().subscribe({
      next: (locais) => this.locais.set(locais),
      error: () => this.errorMessage.set(this.i18n.translate('itemInstances.locationLoadError')),
    });
  }

  private carregarPessoas(): void {
    this.pessoaService.listar().subscribe({
      next: (pessoas) => this.pessoas.set(pessoas),
      error: () => this.errorMessage.set(this.i18n.translate('itemInstances.personLoadError')),
    });
  }

  private excluir(instancia: InstanciaItemResumo): void {
    this.deletingId.set(instancia.id);

    this.instanciaItemService.excluir(instancia.id).subscribe({
      next: () => {
        this.instancias.update((instancias) => instancias.filter((item) => item.id !== instancia.id));
        this.deletingId.set(null);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('itemInstances.deleteError'));
        this.deletingId.set(null);
      },
    });
  }

  private nullIfBlank(value: string | null | undefined): string | null {
    const v = (value ?? '').trim();
    return v ? v : null;
  }

  private extractError(err: any, fallback: string): string {
    return err?.error?.erro || err?.error?.message || fallback;
  }
}
