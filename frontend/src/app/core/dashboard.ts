import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DashboardSummary {
  peopleCount: number;
  mainItemCount: number;
  instanceCount: number;
  availableInstanceCount: number;
  loanedInstanceCount: number;
  locationCount: number;
  itemsWithoutImageCount: number;
  aiRegisteredItemCount: number;
  vectorQueryCount: number;
  topLocations: DashboardLocationQuantity[];
  topCategories: DashboardCategoryQuantity[];
}

export interface DashboardLocationQuantity {
  id: string;
  name: string;
  instanceCount: number;
}

export interface DashboardCategoryQuantity {
  id: string;
  name: string;
  itemCount: number;
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/dashboard`;

  carregarSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.baseUrl}/summary`);
  }
}
