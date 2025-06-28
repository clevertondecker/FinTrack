export interface ImportInvoiceRequest {
  creditCardId: number;
}

export interface ImportInvoiceResponse {
  id: number;
  message: string;
  status: string;
}

export interface InvoiceImport {
  id: number;
  userId: number;
  creditCardId?: number;
  source: ImportSource;
  originalFileName: string;
  filePath: string;
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
  MANUAL_REVIEW = 'MANUAL_REVIEW'
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