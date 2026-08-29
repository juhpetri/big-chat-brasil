export type DocumentType = 'CPF' | 'CNPJ';
export type PlanType = 'PREPAID' | 'POSTPAID';

export interface DocumentId {
  document: string;
  documentType: DocumentType;
}

export interface ClientResponse {
  id: string;
  name: string;
  documentId: DocumentId;
  planType: PlanType;
  balance: number | null;
  limit: number | null;
  active: boolean;
}

export interface AuthResponse {
  token: string;
  client: ClientResponse;
}
