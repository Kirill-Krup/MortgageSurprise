import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { authApi } from '../api/client';
import type { User } from '../types';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (data: { email: string; password: string }) => Promise<void>;
  register: (data: { email: string; password: string }) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  loginWithOAuth2Code: (code: string) => Promise<void>;
  loginWithMagicLink: (token: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchUser = useCallback(async () => {
    try {
      const currentUser = await authApi.getMe();
      setUser(currentUser);
    } catch (err: unknown) {
      const axiosError = err as { response?: { status?: number; data?: { message?: string; email?: string; userId?: string } } };
      if (
        axiosError.response?.status === 403 &&
        (axiosError.response?.data?.message?.includes('ACCOUNT_DISABLED') ||
         axiosError.response?.data?.message?.includes('disabled'))
      ) {
        setUser((prev) => ({
          id: axiosError.response?.data?.userId || prev?.id || '',
          email: axiosError.response?.data?.email || prev?.email || '',
          role: prev?.role || 'ROLE_USER',
          enabled: false,
        }));
      } else {
        setUser(null);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUser();
  }, [fetchUser]);

  useEffect(() => {
    const handleBlocked = (e: Event) => {
      const customEvent = e as CustomEvent<{ email?: string; userId?: string }>;
      setUser((prev) =>
        prev
          ? { ...prev, enabled: false }
          : {
              id: customEvent.detail?.userId || '',
              email: customEvent.detail?.email || '',
              role: 'ROLE_USER',
              enabled: false,
            }
      );
    };
    window.addEventListener('auth:user-blocked', handleBlocked);
    return () => window.removeEventListener('auth:user-blocked', handleBlocked);
  }, []);

  useEffect(() => {
    if (!user || user.enabled === false) return;

    const checkStatus = () => {
      authApi.getMe().then((u) => {
        if (u.enabled === false) {
          setUser(u);
        }
      }).catch((err: unknown) => {
        const axiosError = err as { response?: { status?: number; data?: { message?: string } } };
        if (axiosError.response?.status === 403 && axiosError.response?.data?.message?.includes('ACCOUNT_DISABLED')) {
          setUser((prev) => (prev ? { ...prev, enabled: false } : null));
        }
      });
    };

    const interval = setInterval(checkStatus, 10000);
    window.addEventListener('focus', checkStatus);
    return () => {
      clearInterval(interval);
      window.removeEventListener('focus', checkStatus);
    };
  }, [user]);

  const login = async (data: { email: string; password: string }) => {
    try {
      const res = await authApi.login(data);
      setUser({
        id: res.userId,
        email: res.email,
        role: res.role,
        enabled: res.enabled !== false,
      });
    } catch (err: unknown) {
      const axiosError = err as { response?: { status?: number; data?: { message?: string; email?: string; userId?: string } } };
      if (
        axiosError.response?.status === 403 &&
        (axiosError.response?.data?.message?.includes('ACCOUNT_DISABLED') ||
         axiosError.response?.data?.message?.includes('disabled'))
      ) {
        setUser({
          id: axiosError.response?.data?.userId || '',
          email: axiosError.response?.data?.email || data.email,
          role: 'ROLE_USER',
          enabled: false,
        });
        return;
      }
      throw err;
    }
  };

  const register = async (data: { email: string; password: string }) => {
    const res = await authApi.register(data);
    setUser({
      id: res.userId,
      email: res.email,
      role: res.role,
      enabled: res.enabled !== false,
    });
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
    }
  };

  const loginWithOAuth2Code = useCallback(async (code: string) => {
    const res = await authApi.exchangeOAuth2Code(code);
    setUser({
      id: res.userId,
      email: res.email,
      role: res.role,
      enabled: res.enabled !== false,
    });
  }, []);

  const loginWithMagicLink = useCallback(async (token: string) => {
    const res = await authApi.verifyMagicLink(token);
    setUser({
      id: res.userId,
      email: res.email,
      role: res.role,
      enabled: res.enabled !== false,
    });
  }, []);

  const refreshUser = useCallback(async () => {
    await fetchUser();
  }, [fetchUser]);

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        register,
        logout,
        refreshUser,
        loginWithOAuth2Code,
        loginWithMagicLink,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
