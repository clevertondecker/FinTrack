import axios, { AxiosInstance } from 'axios';
import { LoginRequest, LoginResponse, RegisterRequest, RegisterResponse } from '../types/auth';
import { User } from '../types/user';
import {
  TrustedContact,
  CreateTrustedContactRequest,
  UpdateTrustedContactRequest
} from '../types/trustedContact';
import { 
  CreateCreditCardRequest, 
  CreateCreditCardResponse,
  Bank 
} from '../types/creditCard';
import {
  CreateInvoiceRequest,
  CreateInvoiceItemRequest,
  CreateInvoiceResponse,
  CreateInvoiceItemResponse,
  InvoiceItemDetailResponse,
  Category
} from '../types/invoice';
import {
  CreateItemShareRequest,
  ItemShareListResponse,
  ItemShareCreateResponse,
  ItemShareResponse
} from '../types/itemShare';
import { MySharesResponse } from '../types/itemShare';
import { InvoicePaymentRequest, InvoicePaymentResponse } from '../types/invoice';
import { MarkShareAsPaidRequest } from '../types/itemShare';
import {
  ImportInvoiceRequest,
  ImportInvoiceResponse,
  InvoiceImportListResponse,
  InvoiceImportDetailResponse,
  ManualReviewRequest,
  ManualReviewResponse
} from '../types/invoiceImport';
import {
  ExpenseReportResponse,
  CategoryExpenseSummary
} from '../types/expenseReport';

class ApiService {
  private api: AxiosInstance;

  constructor() {
    // In development, use backend on port 8080
    // In production, use relative /api (nginx proxies to backend)
    const baseURL = process.env.NODE_ENV === 'development' 
      ? 'http://localhost:8080/api' 
      : '/api';
    
    this.api = axios.create({
      baseURL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Interceptor para adicionar token em todas as requisições
    this.api.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Interceptor para tratar erros de resposta
    this.api.interceptors.response.use(
      (response) => {
        return response;
      },
      (error) => {
        if (
          error.response?.status === 401 &&
          !window.location.pathname.endsWith('/login')
        ) {
          localStorage.removeItem('token');
          window.location.href = '/app/login';
        }
        return Promise.reject(error);
      }
    );
  }

  // Auth endpoints
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await this.api.post<LoginResponse>('/auth/login', credentials);
    return response.data;
  }

  async register(userData: RegisterRequest): Promise<RegisterResponse> {
    const response = await this.api.post<RegisterResponse>('/users/register', userData);
    return response.data;
  }

  // User endpoints
  async getCurrentUser() {
    const response = await this.api.get('/users/current-user');
    return response.data;
  }

  async getUsers(): Promise<{ message: string; users: User[]; count: number }> {
    const response = await this.api.get<{ message: string; users: User[]; count: number }>('/users');
    return response.data;
  }

  async searchUserByEmail(email: string): Promise<User | null> {
    try {
      const response = await this.api.get<User>('/users/search', { params: { email: email.trim() } });
      return response.data;
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && err.response?.status === 404) return null;
      throw err;
    }
  }

  async connectUser(email: string): Promise<RegisterResponse> {
    const response = await this.api.post<RegisterResponse>('/users/connect', { email: email.trim() });
    return response.data;
  }

  // Trusted contacts (Circle of Trust - Model A)
  async getTrustedContacts(search?: string): Promise<TrustedContact[]> {
    const params = search ? { search: search.trim() } : {};
    const response = await this.api.get<TrustedContact[]>('/trusted-contacts', { params });
    return response.data;
  }

  async createTrustedContact(data: CreateTrustedContactRequest): Promise<TrustedContact> {
    const response = await this.api.post<TrustedContact>('/trusted-contacts', {
      name: data.name.trim(),
      email: data.email.trim().toLowerCase(),
      tags: data.tags?.trim() || undefined,
      note: data.note?.trim() || undefined
    });
    return response.data;
  }

  async updateTrustedContact(id: number, data: UpdateTrustedContactRequest): Promise<TrustedContact> {
    const response = await this.api.put<TrustedContact>(`/trusted-contacts/${id}`, {
      name: data.name?.trim(),
      email: data.email?.trim().toLowerCase(),
      tags: data.tags?.trim(),
      note: data.note?.trim()
    });
    return response.data;
  }

  async deleteTrustedContact(id: number): Promise<void> {
    await this.api.delete(`/trusted-contacts/${id}`);
  }

  // Credit Card endpoints
  async getCreditCards(includeInactive: boolean = false): Promise<{
    message: string;
    creditCards: any[];
    groupedCards: any[];
    count: number
  }> {
    const response = await this.api.get<{
      message: string;
      creditCards: any[];
      groupedCards: any[];
      count: number
    }>('/credit-cards', { params: { includeInactive } });
    return response.data;
  }

  async getCreditCard(id: number): Promise<{ message: string; creditCard: any }> {
    const response = await this.api.get<{ message: string; creditCard: any }>(`/credit-cards/${id}`);
    return response.data;
  }

  async createCreditCard(data: CreateCreditCardRequest): Promise<CreateCreditCardResponse> {
    const response = await this.api.post<CreateCreditCardResponse>('/credit-cards', data);
    return response.data;
  }

  async updateCreditCard(id: number, data: CreateCreditCardRequest): Promise<CreateCreditCardResponse> {
    const response = await this.api.put<CreateCreditCardResponse>(`/credit-cards/${id}`, data);
    return response.data;
  }

  async deleteCreditCard(id: number): Promise<{ message: string; id: number }> {
    const response = await this.api.delete(`/credit-cards/${id}`);
    return response.data;
  }

  async activateCreditCard(id: number): Promise<{ message: string; id: number }> {
    const response = await this.api.patch(`/credit-cards/${id}/activate`);
    return response.data;
  }

  // Bank endpoints
  async getBanks(): Promise<Bank[]> {
    const response = await this.api.get<{ message: string; banks: Bank[]; count: number }>('/banks');
    return response.data.banks;
  }

  // Invoice endpoints
  async getInvoices(): Promise<{ message: string; invoices: any[]; count: number }> {
    const response = await this.api.get<{ message: string; invoices: any[]; count: number }>('/invoices');
    return response.data;
  }

  async getInvoice(id: number): Promise<{ message: string; invoice: any }> {
    const response = await this.api.get<{ message: string; invoice: any }>(`/invoices/${id}`);
    return response.data;
  }

  async getInvoicesByCreditCard(creditCardId: number): Promise<{ message: string; creditCardId: number; creditCardName: string; invoices: any[]; count: number }> {
    const response = await this.api.get<{ message: string; creditCardId: number; creditCardName: string; invoices: any[]; count: number }>(`/invoices/credit-card/${creditCardId}`);
    return response.data;
  }

  async createInvoice(data: CreateInvoiceRequest): Promise<CreateInvoiceResponse> {
    const response = await this.api.post<CreateInvoiceResponse>('/invoices', data);
    return response.data;
  }

  async getInvoiceItems(invoiceId: number): Promise<{ message: string; invoiceId: number; items: any[]; count: number; totalAmount: number }> {
    const response = await this.api.get<{ message: string; invoiceId: number; items: any[]; count: number; totalAmount: number }>(`/invoices/${invoiceId}/items`);
    return response.data;
  }

  async createInvoiceItem(invoiceId: number, data: CreateInvoiceItemRequest): Promise<CreateInvoiceItemResponse> {
    const response = await this.api.post<CreateInvoiceItemResponse>(`/invoices/${invoiceId}/items`, data);
    return response.data;
  }

  async deleteInvoiceItem(invoiceId: number, itemId: number): Promise<void> {
    await this.api.delete(`/invoices/${invoiceId}/items/${itemId}`);
  }

  async getCategories(): Promise<{ message: string; categories: Category[]; count: number }> {
    const response = await this.api.get<{ message: string; categories: Category[]; count: number }>('/categories');
    return response.data;
  }

  // ItemShare methods
  async getItemShares(invoiceId: number, itemId: number): Promise<ItemShareListResponse> {
    const response = await this.api.get<ItemShareListResponse>(`/invoices/${invoiceId}/items/${itemId}/shares`);
    return response.data;
  }

  async createItemShares(invoiceId: number, itemId: number, request: CreateItemShareRequest): Promise<ItemShareCreateResponse> {
    const response = await this.api.post<ItemShareCreateResponse>(`/invoices/${invoiceId}/items/${itemId}/shares`, request);
    return response.data;
  }

  async deleteItemShares(invoiceId: number, itemId: number): Promise<void> {
    await this.api.delete(`/invoices/${invoiceId}/items/${itemId}/shares`);
  }

  async getMyShares(): Promise<MySharesResponse> {
    const response = await this.api.get<MySharesResponse>('/invoices/shares/my-shares');
    return response.data;
  }

  async markShareAsPaid(shareId: number, request: MarkShareAsPaidRequest): Promise<ItemShareResponse> {
    const response = await this.api.post<ItemShareResponse>(`/invoices/shares/${shareId}/mark-as-paid`, request);
    return response.data;
  }

  async markShareAsUnpaid(shareId: number): Promise<ItemShareResponse> {
    const response = await this.api.post<ItemShareResponse>(`/invoices/shares/${shareId}/mark-as-unpaid`);
    return response.data;
  }

  async markSharesAsPaidBulk(
    shareIds: number[],
    request: MarkShareAsPaidRequest
  ): Promise<{ message: string; updatedCount: number; updatedShares: ItemShareResponse[] }> {
    const body = { shareIds, paymentMethod: request.paymentMethod, paidAt: request.paidAt };
    const response = await this.api.post<{ message: string; updatedCount: number; updatedShares: ItemShareResponse[] }>(
      '/invoices/shares/mark-as-paid-bulk',
      body
    );
    return response.data;
  }

  async payInvoice(invoiceId: number, data: InvoicePaymentRequest): Promise<InvoicePaymentResponse> {
    const response = await this.api.post<InvoicePaymentResponse>(`/invoices/${invoiceId}/pay`, data);
    return response.data;
  }

  // Invoice Import endpoints
  async importInvoice(file: File, request: ImportInvoiceRequest): Promise<ImportInvoiceResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('request', JSON.stringify(request));

    const response = await this.api.post<ImportInvoiceResponse>('/invoice-imports', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  }

  async getInvoiceImports(): Promise<InvoiceImportListResponse> {
    const response = await this.api.get<InvoiceImportListResponse>('/invoice-imports');
    return response.data;
  }

  async getInvoiceImport(id: number): Promise<InvoiceImportDetailResponse> {
    const response = await this.api.get<InvoiceImportDetailResponse>(`/invoice-imports/${id}`);
    return response.data;
  }

  async deleteInvoiceImport(id: number): Promise<{ message: string; id: number }> {
    const response = await this.api.delete(`/invoice-imports/${id}`);
    return response.data;
  }

  async manualReview(id: number, request: ManualReviewRequest): Promise<ManualReviewResponse> {
    const response = await this.api.post<ManualReviewResponse>(`/invoice-imports/${id}/manual-review`, request);
    return response.data;
  }

  // Deleta uma fatura (apenas para admin)
  async deleteInvoice(invoiceId: number): Promise<void> {
    await this.api.delete(`/invoices/${invoiceId}`);
  }

  // Atualiza a categoria de um item de fatura
  async updateInvoiceItemCategory(invoiceId: number, itemId: number, categoryId: number | null): Promise<InvoiceItemDetailResponse> {
    const response = await this.api.put<InvoiceItemDetailResponse>(
      `/invoices/${invoiceId}/items/${itemId}/category`,
      { categoryId }
    );
    return response.data;
  }

  // Expense Report endpoints
  async getExpensesByCategory(
    month?: string,
    categoryId?: number,
    showTotal?: boolean
  ): Promise<ExpenseReportResponse> {
    const params = new URLSearchParams();
    if (month) params.append('month', month);
    if (categoryId) params.append('categoryId', categoryId.toString());
    if (showTotal) params.append('showTotal', 'true');
    
    const queryString = params.toString();
    const url = `/expenses/by-category${queryString ? `?${queryString}` : ''}`;
    const response = await this.api.get<ExpenseReportResponse>(url);
    return response.data;
  }

  async getExpenseSummary(month?: string): Promise<CategoryExpenseSummary[]> {
    const url = month ? `/expenses/summary?month=${month}` : '/expenses/summary';
    const response = await this.api.get<CategoryExpenseSummary[]>(url);
    return response.data;
  }
}

export const apiService = new ApiService();
export default apiService; 