import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

interface ViaCepResponse {
  cep?: string;
  logradouro?: string;
  bairro?: string;
  localidade?: string;
  uf?: string;
  erro?: boolean;
}

export interface CepLookupResult {
  zipCode: string;
  address: string;
  neighborhood: string;
  city: string;
  state: string;
}

@Injectable({
  providedIn: 'root',
})
export class CepService {
  private readonly http = inject(HttpClient);

  buscarEndereco(cep: string): Observable<CepLookupResult | null> {
    const sanitizedCep = cep.replace(/\D/g, '');

    return this.http.get<ViaCepResponse>(`https://viacep.com.br/ws/${sanitizedCep}/json/`).pipe(
      map((response) => {
        if (response.erro) {
          return null;
        }

        return {
          zipCode: response.cep?.replace(/\D/g, '') ?? sanitizedCep,
          address: response.logradouro?.trim() ?? '',
          neighborhood: response.bairro?.trim() ?? '',
          city: response.localidade?.trim() ?? '',
          state: response.uf?.trim().toUpperCase() ?? '',
        };
      }),
    );
  }
}
