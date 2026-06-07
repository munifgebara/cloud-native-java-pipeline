import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { catchError, debounceTime, distinctUntilChanged, filter, map, of, switchMap, tap } from 'rxjs';
import { CepService } from '../../../core/cep/cep';
import { I18nService, TranslatePipe, TranslationKey } from '../../../core/i18n/i18n';
import { PessoaResponse, PessoaRevisao, PessoaService } from '../../../core/pessoa/pessoa';
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
  imports: [ReactiveFormsModule, InputTextModule, ButtonModule, CardModule, RouterLink, TranslatePipe],
  templateUrl: './pessoa-form.html',
  styleUrl: './pessoa-form.css',
})
export class PessoaFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cepService = inject(CepService);
  private readonly i18n = inject(I18nService);
  private readonly pessoaService = inject(PessoaService);

  id = signal<string | null>(null);
  loading = signal(false);
  salvando = signal(false);
  errorMessage = signal('');
  cepMessage = signal('');
  buscandoCep = signal(false);
  pessoa = signal<PessoaResponse | null>(null);
  revisoes = signal<PessoaRevisao[]>([]);
  carregandoRevisoes = signal(false);
  revisoesError = signal('');
  private ultimoCepConsultado: string | null = null;

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
    this.observarCep();

    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      return;
    }

    this.id.set(id);
    this.loading.set(true);

    this.pessoaService.buscarPorId(id).subscribe({
      next: (pessoa) => {
        this.pessoa.set(pessoa);
        this.form.patchValue(
          {
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
          },
          { emitEvent: false },
        );

        this.ultimoCepConsultado = somenteDigitos(pessoa.cep);

        this.form.controls.cpfCnpj.disable();
        this.loading.set(false);
        this.carregarRevisoes(id);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('people.form.loadError'));
        this.loading.set(false);
      },
    });
  }

  salvar(): void {
    this.errorMessage.set('');
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.errorMessage.set(this.i18n.translate('people.form.invalidFields'));
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
          this.errorMessage.set(this.extractError(err, this.i18n.translate('people.form.updateError')));
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
        this.errorMessage.set(this.extractError(err, this.i18n.translate('people.form.createError')));
      },
    });
  }

  campoInvalido(nome: keyof typeof this.form.controls): boolean {
    const campo = this.form.controls[nome];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  formatarDataHora(value: string | null | undefined): string {
    if (!value) {
      return this.i18n.translate('people.audit.notAvailable');
    }

    return new Intl.DateTimeFormat(this.i18n.language(), {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(new Date(value));
  }

  rotuloTipoRevisao(tipo: PessoaRevisao['tipo']): string {
    return this.i18n.translate(`people.audit.type.${tipo}` as TranslationKey);
  }

  campoAlterado(revisao: PessoaRevisao, campo: string): boolean {
    return revisao.camposAlterados.includes(campo);
  }

  private observarCep(): void {
    this.form.controls.cep.valueChanges
      .pipe(
        debounceTime(400),
        distinctUntilChanged(),
        tap((valor) => {
          const cep = somenteDigitos(valor);

          if (cep !== this.ultimoCepConsultado) {
            this.cepMessage.set('');
          }

          if (!cep) {
            this.buscandoCep.set(false);
            this.ultimoCepConsultado = null;
          }
        }),
        filter((valor) => validarCep(valor)),
        map((valor) => somenteDigitos(valor)),
        filter((cep) => cep !== this.ultimoCepConsultado),
        tap(() => {
          this.buscandoCep.set(true);
          this.cepMessage.set('');
        }),
        switchMap((cep) =>
          this.cepService.buscarEndereco(cep).pipe(
            catchError(() => of('erro' as const)),
            map((resultado) => ({ cep, resultado })),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ cep, resultado }) => {
        this.buscandoCep.set(false);
        this.ultimoCepConsultado = cep;

        if (resultado === 'erro') {
          this.cepMessage.set(this.i18n.translate('people.form.cepLookupError'));
          return;
        }

        if (!resultado) {
          this.cepMessage.set(this.i18n.translate('people.form.cepNotFound'));
          return;
        }

        this.form.patchValue({
          endereco: resultado.endereco,
          bairro: resultado.bairro,
          cidade: resultado.cidade,
          uf: resultado.uf,
        });

        this.cepMessage.set(this.i18n.translate('people.form.cepFilled'));
      });
  }

  private carregarRevisoes(id: string): void {
    this.carregandoRevisoes.set(true);
    this.revisoesError.set('');

    this.pessoaService.listarRevisoes(id).subscribe({
      next: (revisoes) => {
        this.revisoes.set(revisoes);
        this.carregandoRevisoes.set(false);
      },
      error: () => {
        this.revisoes.set([]);
        this.revisoesError.set(this.i18n.translate('people.audit.loadError'));
        this.carregandoRevisoes.set(false);
      },
    });
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
