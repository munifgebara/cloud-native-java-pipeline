import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface InactiveRecordsPurgeResult {
  itemLoans: number;
  itemMovements: number;
  mainItemEmbeddings: number;
  itemInstances: number;
  mainItems: number;
  storageLocations: number;
  categories: number;
  people: number;
  total: number;
}

@Injectable({
  providedIn: 'root',
})
export class MaintenanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/admin/maintenance`;

  purgeInactiveRecords(): Observable<InactiveRecordsPurgeResult> {
    return this.http.delete<InactiveRecordsPurgeResult>(`${this.baseUrl}/inactive-records`);
  }
}
