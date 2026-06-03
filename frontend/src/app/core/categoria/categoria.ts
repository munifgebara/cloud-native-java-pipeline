import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TranslationKey } from '../i18n/i18n';

export interface CategoriaResumo {
  id: string;
  nome: string;
  descricao: string | null;
  icone: string | null;
  ativa: boolean;
}

export interface CategoriaResponse {
  id: string;
  nome: string;
  descricao: string | null;
  icone: string | null;
  ativa: boolean;
}

export interface CategoriaCreateRequest {
  nome: string;
  descricao?: string | null;
  icone?: string | null;
  ativa?: boolean | null;
}

export interface CategoriaUpdateRequest {
  nome: string;
  descricao?: string | null;
  icone?: string | null;
  ativa?: boolean | null;
}

export interface CategoriaIconeOption {
  key: string;
  labelKey: TranslationKey;
  iconClass: string;
}

export const CATEGORIA_ICONE_OPTIONS: CategoriaIconeOption[] = [
  { key: 'eletronicos', labelKey: 'categories.icons.eletronicos', iconClass: 'pi pi-desktop' },
  { key: 'moveis', labelKey: 'categories.icons.moveis', iconClass: 'pi pi-home' },
  { key: 'ferramentas', labelKey: 'categories.icons.ferramentas', iconClass: 'pi pi-wrench' },
  { key: 'livros', labelKey: 'categories.icons.livros', iconClass: 'pi pi-book' },
  { key: 'roupas', labelKey: 'categories.icons.roupas', iconClass: 'pi pi-tag' },
  { key: 'cozinha', labelKey: 'categories.icons.cozinha', iconClass: 'pi pi-shopping-bag' },
  { key: 'esportes', labelKey: 'categories.icons.esportes', iconClass: 'pi pi-trophy' },
  { key: 'documentos', labelKey: 'categories.icons.documentos', iconClass: 'pi pi-file' },
  { key: 'outros', labelKey: 'categories.icons.outros', iconClass: 'pi pi-tags' },
];

export function categoriaIconClass(key: string | null | undefined): string {
  return CATEGORIA_ICONE_OPTIONS.find((option) => option.key === key)?.iconClass ?? 'pi pi-tags';
}

@Injectable({
  providedIn: 'root',
})
export class CategoriaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/categorias`;

  listar(): Observable<CategoriaResumo[]> {
    return this.http.get<CategoriaResumo[]>(this.baseUrl);
  }

  buscarPorNome(nome: string): Observable<CategoriaResumo[]> {
    return this.http.get<CategoriaResumo[]>(`${this.baseUrl}/buscar`, {
      params: { nome },
    });
  }

  buscarPorId(id: string): Observable<CategoriaResponse> {
    return this.http.get<CategoriaResponse>(`${this.baseUrl}/${id}`);
  }

  criar(payload: CategoriaCreateRequest): Observable<CategoriaResponse> {
    return this.http.post<CategoriaResponse>(this.baseUrl, payload);
  }

  atualizar(id: string, payload: CategoriaUpdateRequest): Observable<CategoriaResponse> {
    return this.http.put<CategoriaResponse>(`${this.baseUrl}/${id}`, payload);
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
