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
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUser();
  }, [fetchUser]);

  const login = async (data: { email: string; password: string }) => {
    const res = await authApi.login(data);
    setUser({
      id: res.userId,
      email: res.email,
      role: res.role,
    });
  };

  const register = async (data: { email: string; password: string }) => {
    const res = await authApi.register(data);
    setUser({
      id: res.userId,
      email: res.email,
      role: res.role,
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
    });
  }, []);

  const loginWithMagicLink = useCallback(async (token: string) => {
    const res = await authApi.verifyMagicLink(token);
    setUser({
      id: res.userId,
      email: res.email,
      role: res.role,
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
