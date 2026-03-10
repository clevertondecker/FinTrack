export interface ExpenseSearchParams {
  query?: string;
  categoryId?: number;
  cardId?: number;
  dateFrom?: string;
  dateTo?: string;
  amountMin?: number;
  amountMax?: number;
  page: number;
  size: number;
}

export interface ExpenseSearchResult {
  itemId: number;
  invoiceId: number;
  description: string;
  amount: number;
  purchaseDate: string;
  category: {
    id: number | null;
    name: string;
    color: string;
  };
  cardName: string;
  lastFourDigits: string;
  invoiceMonth: string;
  installments: number;
  totalInstallments: number;
}

export interface ExpenseSearchResponse {
  results: ExpenseSearchResult[];
  totalResults: number;
  page: number;
  totalPages: number;
  totalAmount: number;
}
