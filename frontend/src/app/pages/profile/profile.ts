import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MeuPerfil, UserService } from '../../core/user/user';
import { mensagemErroHttp } from '../../core/http-error';
import { I18nService, TranslatePipe } from '../../core/i18n/i18n';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonModule, CardModule, InputTextModule, TranslatePipe],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class PerfilComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly i18n = inject(I18nService);

  profile = signal<MeuPerfil | null>(null);
  loading = signal(false);
  salvandoPerfil = signal(false);
  salvandoSenha = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  profileForm = this.fb.group({
    firstName: ['', [Validators.maxLength(100)]],
    lastName: ['', [Validators.maxLength(100)]],
    email: ['', [Validators.email, Validators.maxLength(150)]],
  });

  senhaForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.userService.meuPerfil().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.profileForm.patchValue({
          firstName: profile.firstName ?? '',
          lastName: profile.lastName ?? '',
          email: profile.email ?? '',
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(mensagemErroHttp(err, this.i18n.translate('profile.loadError')));
        this.loading.set(false);
      },
    });
  }

  salvarPerfil(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    this.profileForm.markAllAsTouched();

    if (this.profileForm.invalid) {
      this.errorMessage.set(this.i18n.translate('users.form.invalidFields'));
      return;
    }

    this.salvandoPerfil.set(true);
    const valor = this.profileForm.getRawValue();

    this.userService.atualizarMeuPerfil({
      firstName: this.nullIfBlank(valor.firstName),
      lastName: this.nullIfBlank(valor.lastName),
      email: this.nullIfBlank(valor.email),
    }).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.successMessage.set(this.i18n.translate('profile.saved'));
        this.salvandoPerfil.set(false);
      },
      error: (err) => {
        this.errorMessage.set(mensagemErroHttp(err, this.i18n.translate('profile.saveError')));
        this.salvandoPerfil.set(false);
      },
    });
  }

  alterarSenha(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    this.senhaForm.markAllAsTouched();

    if (this.senhaForm.invalid) {
      this.errorMessage.set(this.i18n.translate('profile.passwordInvalid'));
      return;
    }

    this.salvandoSenha.set(true);
    const valor = this.senhaForm.getRawValue();

    this.userService.alterarMinhaSenha({
      currentPassword: valor.currentPassword ?? '',
      newPassword: valor.newPassword ?? '',
    }).subscribe({
      next: () => {
        this.senhaForm.reset();
        this.successMessage.set(this.i18n.translate('profile.passwordSaved'));
        this.salvandoSenha.set(false);
      },
      error: (err) => {
        this.errorMessage.set(mensagemErroHttp(err, this.i18n.translate('profile.passwordError')));
        this.salvandoSenha.set(false);
      },
    });
  }

  campoPerfilInvalido(name: keyof typeof this.profileForm.controls): boolean {
    const campo = this.profileForm.controls[name];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  campoSenhaInvalido(name: keyof typeof this.senhaForm.controls): boolean {
    const campo = this.senhaForm.controls[name];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  private nullIfBlank(value: string | null | undefined): string | null {
    const v = (value ?? '').trim();
    return v ? v : null;
  }
}
