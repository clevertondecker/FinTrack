export interface CreditCardOverview {
  cardId: number;
  cardName: string;
  lastFourDigits: string;
  bankName: string;
  bankCode: string;
  dueDate: string | null;
  currentInvoiceAmount: number;
  nextInvoiceAmount: number;
  invoiceStatus: string;
}

export interface CategoryRanking {
  categoryId: number | null;
  categoryName: string;
  color: string;
  amount: number;
  percentage: number;
  transactionCount: number;
}

export interface DailyExpense {
  date: string;
  amount: number;
}

export interface DashboardOverviewResponse {
  user: {
    id: number;
    name: string;
    email: string;
    roles: string[];
    createdAt: string;
    updatedAt: string;
  };
  month: string;
  totalExpenses: number;
  totalExpensesGross: number;
  totalTransactions: number;
  creditCards: CreditCardOverview[];
  categoryRanking: CategoryRanking[];
  dailyExpenses: DailyExpense[];
}
