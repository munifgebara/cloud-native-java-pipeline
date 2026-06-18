import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type StatusOperacionalInstancia = 'DISPONIVEL' | 'EM_MOVIMENTACAO' | 'EMPRESTADO' | 'INATIVO';

export interface ItemInstanceSummary {
  id: string;
  mainItemId: string;
  mainItemName: string;
  categoryName: string | null;
  categoryIcon: string | null;
  currentLocationId: string | null;
  currentLocationName: string | null;
  identifier: string | null;
  assetTag: string | null;
  serialNumber: string | null;
  operationalStatus: StatusOperacionalInstancia;
  active: boolean;
}

export interface ItemInstanceResponse {
  id: string;
  mainItemId: string;
  mainItemName: string;
  categoryId: string | null;
  categoryName: string | null;
  categoryIcon: string | null;
  currentLocationId: string | null;
  currentLocationName: string | null;
  identifier: string | null;
  assetTag: string | null;
  serialNumber: string | null;
  operationalStatus: StatusOperacionalInstancia;
  notes: string | null;
  origemCadastro: string | null;
  active: boolean;
}

export interface ItemInstanceCreateRequest {
  mainItemId: string;
  currentLocationId?: string | null;
  identifier?: string | null;
  assetTag?: string | null;
  serialNumber?: string | null;
  operationalStatus?: StatusOperacionalInstancia | null;
  notes?: string | null;
  origemCadastro?: string | null;
  active?: boolean | null;
}

export interface ItemInstanceUpdateRequest {
  mainItemId: string;
  currentLocationId?: string | null;
  identifier?: string | null;
  assetTag?: string | null;
  serialNumber?: string | null;
  operationalStatus?: StatusOperacionalInstancia | null;
  notes?: string | null;
  origemCadastro?: string | null;
  active?: boolean | null;
}

export interface MovimentacaoEntradaRequest {
  mainItemId: string;
  destinationLocationId: string;
  identifier?: string | null;
  assetTag?: string | null;
  serialNumber?: string | null;
  notes?: string | null;
}

export interface MovimentacaoSaidaRequest {
  instanciaItemId: string;
  reason: string;
  notes?: string | null;
}

export interface MovimentacaoTransferenciaRequest {
  instanciaItemId: string;
  destinationLocationId: string;
  notes?: string | null;
}

export interface ItemLoanRequest {
  instanciaItemId: string;
  personId: string;
  expectedReturnDate?: string | null;
  notes?: string | null;
}

export interface DevolucaoItemRequest {
  instanciaItemId: string;
  localRetornoId: string;
  notes?: string | null;
}

export interface ItemLoanResponse {
  id: string;
  instanciaItemId: string;
  instanciaIdentificacao: string | null;
  personId: string;
  personNome: string;
  loanDate: string;
  expectedReturnDate: string | null;
  returnDate: string | null;
  notes: string | null;
}

export interface ItemMovementResponse {
  id: string;
  type: 'ENTRADA' | 'SAIDA' | 'TRANSFERENCIA';
  movementDate: string;
  instanciaItemId: string;
  instanciaIdentificacao: string | null;
  originLocationId: string | null;
  localOrigemNome: string | null;
  destinationLocationId: string | null;
  localDestinoNome: string | null;
  reason: string | null;
  notes: string | null;
}

export interface ItemInstanceHistoricoResponse {
  instancia: ItemInstanceResponse;
  movimentacoes: ItemMovementResponse[];
}

@Injectable({
  providedIn: 'root',
})
export class ItemInstanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/instances-item`;

  listar(): Observable<ItemInstanceSummary[]> {
    return this.http.get<ItemInstanceSummary[]>(this.baseUrl);
  }

  buscarPorIdentificador(identifier: string): Observable<ItemInstanceSummary[]> {
    return this.http.get<ItemInstanceSummary[]>(`${this.baseUrl}/search`, {
      params: { identifier },
    });
  }

  filtrar(
    identification?: string | null,
    mainItem?: string | null,
    categoryId?: string | null,
    operationalStatus?: StatusOperacionalInstancia | null
  ): Observable<ItemInstanceSummary[]> {
    const params: Record<string, string> = {};
    const identificacaoTratada = (identification ?? '').trim();
    const itemMestreTratado = (mainItem ?? '').trim();

    if (identificacaoTratada) {
      params['identification'] = identificacaoTratada;
    }

    if (itemMestreTratado) {
      params['mainItem'] = itemMestreTratado;
    }

    if (categoryId) {
      params['categoryId'] = categoryId;
    }

    if (operationalStatus) {
      params['operationalStatus'] = operationalStatus;
    }

    return this.http.get<ItemInstanceSummary[]>(`${this.baseUrl}/filter`, { params });
  }

  buscarPorId(id: string): Observable<ItemInstanceResponse> {
    return this.http.get<ItemInstanceResponse>(`${this.baseUrl}/${id}`);
  }

  buscarHistorico(id: string): Observable<ItemInstanceHistoricoResponse> {
    return this.http.get<ItemInstanceHistoricoResponse>(`${this.baseUrl}/${id}/history`);
  }

  criar(payload: ItemInstanceCreateRequest): Observable<ItemInstanceResponse> {
    return this.http.post<ItemInstanceResponse>(this.baseUrl, payload);
  }

  registrarEntrada(payload: MovimentacaoEntradaRequest): Observable<ItemMovementResponse> {
    return this.http.post<ItemMovementResponse>(`${environment.apiBaseUrl}/api/v0/movements-item/inbound`, payload);
  }

  registrarSaida(payload: MovimentacaoSaidaRequest): Observable<ItemMovementResponse> {
    return this.http.post<ItemMovementResponse>(`${environment.apiBaseUrl}/api/v0/movements-item/outbound`, payload);
  }

  registrarTransferencia(payload: MovimentacaoTransferenciaRequest): Observable<ItemMovementResponse> {
    return this.http.post<ItemMovementResponse>(`${environment.apiBaseUrl}/api/v0/movements-item/transfer`, payload);
  }

  registrarEmprestimo(payload: ItemLoanRequest): Observable<ItemLoanResponse> {
    return this.http.post<ItemLoanResponse>(`${environment.apiBaseUrl}/api/v0/loans-item`, payload);
  }

  registrarDevolucao(payload: DevolucaoItemRequest): Observable<ItemLoanResponse> {
    return this.http.post<ItemLoanResponse>(`${environment.apiBaseUrl}/api/v0/loans-item/return`, payload);
  }

  update(id: string, payload: ItemInstanceUpdateRequest): Observable<ItemInstanceResponse> {
    return this.http.put<ItemInstanceResponse>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
