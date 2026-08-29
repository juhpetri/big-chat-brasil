export interface ConversationResponse {
  id: string;
  recipientId: string;
  recipientName: string;
  lastMessageAt: string | null;
  unreadCount: number;
}
