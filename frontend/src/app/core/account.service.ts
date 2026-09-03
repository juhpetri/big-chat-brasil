import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ClientResponse, PlanType } from './auth.models';
import { AuthService } from './auth.service';

export interface TransactionResponse {
  id: string;
  messageId: string | null;
  type: 'DEBIT' | 'CREDIT';
  amount: number;
  description: string | null;
  timestamp: string;
}

export type AccountState = 'idle' | 'loading' | 'error';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  readonly transactions = signal<TransactionResponse[]>([]);
  readonly state = signal<AccountState>('idle');

  refreshClient(): void {
    this.http.get<ClientResponse>(`/api/clients/${this.clientId()}`).subscribe({
      next: (client) => this.authService.updateCurrentClient(client),
    });
  }

  loadTransactions(): void {
    this.state.set('loading');
    this.http.get<TransactionResponse[]>(`/api/clients/${this.clientId()}/transactions`).subscribe({
      next: (data) => {
        this.transactions.set(data);
        this.state.set('idle');
      },
      error: () => this.state.set('error'),
    });
  }

  addCredit(amount: number): Observable<ClientResponse> {
    return this.http
      .post<ClientResponse>(`/api/clients/${this.clientId()}/credit`, { amount })
      .pipe(tap((client) => this.authService.updateCurrentClient(client)));
  }

  adjustLimit(newLimit: number): Observable<ClientResponse> {
    return this.http
      .post<ClientResponse>(`/api/clients/${this.clientId()}/limit`, { newLimit })
      .pipe(tap((client) => this.authService.updateCurrentClient(client)));
  }

  convertPlan(newPlanType: PlanType, initialValue: number): Observable<ClientResponse> {
    return this.http
      .post<ClientResponse>(`/api/clients/${this.clientId()}/plan`, { newPlanType, initialValue })
      .pipe(tap((client) => this.authService.updateCurrentClient(client)));
  }

  private clientId(): string {
    const client = this.authService.currentClient();
    if (!client) {
      throw new Error('Nenhum cliente autenticado.');
    }
    return client.id;
  }
}
