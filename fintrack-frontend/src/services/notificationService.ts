import apiService from './api';
import { CreditCardOverview } from '../types/dashboard';
import { BudgetStatusResponse } from '../types/budget';
import { SubscriptionSuggestion } from '../types/subscription';

export type NotificationSeverity = 'urgent' | 'warning' | 'info';
export type NotificationType =
  | 'OVERDUE_INVOICE'
  | 'UPCOMING_INVOICE'
  | 'BUDGET_EXCEEDED'
  | 'BUDGET_NEAR_LIMIT'
  | 'PENDING_SHARES'
  | 'SUBSCRIPTION_SUGGESTION';

export interface AppNotification {
  id: string;
  type: NotificationType;
  severity: NotificationSeverity;
  titleKey: string;
  descriptionKey: string;
  descriptionParams?: Record<string, string | number>;
  route: string;
}

const UPCOMING_DAYS_THRESHOLD = 5;

function buildInvoiceNotifications(cards: CreditCardOverview[]): AppNotification[] {
  const notifications: AppNotification[] = [];

  cards.forEach((card) => {
    if (card.invoiceStatus === 'OVERDUE') {
      notifications.push({
        id: `overdue-${card.cardId}`,
        type: 'OVERDUE_INVOICE',
        severity: 'urgent',
        titleKey: 'notifications.overdueInvoice',
        descriptionKey: 'notifications.overdueInvoiceDesc',
        descriptionParams: { card: card.cardName },
        route: '/dashboard/invoices',
      });
    }

    if (card.invoiceStatus === 'OPEN' && card.dueDate) {
      const due = new Date(card.dueDate + 'T00:00:00');
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const diffDays = Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
      if (diffDays >= 0 && diffDays <= UPCOMING_DAYS_THRESHOLD) {
        notifications.push({
          id: `upcoming-${card.cardId}`,
          type: 'UPCOMING_INVOICE',
          severity: 'warning',
          titleKey: 'notifications.upcomingInvoice',
          descriptionKey: 'notifications.upcomingInvoiceDesc',
          descriptionParams: { card: card.cardName, days: diffDays },
          route: '/dashboard/invoices',
        });
      }
    }
  });

  return notifications;
}

function buildBudgetNotifications(budgets: BudgetStatusResponse[]): AppNotification[] {
  const notifications: AppNotification[] = [];

  budgets.forEach((b) => {
    const label = b.category?.name ?? 'Total';
    if (b.status === 'OVER_BUDGET') {
      notifications.push({
        id: `budget-over-${b.budgetId}`,
        type: 'BUDGET_EXCEEDED',
        severity: 'urgent',
        titleKey: 'notifications.budgetExceeded',
        descriptionKey: 'notifications.budgetExceededDesc',
        descriptionParams: { category: label, percent: Math.round(b.utilizationPercent) },
        route: '/dashboard/budgets',
      });
    } else if (b.status === 'NEAR_LIMIT') {
      notifications.push({
        id: `budget-near-${b.budgetId}`,
        type: 'BUDGET_NEAR_LIMIT',
        severity: 'warning',
        titleKey: 'notifications.budgetNearLimit',
        descriptionKey: 'notifications.budgetNearLimitDesc',
        descriptionParams: { category: label, percent: Math.round(b.utilizationPercent) },
        route: '/dashboard/budgets',
      });
    }
  });

  return notifications;
}

function buildShareNotifications(unpaidCount: number, totalAmount: number): AppNotification[] {
  if (unpaidCount === 0) return [];
  return [
    {
      id: 'pending-shares',
      type: 'PENDING_SHARES',
      severity: 'info',
      titleKey: 'notifications.pendingShares',
      descriptionKey: 'notifications.pendingSharesDesc',
      descriptionParams: { count: unpaidCount, amount: totalAmount.toFixed(2) },
      route: '/dashboard/my-shares',
    },
  ];
}

function buildSuggestionNotifications(suggestions: SubscriptionSuggestion[]): AppNotification[] {
  if (suggestions.length === 0) return [];
  return [
    {
      id: 'sub-suggestions',
      type: 'SUBSCRIPTION_SUGGESTION',
      severity: 'info',
      titleKey: 'notifications.subscriptionSuggestions',
      descriptionKey: 'notifications.subscriptionSuggestionsDesc',
      descriptionParams: { count: suggestions.length },
      route: '/dashboard/invoices',
    },
  ];
}

const SEVERITY_ORDER: Record<NotificationSeverity, number> = { urgent: 0, warning: 1, info: 2 };

export async function fetchNotifications(): Promise<AppNotification[]> {
  const results = await Promise.allSettled([
    apiService.getDashboardOverview(),
    apiService.getBudgets(),
    apiService.getMyShares(),
    apiService.getSubscriptionSuggestions(),
  ]);

  const notifications: AppNotification[] = [];

  if (results[0].status === 'fulfilled') {
    notifications.push(...buildInvoiceNotifications(results[0].value.creditCards));
  }
  if (results[1].status === 'fulfilled') {
    notifications.push(...buildBudgetNotifications(results[1].value));
  }
  if (results[2].status === 'fulfilled') {
    const shares = results[2].value.shares;
    const unpaid = shares.filter((s) => !s.isPaid);
    const totalAmount = unpaid.reduce((sum, s) => sum + s.myAmount, 0);
    notifications.push(...buildShareNotifications(unpaid.length, totalAmount));
  }
  if (results[3].status === 'fulfilled') {
    notifications.push(...buildSuggestionNotifications(results[3].value));
  }

  notifications.sort((a, b) => SEVERITY_ORDER[a.severity] - SEVERITY_ORDER[b.severity]);

  return notifications;
}
