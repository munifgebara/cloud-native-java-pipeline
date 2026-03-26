import { Injectable, computed, signal } from '@angular/core';

const AUTH_KEY = 'pagadoria_authenticated';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly authenticatedSignal = signal<boolean>(
    localStorage.getItem(AUTH_KEY) === 'true'
  );

  readonly authenticated = computed(() => this.authenticatedSignal());

  login(username: string, password: string): boolean {
    const user = username.trim();
    const pass = password.trim();

    if (!user || !pass) {
      return false;
    }

    localStorage.setItem(AUTH_KEY, 'true');
    this.authenticatedSignal.set(true);
    return true;
  }

  logout(): void {
    localStorage.removeItem(AUTH_KEY);
    this.authenticatedSignal.set(false);
  }

  isAuthenticated(): boolean {
    return this.authenticatedSignal();
  }
}
