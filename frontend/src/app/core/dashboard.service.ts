import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';


export interface DashboardResumo {
  quantidadePessoas: number;
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly http = inject(HttpClient);

  carregarResumo(): Observable<DashboardResumo> {
    return this.http.get<DashboardResumo>(`${environment.apiBaseUrl}/api/v0/dashboard/resumo`);
  }
}

