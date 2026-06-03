import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { InstanciaItemService } from '../../../core/instancia-item/instancia-item';
import { ItemMestreResumo, ItemMestreService } from '../../../core/item-mestre/item-mestre';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-instancia-item-form',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, TextareaModule, ButtonModule, CardModule, CheckboxModule, RouterLink, TranslatePipe],
  templateUrl: './instancia-item-form.html',
  styleUrl: './instancia-item-form.css',
})
export class InstanciaItemFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);
  private readonly instanciaItemService = inject(InstanciaItemService);
  private readonly itemMestreService = inject(ItemMestreService);

  id = signal<string | null>(null);
  itensMestre = signal<ItemMestreResumo[]>([]);
  loading = signal(false);
  salvando = signal(false);
  errorMessage = signal('');

  readonly edicao = computed(() => !!this.id());

  form = this.fb.group({
    itemMestreId: ['', [Validators.required]],
    identificador: ['', [Validators.maxLength(100)]],
    patrimonio: ['', [Validators.maxLength(100)]],
    numeroSerie: ['', [Validators.maxLength(150)]],
    observacoes: ['', [Validators.maxLength(1000)]],
    ativa: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.id.set(id);
    this.carregarItensMestre();

    if (!id) {
      return;
    }

    this.loading.set(true);

    this.instanciaItemService.buscarPorId(id).subscribe({
      next: (instancia) => {
        this.form.patchValue({
          itemMestreId: instancia.itemMestreId ?? '',
          identificador: instancia.identificador ?? '',
          patrimonio: instancia.patrimonio ?? '',
          numeroSerie: instancia.numeroSerie ?? '',
          observacoes: instancia.observacoes ?? '',
          ativa: instancia.ativa,
        });
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('itemInstances.form.loadError'));
        this.loading.set(false);
      },
    });
  }

  salvar(): void {
    this.errorMessage.set('');
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.errorMessage.set(this.i18n.translate('itemInstances.form.invalidFields'));
      return;
    }

    const valor = this.form.getRawValue();
    const payload = {
      itemMestreId: valor.itemMestreId ?? '',
      identificador: this.nullIfBlank(valor.identificador),
      patrimonio: this.nullIfBlank(valor.patrimonio),
      numeroSerie: this.nullIfBlank(valor.numeroSerie),
      observacoes: this.nullIfBlank(valor.observacoes),
      ativa: !!valor.ativa,
    };

    if (!payload.identificador && !payload.patrimonio && !payload.numeroSerie) {
      this.errorMessage.set(this.i18n.translate('itemInstances.form.identificationRequired'));
      return;
    }

    this.salvando.set(true);

    if (this.edicao()) {
      this.instanciaItemService.atualizar(this.id()!, payload).subscribe({
        next: () => {
          this.salvando.set(false);
          this.router.navigate(['/instancias-item']);
        },
        error: (err) => {
          this.salvando.set(false);
          this.errorMessage.set(this.extractError(err, this.i18n.translate('itemInstances.form.updateError')));
        },
      });

      return;
    }

    this.instanciaItemService.criar(payload).subscribe({
      next: () => {
        this.salvando.set(false);
        this.router.navigate(['/instancias-item']);
      },
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('itemInstances.form.createError')));
      },
    });
  }

  campoInvalido(nome: keyof typeof this.form.controls): boolean {
    const campo = this.form.controls[nome];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  private carregarItensMestre(): void {
    this.itemMestreService.listar().subscribe({
      next: (itens) => this.itensMestre.set(itens),
      error: () => this.errorMessage.set(this.i18n.translate('itemInstances.form.masterItemLoadError')),
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
