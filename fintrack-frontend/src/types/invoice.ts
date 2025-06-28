export interface Invoice {
  id: number;
  creditCardId: number;
  creditCardName: string;
  dueDate: string;
  totalAmount: number | null;
  paidAmount: number | null;
  status: string;
  createdAt: string;
  updatedAt?: string;
}

export interface InvoiceItem {
  id: number;
  invoiceId: number;
  description: string;
  amount: number;
  category: string | null;
  purchaseDate: string;
  createdAt: string;
  installments?: number;
  totalInstallments?: number;
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

export interface InvoiceFilters {
  status?: string;
  creditCardId?: number;
  dateFrom?: string;
  dateTo?: string;
  minAmount?: number;
  maxAmount?: number;
}

export interface InvoiceSummary {
  totalInvoices: number;
  totalAmount: number;
  totalPaid: number;
  totalRemaining: number;
  overdueCount: number;
  overdueAmount: number;
  openCount: number;
  openAmount: number;
  partialCount: number;
  partialAmount: number;
  paidCount: number;
  paidAmount: number;
}

export interface GroupedInvoices {
  overdue: Invoice[];
  open: Invoice[];
  partial: Invoice[];
  paid: Invoice[];
  closed: Invoice[];
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
  PARTIAL = 'PARTIAL',
  CLOSED = 'CLOSED'
}

export const InvoiceStatusDisplay = {
  [InvoiceStatus.OPEN]: 'Aberta',
  [InvoiceStatus.PAID]: 'Paga',
  [InvoiceStatus.OVERDUE]: 'Vencida',
  [InvoiceStatus.PARTIAL]: 'Parcial',
  [InvoiceStatus.CLOSED]: 'Fechada'
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