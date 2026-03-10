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

export interface DailyExpenseData {
  date: string;
  amount: number;
}

export interface ExpenseByCategoryResponse {
  category: CategoryResponse;
  totalAmount: number;
  percentage: number;
  transactionCount: number;
  details?: ExpenseDetailResponse[];
  dailyBreakdown?: DailyExpenseData[];
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
  averageMonthly: number | null;
  currentVsAveragePercent: number | null;
  currentVsPreviousMonthPercent: number | null;
  highestMonthAmount: number | null;
  lowestMonthAmount: number | null;
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

export interface ExpenseByCardResponse {
  cardId: number;
  cardName: string;
  lastFourDigits: string;
  bankName: string;
  totalAmount: number;
  percentage: number;
  transactionCount: number;
  categories: CategoryExpenseSummary[];
}

export interface ExpenseByRecurrenceResponse {
  type: string;
  amount: number;
  percentage: number;
  transactionCount: number;
}

export interface PeriodComparisonResponse {
  current: { month: string; totalAmount: number; transactionCount: number };
  comparison: { month: string; totalAmount: number; transactionCount: number };
  differenceAmount: number;
  differencePercentage: number;
  categoryComparisons: CategoryComparison[];
}

export interface CategoryComparison {
  category: CategoryResponse;
  currentAmount: number;
  comparisonAmount: number;
  differenceAmount: number;
  differencePercentage: number;
}
