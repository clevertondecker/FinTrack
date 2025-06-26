import axios, { AxiosInstance } from 'axios';
import { LoginRequest, LoginResponse, RegisterRequest, RegisterResponse } from '../types/auth';
import { User } from '../types/user';
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
  Category
} from '../types/invoice';
import {
  CreateItemShareRequest,
  ItemShareListResponse,
  ItemShareCreateResponse
} from '../types/itemShare';
import { MySharesResponse } from '../types/itemShare';
import { InvoicePaymentRequest, InvoicePaymentResponse } from '../types/invoice';

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: 'http://localhost:8080/api',
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
          window.location.pathname !== '/login'
        ) {
          localStorage.removeItem('token');
          window.location.href = '/login';
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

  // Credit Card endpoints
  async getCreditCards(): Promise<{ message: string; creditCards: any[]; count: number }> {
    const response = await this.api.get<{ message: string; creditCards: any[]; count: number }>('/credit-cards');
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

  async createItemShares(invoiceId: number, itemId: number, data: CreateItemShareRequest): Promise<ItemShareCreateResponse> {
    const response = await this.api.post<ItemShareCreateResponse>(`/invoices/${invoiceId}/items/${itemId}/shares`, data);
    return response.data;
  }

  async removeItemShares(invoiceId: number, itemId: number): Promise<void> {
    await this.api.delete(`/invoices/${invoiceId}/items/${itemId}/shares`);
  }

  async getMyShares(): Promise<MySharesResponse> {
    const response = await this.api.get<MySharesResponse>('/invoices/shares/my-shares');
    return response.data;
  }

  async payInvoice(invoiceId: number, data: InvoicePaymentRequest): Promise<InvoicePaymentResponse> {
    const response = await this.api.post<InvoicePaymentResponse>(`/invoices/${invoiceId}/pay`, data);
    return response.data;
  }
}

export const apiService = new ApiService();
export default apiService; 