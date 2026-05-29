import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { AuthService } from '../../core/auth';
import { I18nService, TranslatePipe } from '../../core/i18n/i18n';
import { LanguageSelectorComponent } from '../../shared/language-selector/language-selector';

@Component({
  selector: 'app-login',
  imports: [
    FormsModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    PasswordModule,
    TranslatePipe,
    LanguageSelectorComponent,
  ],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly i18n = inject(I18nService);
  private readonly router = inject(Router);

  username = '';
  password = '';
  errorMessage = signal('');
  loading = signal(false);

  entrar(): void {
    this.errorMessage.set('');

    if (!this.username.trim() || !this.password.trim()) {
      this.errorMessage.set(this.i18n.translate('login.required'));
      return;
    }

    this.loading.set(true);

    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set(this.i18n.translate('login.invalid'));
      },
    });
  }
}
