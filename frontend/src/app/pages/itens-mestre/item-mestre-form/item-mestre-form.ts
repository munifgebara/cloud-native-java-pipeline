import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { CategoriaResumo, CategoriaService } from '../../../core/categoria/categoria';
import { ItemMestreService } from '../../../core/item-mestre/item-mestre';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-item-mestre-form',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, TextareaModule, ButtonModule, CardModule, CheckboxModule, RouterLink, TranslatePipe],
  templateUrl: './item-mestre-form.html',
  styleUrl: './item-mestre-form.css',
})
export class ItemMestreFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);
  private readonly itemMestreService = inject(ItemMestreService);
  private readonly categoriaService = inject(CategoriaService);

  id = signal<string | null>(null);
  categorias = signal<CategoriaResumo[]>([]);
  loading = signal(false);
  salvando = signal(false);
  errorMessage = signal('');

  readonly edicao = computed(() => !!this.id());

  form = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    categoriaId: [''],
    descricao: ['', [Validators.maxLength(500)]],
    observacoes: ['', [Validators.maxLength(1000)]],
    ativa: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.id.set(id);
    this.carregarCategorias();

    if (!id) {
      return;
    }

    this.loading.set(true);

    this.itemMestreService.buscarPorId(id).subscribe({
      next: (item) => {
        this.form.patchValue({
          nome: item.nome ?? '',
          categoriaId: item.categoriaId ?? '',
          descricao: item.descricao ?? '',
          observacoes: item.observacoes ?? '',
          ativa: item.ativa,
        });
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('masterItems.form.loadError'));
        this.loading.set(false);
      },
    });
  }

  salvar(): void {
    this.errorMessage.set('');
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.errorMessage.set(this.i18n.translate('masterItems.form.invalidFields'));
      return;
    }

    this.salvando.set(true);

    const valor = this.form.getRawValue();
    const payload = {
      nome: valor.nome?.trim() ?? '',
      categoriaId: this.nullIfBlank(valor.categoriaId),
      descricao: this.nullIfBlank(valor.descricao),
      observacoes: this.nullIfBlank(valor.observacoes),
      ativa: !!valor.ativa,
    };

    if (this.edicao()) {
      this.itemMestreService.atualizar(this.id()!, payload).subscribe({
        next: () => {
          this.salvando.set(false);
          this.router.navigate(['/itens-mestre']);
        },
        error: (err) => {
          this.salvando.set(false);
          this.errorMessage.set(this.extractError(err, this.i18n.translate('masterItems.form.updateError')));
        },
      });

      return;
    }

    this.itemMestreService.criar(payload).subscribe({
      next: () => {
        this.salvando.set(false);
        this.router.navigate(['/itens-mestre']);
      },
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('masterItems.form.createError')));
      },
    });
  }

  campoInvalido(nome: keyof typeof this.form.controls): boolean {
    const campo = this.form.controls[nome];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  private carregarCategorias(): void {
    this.categoriaService.listar().subscribe({
      next: (categorias) => this.categorias.set(categorias),
      error: () => this.errorMessage.set(this.i18n.translate('masterItems.form.categoryLoadError')),
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
