export interface Invoice {
  id: number;
  creditCardId: number;
  creditCardName: string;
  dueDate: string;
  totalAmount: number | null;
  paidAmount: number | null;
  status: InvoiceStatus;
  createdAt: string;
  updatedAt?: string;
}

export interface InvoiceItem {
  id: number;
  invoiceId: number;
  description: string;
  amount: number;
  category?: string;
  purchaseDate: string;
  installments?: number;
  totalInstallments?: number;
  createdAt: string;
}

export interface CreateInvoiceRequest {
  creditCardId: number;
  dueDate: string;
}

export interface CreateInvoiceItemRequest {
  description: string;
  amount: number;
  categoryId?: number;
  purchaseDate: string;
}

export interface InvoiceResponse {
  message: string;
  invoices: Invoice[];
  count: number;
}

export interface InvoiceDetailResponse {
  message: string;
  invoice: Invoice;
}

export interface InvoiceItemsResponse {
  message: string;
  invoiceId: number;
  items: InvoiceItem[];
  count: number;
  totalAmount: number;
}

export interface CreateInvoiceResponse {
  message: string;
  id: number;
  creditCardId: number;
  dueDate: string;
  totalAmount: number;
  status: string;
}

export interface CreateInvoiceItemResponse {
  message: string;
  id: number;
  invoiceId: number;
  description: string;
  amount: number;
  category: string;
  invoiceTotalAmount: number;
}

export enum InvoiceStatus {
  OPEN = 'OPEN',
  PAID = 'PAID',
  OVERDUE = 'OVERDUE',
  PARTIAL = 'PARTIAL'
}

export const InvoiceStatusDisplay = {
  [InvoiceStatus.OPEN]: 'Aberta',
  [InvoiceStatus.PAID]: 'Paga',
  [InvoiceStatus.OVERDUE]: 'Vencida',
  [InvoiceStatus.PARTIAL]: 'Parcial'
};

export interface Category {
  id: number;
  name: string;
  color?: string;
}

export interface InvoicePaymentRequest {
  amount: number;
}

export interface InvoicePaymentResponse {
  id: number;
  creditCardId: number;
  creditCardName: string;
  dueDate: string;
  totalAmount: number;
  paidAmount: number;
  status: string;
  updatedAt: string;
  message: string;
} 