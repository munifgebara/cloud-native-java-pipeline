import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/auth';
import { TranslatePipe } from '../../core/i18n/i18n';
import { LanguageSelectorComponent } from '../../shared/language-selector/language-selector';

@Component({
  selector: 'app-app-topbar',
  imports: [ButtonModule, RouterLink, RouterLinkActive, TranslatePipe, LanguageSelectorComponent],
  templateUrl: './app-topbar.html',
  styleUrl: './app-topbar.css',
})
export class AppTopbarComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  sair(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  isAdmin(): boolean {
    return this.authService.hasRole('admin');
  }
}
