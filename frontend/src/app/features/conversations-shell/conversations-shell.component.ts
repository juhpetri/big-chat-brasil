import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConversationListComponent } from '../conversations/conversation-list.component';

@Component({
  selector: 'app-conversations-shell',
  standalone: true,
  imports: [RouterOutlet, ConversationListComponent],
  templateUrl: './conversations-shell.component.html',
  styleUrl: './conversations-shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConversationsShellComponent {
  // Alimentado pelos eventos (activate)/(deactivate) do router-outlet do chat —
  // é assim que o shell sabe, sem duplicar lógica de rota, se uma conversa está aberta
  // (controla tanto o placeholder do painel direito quanto o layout mobile de 1 coluna).
  protected readonly chatActive = signal(false);
}
