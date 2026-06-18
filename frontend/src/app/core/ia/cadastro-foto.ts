import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CadastroFotoInstanciaSugestao {
  identifier: string | null;
  assetTag: string | null;
  serialNumber: string | null;
  condition: string | null;
  notes: string | null;
  confidence: number | null;
}

export interface CadastroFotoItemSugestao {
  name: string;
  description: string | null;
  categoriaSugerida: string | null;
  brand: string | null;
  model: string | null;
  author: string | null;
  publisher: string | null;
  publicationYear: string | null;
  isbn: string | null;
  source: string | null;
  identificationVerified: boolean | null;
  quantidade: number;
  condition: string | null;
  notes: string | null;
  confidence: number | null;
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
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/ai/cadastro-foto`;

  sugerirCadastro(arquivo: File): Observable<CadastroFotoSugestaoResponse> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);

    return this.http.post<CadastroFotoSugestaoResponse>(`${this.baseUrl}/suggestions`, formData);
  }
}
