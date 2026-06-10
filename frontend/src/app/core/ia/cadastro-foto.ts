import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CadastroFotoInstanciaSugestao {
  identificador: string | null;
  patrimonio: string | null;
  numeroSerie: string | null;
  estadoConservacao: string | null;
  observacoes: string | null;
  confianca: number | null;
}

export interface CadastroFotoItemSugestao {
  nome: string;
  descricao: string | null;
  categoriaSugerida: string | null;
  marca: string | null;
  modelo: string | null;
  autor: string | null;
  editora: string | null;
  anoPublicacao: string | null;
  isbn: string | null;
  fontePesquisa: string | null;
  identificacaoVerificada: boolean | null;
  quantidade: number;
  estadoConservacao: string | null;
  observacoes: string | null;
  confianca: number | null;
  instancias: CadastroFotoInstanciaSugestao[];
}

export interface CadastroFotoSugestaoResponse {
  itens: CadastroFotoItemSugestao[];
  mensagem: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class CadastroFotoIaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/ia/cadastro-foto`;

  sugerirCadastro(arquivo: File): Observable<CadastroFotoSugestaoResponse> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);

    return this.http.post<CadastroFotoSugestaoResponse>(`${this.baseUrl}/sugestoes`, formData);
  }
}
