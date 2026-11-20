import axios from 'axios';
import type {
  AbTestStatsResponse,
  AbTestVariantResponse,
  AdminLoanMonitoring,
  AdminStats,
  AdminUser,
  AuthResponse,
  BankOffer,
  BankOfferRequest,
  CalculationRequest,
  CalculationResponse,
  ExperimentConfig,
  MicroloanCalculationRequest,
  MicroloanCalculationResponse,
  MicroloanConfig,
  MicroloanConfigDTO,
  MicroloanRateDTO,
  PassportData,
  PaymentScheduleItem,
  SaveScenarioRequest,
  ScenarioComparisonResponse,
  ScenarioResponse,
  User,
  UserActivityRecord,
  UserAuditActionType,
} from '../types';
import {
  deleteExperimentConfig,
  getExperimentConfig,
  getExperimentConfigs,
  getOrAssignExperimentVariant,
  saveExperimentConfig,
  saveExperimentVariant,
} from '../utils/abTesting';

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

    // Check if account is disabled/blocked
    if (
      error.response?.status === 403 &&
      (error.response?.data?.code === 'ACCOUNT_DISABLED' ||
       error.response?.data?.message?.includes('ACCOUNT_DISABLED') ||
       error.response?.data?.message?.includes('disabled') ||
       error.response?.data?.message?.includes('blocked'))
    ) {
      if (typeof window !== 'undefined') {
        window.dispatchEvent(
          new CustomEvent('auth:user-blocked', {
            detail: {
              email: error.response?.data?.email,
              userId: error.response?.data?.userId,
            },
          })
        );
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
      enabled: res.data.enabled !== false,
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

      calculateMicroloan: async (request: MicroloanCalculationRequest): Promise<MicroloanCalculationResponse> => {
        const res = await apiClient.post<MicroloanCalculationResponse>('/calculator/microloan', request);
        return res.data;
      },

      getSchedule: async (scheduleId: string): Promise<PaymentScheduleItem[]> => {
        const res = await apiClient.get<any>(`/calculator/schedules/${scheduleId}`);
        if (Array.isArray(res.data)) {
          return res.data;
        }
        return res.data?.items || [];
      },

      getMicroloanConfig: async (): Promise<MicroloanConfig> => {
        const res = await apiClient.get<MicroloanConfig>('/microloan/config');
        return res.data;
      },

      updateMicroloanConfig: async (config: MicroloanConfigDTO): Promise<MicroloanConfig> => {
        const res = await apiClient.put<MicroloanConfig>('/admin/microloan/config', config);
        return res.data;
      },

      applyMicroloan: async (data: {
        amount: number;
        termDays: number;
        purpose?: string;
        microloanType?: string;
        totalRepayment?: number;
        scheduleId?: string;
        applicationId?: string;
        applicantFullName?: string;
        currency?: string;
      }): Promise<{ success: boolean; applicationId: string; status: string; message: string }> => {
        const res = await apiClient.post<{ success: boolean; applicationId: string; status: string; message: string }>(
          '/calculator/microloan/apply',
          data
        );
        return res.data;
      },
    getExchangeRates: async (): Promise<Record<string, number>> => {
        const res = await apiClient.get<Record<string, number>>('/calculator/exchange-rates');
        return res.data;
    },
};

export const microloanRateApi = {
  getRates: async (): Promise<MicroloanRateDTO[]> => {
    const res = await apiClient.get<MicroloanRateDTO[]>('/microloan/rates');
    return Array.isArray(res.data) ? res.data : [];
  },

  getRateById: async (id: number): Promise<MicroloanRateDTO> => {
    const res = await apiClient.get<MicroloanRateDTO>(`/microloan/rates/${id}`);
    return res.data;
  },

  getRateByName: async (name: string): Promise<MicroloanRateDTO> => {
    const res = await apiClient.get<MicroloanRateDTO>(`/microloan/rate?name=${encodeURIComponent(name)}`);
    return res.data;
  },

  saveRate: async (dto: MicroloanRateDTO): Promise<MicroloanRateDTO> => {
    const res = await apiClient.post<MicroloanRateDTO>('/admin/microloan/rate', dto);
    return res.data;
  },

  updateRate: async (id: number, dto: MicroloanRateDTO): Promise<MicroloanRateDTO> => {
    const res = await apiClient.put<MicroloanRateDTO>(`/admin/microloan/rate/${id}`, dto);
    return res.data;
  },

  deleteRate: async (id: number): Promise<void> => {
    await apiClient.delete(`/admin/microloan/rate/${id}`);
  },
};

export const tariffApi = microloanRateApi;

export const scenarioApi = {
  getAll: async (): Promise<ScenarioResponse[]> => {
    const res = await apiClient.get<ScenarioResponse[]>('/scenarios');
    return Array.isArray(res.data) ? res.data : [];
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

export const profileApi = {
  getActivity: async (action?: UserAuditActionType): Promise<UserActivityRecord[]> => {
    const params = action ? `?action=${action}` : '';
    const res = await apiClient.get<UserActivityRecord[]>(`/profile/activity${params}`);
    return Array.isArray(res.data) ? res.data : [];
  },
};

export const adminAuditApi = {
  getLogs: async (action?: UserAuditActionType): Promise<UserActivityRecord[]> => {
    const params = action ? `?action=${action}` : '';
    const res = await apiClient.get<UserActivityRecord[]>(`/admin/audit/logs${params}`);
    return Array.isArray(res.data) ? res.data : [];
  },
};

export const adminManagementApi = {
  getStats: async (): Promise<AdminStats> => {
    const res = await apiClient.get<AdminStats>('/admin/stats');
    return res.data;
  },

  getUsers: async (): Promise<AdminUser[]> => {
    const res = await apiClient.get<AdminUser[]>('/admin/users');
    return Array.isArray(res.data) ? res.data : [];
  },

  updateUserStatus: async (userId: string, enabled: boolean): Promise<AdminUser> => {
    const res = await apiClient.patch<AdminUser>(`/admin/users/${userId}/status`, { enabled });
    return res.data;
  },

  getLoans: async (): Promise<AdminLoanMonitoring[]> => {
    const res = await apiClient.get<AdminLoanMonitoring[]>('/admin/loans');
    return Array.isArray(res.data) ? res.data : [];
  },
};

export const abTestApi = {
  assignVariant: async (experimentName: string, color: string): Promise<AbTestVariantResponse> => {
    try {
      const res = await apiClient.post<AbTestVariantResponse>('/ab-test/variant', {
        experimentName,
        color,
      });
      return res.data;
    } catch {
      saveExperimentVariant(experimentName, color);
      return { experimentName, color };
    }
  },

  getVariant: async (experimentName: string): Promise<AbTestVariantResponse> => {
    try {
      const res = await apiClient.get<AbTestVariantResponse>(`/ab-test/variant?experimentName=${encodeURIComponent(experimentName)}`);
      return res.data;
    } catch {
      const config = getExperimentConfig(experimentName);
      const candidates = config ? config.variants.map((v) => v.name) : ['blue', 'emerald'];
      const assigned = getOrAssignExperimentVariant(experimentName, candidates);
      return { experimentName, color: assigned };
    }
  },

  getExperiments: async (): Promise<ExperimentConfig[]> => {
    try {
      const res = await apiClient.get<ExperimentConfig[]>('/ab-test/experiments');
      if (Array.isArray(res.data) && res.data.length > 0) {
        return res.data;
      }
    } catch {
      // Backend endpoint optional; fall back to local storage
    }
    return getExperimentConfigs();
  },

  getExperiment: async (id: string): Promise<ExperimentConfig | null> => {
    try {
      const res = await apiClient.get<ExperimentConfig>(`/ab-test/experiments/${encodeURIComponent(id)}`);
      if (res.data) return res.data;
    } catch {
      // Backend endpoint optional; fall back to local storage
    }
    return getExperimentConfig(id);
  },

  saveExperiment: async (config: ExperimentConfig): Promise<ExperimentConfig> => {
    const savedLocal = saveExperimentConfig(config);
    try {
      const res = await apiClient.post<ExperimentConfig>('/ab-test/experiments', config);
      return res.data || savedLocal;
    } catch {
      return savedLocal;
    }
  },

  deleteExperiment: async (id: string): Promise<void> => {
    deleteExperimentConfig(id);
    try {
      await apiClient.delete(`/ab-test/experiments/${encodeURIComponent(id)}`);
    } catch {
      // Handled by local deletion
    }
  },

  getStats: async (experimentName?: string): Promise<AbTestStatsResponse> => {
    const expName = experimentName || 'mortgage_calculate_button';
    try {
      const params = `?experimentName=${encodeURIComponent(expName)}`;
      const res = await apiClient.get<AbTestStatsResponse>(`/ab-test/stats${params}`);
      if (res.data && res.data.variants && res.data.variants.length > 0) {
        const config = getExperimentConfig(expName);
        if (config) {
          const enrichedVariants = res.data.variants.map((v) => {
            const variantCfg = config.variants.find(
              (cv) => cv.name.toLowerCase() === v.color.toLowerCase()
            );
            return {
              ...v,
              targetWeight: variantCfg ? variantCfg.weight : undefined,
            };
          });
          return { ...res.data, variants: enrichedVariants };
        }
        return res.data;
      }
    } catch {
      // Fallback to local / mock stats for experiments
    }

    const config = getExperimentConfig(expName);
    const variantsList = config?.variants?.length
      ? config.variants
      : [
          { name: 'blue', weight: 50 },
          { name: 'emerald', weight: 50 },
        ];

    const totalUsers = 48;
    const totalClicks = 37;
    const totalWeight = variantsList.reduce((acc, cur) => acc + Math.max(0, cur.weight), 0) || 100;

    const statsVariants = variantsList.map((v) => {
      const fraction = (Math.max(0, v.weight) / totalWeight);
      const variantUsers = Math.max(1, Math.round(totalUsers * fraction));
      const variantClicks = Math.round(totalClicks * fraction);
      return {
        color: v.name,
        totalUsers: variantUsers,
        totalClicks: variantClicks,
        averageClicksPerUser: variantUsers > 0 ? Number((variantClicks / variantUsers).toFixed(2)) : 0,
        targetWeight: v.weight,
      };
    });

    return {
      experimentName: expName,
      totalAssignedUsers: totalUsers,
      totalClicks: totalClicks,
      variants: statsVariants,
      userDetails: [
        {
          userId: 'usr_admin_01',
          userEmail: 'admin@mortgagesurprise.com',
          color: variantsList[0]?.name || 'blue',
          clicksCount: 9,
          assignedAt: new Date(Date.now() - 3600000 * 36).toISOString(),
        },
        {
          userId: 'usr_client_02',
          userEmail: 'client.vip@gmail.com',
          color: variantsList[1]?.name || variantsList[0]?.name || 'emerald',
          clicksCount: 5,
          assignedAt: new Date(Date.now() - 3600000 * 20).toISOString(),
        },
        {
          userId: 'usr_demo_03',
          userEmail: 'ivan.petrov@yandex.ru',
          color: variantsList[0]?.name || 'blue',
          clicksCount: 14,
          assignedAt: new Date(Date.now() - 3600000 * 12).toISOString(),
        },
      ],
    };
  },
};

export const passportApi = {
  get: async (): Promise<PassportData | null> => {
    try {
      const res = await apiClient.get<PassportData>('/profile/passport');
      if (res.status === 204 || !res.data) {
        return null;
      }
      return res.data;
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number } };
      if (axiosErr.response?.status === 204 || axiosErr.response?.status === 404) {
        return null;
      }
      throw err;
    }
  },

  save: async (data: PassportData): Promise<PassportData> => {
    const res = await apiClient.put<PassportData>('/profile/passport', data);
    return res.data;
  },

  delete: async (): Promise<void> => {
    await apiClient.delete('/profile/passport');
  },
};



