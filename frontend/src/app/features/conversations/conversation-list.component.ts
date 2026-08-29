import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, afterNextRender, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { ConversationsService } from '../../core/conversations.service';

@Component({
  selector: 'app-conversation-list',
  standalone: true,
  imports: [DatePipe, RouterLink, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './conversation-list.component.html',
  styleUrl: './conversation-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConversationListComponent {
  protected readonly conversationsService = inject(ConversationsService);
  private readonly router = inject(Router);

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
}
