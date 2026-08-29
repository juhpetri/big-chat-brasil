import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { MessageResponse, SendMessageRequest, SendMessageResponse } from './chat.models';

export type ChatHistoryState = 'idle' | 'loading' | 'error';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);

  readonly messages = signal<MessageResponse[]>([]);
  readonly historyState = signal<ChatHistoryState>('idle');

  loadHistory(conversationId: string): void {
    this.historyState.set('loading');
    this.http.get<MessageResponse[]>(`/api/conversations/${conversationId}/messages`).subscribe({
      next: (data) => {
        this.messages.set(data);
        this.historyState.set('idle');
      },
      error: () => this.historyState.set('error'),
    });
  }

  addOptimistic(message: MessageResponse): void {
    this.messages.update((list) => [...list, message]);
  }

  replaceOptimistic(tempId: string, real: MessageResponse): void {
    this.messages.update((list) => list.map((m) => (m.id === tempId ? real : m)));
  }

  removeOptimistic(tempId: string): void {
    this.messages.update((list) => list.filter((m) => m.id !== tempId));
  }

  sendMessage(request: SendMessageRequest): Observable<SendMessageResponse> {
    return this.http.post<SendMessageResponse>('/api/messages', request);
  }
}
