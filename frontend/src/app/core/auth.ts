import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../environments/environment';

const ACCESS_TOKEN_KEY = 'pagadoria_access_token';
const REFRESH_TOKEN_KEY = 'pagadoria_refresh_token';

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = environment.apiBaseUrl;

  private readonly authenticatedSignal = signal<boolean>(!!localStorage.getItem(ACCESS_TOKEN_KEY));

  readonly authenticated = computed(() => this.authenticatedSignal());

  login(username: string, password: string): Observable<boolean> {
    return this.http
      .post<LoginResponse>(`${this.apiBaseUrl}/api/public/login`, {
        username: username.trim(),
        password: password.trim(),
      })
      .pipe(
        tap((response) => {
          localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
          localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
          this.authenticatedSignal.set(true);
        }),
        map(() => true)
      );
  }

  logout(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    this.authenticatedSignal.set(false);
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }
}

