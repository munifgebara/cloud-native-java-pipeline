import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/auth';

@Component({
  selector: 'app-app-topbar',
  imports: [ButtonModule],
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
}
