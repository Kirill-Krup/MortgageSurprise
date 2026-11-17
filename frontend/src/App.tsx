import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { LanguageProvider, useLanguage } from './context/LanguageContext';
import { Navbar, type NavTab } from './components/Navbar';
import { AuthModal } from './components/AuthModal';
import { CalculatorPage } from './pages/CalculatorPage';
import { MicroloanCalculatorPage } from './pages/MicroloanCalculatorPage';
import { ComparePage } from './pages/ComparePage';
import { BankOffersPage } from './pages/BankOffersPage';
import { MyScenariosPage } from './pages/MyScenariosPage';
import type { BankOffer } from './types';
import { Shield, Sparkles, AlertCircle, Landmark } from 'lucide-react';

export const MainApp: React.FC = () => {
  const { user, loginWithOAuth2Code, loginWithMagicLink } = useAuth();
  const { t } = useLanguage();
  const [activeTab, setActiveTab] = useState<NavTab>('calculator');
  const [presetOffer, setPresetOffer] = useState<BankOffer | null>(null);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authNotice, setAuthNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const handledAuthRef = React.useRef<string | null>(null);

  // If user is Admin, switch activeTab to offers (admin configuration & catalog)
  useEffect(() => {
    if (
      user?.role === 'ROLE_ADMIN' &&
      (activeTab === 'calculator' || activeTab === 'microloan' || activeTab === 'compare' || activeTab === 'scenarios')
    ) {
      setActiveTab('offers');
    }
  }, [user, activeTab]);

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

    // Clean query string
    const cleanPath = window.location.pathname === '/oauth2/callback' ? '/' : window.location.pathname;
    window.history.replaceState({}, document.title, cleanPath);

    if (authError) {
      setAuthNotice({
        type: 'error',
        text: `${t('authErrorNotice')} ${authError}`,
      });
      setTimeout(() => setAuthNotice(null), 6000);
      return;
    }

    if (oauthCode) {
      loginWithOAuth2Code(oauthCode)
        .then(() => {
          setAuthNotice({ type: 'success', text: t('authSuccessNotice') });
        })
        .catch(() => {
          setAuthNotice({
            type: 'error',
            text: t('authSocialError'),
          });
        })
        .finally(() => {
          setTimeout(() => setAuthNotice(null), 6000);
        });
    } else if (magicToken) {
      loginWithMagicLink(magicToken)
        .then(() => {
          setAuthNotice({ type: 'success', text: t('magicLinkSuccess') });
        })
        .catch(() => {
          setAuthNotice({
            type: 'error',
            text: t('magicLinkInvalid'),
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
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 font-sans transition-colors">
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
                ? 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800 text-emerald-800 dark:text-emerald-200'
                : 'bg-red-50 dark:bg-red-950/40 border-red-200 dark:border-red-800 text-red-800 dark:text-red-200'
            }`}
          >
            {authNotice.type === 'success' ? (
              <Sparkles size={18} className="text-emerald-600 dark:text-emerald-400 shrink-0" />
            ) : (
              <AlertCircle size={18} className="text-red-600 dark:text-red-400 shrink-0" />
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

        {activeTab === 'microloan' && (
          <MicroloanCalculatorPage
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

      {/* Official Banking Footer */}
      <footer className="border-t border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 py-8 text-xs text-slate-500 dark:text-slate-400 transition-colors">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-7 h-7 rounded-lg bg-blue-700 flex items-center justify-center text-white font-bold">
              <Landmark size={15} />
            </div>
            <div>
              <span className="font-extrabold text-slate-800 dark:text-slate-200">
                Mortgage<span className="text-blue-600 dark:text-blue-400">Surprise</span> {t('bankFooter')}
              </span>
              <span className="ml-2 text-[10px] text-slate-400">
                &copy; {new Date().getFullYear()} {t('allRightsReserved')}
              </span>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-4 sm:gap-6 text-[11px]">
            <span className="flex items-center gap-1.5 text-slate-600 dark:text-slate-400">
              <Shield size={14} className="text-emerald-500" />
              {t('securitySession')} (httpOnly cookies & TLS)
            </span>
            <span className="hidden md:inline text-slate-300 dark:text-slate-700">|</span>
            <span className="hidden md:inline text-slate-500 dark:text-slate-400">
              {t('bankLicense')}
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
    <ThemeProvider>
      <LanguageProvider>
        <AuthProvider>
          <MainApp />
        </AuthProvider>
      </LanguageProvider>
    </ThemeProvider>
  );
}
