import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { User, UserService } from '../../../core/user/user';
import { mensagemErroHttp } from '../../../core/http-error';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [ButtonModule, TableModule, TagModule, RouterLink, TranslatePipe],
  templateUrl: './user-list.html',
})
export class UserListComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);

  users = signal<User[]>([]);
  loading = signal(false);
  updatingId = signal<string | null>(null);
  errorMessage = signal('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.userService.listar().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(mensagemErroHttp(err, this.i18n.translate('users.loadError')));
        this.loading.set(false);
      },
    });
  }

  create(): void {
    this.router.navigate(['/users/create']);
  }

  alterarStatus(user: User): void {
    this.updatingId.set(user.id);
    this.errorMessage.set('');

    this.userService.alterarStatus(user.id, !user.enabled).subscribe({
      next: () => {
        this.users.update((users) =>
          users.map((item) => item.id === user.id ? { ...item, enabled: !user.enabled } : item)
        );
        this.updatingId.set(null);
      },
      error: (err) => {
        this.errorMessage.set(mensagemErroHttp(err, this.i18n.translate('users.statusError')));
        this.updatingId.set(null);
      },
    });
  }

  nomeCompleto(user: User): string {
    return [user.firstName, user.lastName].filter(Boolean).join(' ') || '-';
  }

  statusLabel(user: User): string {
    return user.enabled ? this.i18n.translate('users.active') : this.i18n.translate('users.inactive');
  }
}
