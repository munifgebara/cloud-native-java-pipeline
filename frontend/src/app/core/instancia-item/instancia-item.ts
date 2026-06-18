import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type StatusOperacionalInstancia = 'DISPONIVEL' | 'EM_MOVIMENTACAO' | 'EMPRESTADO' | 'INATIVO';

export interface InstanciaItemResumo {
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

export interface InstanciaItemResponse {
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

export interface InstanciaItemCreateRequest {
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

export interface InstanciaItemUpdateRequest {
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

export interface EmprestimoItemRequest {
  instanciaItemId: string;
  pessoaId: string;
  expectedReturnDate?: string | null;
  notes?: string | null;
}

export interface DevolucaoItemRequest {
  instanciaItemId: string;
  localRetornoId: string;
  notes?: string | null;
}

export interface EmprestimoItemResponse {
  id: string;
  instanciaItemId: string;
  instanciaIdentificacao: string | null;
  pessoaId: string;
  pessoaNome: string;
  loanDate: string;
  expectedReturnDate: string | null;
  returnDate: string | null;
  notes: string | null;
}

export interface MovimentacaoItemResponse {
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

export interface InstanciaItemHistoricoResponse {
  instancia: InstanciaItemResponse;
  movimentacoes: MovimentacaoItemResponse[];
}

@Injectable({
  providedIn: 'root',
})
export class InstanciaItemService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/instances-item`;

  listar(): Observable<InstanciaItemResumo[]> {
    return this.http.get<InstanciaItemResumo[]>(this.baseUrl);
  }

  buscarPorIdentificador(identifier: string): Observable<InstanciaItemResumo[]> {
    return this.http.get<InstanciaItemResumo[]>(`${this.baseUrl}/search`, {
      params: { identifier },
    });
  }

  filtrar(
    identification?: string | null,
    mainItem?: string | null,
    categoryId?: string | null,
    operationalStatus?: StatusOperacionalInstancia | null
  ): Observable<InstanciaItemResumo[]> {
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

    return this.http.get<InstanciaItemResumo[]>(`${this.baseUrl}/filter`, { params });
  }

  buscarPorId(id: string): Observable<InstanciaItemResponse> {
    return this.http.get<InstanciaItemResponse>(`${this.baseUrl}/${id}`);
  }

  buscarHistorico(id: string): Observable<InstanciaItemHistoricoResponse> {
    return this.http.get<InstanciaItemHistoricoResponse>(`${this.baseUrl}/${id}/history`);
  }

  criar(payload: InstanciaItemCreateRequest): Observable<InstanciaItemResponse> {
    return this.http.post<InstanciaItemResponse>(this.baseUrl, payload);
  }

  registrarEntrada(payload: MovimentacaoEntradaRequest): Observable<MovimentacaoItemResponse> {
    return this.http.post<MovimentacaoItemResponse>(`${environment.apiBaseUrl}/api/v0/movements-item/inbound`, payload);
  }

  registrarSaida(payload: MovimentacaoSaidaRequest): Observable<MovimentacaoItemResponse> {
    return this.http.post<MovimentacaoItemResponse>(`${environment.apiBaseUrl}/api/v0/movements-item/outbound`, payload);
  }

  registrarTransferencia(payload: MovimentacaoTransferenciaRequest): Observable<MovimentacaoItemResponse> {
    return this.http.post<MovimentacaoItemResponse>(`${environment.apiBaseUrl}/api/v0/movements-item/transfer`, payload);
  }

  registrarEmprestimo(payload: EmprestimoItemRequest): Observable<EmprestimoItemResponse> {
    return this.http.post<EmprestimoItemResponse>(`${environment.apiBaseUrl}/api/v0/loans-item`, payload);
  }

  registrarDevolucao(payload: DevolucaoItemRequest): Observable<EmprestimoItemResponse> {
    return this.http.post<EmprestimoItemResponse>(`${environment.apiBaseUrl}/api/v0/loans-item/return`, payload);
  }

  atualizar(id: string, payload: InstanciaItemUpdateRequest): Observable<InstanciaItemResponse> {
    return this.http.put<InstanciaItemResponse>(`${this.baseUrl}/${id}`, payload);
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
