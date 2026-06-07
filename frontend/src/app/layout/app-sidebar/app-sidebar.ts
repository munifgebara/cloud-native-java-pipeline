import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth';
import { TranslatePipe } from '../../core/i18n/i18n';

@Component({
  selector: 'app-app-sidebar',
  imports: [RouterLink, RouterLinkActive, TranslatePipe],
  templateUrl: './app-sidebar.html',
  styleUrl: './app-sidebar.css',
})
export class AppSidebarComponent {
  private readonly authService = inject(AuthService);

  isAdmin(): boolean {
    return this.authService.hasRole('admin');
  }
}
