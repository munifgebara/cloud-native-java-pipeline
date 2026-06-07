import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { Usuario, UsuarioService } from '../../../core/usuario/usuario';
import { mensagemErroHttp } from '../../../core/http-error';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-usuario-list',
  standalone: true,
  imports: [ButtonModule, TableModule, TagModule, RouterLink, TranslatePipe],
  templateUrl: './usuario-list.html',
  styleUrl: './usuario-list.css',
})
export class UsuarioListComponent implements OnInit {
  private readonly usuarioService = inject(UsuarioService);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);

  usuarios = signal<Usuario[]>([]);
  loading = signal(false);
  updatingId = signal<string | null>(null);
  errorMessage = signal('');

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.usuarioService.listar().subscribe({
      next: (usuarios) => {
        this.usuarios.set(usuarios);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(mensagemErroHttp(err, this.i18n.translate('users.loadError')));
        this.loading.set(false);
      },
    });
  }

  novo(): void {
    this.router.navigate(['/usuarios/novo']);
  }

  alterarStatus(usuario: Usuario): void {
    this.updatingId.set(usuario.id);
    this.errorMessage.set('');

    this.usuarioService.alterarStatus(usuario.id, !usuario.enabled).subscribe({
      next: () => {
        this.usuarios.update((usuarios) =>
          usuarios.map((item) => item.id === usuario.id ? { ...item, enabled: !usuario.enabled } : item)
        );
        this.updatingId.set(null);
      },
      error: (err) => {
        this.errorMessage.set(mensagemErroHttp(err, this.i18n.translate('users.statusError')));
        this.updatingId.set(null);
      },
    });
  }

  nomeCompleto(usuario: Usuario): string {
    return [usuario.firstName, usuario.lastName].filter(Boolean).join(' ') || '-';
  }

  statusLabel(usuario: Usuario): string {
    return usuario.enabled ? this.i18n.translate('users.active') : this.i18n.translate('users.inactive');
  }
}
