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

  private requestSequence = 0;

  loadHistory(
    conversationId: string,
    options: { background?: boolean } = {},
    onSettled?: () => void,
  ): void {
    const requestId = ++this.requestSequence;

    if (!options.background) {
      this.historyState.set('loading');
    }
    this.http.get<MessageResponse[]>(`/api/conversations/${conversationId}/messages`).subscribe({
      next: (data) => {
        if (requestId !== this.requestSequence) {
          return;
        }
        this.messages.set(data);
        this.historyState.set('idle');
        onSettled?.();
      },
      error: () => {
        if (requestId !== this.requestSequence) {
          return;
        }
        if (!options.background) {
          this.historyState.set('error');
        }
        onSettled?.();
      },
    });
  }

  reset(): void {
    this.messages.set([]);
    this.historyState.set('idle');
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
