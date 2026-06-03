import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { LocalResumo, LocalService } from '../../../core/local/local';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

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
        next: () => {
          this.salvando.set(false);
          this.router.navigate(['/locais']);
        },
        error: (err) => {
          this.salvando.set(false);
          this.errorMessage.set(this.extractError(err, this.i18n.translate('locations.form.updateError')));
        },
      });

      return;
    }

    this.localService.criar(payload).subscribe({
      next: () => {
        this.salvando.set(false);
        this.router.navigate(['/locais']);
      },
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('locations.form.createError')));
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

  private nullIfBlank(value: string | null | undefined): string | null {
    const v = (value ?? '').trim();
    return v ? v : null;
  }

  private extractError(err: any, fallback: string): string {
    return err?.error?.erro || err?.error?.message || fallback;
  }
}
