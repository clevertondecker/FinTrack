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
}

export interface CreditCardResponse {
  message: string;
  creditCards: CreditCard[];
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