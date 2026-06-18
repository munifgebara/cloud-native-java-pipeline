import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import {
  ItemInstanceHistoricoResponse,
  ItemInstanceResponse,
  ItemInstanceService,
  ItemMovementResponse,
  StatusOperacionalInstancia,
} from '../../../core/instancia-item/instancia-item';
import { I18nService, TranslatePipe } from '../../../core/i18n/i18n';

@Component({
  selector: 'app-instancia-item-historico',
  standalone: true,
  imports: [RouterLink, ButtonModule, CardModule, TagModule, DatePipe, TranslatePipe],
  templateUrl: './instancia-item-historico.html',
  styleUrl: './instancia-item-historico.css',
})
export class ItemInstanceHistoricoComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(ItemInstanceService);
  private readonly i18n = inject(I18nService);

  loading = signal(false);
  errorMessage = signal('');
  historico = signal<ItemInstanceHistoricoResponse | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage.set(this.i18n.translate('itemInstances.history.loadError'));
      return;
    }

    this.loading.set(true);
    this.service.buscarHistorico(id).subscribe({
      next: (historico) => {
        this.historico.set(historico);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.translate('itemInstances.history.loadError'));
        this.loading.set(false);
      },
    });
  }

  nomeInstancia(instancia: ItemInstanceResponse): string {
    return instancia.identifier || instancia.assetTag || instancia.serialNumber || instancia.mainItemName;
  }

  statusLabel(status: StatusOperacionalInstancia): string {
    return this.i18n.translate(`itemInstances.status.${status}`);
  }

  statusSeverity(status: StatusOperacionalInstancia): 'success' | 'info' | 'warn' | 'secondary' {
    const severities: Record<StatusOperacionalInstancia, 'success' | 'info' | 'warn' | 'secondary'> = {
      DISPONIVEL: 'success',
      EM_MOVIMENTACAO: 'info',
      EMPRESTADO: 'warn',
      INATIVO: 'secondary',
    };
    return severities[status];
  }

  tipoLabel(movimentacao: ItemMovementResponse): string {
    return this.i18n.translate(`itemInstances.history.type.${movimentacao.type}`);
  }

  movimentoDescricao(movimentacao: ItemMovementResponse): string {
    if (movimentacao.type === 'ENTRADA') {
      return this.i18n.translate('itemInstances.history.entryDescription', {
        destination: movimentacao.localDestinoNome || '-',
      });
    }
    if (movimentacao.type === 'SAIDA') {
      return this.i18n.translate('itemInstances.history.exitDescription', {
        origin: movimentacao.localOrigemNome || '-',
        reason: movimentacao.reason || '-',
      });
    }
    if (movimentacao.type === 'TRANSFERENCIA') {
      return this.i18n.translate('itemInstances.history.transferDescription', {
        origin: movimentacao.localOrigemNome || '-',
        destination: movimentacao.localDestinoNome || '-',
      });
    }
    return movimentacao.type;
  }
}
