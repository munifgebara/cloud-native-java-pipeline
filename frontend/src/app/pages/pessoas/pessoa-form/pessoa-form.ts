import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { PessoaService } from '../../../core/pessoa/pessoa';
import {
  somenteDigitos,
  validarCep,
  validarCpfCnpj,
  validarTelefone,
} from '../../../core/pessoa/pessoa-form.validators';

function cpfCnpjValidator(control: AbstractControl): ValidationErrors | null {
  const valor = control.value;

  if (!valor || !String(valor).trim()) {
    return { required: true };
  }

  return validarCpfCnpj(valor) ? null : { cpfCnpjInvalido: true };
}

function telefoneValidator(control: AbstractControl): ValidationErrors | null {
  const valor = control.value;
  if (!valor || !String(valor).trim()) return null;
  return validarTelefone(valor) ? null : { telefoneInvalido: true };
}

function cepValidator(control: AbstractControl): ValidationErrors | null {
  const valor = control.value;
  if (!valor || !String(valor).trim()) return null;
  return validarCep(valor) ? null : { cepInvalido: true };
}

@Component({
  selector: 'app-pessoa-form',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, ButtonModule, CardModule, RouterLink],
  templateUrl: './pessoa-form.html',
  styleUrl: './pessoa-form.css',
})
export class PessoaFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly pessoaService = inject(PessoaService);

  id = signal<string | null>(null);
  loading = signal(false);
  salvando = signal(false);
  errorMessage = signal('');

  readonly edicao = computed(() => !!this.id());

  form = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    cpfCnpj: ['', [cpfCnpjValidator]],
    telefonePrincipal: ['', [Validators.maxLength(20), telefoneValidator]],
    telefoneSecundario: ['', [Validators.maxLength(20), telefoneValidator]],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    cep: ['', [cepValidator]],
    endereco: ['', [Validators.maxLength(200)]],
    complemento: ['', [Validators.maxLength(100)]],
    bairro: ['', [Validators.maxLength(100)]],
    cidade: ['', [Validators.maxLength(100)]],
    uf: ['', [Validators.maxLength(2), Validators.pattern(/^[A-Za-z]{2}$/)]],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      return;
    }

    this.id.set(id);
    this.loading.set(true);

    this.pessoaService.buscarPorId(id).subscribe({
      next: (pessoa) => {
        this.form.patchValue({
          nome: pessoa.nome ?? '',
          cpfCnpj: pessoa.cpfCnpj ?? '',
          telefonePrincipal: pessoa.telefonePrincipal ?? '',
          telefoneSecundario: pessoa.telefoneSecundario ?? '',
          email: pessoa.email ?? '',
          cep: pessoa.cep ?? '',
          endereco: pessoa.endereco ?? '',
          complemento: pessoa.complemento ?? '',
          bairro: pessoa.bairro ?? '',
          cidade: pessoa.cidade ?? '',
          uf: pessoa.uf ?? '',
        });

        this.form.controls.cpfCnpj.disable();
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Não foi possível carregar a pessoa.');
        this.loading.set(false);
      },
    });
  }

  salvar(): void {
    this.errorMessage.set('');
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.errorMessage.set('Corrija os campos inválidos antes de salvar.');
      return;
    }

    this.salvando.set(true);

    const valor = this.form.getRawValue();

    if (this.edicao()) {
      const payload = {
        nome: valor.nome?.trim() ?? '',
        telefonePrincipal: this.nullIfBlank(valor.telefonePrincipal),
        telefoneSecundario: this.nullIfBlank(valor.telefoneSecundario),
        email: this.nullIfBlank(valor.email),
        cep: this.onlyDigitsOrNull(valor.cep),
        endereco: this.nullIfBlank(valor.endereco),
        complemento: this.nullIfBlank(valor.complemento),
        bairro: this.nullIfBlank(valor.bairro),
        cidade: this.nullIfBlank(valor.cidade),
        uf: this.upperOrNull(valor.uf),
      };

      this.pessoaService.atualizar(this.id()!, payload).subscribe({
        next: () => {
          this.salvando.set(false);
          this.router.navigate(['/pessoas']);
        },
        error: (err) => {
          this.salvando.set(false);
          this.errorMessage.set(this.extractError(err, 'Não foi possível atualizar a pessoa.'));
        },
      });

      return;
    }

    const payload = {
      nome: valor.nome?.trim() ?? '',
      cpfCnpj: this.onlyDigitsOrNull(valor.cpfCnpj) ?? '',
      telefonePrincipal: this.nullIfBlank(valor.telefonePrincipal),
      telefoneSecundario: this.nullIfBlank(valor.telefoneSecundario),
      email: this.nullIfBlank(valor.email),
      cep: this.onlyDigitsOrNull(valor.cep),
      endereco: this.nullIfBlank(valor.endereco),
      complemento: this.nullIfBlank(valor.complemento),
      bairro: this.nullIfBlank(valor.bairro),
      cidade: this.nullIfBlank(valor.cidade),
      uf: this.upperOrNull(valor.uf),
    };

    this.pessoaService.criar(payload).subscribe({
      next: () => {
        this.salvando.set(false);
        this.router.navigate(['/pessoas']);
      },
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, 'Não foi possível criar a pessoa.'));
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

  private onlyDigitsOrNull(value: string | null | undefined): string | null {
    const v = somenteDigitos(value);
    return v ? v : null;
  }

  private upperOrNull(value: string | null | undefined): string | null {
    const v = (value ?? '').trim();
    return v ? v.toUpperCase() : null;
  }

  private extractError(err: any, fallback: string): string {
    return err?.error?.message || fallback;
  }
}

