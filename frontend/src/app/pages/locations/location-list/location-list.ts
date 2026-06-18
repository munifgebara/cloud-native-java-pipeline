import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { LocationSummary, LocationService } from '../../../core/location/location';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-location-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputTextModule, TagModule, ConfirmDialogModule, RouterLink, TranslatePipe],
  providers: [ConfirmationService],
  templateUrl: './location-list.html',
  styleUrl: './location-list.css',
})
export class LocationListComponent implements OnInit {
  private readonly localService = inject(LocationService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly i18n = inject(I18nService);

  locations = signal<LocationSummary[]>([]);
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

    this.localService.listar().subscribe({
      next: (dados) => {
        this.locations.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('locations.loadError'));
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

    this.localService.buscarPorNome(name).subscribe({
      next: (dados) => {
        this.locations.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('locations.searchError'));
        this.loading.set(false);
      },
    });
  }

  create(): void {
    this.router.navigate(['/locations/create']);
  }

  confirmDelete(local: LocationSummary): void {
    this.errorMessage.set('');

    this.confirmationService.confirm({
      header: this.i18n.translate('locations.deleteConfirmTitle'),
      message: this.i18n.translate('locations.deleteConfirmMessage', { name: local.name }),
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: this.i18n.translate('common.cancel'),
      acceptLabel: this.i18n.translate('locations.delete'),
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.delete(local),
    });
  }

  statusLabel(local: LocationSummary): string {
    return local.active ? this.i18n.translate('locations.active') : this.i18n.translate('locations.inactive');
  }

  indent(local: LocationSummary): string {
    return `${local.level * 1.25}rem`;
  }

  private delete(local: LocationSummary): void {
    this.deletingId.set(local.id);

    this.localService.delete(local.id).subscribe({
      next: () => {
        this.locations.update((locations) => locations.filter((item) => item.id !== local.id));
        this.deletingId.set(null);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('locations.deleteError'));
        this.deletingId.set(null);
      },
    });
  }
}
