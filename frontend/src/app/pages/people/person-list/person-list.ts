import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { PersonSummary, PersonService } from '../../../core/person/person';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-person-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputTextModule, ConfirmDialogModule, RouterLink, TranslatePipe],
  providers: [ConfirmationService],
  templateUrl: './person-list.html',
})
export class PersonListComponent implements OnInit {
  private readonly personService = inject(PersonService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly i18n = inject(I18nService);

  people = signal<PersonSummary[]>([]);
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

    this.personService.listar().subscribe({
      next: (dados) => {
        this.people.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('people.loadError'));
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

    this.personService.buscarPorNome(name).subscribe({
      next: (dados) => {
        this.people.set(dados);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('people.searchError'));
        this.loading.set(false);
      },
    });
  }

  create(): void {
    this.router.navigate(['/people/nova']);
  }

  confirmDelete(person: PersonSummary): void {
    this.errorMessage.set('');

    this.confirmationService.confirm({
      header: this.i18n.translate('people.deleteConfirmTitle'),
      message: this.i18n.translate('people.deleteConfirmMessage', { name: person.name }),
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: this.i18n.translate('common.cancel'),
      acceptLabel: this.i18n.translate('people.delete'),
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.delete(person),
    });
  }

  private delete(person: PersonSummary): void {
    this.deletingId.set(person.id);

    this.personService.delete(person.id).subscribe({
      next: () => {
        this.people.update((people) => people.filter((item) => item.id !== person.id));
        this.deletingId.set(null);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('people.deleteError'));
        this.deletingId.set(null);
      },
    });
  }
}
