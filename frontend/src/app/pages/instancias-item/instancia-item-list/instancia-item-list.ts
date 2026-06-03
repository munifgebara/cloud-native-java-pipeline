import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { categoriaIconClass } from '../../../core/categoria/categoria';
import { InstanciaItemResumo, InstanciaItemService } from '../../../core/instancia-item/instancia-item';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-instancia-item-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputTextModule, TagModule, ConfirmDialogModule, RouterLink, TranslatePipe],
  providers: [ConfirmationService],
  templateUrl: './instancia-item-list.html',
  styleUrl: './instancia-item-list.css',
})
export class InstanciaItemListComponent implements OnInit {
  private readonly instanciaItemService = inject(InstanciaItemService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly i18n = inject(I18nService);

  instancias = signal<InstanciaItemResumo[]>([]);
  loading = signal(false);
  deletingId = signal<string | null>(null);
  errorMessage = signal('');
  filtroIdentificador = '';

  ngOnInit(): void {
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
    const identificador = this.filtroIdentificador.trim();

    if (!identificador) {
      this.carregar();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.instanciaItemService.buscarPorIdentificador(identificador).subscribe({
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

  statusLabel(instancia: InstanciaItemResumo): string {
    return instancia.ativa ? this.i18n.translate('itemInstances.active') : this.i18n.translate('itemInstances.inactive');
  }

  iconClass(instancia: InstanciaItemResumo): string {
    return categoriaIconClass(instancia.categoriaIcone);
  }

  nomeInstancia(instancia: InstanciaItemResumo): string {
    return instancia.identificador || instancia.patrimonio || instancia.numeroSerie || instancia.itemMestreNome;
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
}
