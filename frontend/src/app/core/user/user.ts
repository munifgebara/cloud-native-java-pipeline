import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface User {
  id: string;
  username: string;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  enabled: boolean;
  roles: string[];
}

export interface MeuPerfil {
  id: string;
  username: string;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  roles: string[];
  alteracaoSenhaUrl: string;
}

export interface UserCreateRequest {
  username: string;
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
  password: string;
  enabled: boolean;
  roles: string[];
}

export interface UserUpdateRequest {
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
  enabled: boolean;
  roles: string[];
}

export interface MeuPerfilUpdateRequest {
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
}

export interface AlterarSenhaRequest {
  currentPassword: string;
  newPassword: string;
}

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/users`;

  listar(): Observable<User[]> {
    return this.http.get<User[]>(this.baseUrl);
  }

  buscarPorId(id: string): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/${id}`);
  }

  criar(payload: UserCreateRequest): Observable<User> {
    return this.http.post<User>(this.baseUrl, payload);
  }

  update(id: string, payload: UserUpdateRequest): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/${id}`, payload);
  }

  alterarStatus(id: string, enabled: boolean): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/status`, { enabled });
  }

  meuPerfil(): Observable<MeuPerfil> {
    return this.http.get<MeuPerfil>(`${this.baseUrl}/me`);
  }

  atualizarMeuPerfil(payload: MeuPerfilUpdateRequest): Observable<MeuPerfil> {
    return this.http.put<MeuPerfil>(`${this.baseUrl}/me`, payload);
  }

  alterarMinhaSenha(payload: AlterarSenhaRequest): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/me/password`, payload);
  }
}
