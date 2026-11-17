import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { useLanguage } from '../context/LanguageContext';
import {
  Calculator,
  GitCompare,
  Building2,
  FolderBookmark,
  LogOut,
  User as UserIcon,
  ShieldCheck,
  ChevronDown,
  Sun,
  Moon,
  Globe,
  Home,
  Zap,
  Landmark,
} from 'lucide-react';

export type NavTab = 'calculator' | 'microloan' | 'compare' | 'offers' | 'scenarios';

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
  const isAdmin = user?.role === 'ROLE_ADMIN';

  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const isCalculatorActive = activeTab === 'calculator' || activeTab === 'microloan';

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
            onClick={() => setActiveTab(isAdmin ? 'offers' : 'calculator')}
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

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center gap-1">
            {/* Calculators Dropdown (Hidden for Admin) */}
            {!isAdmin && (
              <div className="relative" ref={dropdownRef}>
                <button
                  onClick={() => setDropdownOpen((prev) => !prev)}
                  className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-sm font-semibold transition-all cursor-pointer ${
                    isCalculatorActive
                      ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-400'
                      : 'text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800'
                  }`}
                >
                  <Calculator size={17} />
                  <span>{t('navCalculators')}</span>
                  <ChevronDown
                    size={15}
                    className={`transition-transform duration-200 ${dropdownOpen ? 'rotate-180' : ''}`}
                  />
                </button>

                {/* Dropdown Menu */}
                {dropdownOpen && (
                  <div className="absolute top-full left-0 mt-2 w-72 rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 shadow-xl py-2 z-50 animate-in fade-in slide-in-from-top-2 duration-150">
                    <div className="px-3 py-1.5 text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                      {t('calculatorsTitle')}
                    </div>
                    <button
                      onClick={() => {
                        setActiveTab('calculator');
                        setDropdownOpen(false);
                      }}
                      className={`w-full text-left px-3.5 py-2.5 flex items-start gap-3 transition-colors cursor-pointer ${
                        activeTab === 'calculator'
                          ? 'bg-blue-50 dark:bg-blue-950/50 text-blue-700 dark:text-blue-400'
                          : 'text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700/50'
                      }`}
                    >
                      <div className="p-2 rounded-xl bg-blue-100 dark:bg-blue-900/60 text-blue-700 dark:text-blue-300 mt-0.5">
                        <Home size={17} />
                      </div>
                      <div>
                        <div className="font-semibold text-sm">{t('navMortgage')}</div>
                        <div className="text-xs text-slate-500 dark:text-slate-400">
                          {t('navMortgageDesc')}
                        </div>
                      </div>
                    </button>

                    <button
                      onClick={() => {
                        setActiveTab('microloan');
                        setDropdownOpen(false);
                      }}
                      className={`w-full text-left px-3.5 py-2.5 flex items-start gap-3 transition-colors cursor-pointer ${
                        activeTab === 'microloan'
                          ? 'bg-blue-50 dark:bg-blue-950/50 text-blue-700 dark:text-blue-400'
                          : 'text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700/50'
                      }`}
                    >
                      <div className="p-2 rounded-xl bg-amber-100 dark:bg-amber-900/60 text-amber-700 dark:text-amber-300 mt-0.5">
                        <Zap size={17} />
                      </div>
                      <div>
                        <div className="font-semibold text-sm flex items-center gap-1.5">
                          <span>{t('navMicroloan')}</span>
                          <span className="text-[10px] font-bold px-1.5 py-0.2 rounded bg-amber-100 dark:bg-amber-900 text-amber-800 dark:text-amber-200">
                            NEW
                          </span>
                        </div>
                        <div className="text-xs text-slate-500 dark:text-slate-400">
                          {t('navMicroloanDesc')}
                        </div>
                      </div>
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* Compare (Hidden for Admin) */}
            {!isAdmin && (
              <button
                onClick={() => setActiveTab('compare')}
                className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-sm font-semibold transition-all cursor-pointer ${
                  activeTab === 'compare'
                    ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-400'
                    : 'text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                <GitCompare size={17} />
                <span>{t('navCompare')}</span>
              </button>
            )}

            <button
              onClick={() => setActiveTab('offers')}
              className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-sm font-semibold transition-all cursor-pointer ${
                activeTab === 'offers'
                  ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-400'
                  : 'text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              <Building2 size={17} />
              <span>{t('navOffers')}</span>
            </button>

            {user && user.role !== 'ROLE_ADMIN' && (
              <button
                onClick={() => setActiveTab('scenarios')}
                className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-sm font-semibold transition-all cursor-pointer ${
                  activeTab === 'scenarios'
                    ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-400'
                    : 'text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                <FolderBookmark size={17} />
                <span>{t('navScenarios')}</span>
              </button>
            )}
          </nav>

          {/* User actions + Theme & Language Switchers */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Language Switcher */}
            <button
              onClick={toggleLanguage}
              title={`Switch to ${language === 'ru' ? 'English' : 'Русский'}`}
              className="flex items-center gap-1 px-2.5 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-bold text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
            >
              <Globe size={14} className="text-blue-600 dark:text-blue-400" />
              <span>{language.toUpperCase()}</span>
            </button>

            {/* Theme Toggle */}
            <button
              onClick={toggleTheme}
              title={theme === 'dark' ? t('themeLight') : t('themeDark')}
              className="p-2 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
            >
              {theme === 'dark' ? (
                <Sun size={17} className="text-amber-400" />
              ) : (
                <Moon size={17} className="text-slate-600" />
              )}
            </button>

            {/* Auth section */}
            {user ? (
              <div className="flex items-center gap-2">
                <div className="hidden sm:flex items-center gap-2 text-xs bg-slate-50 dark:bg-slate-800 px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700">
                  <UserIcon size={14} className="text-blue-600" />
                  <span className="font-semibold text-slate-700 dark:text-slate-300 max-w-[130px] truncate">
                    {user.email}
                  </span>
                  {user.role === 'ROLE_ADMIN' && (
                    <span className="flex items-center gap-1 text-[10px] font-extrabold px-1.5 py-0.5 rounded bg-amber-100 text-amber-800">
                      ADMIN
                    </span>
                  )}
                </div>
                <button
                  onClick={logout}
                  title={t('navLogout')}
                  className="p-2 text-slate-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30 rounded-xl transition-colors cursor-pointer"
                >
                  <LogOut size={17} />
                </button>
              </div>
            ) : (
              <button
                onClick={onOpenAuth}
                className="px-4 py-2 bg-blue-700 hover:bg-blue-800 text-white text-xs sm:text-sm font-semibold rounded-xl shadow-sm transition-all cursor-pointer"
              >
                {t('navLogin')}
              </button>
            )}
          </div>
        </div>

        {/* Mobile Navigation bar */}
        <div className="flex md:hidden border-t border-slate-100 dark:border-slate-800 py-2 justify-around">
          {!isAdmin && (
            <>
              <button
                onClick={() => setActiveTab('calculator')}
                className={`flex flex-col items-center gap-1 py-1 px-2 text-[11px] font-semibold cursor-pointer ${
                  activeTab === 'calculator'
                    ? 'text-blue-600 dark:text-blue-400'
                    : 'text-slate-500 dark:text-slate-400'
                }`}
              >
                <Home size={17} />
                <span>{t('navMortgage')}</span>
              </button>
              <button
                onClick={() => setActiveTab('microloan')}
                className={`flex flex-col items-center gap-1 py-1 px-2 text-[11px] font-semibold cursor-pointer ${
                  activeTab === 'microloan'
                    ? 'text-amber-600 dark:text-amber-400'
                    : 'text-slate-500 dark:text-slate-400'
                }`}
              >
                <Zap size={17} />
                <span>{t('navMicroloan')}</span>
              </button>
              <button
                onClick={() => setActiveTab('compare')}
                className={`flex flex-col items-center gap-1 py-1 px-2 text-[11px] font-semibold cursor-pointer ${
                  activeTab === 'compare'
                    ? 'text-blue-600 dark:text-blue-400'
                    : 'text-slate-500 dark:text-slate-400'
                }`}
              >
                <GitCompare size={17} />
                <span>{t('navCompare')}</span>
              </button>
            </>
          )}
          <button
            onClick={() => setActiveTab('offers')}
            className={`flex flex-col items-center gap-1 py-1 px-2 text-[11px] font-semibold cursor-pointer ${
              activeTab === 'offers'
                ? 'text-blue-600 dark:text-blue-400'
                : 'text-slate-500 dark:text-slate-400'
            }`}
          >
            <Building2 size={17} />
            <span>{t('navOffers')}</span>
          </button>
          {user && user.role !== 'ROLE_ADMIN' && (
            <button
              onClick={() => setActiveTab('scenarios')}
              className={`flex flex-col items-center gap-1 py-1 px-2 text-[11px] font-semibold cursor-pointer ${
                activeTab === 'scenarios'
                  ? 'text-blue-600 dark:text-blue-400'
                  : 'text-slate-500 dark:text-slate-400'
              }`}
            >
              <FolderBookmark size={17} />
              <span>{t('navScenarios')}</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
