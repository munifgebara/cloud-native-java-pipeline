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

  private readonly authenticatedSignal = signal<boolean>(this.hasValidAccessToken());

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
    const valid = this.hasValidAccessToken();

    if (!valid) {
      this.logout();
    }

    return valid;
  }

  getAccessToken(): string | null {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);

    if (!token || this.isTokenExpired(token)) {
      this.logout();
      return null;
    }

    return token;
  }

  private hasValidAccessToken(): boolean {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    return !!token && !this.isTokenExpired(token);
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payloadBase64 = token.split('.')[1];
      const payloadJson = atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/'));
      const payload = JSON.parse(payloadJson);
      const exp = payload.exp;

      if (!exp) {
        return true;
      }

      const nowInSeconds = Math.floor(Date.now() / 1000);
      return exp <= nowInSeconds;
    } catch {
      return true;
    }
  }
}
