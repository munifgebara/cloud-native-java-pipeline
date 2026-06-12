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
import { LocalResumo, LocalService } from '../../../core/local/local';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-local-form',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, TextareaModule, ButtonModule, CardModule, CheckboxModule, RouterLink, TranslatePipe],
  templateUrl: './local-form.html',
  styleUrl: './local-form.css',
})
export class LocalFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);
  private readonly localService = inject(LocalService);

  id = signal<string | null>(null);
  locaisPai = signal<LocalResumo[]>([]);
  loading = signal(false);
  salvando = signal(false);
  errorMessage = signal('');
  imagemAtualUrl = signal<string | null>(null);
  imagemPreviewUrl = signal<string | null>(null);
  imagemSelecionada = signal<File | null>(null);
  removendoImagem = signal(false);

  readonly edicao = computed(() => !!this.id());
  readonly opcoesPai = computed(() => this.locaisPai().filter((local) => local.id !== this.id()));

  form = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    descricao: ['', [Validators.maxLength(500)]],
    paiId: [''],
    ativa: [true],
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
          nome: local.nome ?? '',
          descricao: local.descricao ?? '',
          paiId: local.paiId ?? '',
          ativa: local.ativa,
        });
        this.imagemAtualUrl.set(local.imagemUrl);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('locations.form.loadError'));
        this.loading.set(false);
      },
    });
  }

  salvar(): void {
    this.errorMessage.set('');
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.errorMessage.set(this.i18n.translate('locations.form.invalidFields'));
      return;
    }

    this.salvando.set(true);

    const valor = this.form.getRawValue();
    const payload = {
      nome: valor.nome?.trim() ?? '',
      descricao: this.nullIfBlank(valor.descricao),
      paiId: this.nullIfBlank(valor.paiId),
      ativa: !!valor.ativa,
    };

    if (this.edicao()) {
      this.localService.atualizar(this.id()!, payload).subscribe({
        next: (local) => this.enviarImagemSeNecessario(local.id),
        error: (err) => {
          this.salvando.set(false);
          this.errorMessage.set(this.extractError(err, this.i18n.translate('locations.form.updateError')));
        },
      });

      return;
    }

    this.localService.criar(payload).subscribe({
      next: (local) => this.enviarImagemSeNecessario(local.id),
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('locations.form.createError')));
      },
    });
  }

  selecionarImagem(event: Event): void {
    this.errorMessage.set('');
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0] ?? null;

    if (!arquivo) {
      this.imagemSelecionada.set(null);
      this.imagemPreviewUrl.set(null);
      return;
    }

    if (!this.aplicarImagemSelecionada(arquivo)) {
      input.value = '';
      return;
    }
  }

  colarImagem(event: ClipboardEvent): void {
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

    this.aplicarImagemSelecionada(result.file);
  }

  private aplicarImagemSelecionada(arquivo: File): boolean {
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

  removerImagem(): void {
    const id = this.id();
    if (!id || (!this.imagemAtualUrl() && !this.imagemSelecionada())) {
      this.imagemSelecionada.set(null);
      this.imagemPreviewUrl.set(null);
      return;
    }

    this.errorMessage.set('');
    this.removendoImagem.set(true);

    this.localService.removerImagem(id).subscribe({
      next: () => {
        this.imagemAtualUrl.set(null);
        this.imagemSelecionada.set(null);
        this.imagemPreviewUrl.set(null);
        this.removendoImagem.set(false);
      },
      error: (err) => {
        this.removendoImagem.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('locations.form.imageRemoveError')));
      },
    });
  }

  campoInvalido(nome: keyof typeof this.form.controls): boolean {
    const campo = this.form.controls[nome];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  private carregarLocaisPai(): void {
    this.localService.listar().subscribe({
      next: (locais) => this.locaisPai.set(locais),
      error: () => this.errorMessage.set(this.i18n.translate('locations.form.parentLoadError')),
    });
  }

  private enviarImagemSeNecessario(localId: string): void {
    const imagem = this.imagemSelecionada();

    if (!imagem) {
      this.salvando.set(false);
      this.router.navigate(['/locais']);
      return;
    }

    this.localService.atualizarImagem(localId, imagem).subscribe({
      next: () => {
        this.salvando.set(false);
        this.router.navigate(['/locais']);
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
