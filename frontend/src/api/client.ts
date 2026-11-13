import axios from 'axios';
import type {
  AuthResponse,
  BankOffer,
  BankOfferRequest,
  CalculationRequest,
  CalculationResponse,
  PaymentScheduleItem,
  SaveScenarioRequest,
  ScenarioComparisonResponse,
  ScenarioResponse,
  User,
} from '../types';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: unknown) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error);
    } else {
      promise.resolve();
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Do not attempt refresh on auth endpoints to prevent loops
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/login') &&
      !originalRequest.url?.includes('/auth/refresh') &&
      !originalRequest.url?.includes('/auth/register') &&
      !originalRequest.url?.includes('/auth/magic-link')
    ) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => apiClient(originalRequest))
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await apiClient.post('/auth/refresh');
        processQueue(null);
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError);
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export const authApi = {
  register: async (data: { email: string; password: string }): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/auth/register', data);
    return res.data;
  },

  login: async (data: { email: string; password: string }): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/auth/login', data);
    return res.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/auth/logout');
  },

  getMe: async (): Promise<User> => {
    const res = await apiClient.get<AuthResponse>('/auth/me');
    return {
      id: res.data.userId,
      email: res.data.email,
      role: res.data.role,
    };
  },

  requestMagicLink: async (email: string): Promise<{ message: string }> => {
    const res = await apiClient.post<{ message: string }>('/auth/magic-link', { email });
    return res.data;
  },

  verifyMagicLink: async (token: string): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/auth/magic-link/verify', { token });
    return res.data;
  },

  exchangeOAuth2Code: async (code: string): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/auth/oauth2/exchange', { code });
    return res.data;
  },
};

export const calculatorApi = {
  calculate: async (request: CalculationRequest): Promise<CalculationResponse> => {
    const res = await apiClient.post<CalculationResponse>('/calculator/calculate', request);
    return res.data;
  },

  getSchedule: async (scheduleId: string): Promise<PaymentScheduleItem[]> => {
    const res = await apiClient.get<any>(`/calculator/schedules/${scheduleId}`);
    if (Array.isArray(res.data)) {
      return res.data;
    }
    return res.data?.items || [];
  },
};

export const scenarioApi = {
  getAll: async (): Promise<ScenarioResponse[]> => {
    const res = await apiClient.get<ScenarioResponse[]>('/scenarios');
    return res.data;
  },

  getById: async (id: string): Promise<ScenarioResponse> => {
    const res = await apiClient.get<ScenarioResponse>(`/scenarios/${id}`);
    return res.data;
  },

  save: async (request: SaveScenarioRequest): Promise<ScenarioResponse> => {
    const res = await apiClient.post<ScenarioResponse>('/scenarios', request);
    return res.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/scenarios/${id}`);
  },

  compare: async (scenarioIds: string[]): Promise<ScenarioComparisonResponse> => {
    const res = await apiClient.post<ScenarioComparisonResponse>('/scenarios/compare', {
      scenarioIds,
    });
    return res.data;
  },

  exportPdf: async (id: string): Promise<{ message: string }> => {
    const res = await apiClient.post<{ message: string }>(`/scenarios/${id}/export-pdf`);
    return res.data;
  },
};

export const bankOfferApi = {
  getAll: async (all = false): Promise<BankOffer[]> => {
    const res = await apiClient.get<BankOffer[]>(`/bank-offers${all ? '?all=true' : ''}`);
    return res.data;
  },

  getById: async (id: string): Promise<BankOffer> => {
    const res = await apiClient.get<BankOffer>(`/bank-offers/${id}`);
    return res.data;
  },

  create: async (request: BankOfferRequest): Promise<BankOffer> => {
    const res = await apiClient.post<BankOffer>('/bank-offers', request);
    return res.data;
  },

  update: async (id: string, request: BankOfferRequest): Promise<BankOffer> => {
    const res = await apiClient.put<BankOffer>(`/bank-offers/${id}`, request);
    return res.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/bank-offers/${id}`);
  },
};

export const adminBankOfferApi = {
  getAll: async (): Promise<BankOffer[]> => {
    const res = await apiClient.get<BankOffer[]>('/admin/bank-offers');
    return res.data;
  },

  getById: async (id: string): Promise<BankOffer> => {
    const res = await apiClient.get<BankOffer>(`/admin/bank-offers/${id}`);
    return res.data;
  },

  create: async (request: BankOfferRequest): Promise<BankOffer> => {
    const res = await apiClient.post<BankOffer>('/admin/bank-offers', request);
    return res.data;
  },

  update: async (id: string, request: BankOfferRequest): Promise<BankOffer> => {
    const res = await apiClient.put<BankOffer>(`/admin/bank-offers/${id}`, request);
    return res.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/admin/bank-offers/${id}`);
  },
};
