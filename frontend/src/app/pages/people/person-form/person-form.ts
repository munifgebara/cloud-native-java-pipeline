import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { catchError, debounceTime, distinctUntilChanged, filter, map, of, switchMap, tap } from 'rxjs';
import { CepService } from '../../../core/cep/cep';
import { mensagemErroHttp } from '../../../core/http-error';
import { IMAGE_CONTENT_TYPES, imageFileFromPaste } from '../../../core/image/image-clipboard';
import { I18nService, TranslatePipe, TranslationKey } from '../../../core/i18n/i18n';
import { PersonResponse, PersonRevision, PersonService } from '../../../core/person/person';
import {
  somenteDigitos,
  validarCep,
  validarCpfCnpj,
  validarTelefone,
} from '../../../core/person/person-form.validators';

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

const MAX_PHOTO_SIZE_BYTES = 5 * 1024 * 1024;
const PERSON_PHOTO_CONTENT_TYPES = IMAGE_CONTENT_TYPES.filter((type) => type !== 'image/gif');

@Component({
  selector: 'app-person-form',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, ButtonModule, CardModule, RouterLink, TranslatePipe],
  templateUrl: './person-form.html',
  styleUrl: './person-form.css',
})
export class PersonFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cepService = inject(CepService);
  private readonly i18n = inject(I18nService);
  private readonly personService = inject(PersonService);

  id = signal<string | null>(null);
  loading = signal(false);
  salvando = signal(false);
  errorMessage = signal('');
  cepMessage = signal('');
  buscandoCep = signal(false);
  person = signal<PersonResponse | null>(null);
  revisions = signal<PersonRevision[]>([]);
  carregandoRevisoes = signal(false);
  revisionsError = signal('');
  fotoAtualUrl = signal<string | null>(null);
  fotoPreviewUrl = signal<string | null>(null);
  fotoSelecionada = signal<File | null>(null);
  removendoFoto = signal(false);
  private ultimoCepConsultado: string | null = null;

  readonly edicao = computed(() => !!this.id());

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    taxId: ['', [cpfCnpjValidator]],
    primaryPhone: ['', [Validators.maxLength(20), telefoneValidator]],
    secondaryPhone: ['', [Validators.maxLength(20), telefoneValidator]],
    email: ['', [Validators.email, Validators.maxLength(150)]],
    zipCode: ['', [cepValidator]],
    address: ['', [Validators.maxLength(200)]],
    complement: ['', [Validators.maxLength(100)]],
    neighborhood: ['', [Validators.maxLength(100)]],
    city: ['', [Validators.maxLength(100)]],
    state: ['', [Validators.maxLength(2), Validators.pattern(/^[A-Za-z]{2}$/)]],
  });

  ngOnInit(): void {
    this.observarCep();

    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      return;
    }

    this.id.set(id);
    this.loading.set(true);

    this.personService.buscarPorId(id).subscribe({
      next: (person) => {
        this.person.set(person);
        this.fotoAtualUrl.set(person.photoUrl);
        this.form.patchValue(
          {
            name: person.name ?? '',
            taxId: person.taxId ?? '',
            primaryPhone: person.primaryPhone ?? '',
            secondaryPhone: person.secondaryPhone ?? '',
            email: person.email ?? '',
            zipCode: person.zipCode ?? '',
            address: person.address ?? '',
            complement: person.complement ?? '',
            neighborhood: person.neighborhood ?? '',
            city: person.city ?? '',
            state: person.state ?? '',
          },
          { emitEvent: false },
        );

        this.ultimoCepConsultado = somenteDigitos(person.zipCode);

        this.form.controls.taxId.disable();
        this.loading.set(false);
        this.carregarRevisoes(id);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('people.form.loadError'));
        this.loading.set(false);
      },
    });
  }

  save(): void {
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
        name: valor.name?.trim() ?? '',
        primaryPhone: this.nullIfBlank(valor.primaryPhone),
        secondaryPhone: this.nullIfBlank(valor.secondaryPhone),
        email: this.nullIfBlank(valor.email),
        zipCode: this.onlyDigitsOrNull(valor.zipCode),
        address: this.nullIfBlank(valor.address),
        complement: this.nullIfBlank(valor.complement),
        neighborhood: this.nullIfBlank(valor.neighborhood),
        city: this.nullIfBlank(valor.city),
        state: this.upperOrNull(valor.state),
      };

      this.personService.update(this.id()!, payload).subscribe({
        next: (person) => this.enviarFotoSeNecessario(person.id),
        error: (err) => {
          this.salvando.set(false);
          this.errorMessage.set(this.extractError(err, this.i18n.translate('people.form.updateError')));
        },
      });

      return;
    }

    const payload = {
      name: valor.name?.trim() ?? '',
      taxId: this.onlyDigitsOrNull(valor.taxId) ?? '',
      primaryPhone: this.nullIfBlank(valor.primaryPhone),
      secondaryPhone: this.nullIfBlank(valor.secondaryPhone),
      email: this.nullIfBlank(valor.email),
      zipCode: this.onlyDigitsOrNull(valor.zipCode),
      address: this.nullIfBlank(valor.address),
      complement: this.nullIfBlank(valor.complement),
      neighborhood: this.nullIfBlank(valor.neighborhood),
      city: this.nullIfBlank(valor.city),
      state: this.upperOrNull(valor.state),
    };

    this.personService.criar(payload).subscribe({
      next: (person) => this.enviarFotoSeNecessario(person.id),
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('people.form.createError')));
      },
    });
  }

  selecionarFoto(event: Event): void {
    this.errorMessage.set('');
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0] ?? null;

    if (!arquivo) {
      this.fotoSelecionada.set(null);
      this.fotoPreviewUrl.set(null);
      return;
    }

    if (!this.aplicarFotoSelecionada(arquivo)) {
      input.value = '';
    }
  }

  colarFoto(event: ClipboardEvent): void {
    this.errorMessage.set('');
    const result = imageFileFromPaste(event, MAX_PHOTO_SIZE_BYTES);

    if (!result.ok) {
      if (result.reason === 'missing') {
        this.errorMessage.set(this.i18n.translate('people.form.photoPasteMissing'));
      } else if (result.reason === 'invalid-type') {
        this.errorMessage.set(this.i18n.translate('people.form.photoInvalidType'));
      } else {
        this.errorMessage.set(this.i18n.translate('people.form.photoTooLarge'));
      }
      return;
    }

    this.aplicarFotoSelecionada(result.file);
  }

  removerFoto(): void {
    const id = this.id();
    if (!id) {
      this.fotoSelecionada.set(null);
      this.limparPreviewFoto();
      return;
    }

    if (!this.fotoAtualUrl() && !this.fotoSelecionada()) {
      return;
    }

    if (this.fotoSelecionada() && !this.fotoAtualUrl()) {
      this.fotoSelecionada.set(null);
      this.limparPreviewFoto();
      return;
    }

    this.errorMessage.set('');
    this.removendoFoto.set(true);

    this.personService.removerFoto(id).subscribe({
      next: (person) => {
        this.person.set(person);
        this.fotoAtualUrl.set(null);
        this.fotoSelecionada.set(null);
        this.limparPreviewFoto();
        this.removendoFoto.set(false);
      },
      error: (err) => {
        this.removendoFoto.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('people.form.photoRemoveError')));
      },
    });
  }

  campoInvalido(name: keyof typeof this.form.controls): boolean {
    const campo = this.form.controls[name];
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

  rotuloTipoRevisao(type: PersonRevision['type']): string {
    return this.i18n.translate(`people.audit.type.${type}` as TranslationKey);
  }

  campoAlterado(revision: PersonRevision, campo: string): boolean {
    return revision.changedFields.includes(campo);
  }

  private observarCep(): void {
    this.form.controls.zipCode.valueChanges
      .pipe(
        debounceTime(400),
        distinctUntilChanged(),
        tap((valor) => {
          const zipCode = somenteDigitos(valor);

          if (zipCode !== this.ultimoCepConsultado) {
            this.cepMessage.set('');
          }

          if (!zipCode) {
            this.buscandoCep.set(false);
            this.ultimoCepConsultado = null;
          }
        }),
        filter((valor) => validarCep(valor)),
        map((valor) => somenteDigitos(valor)),
        filter((zipCode) => zipCode !== this.ultimoCepConsultado),
        tap(() => {
          this.buscandoCep.set(true);
          this.cepMessage.set('');
        }),
        switchMap((zipCode) =>
          this.cepService.buscarEndereco(zipCode).pipe(
            catchError(() => of('error' as const)),
            map((resultado) => ({ zipCode, resultado })),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ zipCode, resultado }) => {
        this.buscandoCep.set(false);
        this.ultimoCepConsultado = zipCode;

        if (resultado === 'error') {
          this.cepMessage.set(this.i18n.translate('people.form.cepLookupError'));
          return;
        }

        if (!resultado) {
          this.cepMessage.set(this.i18n.translate('people.form.cepNotFound'));
          return;
        }

        this.patchIfUserDidNotChange('address', resultado.address);
        this.patchIfUserDidNotChange('neighborhood', resultado.neighborhood);
        this.patchIfUserDidNotChange('city', resultado.city);
        this.patchIfUserDidNotChange('state', resultado.state);

        this.cepMessage.set(this.i18n.translate('people.form.cepFilled'));
      });
  }

  private patchIfUserDidNotChange(name: 'address' | 'neighborhood' | 'city' | 'state', value: string): void {
    const control = this.form.controls[name];
    const currentValue = control.value?.trim() ?? '';

    if (control.dirty || currentValue) {
      return;
    }

    control.patchValue(value);
  }

  private carregarRevisoes(id: string): void {
    this.carregandoRevisoes.set(true);
    this.revisionsError.set('');

    this.personService.listRevisions(id).subscribe({
      next: (revisions) => {
        this.revisions.set(revisions);
        this.carregandoRevisoes.set(false);
      },
      error: () => {
        this.revisions.set([]);
        this.revisionsError.set(this.i18n.translate('people.audit.loadError'));
        this.carregandoRevisoes.set(false);
      },
    });
  }

  private aplicarFotoSelecionada(arquivo: File): boolean {
    if (!PERSON_PHOTO_CONTENT_TYPES.includes(arquivo.type as (typeof PERSON_PHOTO_CONTENT_TYPES)[number])) {
      this.errorMessage.set(this.i18n.translate('people.form.photoInvalidType'));
      return false;
    }

    if (arquivo.size > MAX_PHOTO_SIZE_BYTES) {
      this.errorMessage.set(this.i18n.translate('people.form.photoTooLarge'));
      return false;
    }

    this.limparPreviewFoto();
    this.fotoSelecionada.set(arquivo);
    this.fotoPreviewUrl.set(URL.createObjectURL(arquivo));
    return true;
  }

  private enviarFotoSeNecessario(personId: string): void {
    const foto = this.fotoSelecionada();

    if (!foto) {
      this.salvando.set(false);
      this.router.navigate(['/people']);
      return;
    }

    this.personService.atualizarFoto(personId, foto).subscribe({
      next: () => {
        this.salvando.set(false);
        this.router.navigate(['/people']);
      },
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('people.form.photoUploadError')));
      },
    });
  }

  private limparPreviewFoto(): void {
    if (this.fotoPreviewUrl()) {
      URL.revokeObjectURL(this.fotoPreviewUrl()!);
    }
    this.fotoPreviewUrl.set(null);
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
    return mensagemErroHttp(err, fallback);
  }
}
