import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import {
  ItemInstanceHistoryResponse,
  ItemInstanceResponse,
  ItemInstanceService,
  ItemMovementResponse,
  InstanceOperationalStatus,
} from '../../../core/item-instance/item-instance';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-item-instance-history',
  standalone: true,
  imports: [RouterLink, ButtonModule, CardModule, TagModule, DatePipe, TranslatePipe],
  templateUrl: './item-instance-history.html',
  styleUrl: './item-instance-history.css',
})
export class ItemInstanceHistoricoComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(ItemInstanceService);
  private readonly i18n = inject(I18nService);

  loading = signal(false);
  errorMessage = signal('');
  history = signal<ItemInstanceHistoryResponse | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage.set(this.i18n.translate('itemInstances.history.loadError'));
      return;
    }

    this.loading.set(true);
    this.service.buscarHistorico(id).subscribe({
      next: (history) => {
        this.history.set(history);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('itemInstances.history.loadError'));
        this.loading.set(false);
      },
    });
  }

  nomeInstancia(instance: ItemInstanceResponse): string {
    return instance.identifier || instance.assetTag || instance.serialNumber || instance.mainItemName;
  }

  statusLabel(status: InstanceOperationalStatus): string {
    return this.i18n.translate(`itemInstances.status.${status}`);
  }

  statusSeverity(status: InstanceOperationalStatus): 'success' | 'info' | 'warn' | 'secondary' {
    const severities: Record<InstanceOperationalStatus, 'success' | 'info' | 'warn' | 'secondary'> = {
      DISPONIVEL: 'success',
      EM_MOVIMENTACAO: 'info',
      EMPRESTADO: 'warn',
      INATIVO: 'secondary',
    };
    return severities[status];
  }

  tipoLabel(movement: ItemMovementResponse): string {
    return this.i18n.translate(`itemInstances.history.type.${movement.type}`);
  }

  movimentoDescricao(movement: ItemMovementResponse): string {
    if (movement.type === 'ENTRADA') {
      return this.i18n.translate('itemInstances.history.entryDescription', {
        destination: movement.destinationLocationName || '-',
      });
    }
    if (movement.type === 'SAIDA') {
      return this.i18n.translate('itemInstances.history.exitDescription', {
        origin: movement.originLocationName || '-',
        reason: movement.reason || '-',
      });
    }
    if (movement.type === 'TRANSFERENCIA') {
      return this.i18n.translate('itemInstances.history.transferDescription', {
        origin: movement.originLocationName || '-',
        destination: movement.destinationLocationName || '-',
      });
    }
    return movement.type;
  }
}
