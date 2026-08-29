import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, afterNextRender, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { ConversationsService } from '../../core/conversations.service';
import { ChatService } from '../../core/chat.service';
import { MessagePriority, SendMessageRequest } from '../../core/chat.models';

@Component({
  selector: 'app-conversation-list',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './conversation-list.component.html',
  styleUrl: './conversation-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConversationListComponent {
  protected readonly conversationsService = inject(ConversationsService);
  private readonly chatService = inject(ChatService);
  private readonly router = inject(Router);

  protected readonly showNewConversationForm = signal(false);
  protected readonly recipientId = signal('');
  protected readonly recipientName = signal('');
  protected readonly draft = signal('');
  protected readonly priority = signal<MessagePriority>('NORMAL');
  protected readonly sending = signal(false);
  protected readonly formError = signal('');

  constructor() {
    // Roda só no browser, mesmo padrão já usado no projeto pra chamadas HTTP no boot (evita quebrar SSR/prerender).
    afterNextRender(() => this.conversationsService.load());
  }

  protected retry(): void {
    this.conversationsService.load();
  }

  protected open(conversationId: string): void {
    this.router.navigate(['/conversations', conversationId]);
  }

  protected toggleNewConversationForm(): void {
    this.showNewConversationForm.update((shown) => !shown);
    this.formError.set('');
  }

  protected startConversation(): void {
    const recipientId = this.recipientId().trim();
    const content = this.draft().trim();
    if (!recipientId || !content) {
      return;
    }

    const request: SendMessageRequest = {
      recipientId,
      recipientName: this.recipientName().trim() || undefined,
      content,
      priority: this.priority(),
    };

    this.sending.set(true);
    this.formError.set('');
    this.chatService.sendMessage(request).subscribe({
      next: (response) => {
        this.sending.set(false);
        this.showNewConversationForm.set(false);
        this.recipientId.set('');
        this.recipientName.set('');
        this.draft.set('');
        this.conversationsService.load();
        this.router.navigate(['/conversations', response.conversationId]);
      },
      error: (err: HttpErrorResponse) => {
        this.sending.set(false);
        this.formError.set(err.error?.message ?? 'Não foi possível iniciar a conversa agora.');
      },
    });
  }
}
