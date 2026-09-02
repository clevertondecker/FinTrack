export interface ImportInvoiceRequest {
  creditCardId: number;
}

export interface ImportInvoiceResponse {
  message: string;
  importId: number;
  status: ImportStatus;
  source: ImportSource;
  originalFileName: string;
  errorMessage?: string;
  importedAt: string;
  processedAt?: string;
  totalAmount?: number;
  bankName?: string;
  cardLastFourDigits?: string;
}

export interface InvoiceImport {
  id: number;
  creditCardId?: number;
  source: ImportSource;
  originalFileName: string;
  status: ImportStatus;
  importedAt: string;
  processedAt?: string;
  errorMessage?: string;
  extractedText?: string;
  parsedData?: string;
  totalAmount?: number;
  dueDate?: string;
  bankName?: string;
  cardLastFourDigits?: string;
  createdInvoiceId?: number;
}

export enum ImportSource {
  PDF = 'PDF',
  IMAGE = 'IMAGE',
  EMAIL = 'EMAIL',
  MANUAL = 'MANUAL'
}

export enum ImportStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  MANUAL_REVIEW = 'MANUAL_REVIEW',
  PENDING_REVIEW = 'PENDING_REVIEW'
}

export interface InvoiceImportListResponse {
  message: string;
  imports: InvoiceImport[];
  count: number;
}

export interface InvoiceImportDetailResponse {
  message: string;
  import: InvoiceImport;
}

export interface ManualReviewRequest {
  totalAmount: number;
  dueDate: string;
  bankName?: string;
  cardLastFourDigits?: string;
  items: ManualReviewItem[];
}

export interface ManualReviewItem {
  description: string;
  amount: number;
  categoryId?: number;
  purchaseDate: string;
  installments?: number;
  totalInstallments?: number;
}

export interface ManualReviewResponse {
  message: string;
  invoiceId: number;
}

// Multi-card import preview/confirm types

export interface ParsedInvoiceItem {
  description: string;
  amount: number;
  purchaseDate?: string;
  category?: string;
  installments?: number;
  totalInstallments?: number;
  confidence?: number;
}

export interface DetectedCardMapping {
  detectedLastFourDigits: string;
  detectedCardName?: string;
  matchedCreditCardId?: number;
  matchedCardName?: string;
  autoMatched: boolean;
  ambiguous: boolean;
  candidateCardIds: number[];
  items: ParsedInvoiceItem[];
  subtotal: number;
  declaredTotal?: number;
}

export type ReconciliationStatus = 'RECONCILED' | 'DIVERGENT' | 'REVIEW_REQUIRED' | 'NOT_APPLICABLE';

export interface ImportReconciliation {
  status: ReconciliationStatus;
  message: string;
  difference?: number;
}

export interface ImportPreviewResponse {
  importId: number;
  bankName?: string;
  invoiceMonth?: string;
  dueDate?: string;
  totalAmount?: number;
  confidence?: number;
  detectedCards: DetectedCardMapping[];
  allCardsMatched: boolean;
  reconciliation?: ImportReconciliation;
}

export interface CardMapping {
  detectedLastFourDigits: string;
  creditCardId: number;
}

export interface ConfirmImportRequest {
  cardMappings: CardMapping[];
}

export interface ConfirmImportResponse {
  message: string;
  importId: number;
  createdInvoiceIds: number[];
  itemsImported: number;
}
