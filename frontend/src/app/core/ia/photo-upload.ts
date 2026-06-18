import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PhotoUploadInstanceSuggestion {
  identifier: string | null;
  assetTag: string | null;
  serialNumber: string | null;
  condition: string | null;
  notes: string | null;
  confidence: number | null;
}

export interface PhotoUploadItemSuggestion {
  name: string;
  description: string | null;
  suggestedCategory: string | null;
  brand: string | null;
  model: string | null;
  author: string | null;
  publisher: string | null;
  publicationYear: string | null;
  isbn: string | null;
  source: string | null;
  identificationVerified: boolean | null;
  quantity: number;
  condition: string | null;
  notes: string | null;
  confidence: number | null;
  instances: PhotoUploadInstanceSuggestion[];
}

export interface PhotoUploadSuggestionResponse {
  items: PhotoUploadItemSuggestion[];
  message: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class PhotoUploadAiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/ai/photo-upload`;

  sugerirCadastro(arquivo: File): Observable<PhotoUploadSuggestionResponse> {
    const formData = new FormData();
    formData.append('arquivo', arquivo);

    return this.http.post<PhotoUploadSuggestionResponse>(`${this.baseUrl}/suggestions`, formData);
  }
}
