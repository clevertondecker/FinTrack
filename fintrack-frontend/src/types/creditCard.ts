export interface CreditCard {
  id: number;
  name: string;
  lastFourDigits: string;
  limit: number;
  bankName: string;
  bankCode?: string;
  active: boolean;
}

export interface CreateCreditCardRequest {
  name: string;
  lastFourDigits: string;
  limit: number;
  bankId: number;
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