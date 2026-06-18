import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { IMAGE_CONTENT_TYPES, imageFileFromPaste } from '../../../core/image/image-clipboard';
import { mensagemErroHttp } from '../../../core/http-error';
import { LocationSummary, LocationService } from '../../../core/location/location';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-location-form',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, TextareaModule, ButtonModule, CardModule, CheckboxModule, RouterLink, TranslatePipe],
  templateUrl: './location-form.html',
  styleUrl: './location-form.css',
})
export class LocationFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);
  private readonly localService = inject(LocationService);

  id = signal<string | null>(null);
  locationsPai = signal<LocationSummary[]>([]);
  loading = signal(false);
  salvando = signal(false);
  errorMessage = signal('');
  imagemAtualUrl = signal<string | null>(null);
  imagemPreviewUrl = signal<string | null>(null);
  imagemSelecionada = signal<File | null>(null);
  removendoImage = signal(false);

  readonly edicao = computed(() => !!this.id());
  readonly opcoesPai = computed(() => this.locationsPai().filter((local) => local.id !== this.id()));

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.maxLength(500)]],
    paiId: [''],
    active: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.id.set(id);
    this.carregarLocaisPai();

    if (!id) {
      return;
    }

    this.loading.set(true);

    this.localService.buscarPorId(id).subscribe({
      next: (local) => {
        this.form.patchValue({
          name: local.name ?? '',
          description: local.description ?? '',
          paiId: local.paiId ?? '',
          active: local.active,
        });
        this.imagemAtualUrl.set(local.imageUrl);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('locations.form.loadError'));
        this.loading.set(false);
      },
    });
  }

  save(): void {
    this.errorMessage.set('');
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.errorMessage.set(this.i18n.translate('locations.form.invalidFields'));
      return;
    }

    this.salvando.set(true);

    const valor = this.form.getRawValue();
    const payload = {
      name: valor.name?.trim() ?? '',
      description: this.nullIfBlank(valor.description),
      paiId: this.nullIfBlank(valor.paiId),
      active: !!valor.active,
    };

    if (this.edicao()) {
      this.localService.update(this.id()!, payload).subscribe({
        next: (local) => this.enviarImageSeNecessario(local.id),
        error: (err) => {
          this.salvando.set(false);
          this.errorMessage.set(this.extractError(err, this.i18n.translate('locations.form.updateError')));
        },
      });

      return;
    }

    this.localService.criar(payload).subscribe({
      next: (local) => this.enviarImageSeNecessario(local.id),
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('locations.form.createError')));
      },
    });
  }

  selecionarImage(event: Event): void {
    this.errorMessage.set('');
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0] ?? null;

    if (!arquivo) {
      this.imagemSelecionada.set(null);
      this.imagemPreviewUrl.set(null);
      return;
    }

    if (!this.aplicarImageSelecionada(arquivo)) {
      input.value = '';
      return;
    }
  }

  colarImage(event: ClipboardEvent): void {
    this.errorMessage.set('');
    const result = imageFileFromPaste(event, MAX_IMAGE_SIZE_BYTES);

    if (!result.ok) {
      if (result.reason === 'missing') {
        this.errorMessage.set(this.i18n.translate('locations.form.imagePasteMissing'));
      } else if (result.reason === 'invalid-type') {
        this.errorMessage.set(this.i18n.translate('locations.form.imageInvalidType'));
      } else {
        this.errorMessage.set(this.i18n.translate('locations.form.imageTooLarge'));
      }
      return;
    }

    this.aplicarImageSelecionada(result.file);
  }

  private aplicarImageSelecionada(arquivo: File): boolean {
    if (!IMAGE_CONTENT_TYPES.includes(arquivo.type as (typeof IMAGE_CONTENT_TYPES)[number])) {
      this.errorMessage.set(this.i18n.translate('locations.form.imageInvalidType'));
      return false;
    }

    if (arquivo.size > MAX_IMAGE_SIZE_BYTES) {
      this.errorMessage.set(this.i18n.translate('locations.form.imageTooLarge'));
      return false;
    }

    if (this.imagemPreviewUrl()) {
      URL.revokeObjectURL(this.imagemPreviewUrl()!);
    }

    this.imagemSelecionada.set(arquivo);
    this.imagemPreviewUrl.set(URL.createObjectURL(arquivo));
    return true;
  }

  removerImage(): void {
    const id = this.id();
    if (!id || (!this.imagemAtualUrl() && !this.imagemSelecionada())) {
      this.imagemSelecionada.set(null);
      this.imagemPreviewUrl.set(null);
      return;
    }

    this.errorMessage.set('');
    this.removendoImage.set(true);

    this.localService.removerImage(id).subscribe({
      next: () => {
        this.imagemAtualUrl.set(null);
        this.imagemSelecionada.set(null);
        this.imagemPreviewUrl.set(null);
        this.removendoImage.set(false);
      },
      error: (err) => {
        this.removendoImage.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('locations.form.imageRemoveError')));
      },
    });
  }

  campoInvalido(name: keyof typeof this.form.controls): boolean {
    const campo = this.form.controls[name];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  private carregarLocaisPai(): void {
    this.localService.listar().subscribe({
      next: (locations) => this.locationsPai.set(locations),
      error: () => this.errorMessage.set(this.i18n.translate('locations.form.parentLoadError')),
    });
  }

  private enviarImageSeNecessario(localId: string): void {
    const imagem = this.imagemSelecionada();

    if (!imagem) {
      this.salvando.set(false);
      this.router.navigate(['/locations']);
      return;
    }

    this.localService.atualizarImage(localId, imagem).subscribe({
      next: () => {
        this.salvando.set(false);
        this.router.navigate(['/locations']);
      },
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('locations.form.imageUploadError')));
      },
    });
  }

  private nullIfBlank(value: string | null | undefined): string | null {
    const v = (value ?? '').trim();
    return v ? v : null;
  }

  private extractError(err: any, fallback: string): string {
    return mensagemErroHttp(err, fallback);
  }
}
