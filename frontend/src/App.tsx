import React, { useState, useEffect, useRef } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { LanguageProvider, useLanguage } from './context/LanguageContext';
import { Navbar, type NavTab } from './components/Navbar';
import { AuthModal } from './components/AuthModal';
import { CalculatorPage } from './pages/CalculatorPage';
import { MicroloanCalculatorPage } from './pages/MicroloanCalculatorPage';
import { ProfilePage } from './pages/ProfilePage';
import { AdminPage, type AdminSection } from './pages/AdminPage';
import { BannedUserPage } from './pages/BannedUserPage';
import type { BankOffer } from './types';
import { Shield, Sparkles, AlertCircle, Landmark } from 'lucide-react';

type ProfileSection = 'overview' | 'microloans' | 'scenarios' | 'compare' | 'history';

interface AppLocation {
  tab: NavTab | 'admin';
  profile: ProfileSection;
  admin: AdminSection;
}

const PROFILE_SECTIONS: ProfileSection[] = ['overview', 'microloans', 'scenarios', 'compare', 'history'];
const ADMIN_SECTIONS: AdminSection[] = ['overview', 'users', 'loans', 'products', 'rates', 'journal'];

const parseHash = (): AppLocation => {
  const raw = window.location.hash.replace('#', '').toLowerCase();
  const [head, sub] = raw.split('/');
  if (head === 'admin') {
    const adminSection = sub === 'applications' ? 'loans' : (sub as AdminSection);
    return {
      tab: 'admin',
      profile: 'overview',
      admin: ADMIN_SECTIONS.includes(adminSection) ? adminSection : 'overview',
    };
  }
  if (head === 'profile' || head === 'scenarios' || head === 'compare' || head === 'history' || head === 'microloans') {
    const profile: ProfileSection =
      head === 'scenarios'
        ? 'scenarios'
        : head === 'compare'
          ? 'compare'
          : head === 'history'
            ? 'history'
            : head === 'microloans'
              ? 'microloans'
              : PROFILE_SECTIONS.includes(sub as ProfileSection)
                ? (sub as ProfileSection)
                : 'overview';
    return { tab: 'profile', profile, admin: 'overview' };
  }
  if (head === 'microloan') {
    return { tab: 'microloan', profile: 'overview', admin: 'overview' };
  }
  return { tab: 'calculator', profile: 'overview', admin: 'overview' };
};

const toHash = (location: AppLocation) => {
  if (location.tab === 'admin') {
    return location.admin === 'overview' ? 'admin' : `admin/${location.admin}`;
  }
  if (location.tab === 'profile') {
    return location.profile === 'overview' ? 'profile' : `profile/${location.profile}`;
  }
  return location.tab;
};

export const MainApp: React.FC = () => {
  const { user, loginWithOAuth2Code, loginWithMagicLink } = useAuth();
  const { t } = useLanguage();
  const [location, setLocation] = useState<AppLocation>(parseHash);
  const [presetOffer, setPresetOffer] = useState<BankOffer | null>(null);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authNotice, setAuthNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const handledAuthRef = useRef<string | null>(null);

  const go = (next: AppLocation) => {
    setLocation(next);
    const hash = toHash(next);
    if (window.location.hash.replace('#', '') !== hash) {
      window.location.hash = hash;
    }
  };

  const setActiveTab = (tab: NavTab) => {
    go({
      tab,
      profile: tab === 'profile' ? location.profile : 'overview',
      admin: 'overview',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  useEffect(() => {
    const handleHashChange = () => setLocation(parseHash());
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, []);

  useEffect(() => {
    if (user?.role === 'ROLE_ADMIN') {
      if (location.tab !== 'admin') {
        go({ tab: 'admin', profile: 'overview', admin: location.admin || 'overview' });
      }
    } else {
      if (location.tab === 'admin') {
        go({ tab: 'calculator', profile: 'overview', admin: 'overview' });
      }
    }
  }, [user, location.tab]);

  useEffect(() => {
    if (user && user.enabled === false) {
      if (window.location.hash !== '#banned') {
        window.location.hash = 'banned';
      }
    } else if (window.location.hash === '#banned') {
      window.location.hash = '';
    }
  }, [user]);

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
  }, [loginWithOAuth2Code, loginWithMagicLink, t]);

  const isAdmin = user?.role === 'ROLE_ADMIN';

  if (user && user.enabled === false) {
    return <BannedUserPage />;
  }

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 font-sans transition-colors">
      {!isAdmin && (
        <Navbar
          activeTab={location.tab === 'admin' ? 'calculator' : location.tab}
          setActiveTab={setActiveTab}
          onOpenAuth={() => setAuthModalOpen(true)}
        />
      )}

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
        {location.tab === 'admin' && (
          <AdminPage
            section={location.admin}
            onSectionChange={(nextSection) =>
              go({ tab: 'admin', profile: 'overview', admin: nextSection })
            }
          />
        )}

        {location.tab === 'calculator' && (
          <CalculatorPage
            presetOffer={presetOffer}
            onClearPreset={() => setPresetOffer(null)}
            onOpenAuth={() => setAuthModalOpen(true)}
          />
        )}

        {location.tab === 'microloan' && (
          <MicroloanCalculatorPage
            onOpenAuth={() => setAuthModalOpen(true)}
            onNavigateToProfile={() => setActiveTab('profile')}
          />
        )}

        {location.tab === 'profile' && (
          <ProfilePage
            onOpenAuth={() => setAuthModalOpen(true)}
            onNavigateToCalculator={() => setActiveTab('calculator')}
            onNavigateToMicroloan={() => setActiveTab('microloan')}
          />
        )}
      </main>

      {/* Official Banking Footer */}
      {!isAdmin && (
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
      )}

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
