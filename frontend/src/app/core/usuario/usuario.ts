import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Usuario {
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

export interface UsuarioCreateRequest {
  username: string;
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
  password: string;
  enabled: boolean;
  roles: string[];
}

export interface UsuarioUpdateRequest {
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
  senhaAtual: string;
  novaSenha: string;
}

@Injectable({
  providedIn: 'root',
})
export class UsuarioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v0/usuarios`;

  listar(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.baseUrl);
  }

  buscarPorId(id: string): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.baseUrl}/${id}`);
  }

  criar(payload: UsuarioCreateRequest): Observable<Usuario> {
    return this.http.post<Usuario>(this.baseUrl, payload);
  }

  atualizar(id: string, payload: UsuarioUpdateRequest): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.baseUrl}/${id}`, payload);
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
    return this.http.put<void>(`${this.baseUrl}/me/senha`, payload);
  }
}
