export interface CategoryResponse {
  id: number | null;
  name: string;
  color: string;
}

export interface ExpenseDetailResponse {
  shareId: number | null;
  itemId: number;
  itemDescription: string;
  amount: number;
  purchaseDate: string;
  invoiceId: number;
}

export interface ExpenseByCategoryResponse {
  category: CategoryResponse;
  totalAmount: number;
  transactionCount: number;
  details?: ExpenseDetailResponse[];
}

export interface CategoryExpenseSummary {
  categoryId: number | null;
  categoryName: string;
  categoryColor: string;
  totalAmount: number;
  transactionCount: number;
}

export interface ExpenseReportResponse {
  user: {
    id: number;
    name: string;
    email: string;
    roles: string[];
    createdAt: string;
    updatedAt: string;
  };
  month: string;
  expensesByCategory: ExpenseByCategoryResponse[];
  totalAmount: number;
}

export interface MonthlyExpenseData {
  month: string;
  totalAmount: number;
  categories: CategoryExpenseSummary[];
}

export interface ExpenseTrendsResponse {
  user: {
    id: number;
    name: string;
    email: string;
  };
  months: MonthlyExpenseData[];
}

export interface TopExpenseItem {
  rank: number;
  itemId: number;
  description: string;
  amount: number;
  purchaseDate: string;
  invoiceId: number;
  category: CategoryResponse | null;
  percentageOfTotal: number;
}

export interface TopExpensesResponse {
  user: {
    id: number;
    name: string;
    email: string;
  };
  month: string;
  totalAmount: number;
  topExpenses: TopExpenseItem[];
}

