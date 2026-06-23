import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth';
import { TranslatePipe } from '../../core/i18n/i18n';

@Component({
  selector: 'app-auth-callback',
  imports: [TranslatePipe],
  templateUrl: './auth-callback.html',
  styleUrl: './auth-callback.css',
})
export class AuthCallbackComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  failed = signal(false);

  constructor() {
    const code = this.route.snapshot.queryParamMap.get('code');
    const state = this.route.snapshot.queryParamMap.get('state');

    if (!code || !state) {
      this.fail();
      return;
    }

    this.authService.completeSocialLogin(code, state).subscribe({
      next: (returnUrl) => this.router.navigateByUrl(returnUrl),
      error: () => this.fail(),
    });
  }

  private fail(): void {
    this.failed.set(true);
    this.router.navigate(['/login'], {
      queryParams: { reason: 'social-login-failed' },
    });
  }
}
