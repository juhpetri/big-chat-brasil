import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthResponse, ClientResponse } from './auth.models';

const TOKEN_KEY = 'bcb_token';
const CLIENT_KEY = 'bcb_client';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly token = signal<string | null>(this.readStoredToken());
  readonly currentClient = signal<ClientResponse | null>(this.readStoredClient());

  readonly isAuthenticated = computed(() => this.token() !== null);

  login(document: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth', { document })
      .pipe(tap((response) => this.setSession(response)));
  }

  logout(): void {
    this.token.set(null);
    this.currentClient.set(null);
    this.clearStorage();
  }

  getToken(): string | null {
    return this.token();
  }

  updateCurrentClient(client: ClientResponse): void {
    this.currentClient.set(client);
    if (this.hasStorage()) {
      localStorage.setItem(CLIENT_KEY, JSON.stringify(client));
    }
  }

  private setSession(response: AuthResponse): void {
    this.token.set(response.token);
    this.currentClient.set(response.client);

    if (this.hasStorage()) {
      localStorage.setItem(TOKEN_KEY, response.token);
      localStorage.setItem(CLIENT_KEY, JSON.stringify(response.client));
    }
  }

  private clearStorage(): void {
    if (this.hasStorage()) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(CLIENT_KEY);
    }
  }

  private readStoredToken(): string | null {
    return this.hasStorage() ? localStorage.getItem(TOKEN_KEY) : null;
  }

  private readStoredClient(): ClientResponse | null {
    if (!this.hasStorage()) {
      return null;
    }
    const raw = localStorage.getItem(CLIENT_KEY);
    return raw ? (JSON.parse(raw) as ClientResponse) : null;
  }

  private hasStorage(): boolean {
    return typeof localStorage !== 'undefined';
  }
}
