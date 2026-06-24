import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { AuthService, SocialProvider } from '../../core/auth';
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
  private readonly route = inject(ActivatedRoute);

  username = '';
  password = '';
  errorMessage = signal(this.initialMessage());
  loading = signal(false);
  socialLoading = signal<SocialProvider | null>(null);

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
        this.router.navigateByUrl(this.returnUrl());
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set(this.i18n.translate('login.invalid'));
      },
    });
  }

  entrarCom(provider: SocialProvider): void {
    this.errorMessage.set('');
    this.socialLoading.set(provider);
    this.authService.startSocialLogin(provider, this.returnUrl()).subscribe({
      error: () => {
        this.socialLoading.set(null);
        this.errorMessage.set(this.i18n.translate('login.socialStartError'));
      },
    });
  }

  private returnUrl(): string {
    return this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';
  }

  private initialMessage(): string {
    const reason = this.route.snapshot.queryParamMap.get('reason');
    if (reason === 'session-expired') {
      return this.i18n.translate('login.sessionExpired');
    }
    if (reason === 'social-login-failed') {
      return this.i18n.translate('login.socialCallbackError');
    }
    return '';
  }
}
