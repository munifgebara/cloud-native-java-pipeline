import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TranslationKey } from '../i18n/i18n';

export interface CategorySummary {
  id: string;
  name: string;
  description: string | null;
  icon: string | null;
  active: boolean;
}

export interface CategoryResponse {
  id: string;
  name: string;
  description: string | null;
  icon: string | null;
  active: boolean;
}

export interface CategoryCreateRequest {
  name: string;
  description?: string | null;
  icon?: string | null;
  active?: boolean | null;
}

export interface CategoryUpdateRequest {
  name: string;
  description?: string | null;
  icon?: string | null;
  active?: boolean | null;
}

export interface CategoryIconOption {
  key: string;
  labelKey: TranslationKey;
  iconClass: string;
}

export const CATEGORIA_ICONE_OPTIONS: CategoryIconOption[] = [
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

export function categoryIconClass(key: string | null | undefined): string {
  return CATEGORIA_ICONE_OPTIONS.find((option) => option.key === key)?.iconClass ?? 'pi pi-tags';
}

@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/categories`;

  listar(): Observable<CategorySummary[]> {
    return this.http.get<CategorySummary[]>(this.baseUrl);
  }

  buscarPorNome(name: string): Observable<CategorySummary[]> {
    return this.http.get<CategorySummary[]>(`${this.baseUrl}/search`, {
      params: { name },
    });
  }

  buscarPorId(id: string): Observable<CategoryResponse> {
    return this.http.get<CategoryResponse>(`${this.baseUrl}/${id}`);
  }

  criar(payload: CategoryCreateRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(this.baseUrl, payload);
  }

  update(id: string, payload: CategoryUpdateRequest): Observable<CategoryResponse> {
    return this.http.put<CategoryResponse>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
