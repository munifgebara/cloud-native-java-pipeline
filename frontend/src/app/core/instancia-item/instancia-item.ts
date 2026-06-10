import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type StatusOperacionalInstancia = 'DISPONIVEL' | 'EM_MOVIMENTACAO' | 'EMPRESTADO' | 'INATIVO';

export interface InstanciaItemResumo {
  id: string;
  itemMestreId: string;
  itemMestreNome: string;
  categoriaNome: string | null;
  categoriaIcone: string | null;
  localAtualId: string | null;
  localAtualNome: string | null;
  identificador: string | null;
  patrimonio: string | null;
  numeroSerie: string | null;
  statusOperacional: StatusOperacionalInstancia;
  ativa: boolean;
}

export interface InstanciaItemResponse {
  id: string;
  itemMestreId: string;
  itemMestreNome: string;
  categoriaId: string | null;
  categoriaNome: string | null;
  categoriaIcone: string | null;
  localAtualId: string | null;
  localAtualNome: string | null;
  identificador: string | null;
  patrimonio: string | null;
  numeroSerie: string | null;
  statusOperacional: StatusOperacionalInstancia;
  observacoes: string | null;
  origemCadastro: string | null;
  ativa: boolean;
}

export interface InstanciaItemCreateRequest {
  itemMestreId: string;
  localAtualId?: string | null;
  identificador?: string | null;
  patrimonio?: string | null;
  numeroSerie?: string | null;
  statusOperacional?: StatusOperacionalInstancia | null;
  observacoes?: string | null;
  origemCadastro?: string | null;
  ativa?: boolean | null;
}

export interface InstanciaItemUpdateRequest {
  itemMestreId: string;
  localAtualId?: string | null;
  identificador?: string | null;
  patrimonio?: string | null;
  numeroSerie?: string | null;
  statusOperacional?: StatusOperacionalInstancia | null;
  observacoes?: string | null;
  origemCadastro?: string | null;
  ativa?: boolean | null;
}

export interface MovimentacaoEntradaRequest {
  itemMestreId: string;
  localDestinoId: string;
  identificador?: string | null;
  patrimonio?: string | null;
  numeroSerie?: string | null;
  observacao?: string | null;
}

export interface MovimentacaoSaidaRequest {
  instanciaItemId: string;
  motivo: string;
  observacao?: string | null;
}

export interface MovimentacaoTransferenciaRequest {
  instanciaItemId: string;
  localDestinoId: string;
  observacao?: string | null;
}

export interface EmprestimoItemRequest {
  instanciaItemId: string;
  pessoaId: string;
  previsaoDevolucao?: string | null;
  observacao?: string | null;
}

export interface DevolucaoItemRequest {
  instanciaItemId: string;
  localRetornoId: string;
  observacao?: string | null;
}

export interface EmprestimoItemResponse {
  id: string;
  instanciaItemId: string;
  instanciaIdentificacao: string | null;
  pessoaId: string;
  pessoaNome: string;
  dataEmprestimo: string;
  previsaoDevolucao: string | null;
  dataDevolucao: string | null;
  observacao: string | null;
}

export interface MovimentacaoItemResponse {
  id: string;
  tipo: 'ENTRADA' | 'SAIDA' | 'TRANSFERENCIA';
  dataMovimentacao: string;
  instanciaItemId: string;
  instanciaIdentificacao: string | null;
  localOrigemId: string | null;
  localOrigemNome: string | null;
  localDestinoId: string | null;
  localDestinoNome: string | null;
  motivo: string | null;
  observacao: string | null;
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
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/instancias-item`;

  listar(): Observable<InstanciaItemResumo[]> {
    return this.http.get<InstanciaItemResumo[]>(this.baseUrl);
  }

  buscarPorIdentificador(identificador: string): Observable<InstanciaItemResumo[]> {
    return this.http.get<InstanciaItemResumo[]>(`${this.baseUrl}/buscar`, {
      params: { identificador },
    });
  }

  filtrar(
    identificacao?: string | null,
    itemMestre?: string | null,
    categoriaId?: string | null,
    statusOperacional?: StatusOperacionalInstancia | null
  ): Observable<InstanciaItemResumo[]> {
    const params: Record<string, string> = {};
    const identificacaoTratada = (identificacao ?? '').trim();
    const itemMestreTratado = (itemMestre ?? '').trim();

    if (identificacaoTratada) {
      params['identificacao'] = identificacaoTratada;
    }

    if (itemMestreTratado) {
      params['itemMestre'] = itemMestreTratado;
    }

    if (categoriaId) {
      params['categoriaId'] = categoriaId;
    }

    if (statusOperacional) {
      params['statusOperacional'] = statusOperacional;
    }

    return this.http.get<InstanciaItemResumo[]>(`${this.baseUrl}/filtrar`, { params });
  }

  buscarPorId(id: string): Observable<InstanciaItemResponse> {
    return this.http.get<InstanciaItemResponse>(`${this.baseUrl}/${id}`);
  }

  buscarHistorico(id: string): Observable<InstanciaItemHistoricoResponse> {
    return this.http.get<InstanciaItemHistoricoResponse>(`${this.baseUrl}/${id}/historico`);
  }

  criar(payload: InstanciaItemCreateRequest): Observable<InstanciaItemResponse> {
    return this.http.post<InstanciaItemResponse>(this.baseUrl, payload);
  }

  registrarEntrada(payload: MovimentacaoEntradaRequest): Observable<MovimentacaoItemResponse> {
    return this.http.post<MovimentacaoItemResponse>(`${environment.apiBaseUrl}/api/v0/movimentacoes-item/entrada`, payload);
  }

  registrarSaida(payload: MovimentacaoSaidaRequest): Observable<MovimentacaoItemResponse> {
    return this.http.post<MovimentacaoItemResponse>(`${environment.apiBaseUrl}/api/v0/movimentacoes-item/saida`, payload);
  }

  registrarTransferencia(payload: MovimentacaoTransferenciaRequest): Observable<MovimentacaoItemResponse> {
    return this.http.post<MovimentacaoItemResponse>(`${environment.apiBaseUrl}/api/v0/movimentacoes-item/transferencia`, payload);
  }

  registrarEmprestimo(payload: EmprestimoItemRequest): Observable<EmprestimoItemResponse> {
    return this.http.post<EmprestimoItemResponse>(`${environment.apiBaseUrl}/api/v0/emprestimos-item`, payload);
  }

  registrarDevolucao(payload: DevolucaoItemRequest): Observable<EmprestimoItemResponse> {
    return this.http.post<EmprestimoItemResponse>(`${environment.apiBaseUrl}/api/v0/emprestimos-item/devolucao`, payload);
  }

  atualizar(id: string, payload: InstanciaItemUpdateRequest): Observable<InstanciaItemResponse> {
    return this.http.put<InstanciaItemResponse>(`${this.baseUrl}/${id}`, payload);
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
