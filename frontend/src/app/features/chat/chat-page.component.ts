import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { ChatService } from '../../core/chat.service';
import { MessagePriority, MessageResponse, MessageStatus, SendMessageRequest } from '../../core/chat.models';
import { ConversationsService } from '../../core/conversations.service';
import { MessageStatusBadgeComponent } from '../../shared/message-status-badge.component';

const POLL_INTERVAL_MS = 2000;
const NON_TERMINAL_STATUSES: MessageStatus[] = ['QUEUED', 'PROCESSING', 'SENT', 'DELIVERED'];

@Component({
  selector: 'app-chat-page',
  standalone: true,
  imports: [
    DecimalPipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MessageStatusBadgeComponent,
  ],
  templateUrl: './chat-page.component.html',
  styleUrl: './chat-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly chatService = inject(ChatService);
  protected readonly conversationsService = inject(ConversationsService);

  // Reativo (não snapshot): o Router reaproveita a mesma instância de ChatPageComponent ao
  // navegar entre /conversations/:id1 e /conversations/:id2 (rotas irmãs), então um snapshot
  // capturado só na construção nunca atualizava — o histórico ficava travado na primeira
  // conversa aberta.
  protected readonly conversationId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('id')!)),
    { requireSync: true },
  );

  protected readonly conversation = computed(
    () => this.conversationsService.conversations().find((c) => c.id === this.conversationId()) ?? null,
  );

  protected readonly draft = signal('');
  protected readonly priority = signal<MessagePriority>('NORMAL');
  protected readonly sendError = signal('');
  protected readonly searchQuery = signal('');

  protected readonly cost = computed(() => (this.priority() === 'URGENT' ? 0.5 : 0.25));

  protected readonly chatBody = viewChild<ElementRef<HTMLDivElement>>('chatBody');

  protected readonly filteredMessages = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    if (!query) {
      return this.chatService.messages();
    }
    return this.chatService.messages().filter((message) => message.content.toLowerCase().includes(query));
  });

  private pollTimer: ReturnType<typeof setTimeout> | null = null;
  private previousConversationId: string | undefined;

  constructor() {
    afterNextRender(() => {
      if (this.conversationsService.conversations().length === 0) {
        this.conversationsService.load();
      }
    });

    effect(() => {
      const id = this.conversationId();
      const isConversationSwitch = this.previousConversationId !== undefined;
      this.previousConversationId = id;

      if (isConversationSwitch) {
        this.clearPoll();
        this.searchQuery.set('');
      }
      this.chatService.loadHistory(id);
    });

    effect(() => {
      const hasNonTerminal = this.chatService
        .messages()
        .some((message) => NON_TERMINAL_STATUSES.includes(message.status));

      if (hasNonTerminal && this.pollTimer === null) {
        this.schedulePoll();
      }
    });

    effect(() => {
      this.filteredMessages();
      const el = this.chatBody()?.nativeElement;
      if (!el) {
        return;
      }
      queueMicrotask(() => {
        el.scrollTop = el.scrollHeight;
      });
    });

    this.destroyRef.onDestroy(() => this.clearPoll());
  }

  private schedulePoll(): void {
    this.pollTimer = setTimeout(() => {
      this.pollTimer = null;
      this.chatService.loadHistory(this.conversationId(), { background: true });
    }, POLL_INTERVAL_MS);
  }

  private clearPoll(): void {
    if (this.pollTimer !== null) {
      clearTimeout(this.pollTimer);
      this.pollTimer = null;
    }
  }

  protected reloadHistory(): void {
    this.chatService.loadHistory(this.conversationId());
  }

  protected submit(): void {
    const content = this.draft().trim();
    const conversation = this.conversation();
    if (!content || !conversation) {
      return;
    }

    const tempId = crypto.randomUUID();
    const priority = this.priority();
    const optimisticMessage: MessageResponse = {
      id: tempId,
      content,
      priority,
      status: 'QUEUED',
      cost: this.cost(),
      sentByType: 'CLIENT',
      queuedAt: new Date().toISOString(),
      processedAt: null,
    };

    this.chatService.addOptimistic(optimisticMessage);
    this.draft.set('');
    this.sendError.set('');

    const request: SendMessageRequest = {
      recipientId: conversation.recipientId,
      recipientName: conversation.recipientName,
      content,
      priority,
    };

    this.chatService.sendMessage(request).subscribe({
      next: (response) => {
        this.chatService.replaceOptimistic(tempId, {
          id: response.id,
          content,
          priority,
          status: response.status,
          cost: response.cost,
          sentByType: 'CLIENT',
          queuedAt: response.timestamp,
          processedAt: null,
        });
      },
      error: (err: HttpErrorResponse) => {
        this.chatService.removeOptimistic(tempId);
        this.sendError.set(this.messageFor(err));
      },
    });
  }

  private messageFor(err: HttpErrorResponse): string {
    const backendMessage: string | undefined = err.error?.message;
    if (err.status === 402) {
      return backendMessage ?? 'Saldo insuficiente para enviar esta mensagem.';
    }
    if (err.status === 400) {
      return backendMessage ?? 'Limite mensal excedido.';
    }
    return 'Não foi possível enviar a mensagem agora. Tente novamente.';
  }
}
