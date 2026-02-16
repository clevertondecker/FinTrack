export interface UserShare {
  userId?: number;
  contactId?: number;
  percentage: number;
  responsible: boolean;
}

export interface CreateItemShareRequest {
  userShares: UserShare[];
}

export interface ItemShareResponse {
  id: number;
  userId: number | null;
  userName: string;
  userEmail: string;
  contactId: number | null;
  contactName: string | null;
  contactEmail: string | null;
  percentage: number;
  amount: number;
  responsible: boolean;
  paid: boolean;
  paymentMethod: string | null;
  paidAt: string | null;
  createdAt: string;
}

export interface ItemShareListResponse {
  message: string;
  invoiceId: number;
  itemId: number;
  itemDescription: string;
  itemAmount: number;
  shares: ItemShareResponse[];
  shareCount: number;
  totalSharedAmount: number;
  unsharedAmount: number;
}

export interface ItemShareCreateResponse {
  message: string;
  invoiceId: number;
  itemId: number;
  itemDescription: string;
  itemAmount: number;
  shares: ItemShareResponse[];
  shareCount: number;
  totalSharedAmount: number;
  unsharedAmount: number;
}

// Types for user's shares
export interface MyShareResponse {
  shareId: number;
  invoiceId: number;
  itemId: number;
  itemDescription: string;
  itemAmount: number;
  myAmount: number;
  myPercentage: number;
  isResponsible: boolean;
  isPaid: boolean;
  paymentMethod: string | null;
  paidAt: string | null;
  creditCardName: string;
  creditCardOwnerName: string;
  invoiceDueDate: string;
  invoiceStatus: string;
  shareCreatedAt: string;
  installments: number;
  totalInstallments: number;
  remainingInstallments: number;
  totalItemAmount: number;
  remainingItemAmount: number;
}

export interface MySharesResponse {
  message: string;
  shares: MyShareResponse[];
  shareCount: number;
}

export interface MarkShareAsPaidRequest {
  paymentMethod: string;
  paidAt: string;
} 