import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { mensagemErroHttp } from '../../../core/http-error';
import { InstanciaItemService, StatusOperacionalInstancia } from '../../../core/instancia-item/instancia-item';
import { LocalResumo, LocalService } from '../../../core/local/local';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-instancia-item-form',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, TextareaModule, ButtonModule, CardModule, CheckboxModule, TranslatePipe],
  templateUrl: './instancia-item-form.html',
  styleUrl: './instancia-item-form.css',
})
export class InstanciaItemFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly i18n = inject(I18nService);
  private readonly instanciaItemService = inject(InstanciaItemService);
  private readonly localService = inject(LocalService);

  private itemMestreId = '';

  id = signal<string | null>(null);
  itemMestreNome = signal('');
  locais = signal<LocalResumo[]>([]);
  loading = signal(false);
  salvando = signal(false);
  errorMessage = signal('');
  statusOptions: StatusOperacionalInstancia[] = ['DISPONIVEL', 'EM_MOVIMENTACAO', 'EMPRESTADO', 'INATIVO'];

  form = this.fb.group({
    localAtualId: ['', [Validators.required]],
    identificador: ['', [Validators.maxLength(100)]],
    patrimonio: ['', [Validators.maxLength(100)]],
    numeroSerie: ['', [Validators.maxLength(150)]],
    statusOperacional: ['DISPONIVEL' as StatusOperacionalInstancia, [Validators.required]],
    observacoes: ['', [Validators.maxLength(1000)]],
    ativa: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.id.set(id);
    this.carregarLocais();

    if (!id) {
      this.router.navigate(['/itens-mestre']);
      return;
    }

    this.loading.set(true);

    this.instanciaItemService.buscarPorId(id).subscribe({
      next: (instancia) => {
        this.itemMestreId = instancia.itemMestreId ?? '';
        this.itemMestreNome.set(instancia.itemMestreNome ?? '');
        this.form.patchValue({
          localAtualId: instancia.localAtualId ?? '',
          identificador: instancia.identificador ?? '',
          patrimonio: instancia.patrimonio ?? '',
          numeroSerie: instancia.numeroSerie ?? '',
          statusOperacional: instancia.statusOperacional ?? 'DISPONIVEL',
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
      itemMestreId: this.itemMestreId,
      localAtualId: valor.localAtualId ?? '',
      identificador: this.nullIfBlank(valor.identificador),
      patrimonio: this.nullIfBlank(valor.patrimonio),
      numeroSerie: this.nullIfBlank(valor.numeroSerie),
      statusOperacional: valor.statusOperacional ?? 'DISPONIVEL',
      observacoes: this.nullIfBlank(valor.observacoes),
      ativa: !!valor.ativa,
    };

    if (!payload.identificador && !payload.patrimonio && !payload.numeroSerie) {
      this.errorMessage.set(this.i18n.translate('itemInstances.form.identificationRequired'));
      return;
    }

    this.salvando.set(true);

    this.instanciaItemService.atualizar(this.id()!, payload).subscribe({
      next: () => {
        this.salvando.set(false);
        this.location.back();
      },
      error: (err) => {
        this.salvando.set(false);
        this.errorMessage.set(this.extractError(err, this.i18n.translate('itemInstances.form.updateError')));
      },
    });
  }

  voltar(): void {
    this.location.back();
  }

  campoInvalido(nome: keyof typeof this.form.controls): boolean {
    const campo = this.form.controls[nome];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  statusLabel(status: StatusOperacionalInstancia): string {
    return this.i18n.translate(`itemInstances.status.${status}`);
  }

  private carregarLocais(): void {
    this.localService.listar().subscribe({
      next: (locais) => this.locais.set(locais),
      error: () => this.errorMessage.set(this.i18n.translate('itemInstances.form.locationLoadError')),
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
