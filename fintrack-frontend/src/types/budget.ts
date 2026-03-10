import { CategoryResponse } from './expenseReport';

export interface CreateBudgetRequest {
  categoryId: number | null;
  limitAmount: number;
  month?: string;
}

export interface UpdateBudgetRequest {
  limitAmount: number;
}

export interface BudgetResponse {
  id: number;
  category: CategoryResponse | null;
  limitAmount: number;
  month: string | null;
  recurring: boolean;
  active: boolean;
}

export interface BudgetStatusResponse {
  budgetId: number;
  category: CategoryResponse | null;
  budgetLimit: number;
  actualSpent: number;
  remaining: number;
  utilizationPercent: number;
  status: string;
}
