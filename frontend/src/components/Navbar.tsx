import React from 'react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { useLanguage } from '../context/LanguageContext';
import {
  LogOut,
  User as UserIcon,
  ShieldCheck,
  Sun,
  Moon,
  Globe,
  Home,
  Zap,
  Landmark,
} from 'lucide-react';

export type NavTab = 'calculator' | 'microloan' | 'profile';

interface NavbarProps {
  activeTab: NavTab;
  setActiveTab: (tab: NavTab) => void;
  onOpenAuth: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  onOpenAuth,
}) => {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const { language, toggleLanguage, t } = useLanguage();

  const handleLogout = async () => {
    await logout();
    if (activeTab === 'profile') {
      setActiveTab('calculator');
    }
  };

  return (
    <header className="bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 sticky top-0 z-50 transition-colors shadow-xs">
      {/* Top official banner */}
      <div className="bg-slate-950 text-slate-300 text-[11px] py-1 px-4 sm:px-8 border-b border-slate-800 hidden sm:flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="flex items-center gap-1 font-semibold text-slate-200">
            <Landmark size={12} className="text-blue-400" />
            {t('bankName')} {t('bankType')}
          </span>
          <span className="text-slate-600">|</span>
          <span className="text-slate-400">{t('bankLicense')}</span>
        </div>
        <div className="flex items-center gap-4 text-slate-400">
          <span className="flex items-center gap-1">
            <ShieldCheck size={12} className="text-emerald-400" />
            {t('securitySession')}
          </span>
          <span>{t('insuranceBadge')}</span>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Bank Logo & Brand */}
          <div
            className="flex items-center gap-3 cursor-pointer select-none"
            onClick={() => setActiveTab('calculator')}
          >
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-700 to-indigo-900 flex items-center justify-center text-white shadow-md shadow-blue-900/20 border border-blue-600/30">
              <Landmark size={20} className="text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-extrabold text-lg text-slate-950 dark:text-white tracking-tight">
                  Mortgage<span className="text-blue-600 dark:text-blue-400">Surprise</span>
                </span>
                <span className="text-[10px] font-black uppercase tracking-wider px-1.5 py-0.5 rounded bg-blue-100 dark:bg-blue-950 text-blue-800 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                  BANK
                </span>
              </div>
              <p className="text-[10px] text-slate-500 dark:text-slate-400 font-medium tracking-wider uppercase -mt-0.5">
                Financial Services
              </p>
            </div>
          </div>

          {/* Calculators Only Navigation */}
          <nav className="hidden md:flex items-center bg-slate-100/90 dark:bg-slate-800/80 p-1 rounded-2xl border border-slate-200/70 dark:border-slate-700/60 shadow-xs">
            <button
              onClick={() => setActiveTab('calculator')}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs lg:text-sm font-semibold transition-all cursor-pointer ${
                activeTab === 'calculator'
                  ? 'bg-white dark:bg-slate-900 text-blue-700 dark:text-blue-400 shadow-sm font-bold'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-white/40 dark:hover:bg-slate-700/40'
              }`}
            >
              <Home size={16} />
              <span>{t('navMortgage')}</span>
            </button>

            <button
              onClick={() => setActiveTab('microloan')}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs lg:text-sm font-semibold transition-all cursor-pointer ${
                activeTab === 'microloan'
                  ? 'bg-white dark:bg-slate-900 text-amber-600 dark:text-amber-400 shadow-sm font-bold'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-white/40 dark:hover:bg-slate-700/40'
              }`}
            >
              <Zap size={16} />
              <span>{t('navMicroloan')}</span>
            </button>
          </nav>

          {/* User actions + Theme & Language Switchers */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Language & Theme capsule */}
            <div className="flex items-center bg-slate-100 dark:bg-slate-800/70 p-1 rounded-xl border border-slate-200/70 dark:border-slate-700/60">
              <button
                onClick={toggleLanguage}
                title={`Switch to ${language === 'ru' ? 'English' : 'Русский'}`}
                className="flex items-center gap-1 px-2 py-1 rounded-lg text-xs font-bold text-slate-700 dark:text-slate-200 hover:bg-white dark:hover:bg-slate-700 transition-colors cursor-pointer"
              >
                <Globe size={13} className="text-blue-600 dark:text-blue-400" />
                <span>{language.toUpperCase()}</span>
              </button>

              <div className="w-px h-3.5 bg-slate-300 dark:bg-slate-700 mx-0.5" />

              <button
                onClick={toggleTheme}
                title={theme === 'dark' ? t('themeLight') : t('themeDark')}
                className="p-1 rounded-lg text-slate-700 dark:text-slate-200 hover:bg-white dark:hover:bg-slate-700 transition-colors cursor-pointer"
              >
                {theme === 'dark' ? (
                  <Sun size={15} className="text-amber-400" />
                ) : (
                  <Moon size={15} className="text-slate-600" />
                )}
              </button>
            </div>

            {/* Auth / Profile section */}
            {user ? (
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={() => setActiveTab('profile')}
                  className={`flex items-center gap-2 text-xs font-semibold px-3 py-1.5 rounded-xl border transition-all cursor-pointer ${
                    activeTab === 'profile'
                      ? 'bg-blue-50 dark:bg-blue-950/60 border-blue-400 text-blue-700 dark:text-blue-300 shadow-xs'
                      : 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 hover:border-blue-300 dark:hover:border-blue-700'
                  }`}
                  title={t('navProfile')}
                >
                  <div className="w-5 h-5 rounded-lg bg-blue-600 text-white flex items-center justify-center text-[10px] font-black">
                    {(user.email?.[0] || 'U').toUpperCase()}
                  </div>
                  <span className="hidden sm:inline max-w-[130px] truncate">
                    {user.email}
                  </span>
                  <span className="sm:hidden">{t('navProfile')}</span>
                </button>

                <button
                  onClick={handleLogout}
                  title={t('navLogout')}
                  className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30 rounded-xl transition-colors cursor-pointer"
                >
                  <LogOut size={16} />
                </button>
              </div>
            ) : (
              <button
                onClick={onOpenAuth}
                className="flex items-center gap-1.5 px-3.5 py-1.5 bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white text-xs font-bold rounded-xl shadow-xs transition-all cursor-pointer"
              >
                <UserIcon size={14} />
                <span>{t('navLogin')}</span>
              </button>
            )}
          </div>
        </div>

        {/* Mobile Navigation bar */}
        <div className="flex md:hidden border-t border-slate-100 dark:border-slate-800 py-2 justify-around">
          <button
            onClick={() => setActiveTab('calculator')}
            className={`flex flex-col items-center gap-1 py-1 px-3 text-[11px] font-semibold cursor-pointer ${
              activeTab === 'calculator'
                ? 'text-blue-600 dark:text-blue-400 font-bold'
                : 'text-slate-500 dark:text-slate-400'
            }`}
          >
            <Home size={18} />
            <span>{t('navMortgage')}</span>
          </button>
          <button
            onClick={() => setActiveTab('microloan')}
            className={`flex flex-col items-center gap-1 py-1 px-3 text-[11px] font-semibold cursor-pointer ${
              activeTab === 'microloan'
                ? 'text-amber-600 dark:text-amber-400 font-bold'
                : 'text-slate-500 dark:text-slate-400'
            }`}
          >
            <Zap size={18} />
            <span>{t('navMicroloan')}</span>
          </button>
          <button
            onClick={() => setActiveTab('profile')}
            className={`flex flex-col items-center gap-1 py-1 px-3 text-[11px] font-semibold cursor-pointer ${
              activeTab === 'profile'
                ? 'text-blue-600 dark:text-blue-400 font-bold'
                : 'text-slate-500 dark:text-slate-400'
            }`}
          >
            <UserIcon size={18} />
            <span>{t('navProfile')}</span>
          </button>
        </div>
      </div>
    </header>
  );
};
