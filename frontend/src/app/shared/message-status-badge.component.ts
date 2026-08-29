import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MessageStatus } from '../core/chat.models';

interface StatusMeta {
  label: string;
  className: string;
}

const STATUS_META: Record<MessageStatus, StatusMeta> = {
  QUEUED: { label: 'na fila', className: 'status-queued' },
  PROCESSING: { label: 'processando', className: 'status-processing' },
  SENT: { label: 'enviada', className: 'status-sent' },
  DELIVERED: { label: 'entregue', className: 'status-delivered' },
  READ: { label: 'lida', className: 'status-read' },
  FAILED: { label: 'falhou', className: 'status-failed' },
};

@Component({
  selector: 'app-message-status-badge',
  standalone: true,
  template: `<span class="status-badge" [class]="meta().className">{{ meta().label }}</span>`,
  styleUrl: './message-status-badge.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MessageStatusBadgeComponent {
  readonly status = input.required<MessageStatus>();

  protected readonly meta = computed(() => STATUS_META[this.status()]);
}
