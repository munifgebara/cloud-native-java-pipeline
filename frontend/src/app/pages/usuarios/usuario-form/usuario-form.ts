import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { UsuarioService } from '../../../core/usuario/usuario';
import { mensagemErroHttp } from '../../../core/http-error';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-usuario-form',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonModule, CardModule, CheckboxModule, InputTextModule, MultiSelectModule, RouterLink, TranslatePipe],
  templateUrl: './usuario-form.html',
  styleUrl: './usuario-form.css',
})
export class UsuarioFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly usuarioService = inject(UsuarioService);
  private readonly i18n = inject(I18nService);

  id = signal<string | null>(null);
  loading = signal(false);
  salvando = signal(false);
  errorMessage = signal('');
  readonly edicao = computed(() => !!this.id());
  readonly roleOptions = [
    { label: 'admin', value: 'admin' },
    { label: 'proprietario', value: 'proprietario' },
    { label: 'usuario', value: 'usuario' },
  ];

  form = this.fb.group({
    username: ['', [Validators.required, Validators.maxLength(100)]],
    firstName: ['', [Validators.maxLength(100)]],
    lastName: ['', [Validators.maxLength(100)]],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    password: ['', [Validators.minLength(6), Validators.maxLength(100)]],
    enabled: [true],
    roles: [['usuario'] as string[]],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      this.form.controls.password.addValidators(Validators.required);
      return;
    }

    this.id.set(id);
    this.form.controls.username.disable();
    this.loading.set(true);

    this.usuarioService.buscarPorId(id).subscribe({
      next: (usuario) => {
        this.form.patchValue({
          username: usuario.username,
          firstName: usuario.firstName ?? '',
          lastName: usuario.lastName ?? '',
          email: usuario.email ?? '',
          enabled: usuario.enabled,
          roles: usuario.roles,
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(mensagemErroHttp(err, this.i18n.translate('users.form.loadError')));
        this.loading.set(false);
      },
    });
  }

  salvar(): void {
    this.errorMessage.set('');
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.errorMessage.set(this.i18n.translate('users.form.invalidFields'));
      return;
    }

    this.salvando.set(true);
    const valor = this.form.getRawValue();
    const payload = {
      firstName: this.nullIfBlank(valor.firstName),
      lastName: this.nullIfBlank(valor.lastName),
      email: this.nullIfBlank(valor.email),
      enabled: !!valor.enabled,
      roles: valor.roles ?? [],
    };

    if (this.edicao()) {
      this.usuarioService.atualizar(this.id()!, payload).subscribe({
        next: () => {
          this.salvando.set(false);
          this.router.navigate(['/usuarios']);
        },
        error: (err) => {
          this.salvando.set(false);
          this.errorMessage.set(mensagemErroHttp(err, this.i18n.translate('users.form.updateError')));
        },
      });
      return;
    }

    this.usuarioService.criar({
      username: valor.username?.trim() ?? '',
      password: valor.password ?? '',
      ...payload,
    }).subscribe({
      next: () => {
        this.salvando.set(false);
        this.router.navigate(['/usuarios']);
      },
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(mensagemErroHttp(err, this.i18n.translate('users.form.createError')));
      },
    });
  }

  campoInvalido(nome: keyof typeof this.form.controls): boolean {
    const campo = this.form.controls[nome];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  private nullIfBlank(value: string | null | undefined): string | null {
    const v = (value ?? '').trim();
    return v ? v : null;
  }
}
