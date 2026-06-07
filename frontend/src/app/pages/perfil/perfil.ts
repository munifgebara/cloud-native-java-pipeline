import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MeuPerfil, UsuarioService } from '../../core/usuario/usuario';
import { mensagemErroHttp } from '../../core/http-error';
import { I18nService, TranslatePipe } from '../../core/i18n/i18n';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonModule, CardModule, InputTextModule, TranslatePipe],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css',
})
export class PerfilComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly usuarioService = inject(UsuarioService);
  private readonly i18n = inject(I18nService);

  perfil = signal<MeuPerfil | null>(null);
  loading = signal(false);
  salvandoPerfil = signal(false);
  salvandoSenha = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  perfilForm = this.fb.group({
    firstName: ['', [Validators.maxLength(100)]],
    lastName: ['', [Validators.maxLength(100)]],
    email: ['', [Validators.email, Validators.maxLength(150)]],
  });

  senhaForm = this.fb.group({
    senhaAtual: ['', [Validators.required]],
    novaSenha: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.usuarioService.meuPerfil().subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        this.perfilForm.patchValue({
          firstName: perfil.firstName ?? '',
          lastName: perfil.lastName ?? '',
          email: perfil.email ?? '',
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
    this.perfilForm.markAllAsTouched();

    if (this.perfilForm.invalid) {
      this.errorMessage.set(this.i18n.translate('users.form.invalidFields'));
      return;
    }

    this.salvandoPerfil.set(true);
    const valor = this.perfilForm.getRawValue();

    this.usuarioService.atualizarMeuPerfil({
      firstName: this.nullIfBlank(valor.firstName),
      lastName: this.nullIfBlank(valor.lastName),
      email: this.nullIfBlank(valor.email),
    }).subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
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

    this.usuarioService.alterarMinhaSenha({
      senhaAtual: valor.senhaAtual ?? '',
      novaSenha: valor.novaSenha ?? '',
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

  campoPerfilInvalido(nome: keyof typeof this.perfilForm.controls): boolean {
    const campo = this.perfilForm.controls[nome];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  campoSenhaInvalido(nome: keyof typeof this.senhaForm.controls): boolean {
    const campo = this.senhaForm.controls[nome];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  private nullIfBlank(value: string | null | undefined): string | null {
    const v = (value ?? '').trim();
    return v ? v : null;
  }
}
