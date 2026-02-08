export interface CreditCard {
  id: number;
  name: string;
  lastFourDigits: string;
  limit: number;
  bankName: string;
  bankCode?: string;
  active: boolean;
  cardType: 'PHYSICAL' | 'VIRTUAL' | 'ADDITIONAL';
  parentCardId?: number;
  parentCardName?: string;
  cardholderName?: string;
  assignedUserId?: number | null;
  assignedUserName?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCreditCardRequest {
  name: string;
  lastFourDigits: string;
  limit: number;
  bankId: number;
  cardType: 'PHYSICAL' | 'VIRTUAL' | 'ADDITIONAL';
  parentCardId?: number;
  cardholderName?: string;
  /** ID of the user this card is assigned to (who uses it). Omit or null = card is for the owner. */
  assignedUserId?: number | null;
}

export interface CreditCardGroup {
  parentCard: CreditCard;
  subCards: CreditCard[];
}

export interface CreditCardResponse {
  message: string;
  creditCards: CreditCard[];
  groupedCards: CreditCardGroup[];
  count: number;
}

export interface CreateCreditCardResponse {
  message: string;
  id: number;
  name: string;
  lastFourDigits: string;
  limit: number;
  bankName: string;
}

export interface Bank {
  id: number;
  code: string;
  name: string;
} 