export type MessagePriority = 'NORMAL' | 'URGENT';
export type MessageStatus = 'QUEUED' | 'PROCESSING' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED';
export type SenderType = 'CLIENT' | 'USER';

export interface MessageResponse {
  id: string;
  content: string;
  priority: MessagePriority;
  status: MessageStatus;
  cost: number;
  sentByType: SenderType;
  queuedAt: string;
  processedAt: string | null;
}

export interface SendMessageRequest {
  recipientId: string;
  recipientName?: string;
  content: string;
  priority: MessagePriority;
}

export interface SendMessageResponse {
  id: string;
  conversationId: string;
  status: MessageStatus;
  timestamp: string;
  estimatedDelivery: string;
  cost: number;
  currentBalance: number | null;
}
