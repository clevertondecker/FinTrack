import axios, { AxiosInstance } from 'axios';
import { LoginRequest, LoginResponse, RegisterRequest, RegisterResponse } from '../types/auth';
import { 
  CreditCard, 
  CreateCreditCardRequest, 
  CreditCardResponse, 
  CreateCreditCardResponse,
  Bank 
} from '../types/creditCard';

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
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
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

  // Credit Card endpoints
  async getCreditCards(): Promise<CreditCardResponse> {
    const response = await this.api.get<CreditCardResponse>('/credit-cards');
    return response.data;
  }

  async getCreditCard(id: number): Promise<{ message: string; creditCard: CreditCard }> {
    const response = await this.api.get(`/credit-cards/${id}`);
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

  // Bank endpoints
  async getBanks(): Promise<Bank[]> {
    const response = await this.api.get<{ banks: Bank[] }>('/banks');
    return response.data.banks;
  }
}

export const apiService = new ApiService();
export default apiService; 