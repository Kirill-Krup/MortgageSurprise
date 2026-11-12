import React from 'react';
import { useAuth } from '../context/AuthContext';
import {
  Calculator,
  GitCompare,
  Building2,
  FolderBookmark,
  LogOut,
  User as UserIcon,
  ShieldCheck,
} from 'lucide-react';

interface NavbarProps {
  activeTab: 'calculator' | 'compare' | 'offers' | 'scenarios';
  setActiveTab: (tab: 'calculator' | 'compare' | 'offers' | 'scenarios') => void;
  onOpenAuth: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  onOpenAuth,
}) => {
  const { user, logout } = useAuth();

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Brand */}
          <div
            className="flex items-center gap-2.5 cursor-pointer"
            onClick={() => setActiveTab('calculator')}
          >
            <div className="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center text-white shadow-md shadow-indigo-100">
              <Calculator size={22} />
            </div>
            <div>
              <span className="font-extrabold text-lg text-slate-900 tracking-tight">
                Mortgage<span className="text-indigo-600">Surprise</span>
              </span>
              <span className="hidden sm:inline-block ml-2 text-xs font-semibold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700">
                PRO
              </span>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center gap-1">
            <button
              onClick={() => setActiveTab('calculator')}
              className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-medium transition-all cursor-pointer ${
                activeTab === 'calculator'
                  ? 'bg-indigo-50 text-indigo-600 font-semibold'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
              }`}
            >
              <Calculator size={18} />
              <span>Калькулятор</span>
            </button>

            <button
              onClick={() => setActiveTab('compare')}
              className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-medium transition-all cursor-pointer ${
                activeTab === 'compare'
                  ? 'bg-indigo-50 text-indigo-600 font-semibold'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
              }`}
            >
              <GitCompare size={18} />
              <span>Сравнение</span>
            </button>

            <button
              onClick={() => setActiveTab('offers')}
              className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-medium transition-all cursor-pointer ${
                activeTab === 'offers'
                  ? 'bg-indigo-50 text-indigo-600 font-semibold'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
              }`}
            >
              <Building2 size={18} />
              <span>Банки</span>
            </button>

            {user && (
              <button
                onClick={() => setActiveTab('scenarios')}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-sm font-medium transition-all cursor-pointer ${
                  activeTab === 'scenarios'
                    ? 'bg-indigo-50 text-indigo-600 font-semibold'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <FolderBookmark size={18} />
                <span>Мои сценарии</span>
              </button>
            )}
          </nav>

          {/* User actions */}
          <div className="flex items-center gap-3">
            {user ? (
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2 text-sm bg-slate-50 px-3 py-1.5 rounded-xl border border-slate-200">
                  <UserIcon size={16} className="text-slate-500" />
                  <span className="font-medium text-slate-700 max-w-[140px] truncate">
                    {user.email}
                  </span>
                  {user.role === 'ROLE_ADMIN' && (
                    <span className="flex items-center gap-1 text-[11px] font-bold px-1.5 py-0.5 rounded bg-amber-100 text-amber-800">
                      <ShieldCheck size={12} />
                      ADMIN
                    </span>
                  )}
                </div>
                <button
                  onClick={logout}
                  title="Выйти"
                  className="p-2 text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-xl transition-colors cursor-pointer"
                >
                  <LogOut size={18} />
                </button>
              </div>
            ) : (
              <button
                onClick={onOpenAuth}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white text-sm font-medium rounded-xl shadow-sm transition-all cursor-pointer"
              >
                Войти
              </button>
            )}
          </div>
        </div>

        {/* Mobile Navigation bar */}
        <div className="flex md:hidden border-t border-slate-100 py-2 justify-around">
          <button
            onClick={() => setActiveTab('calculator')}
            className={`flex flex-col items-center gap-1 py-1 px-2 text-xs font-medium ${
              activeTab === 'calculator' ? 'text-indigo-600' : 'text-slate-500'
            }`}
          >
            <Calculator size={18} />
            <span>Расчет</span>
          </button>
          <button
            onClick={() => setActiveTab('compare')}
            className={`flex flex-col items-center gap-1 py-1 px-2 text-xs font-medium ${
              activeTab === 'compare' ? 'text-indigo-600' : 'text-slate-500'
            }`}
          >
            <GitCompare size={18} />
            <span>Сравнение</span>
          </button>
          <button
            onClick={() => setActiveTab('offers')}
            className={`flex flex-col items-center gap-1 py-1 px-2 text-xs font-medium ${
              activeTab === 'offers' ? 'text-indigo-600' : 'text-slate-500'
            }`}
          >
            <Building2 size={18} />
            <span>Банки</span>
          </button>
          {user && (
            <button
              onClick={() => setActiveTab('scenarios')}
              className={`flex flex-col items-center gap-1 py-1 px-2 text-xs font-medium ${
                activeTab === 'scenarios' ? 'text-indigo-600' : 'text-slate-500'
              }`}
            >
              <FolderBookmark size={18} />
              <span>Сценарии</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
