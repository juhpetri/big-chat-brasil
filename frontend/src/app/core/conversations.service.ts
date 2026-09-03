import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { ConversationResponse } from './conversations.models';

export type ConversationsState = 'idle' | 'loading' | 'error';

@Injectable({ providedIn: 'root' })
export class ConversationsService {
  private readonly http = inject(HttpClient);

  readonly conversations = signal<ConversationResponse[]>([]);
  readonly state = signal<ConversationsState>('idle');

  reset(): void {
    this.conversations.set([]);
    this.state.set('idle');
  }

  load(): void {
    this.state.set('loading');
    this.http.get<ConversationResponse[]>('/api/conversations').subscribe({
      next: (data) => {
        this.conversations.set(data);
        this.state.set('idle');
      },
      error: () => this.state.set('error'),
    });
  }
}
