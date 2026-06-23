import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, from, map, of, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

const ACCESS_TOKEN_KEY = 'stella_access_token';
const REFRESH_TOKEN_KEY = 'stella_refresh_token';
const PKCE_VERIFIER_KEY = 'stella_pkce_verifier';
const PKCE_STATE_KEY = 'stella_pkce_state';
const PKCE_RETURN_URL_KEY = 'stella_pkce_return_url';
const TOKEN_EXPIRATION_SKEW_SECONDS = 30;

export type SocialProvider = 'google' | 'github';

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

interface KeycloakTokenResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiBaseUrl = environment.apiBaseUrl;
  private readonly keycloakBaseUrl = environment.keycloakBaseUrl;
  private readonly keycloakRealm = environment.keycloakRealm;
  private readonly keycloakClientId = environment.keycloakClientId;
  private readonly authRedirectPath = environment.authRedirectPath;
  private refreshInFlight$?: Observable<LoginResponse>;

  private readonly authenticatedSignal = signal<boolean>(this.hasUsableSession());

  readonly authenticated = computed(() => this.authenticatedSignal());

  login(username: string, password: string): Observable<boolean> {
    return this.http
      .post<LoginResponse>(`${this.apiBaseUrl}/api/public/login`, {
        username: username.trim(),
        password: password.trim(),
      })
      .pipe(
        tap((response) => this.storeSession(response)),
        map(() => true)
      );
  }

  startSocialLogin(provider: SocialProvider, returnUrl = '/dashboard'): Observable<void> {
    return from(this.createPkceChallenge()).pipe(
      tap(({ verifier, challenge }) => {
        const state = crypto.randomUUID();
        sessionStorage.setItem(PKCE_VERIFIER_KEY, verifier);
        sessionStorage.setItem(PKCE_STATE_KEY, state);
        sessionStorage.setItem(PKCE_RETURN_URL_KEY, returnUrl);

        const params = new URLSearchParams({
          client_id: this.keycloakClientId,
          response_type: 'code',
          scope: 'openid profile email',
          redirect_uri: this.redirectUri(),
          state,
          code_challenge: challenge,
          code_challenge_method: 'S256',
          kc_idp_hint: provider,
        });

        window.location.href = `${this.keycloakRealmUrl()}/protocol/openid-connect/auth?${params.toString()}`;
      }),
      map(() => undefined)
    );
  }

  completeSocialLogin(code: string, state: string): Observable<string> {
    const expectedState = sessionStorage.getItem(PKCE_STATE_KEY);
    const verifier = sessionStorage.getItem(PKCE_VERIFIER_KEY);
    const returnUrl = sessionStorage.getItem(PKCE_RETURN_URL_KEY) || '/dashboard';

    if (!expectedState || !verifier || expectedState !== state) {
      this.clearPkceState();
      return throwError(() => new Error('Invalid authentication callback state.'));
    }

    const body = new HttpParams()
      .set('client_id', this.keycloakClientId)
      .set('grant_type', 'authorization_code')
      .set('code', code)
      .set('redirect_uri', this.redirectUri())
      .set('code_verifier', verifier);

    return this.http
      .post<KeycloakTokenResponse>(`${this.keycloakRealmUrl()}/protocol/openid-connect/token`, body.toString(), {
        headers: new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' }),
      })
      .pipe(
        tap((response) => {
          this.storeSession(this.fromKeycloakTokenResponse(response));
          this.clearPkceState();
        }),
        map(() => returnUrl),
        catchError((error) => {
          this.clearPkceState();
          return throwError(() => error);
        })
      );
  }

  logout(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    this.clearPkceState();
    this.authenticatedSignal.set(false);
  }

  redirectToLogin(reason = 'session-expired', returnUrl = this.router.url): void {
    this.logout();
    this.router.navigate(['/login'], {
      queryParams: { reason, returnUrl },
    });
  }

  isAuthenticated(): boolean {
    const valid = this.hasUsableSession();
    this.authenticatedSignal.set(valid);
    return valid;
  }

  accessTokenForRequest(): Observable<string | null> {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (token && !this.isTokenExpired(token, TOKEN_EXPIRATION_SKEW_SECONDS)) {
      return of(token);
    }

    return this.refreshAccessToken().pipe(
      map((response) => response.accessToken),
      catchError((error) => {
        this.redirectToLogin('session-expired');
        return throwError(() => error);
      })
    );
  }

  refreshAccessToken(): Observable<LoginResponse> {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (!refreshToken || this.isTokenExpired(refreshToken)) {
      return throwError(() => new Error('Refresh token is not available.'));
    }

    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http
        .post<LoginResponse>(`${this.apiBaseUrl}/api/public/refresh`, { refreshToken })
        .pipe(
          tap((response) => this.storeSession(response)),
          finalize(() => {
            this.refreshInFlight$ = undefined;
          }),
          shareReplay({ bufferSize: 1, refCount: false })
        );
    }

    return this.refreshInFlight$;
  }

  getAccessToken(): string | null {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    return token && !this.isTokenExpired(token) ? token : null;
  }

  hasRole(role: string): boolean {
    const payload = this.tokenPayload();
    const roles = payload?.realm_access?.roles;
    return Array.isArray(roles) && roles.includes(role);
  }

  username(): string {
    return this.tokenPayload()?.preferred_username ?? '';
  }

  avatarUrl(): string {
    return this.tokenPayload()?.picture ?? '';
  }

  private storeSession(response: LoginResponse): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    this.authenticatedSignal.set(true);
  }

  private fromKeycloakTokenResponse(response: KeycloakTokenResponse): LoginResponse {
    return {
      accessToken: response.access_token,
      refreshToken: response.refresh_token,
      tokenType: response.token_type,
      expiresIn: response.expires_in,
    };
  }

  private tokenPayload(): any | null {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);

    if (!token || this.isTokenExpired(token)) {
      return null;
    }

    return this.decodeToken(token);
  }

  private hasUsableSession(): boolean {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (token && !this.isTokenExpired(token)) {
      return true;
    }

    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    return !!refreshToken && !this.isTokenExpired(refreshToken);
  }

  private isTokenExpired(token: string, skewSeconds = 0): boolean {
    const payload = this.decodeToken(token);
    const exp = payload?.exp;

    if (!exp) {
      return true;
    }

    const nowInSeconds = Math.floor(Date.now() / 1000);
    return exp <= nowInSeconds + skewSeconds;
  }

  private decodeToken(token: string): any | null {
    try {
      const payloadBase64 = token.split('.')[1];
      const payloadJson = atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(payloadJson);
    } catch {
      return null;
    }
  }

  private keycloakRealmUrl(): string {
    return `${this.keycloakBaseUrl.replace(/\/$/, '')}/realms/${this.keycloakRealm}`;
  }

  private redirectUri(): string {
    return new URL(this.authRedirectPath, window.location.origin).toString();
  }

  private async createPkceChallenge(): Promise<{ verifier: string; challenge: string }> {
    const verifier = this.base64Url(crypto.getRandomValues(new Uint8Array(32)));
    const encoded = new TextEncoder().encode(verifier);
    const digest = await crypto.subtle.digest('SHA-256', encoded);
    return {
      verifier,
      challenge: this.base64Url(new Uint8Array(digest)),
    };
  }

  private base64Url(bytes: Uint8Array): string {
    return btoa(String.fromCharCode(...bytes))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }

  private clearPkceState(): void {
    sessionStorage.removeItem(PKCE_VERIFIER_KEY);
    sessionStorage.removeItem(PKCE_STATE_KEY);
    sessionStorage.removeItem(PKCE_RETURN_URL_KEY);
  }
}
