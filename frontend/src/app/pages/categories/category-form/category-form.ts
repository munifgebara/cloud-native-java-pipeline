import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { CATEGORIA_ICONE_OPTIONS, CategoryService } from '../../../core/category/category';
import { mensagemErroHttp } from '../../../core/http-error';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-category-form',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, TextareaModule, ButtonModule, CardModule, CheckboxModule, RouterLink, TranslatePipe],
  templateUrl: './category-form.html',
  styleUrl: './category-form.css',
})
export class CategoryFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);
  private readonly categoryService = inject(CategoryService);

  id = signal<string | null>(null);
  loading = signal(false);
  salvando = signal(false);
  errorMessage = signal('');
  readonly icones = CATEGORIA_ICONE_OPTIONS;

  readonly edicao = computed(() => !!this.id());

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.maxLength(500)]],
    icone: [null as string | null],
    active: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (!id) {
      return;
    }

    this.id.set(id);
    this.loading.set(true);

    this.categoryService.buscarPorId(id).subscribe({
      next: (category) => {
        this.form.patchValue({
          name: category.name ?? '',
          description: category.description ?? '',
          icone: category.icone ?? null,
          active: category.active,
        });
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('categories.form.loadError'));
        this.loading.set(false);
      },
    });
  }

  save(): void {
    this.errorMessage.set('');
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.errorMessage.set(this.i18n.translate('categories.form.invalidFields'));
      return;
    }

    this.salvando.set(true);

    const valor = this.form.getRawValue();
    const payload = {
      name: valor.name?.trim() ?? '',
      description: this.nullIfBlank(valor.description),
      icone: valor.icone ?? null,
      active: !!valor.active,
    };

    if (this.edicao()) {
      this.categoryService.update(this.id()!, payload).subscribe({
        next: () => {
          this.salvando.set(false);
          this.router.navigate(['/categories']);
        },
        error: (err) => {
          this.salvando.set(false);
          this.errorMessage.set(this.extractError(err, this.i18n.translate('categories.form.updateError')));
        },
      });

      return;
    }

    this.categoryService.criar(payload).subscribe({
      next: () => {
        this.salvando.set(false);
        this.router.navigate(['/categories']);
      },
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('categories.form.createError')));
      },
    });
  }

  campoInvalido(name: keyof typeof this.form.controls): boolean {
    const campo = this.form.controls[name];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  selecionarIcon(icone: string | null): void {
    this.form.controls.icone.setValue(icone);
    this.form.controls.icone.markAsDirty();
  }

  private nullIfBlank(value: string | null | undefined): string | null {
    const v = (value ?? '').trim();
    return v ? v : null;
  }

  private extractError(err: any, fallback: string): string {
    return mensagemErroHttp(err, fallback);
  }
}
