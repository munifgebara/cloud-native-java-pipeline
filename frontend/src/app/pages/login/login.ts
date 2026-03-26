import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { AuthService } from '../../core/auth';

@Component({
  selector: 'app-login',
  imports: [
    FormsModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    PasswordModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  username = '';
  password = '';
  errorMessage = signal('');

  entrar(): void {
    this.errorMessage.set('');

    const ok = this.authService.login(this.username, this.password);

    if (!ok) {
      this.errorMessage.set('Informe usuário e senha.');
      return;
    }

    this.router.navigate(['/dashboard']);
  }
}
