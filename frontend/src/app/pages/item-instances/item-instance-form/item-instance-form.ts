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
import { ItemInstanceService, StatusOperacionalInstancia } from '../../../core/item-instance/item-instance';
import { LocationSummary, LocationService } from '../../../core/location/location';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-item-instance-form',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, TextareaModule, ButtonModule, CardModule, CheckboxModule, TranslatePipe],
  templateUrl: './item-instance-form.html',
  styleUrl: './item-instance-form.css',
})
export class ItemInstanceFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly i18n = inject(I18nService);
  private readonly instanciaItemService = inject(ItemInstanceService);
  private readonly localService = inject(LocationService);

  private mainItemId = '';

  id = signal<string | null>(null);
  mainItemName = signal('');
  locations = signal<LocationSummary[]>([]);
  loading = signal(false);
  salvando = signal(false);
  errorMessage = signal('');
  statusOptions: StatusOperacionalInstancia[] = ['DISPONIVEL', 'EM_MOVIMENTACAO', 'EMPRESTADO', 'INATIVO'];

  form = this.fb.group({
    currentLocationId: ['', [Validators.required]],
    identifier: ['', [Validators.maxLength(100)]],
    assetTag: ['', [Validators.maxLength(100)]],
    serialNumber: ['', [Validators.maxLength(150)]],
    operationalStatus: ['DISPONIVEL' as StatusOperacionalInstancia, [Validators.required]],
    notes: ['', [Validators.maxLength(1000)]],
    active: [true],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.id.set(id);
    this.carregarLocais();

    if (!id) {
      this.router.navigate(['/main-items']);
      return;
    }

    this.loading.set(true);

    this.instanciaItemService.buscarPorId(id).subscribe({
      next: (instancia) => {
        this.mainItemId = instancia.mainItemId ?? '';
        this.mainItemName.set(instancia.mainItemName ?? '');
        this.form.patchValue({
          currentLocationId: instancia.currentLocationId ?? '',
          identifier: instancia.identifier ?? '',
          assetTag: instancia.assetTag ?? '',
          serialNumber: instancia.serialNumber ?? '',
          operationalStatus: instancia.operationalStatus ?? 'DISPONIVEL',
          notes: instancia.notes ?? '',
          active: instancia.active,
        });
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('itemInstances.form.loadError'));
        this.loading.set(false);
      },
    });
  }

  save(): void {
    this.errorMessage.set('');
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      this.errorMessage.set(this.i18n.translate('itemInstances.form.invalidFields'));
      return;
    }

    const valor = this.form.getRawValue();
    const payload = {
      mainItemId: this.mainItemId,
      currentLocationId: valor.currentLocationId ?? '',
      identifier: this.nullIfBlank(valor.identifier),
      assetTag: this.nullIfBlank(valor.assetTag),
      serialNumber: this.nullIfBlank(valor.serialNumber),
      operationalStatus: valor.operationalStatus ?? 'DISPONIVEL',
      notes: this.nullIfBlank(valor.notes),
      active: !!valor.active,
    };

    if (!payload.identifier && !payload.assetTag && !payload.serialNumber) {
      this.errorMessage.set(this.i18n.translate('itemInstances.form.identificationRequired'));
      return;
    }

    this.salvando.set(true);

    this.instanciaItemService.update(this.id()!, payload).subscribe({
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

  back(): void {
    this.location.back();
  }

  campoInvalido(name: keyof typeof this.form.controls): boolean {
    const campo = this.form.controls[name];
    return !!campo && campo.invalid && (campo.touched || campo.dirty);
  }

  statusLabel(status: StatusOperacionalInstancia): string {
    return this.i18n.translate(`itemInstances.status.${status}`);
  }

  private carregarLocais(): void {
    this.localService.listar().subscribe({
      next: (locations) => this.locations.set(locations),
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
