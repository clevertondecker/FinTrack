export type BillingCycle = 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUAL' | 'ANNUAL';
export type SubscriptionStatus = 'ACTIVE' | 'PAUSED' | 'CANCELLED';
export type SubscriptionSource = 'AUTO_DETECTED' | 'MANUAL';
export type MonthStatus = 'DETECTED' | 'MISSED' | 'PRICE_CHANGED' | 'PAUSED' | 'CANCELLED' | 'ACTIVE';

export interface SubscriptionResponse {
  id: number;
  name: string;
  merchantKey: string;
  expectedAmount: number;
  category: {
    id: number | null;
    name: string;
    color: string;
  } | null;
  creditCardName: string | null;
  creditCardId: number | null;
  billingCycle: BillingCycle;
  status: SubscriptionStatus;
  source: SubscriptionSource;
  startDate: string | null;
  lastDetectedDate: string | null;
  lastDetectedAmount: number | null;
  priceChanged: boolean;
  monthStatus: MonthStatus;
}

export interface SubscriptionSuggestion {
  merchantKey: string;
  displayName: string;
  averageAmount: number;
  occurrences: number;
  firstSeen: string;
  lastSeen: string;
  categoryName: string | null;
  categoryColor: string | null;
  cardName: string | null;
}

export interface CreateSubscriptionRequest {
  name: string;
  merchantKey: string;
  expectedAmount: number;
  billingCycle: BillingCycle;
  categoryId?: number;
  creditCardId?: number;
}

export interface UpdateSubscriptionRequest {
  name: string;
  expectedAmount: number;
  billingCycle: BillingCycle;
  categoryId?: number;
  creditCardId?: number;
}
