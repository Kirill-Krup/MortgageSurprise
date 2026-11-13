import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api/client';
import { X, Lock, Mail, Sparkles, AlertCircle, CheckCircle2 } from 'lucide-react';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialMode?: 'login' | 'register' | 'magic';
}

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  initialMode = 'login',
}) => {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<'login' | 'register' | 'magic'>(initialMode);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [magicSent, setMagicSent] = useState(false);

  if (!isOpen) return null;

  const handleOAuthLogin = (provider: 'google' | 'github') => {
    const callbackUrl = window.location.origin + '/oauth2/callback';
    document.cookie = `oauth2_redirect_uri=${encodeURIComponent(
      callbackUrl
    )}; path=/; max-age=300; SameSite=Lax`;

    const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
    window.location.href = `${backendUrl}/oauth2/authorization/${provider}`;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (mode === 'login') {
        await login({ email, password });
        onClose();
      } else if (mode === 'register') {
        await register({ email, password });
        onClose();
      } else if (mode === 'magic') {
        await authApi.requestMagicLink(email);
        setMagicSent(true);
      }
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(
        axiosError.response?.data?.message ||
          'Произошла ошибка при выполнении запроса. Пожалуйста, попробуйте снова.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden relative border border-slate-100">
        {/* Header */}
        <div className="flex items-center justify-between p-6 pb-4 border-b border-slate-100">
          <h2 className="text-xl font-bold text-slate-800">
            {mode === 'login' && 'Вход в аккаунт'}
            {mode === 'register' && 'Регистрация'}
            {mode === 'magic' && 'Вход по волшебной ссылке'}
          </h2>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-100 transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        {/* Tab switchers */}
        <div className="flex border-b border-slate-100 bg-slate-50 p-1 text-sm font-medium">
          <button
            type="button"
            onClick={() => {
              setMode('login');
              setError(null);
              setMagicSent(false);
            }}
            className={`flex-1 py-2 rounded-lg text-center transition-all ${
              mode === 'login'
                ? 'bg-white text-indigo-600 shadow-sm font-semibold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Вход
          </button>
          <button
            type="button"
            onClick={() => {
              setMode('register');
              setError(null);
              setMagicSent(false);
            }}
            className={`flex-1 py-2 rounded-lg text-center transition-all ${
              mode === 'register'
                ? 'bg-white text-indigo-600 shadow-sm font-semibold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Регистрация
          </button>
          <button
            type="button"
            onClick={() => {
              setMode('magic');
              setError(null);
            }}
            className={`flex-1 py-2 rounded-lg text-center transition-all ${
              mode === 'magic'
                ? 'bg-white text-indigo-600 shadow-sm font-semibold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Magic Link
          </button>
        </div>

        {/* Form Body */}
        <div className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-xl flex items-start gap-3 text-red-700 text-sm">
              <AlertCircle size={18} className="shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {magicSent ? (
            <div className="text-center py-6">
              <div className="w-14 h-14 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto mb-4">
                <CheckCircle2 size={32} />
              </div>
              <h3 className="text-lg font-bold text-slate-800 mb-2">Ссылка отправлена!</h3>
              <p className="text-slate-600 text-sm mb-6">
                Мы отправили ссылку для входа на адрес <span className="font-semibold text-slate-800">{email}</span>. Перейдите по ней для автоматического входа.
              </p>
              <button
                type="button"
                onClick={() => setMagicSent(false)}
                className="w-full py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium rounded-xl transition-colors"
              >
                Отправить другую ссылку
              </button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider mb-1.5">
                  Email
                </label>
                <div className="relative">
                  <Mail
                    size={18}
                    className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"
                  />
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="user@example.com"
                    className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white text-sm"
                  />
                </div>
              </div>

              {mode !== 'magic' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider mb-1.5">
                    Пароль
                  </label>
                  <div className="relative">
                    <Lock
                      size={18}
                      className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"
                    />
                    <input
                      type="password"
                      required
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white text-sm"
                    />
                  </div>
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full mt-2 py-3 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white font-medium rounded-xl shadow-lg shadow-indigo-200 transition-all flex items-center justify-center gap-2 disabled:opacity-70 cursor-pointer"
              >
                {loading ? (
                  <span className="inline-block animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent" />
                ) : mode === 'login' ? (
                  'Войти'
                ) : mode === 'register' ? (
                  'Зарегистрироваться'
                ) : (
                  <>
                    <Sparkles size={18} />
                    <span>Отправить Magic Link</span>
                  </>
                )}
              </button>

              {mode === 'login' && (
                <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs flex items-center justify-between gap-2">
                  <div className="min-w-0">
                    <span className="font-semibold text-slate-700 block">Аккаунт администратора:</span>
                    <span className="text-[11px] text-slate-500 font-mono block truncate">
                      admin@mortgagesurprise.com / admin123
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setEmail('admin@mortgagesurprise.com');
                      setPassword('admin123');
                    }}
                    className="px-2.5 py-1 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-semibold rounded-lg text-xs transition-colors shrink-0 cursor-pointer"
                  >
                    Заполнить
                  </button>
                </div>
              )}
            </form>
          )}

          {!magicSent && (
            <div className="mt-5 pt-4 border-t border-slate-100">
              <div className="relative mb-4">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-slate-200" />
                </div>
                <div className="relative flex justify-center text-xs uppercase">
                  <span className="bg-white px-2.5 text-slate-400 font-semibold tracking-wider">
                    или войдите через
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => handleOAuthLogin('google')}
                  className="flex items-center justify-center gap-2.5 px-3 py-2.5 border border-slate-200 hover:border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-semibold rounded-xl transition-all shadow-xs cursor-pointer active:scale-98"
                >
                  <svg className="w-4 h-4 shrink-0" viewBox="0 0 24 24">
                    <path
                      fill="#4285F4"
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                    />
                    <path
                      fill="#34A853"
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                    />
                    <path
                      fill="#FBBC05"
                      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                    />
                    <path
                      fill="#EA4335"
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                    />
                  </svg>
                  <span>Google</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleOAuthLogin('github')}
                  className="flex items-center justify-center gap-2.5 px-3 py-2.5 border border-slate-200 hover:border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-semibold rounded-xl transition-all shadow-xs cursor-pointer active:scale-98"
                >
                  <svg className="w-4 h-4 shrink-0 text-slate-900 fill-current" viewBox="0 0 24 24">
                    <path
                      fillRule="evenodd"
                      clipRule="evenodd"
                      d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
                    />
                  </svg>
                  <span>GitHub</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
