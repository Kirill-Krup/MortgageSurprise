import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { AuthModal } from './components/AuthModal';
import { CalculatorPage } from './pages/CalculatorPage';
import { ComparePage } from './pages/ComparePage';
import { BankOffersPage } from './pages/BankOffersPage';
import { MyScenariosPage } from './pages/MyScenariosPage';
import type { BankOffer } from './types';
import { Shield, Sparkles, AlertCircle } from 'lucide-react';

export const MainApp: React.FC = () => {
  const { loginWithOAuth2Code, loginWithMagicLink } = useAuth();
  const [activeTab, setActiveTab] = useState<'calculator' | 'compare' | 'offers' | 'scenarios'>('calculator');
  const [presetOffer, setPresetOffer] = useState<BankOffer | null>(null);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authNotice, setAuthNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const handledAuthRef = React.useRef<string | null>(null);

  // Handle OAuth2 code or Magic Link token on mount
  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    const oauthCode = searchParams.get('code');
    const magicToken = searchParams.get('token');
    const authError = searchParams.get('error');

    const authKey = oauthCode ? `code:${oauthCode}` : magicToken ? `token:${magicToken}` : authError ? `error:${authError}` : null;
    if (!authKey || handledAuthRef.current === authKey) {
      return;
    }
    handledAuthRef.current = authKey;

    // Immediately clean the URL query string so subsequent re-renders or reloads don't re-run
    const cleanPath = window.location.pathname === '/oauth2/callback' ? '/' : window.location.pathname;
    window.history.replaceState({}, document.title, cleanPath);

    if (authError) {
      setAuthNotice({
        type: 'error',
        text: `Ошибка авторизации: ${authError}`,
      });
      setTimeout(() => setAuthNotice(null), 6000);
      return;
    }

    if (oauthCode) {
      loginWithOAuth2Code(oauthCode)
        .then(() => {
          setAuthNotice({ type: 'success', text: 'Успешная авторизация через социальную сеть!' });
        })
        .catch(() => {
          setAuthNotice({
            type: 'error',
            text: 'Ошибка при авторизации через соцсеть или срок действия кода истек.',
          });
        })
        .finally(() => {
          setTimeout(() => setAuthNotice(null), 6000);
        });
    } else if (magicToken) {
      loginWithMagicLink(magicToken)
        .then(() => {
          setAuthNotice({ type: 'success', text: 'Успешный вход по волшебной ссылке!' });
        })
        .catch(() => {
          setAuthNotice({
            type: 'error',
            text: 'Неверная или просроченная ссылка для входа.',
          });
        })
        .finally(() => {
          setTimeout(() => setAuthNotice(null), 6000);
        });
    }
  }, [loginWithOAuth2Code, loginWithMagicLink]);

  const handleSelectOffer = (offer: BankOffer) => {
    setPresetOffer(offer);
    setActiveTab('calculator');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900 font-sans">
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onOpenAuth={() => setAuthModalOpen(true)}
      />

      {/* Global Auth Notification Banner */}
      {authNotice && (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-4 w-full animate-in fade-in">
          <div
            className={`p-4 rounded-2xl flex items-center gap-3 text-sm font-semibold border ${
              authNotice.type === 'success'
                ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                : 'bg-red-50 border-red-200 text-red-800'
            }`}
          >
            {authNotice.type === 'success' ? (
              <Sparkles size={18} className="text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle size={18} className="text-red-600 shrink-0" />
            )}
            <span>{authNotice.text}</span>
          </div>
        </div>
      )}

      <main className="flex-1 pb-16">
        {activeTab === 'calculator' && (
          <CalculatorPage
            presetOffer={presetOffer}
            onClearPreset={() => setPresetOffer(null)}
            onOpenAuth={() => setAuthModalOpen(true)}
          />
        )}

        {activeTab === 'compare' && (
          <ComparePage
            onOpenAuth={() => setAuthModalOpen(true)}
            onNavigateToCalculator={() => setActiveTab('calculator')}
          />
        )}

        {activeTab === 'offers' && (
          <BankOffersPage onSelectOffer={handleSelectOffer} />
        )}

        {activeTab === 'scenarios' && (
          <MyScenariosPage
            onOpenAuth={() => setAuthModalOpen(true)}
            onNavigateToCalculator={() => setActiveTab('calculator')}
          />
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-200 bg-white py-8 text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-lg bg-indigo-600 flex items-center justify-center text-white font-bold text-xs">
              M
            </div>
            <span className="font-semibold text-slate-700">MortgageSurprise &copy; {new Date().getFullYear()}</span>
          </div>

          <div className="flex items-center gap-6">
            <span className="flex items-center gap-1.5 text-slate-600">
              <Shield size={14} className="text-emerald-500" />
              Безопасная сессия (httpOnly cookies)
            </span>
            <span className="hidden md:inline text-slate-300">|</span>
            <span className="hidden md:inline">
              Расчеты носят информационный характер
            </span>
          </div>
        </div>
      </footer>

      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
      />
    </div>
  );
};

export default function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}
