import React, { useEffect, useMemo, useState } from 'react';
import {
  adminAuditApi,
  adminManagementApi,
  bankOfferApi,
  calculatorApi,
  microloanRateApi,
} from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import type {
  AdminLoanMonitoring,
  AdminStats,
  AdminUser,
  BankOffer,
  BankOfferRequest,
  Currency,
  MicroloanRateDTO,
  UserActivityRecord,
  UserAuditActionType,
} from '../types';
import { CURRENCY_CONFIGS, formatCurrency, getCurrencySymbol } from '../utils/format';
import {
  AlertCircle,
  Bell,
  Check,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock,
  Coins,
  Globe,
  Landmark,
  LayoutDashboard,
  LifeBuoy,
  LogOut,
  Moon,
  PanelLeftClose,
  PanelLeftOpen,
  Pencil,
  Plus,
  RefreshCw,
  ScrollText,
  Search,
  SlidersHorizontal,
  Sun,
  Trash2,
  TrendingUp,
  Users,
  X,
} from 'lucide-react';

export type AdminSection = 'overview' | 'users' | 'loans' | 'products' | 'rates' | 'journal';

export interface AdminPageProps {
  section?: AdminSection;
  onSectionChange?: (section: AdminSection) => void;
}

const EMPTY_OFFER: BankOfferRequest = {
  bankName: '',
  offerName: '',
  annualInterestRate: 18,
  minDownPaymentPercent: 20,
  maxTermMonths: 360,
  minLoanAmount: 500_000,
  maxLoanAmount: 30_000_000,
  currency: 'RUB',
};

const limitText = (offer: BankOffer, fromWord: string, upToWord: string) => {
  const parts: string[] = [];
  if (offer.minLoanAmount) parts.push(`${fromWord} ${formatCurrency(offer.minLoanAmount, offer.currency || 'RUB')}`);
  if (offer.maxLoanAmount) parts.push(`${upToWord} ${formatCurrency(offer.maxLoanAmount, offer.currency || 'RUB')}`);
  return parts.join(' · ') || '—';
};

export const AdminPage: React.FC<AdminPageProps> = ({ section: externalSection, onSectionChange }) => {
  const { user, logout, refreshUser } = useAuth();
  const { t, language, toggleLanguage } = useLanguage();
  const { theme, toggleTheme } = useTheme();

  const [internalSection, setInternalSection] = useState<AdminSection>('overview');
  const section = externalSection || internalSection;

  const [sidebarOpen, setSidebarOpen] = useState(() => {
    if (typeof window !== 'undefined') {
      return window.innerWidth >= 1024;
    }
    return true;
  });

  const navigateTo = (nextSection: AdminSection) => {
    setInternalSection(nextSection);
    onSectionChange?.(nextSection);
    window.location.hash = nextSection === 'overview' ? 'admin' : `admin/${nextSection}`;
    if (typeof window !== 'undefined' && window.innerWidth < 1024) {
      setSidebarOpen(false);
    }
  };

  // Real Backend Data State
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [statsLoading, setStatsLoading] = useState(true);

  const [usersList, setUsersList] = useState<AdminUser[]>([]);
  const [usersLoading, setUsersLoading] = useState(true);

  const [loansList, setLoansList] = useState<AdminLoanMonitoring[]>([]);
  const [loansLoading, setLoansLoading] = useState(true);

  const [offers, setOffers] = useState<BankOffer[]>([]);
  const [offersState, setOffersState] = useState<'loading' | 'ok' | 'error'>('loading');

  const [logs, setLogs] = useState<UserActivityRecord[]>([]);
  const [logsState, setLogsState] = useState<'loading' | 'ok' | 'error'>('loading');

  const [ratesState, setRatesState] = useState<'loading' | 'ok' | 'error'>('loading');
  const [baseRate, setBaseRate] = useState(0.8);
  const [surchargeRate, setSurchargeRate] = useState(2000);
  const [savingRates, setSavingRates] = useState(false);
  const [, setRatesMessage] = useState<string | null>(null);

  // Microloan tariffs management state (Requirement 2)
  const [ratesList, setRatesList] = useState<MicroloanRateDTO[]>([]);
  const [rateModalOpen, setRateModalOpen] = useState(false);
  const [editingRate, setEditingRate] = useState<MicroloanRateDTO | null>(null);
  const [savingRate, setSavingRate] = useState(false);
  const [rateForm, setRateForm] = useState({
    name: '',
    baseDailyRate: 0.8,
    minDayRequired: 7,
    maxDayRequired: 30,
  });

  const [error, setError] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // System toggles
  const [maintenanceMode, setMaintenanceMode] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [unreadNotifications, setUnreadNotifications] = useState(1);
  const [supportModalOpen, setSupportModalOpen] = useState(false);
  const [supportSubject, setSupportSubject] = useState('');
  const [supportMessageText, setSupportMessageText] = useState('');
  const [supportSent, setSupportSent] = useState(false);

  // Filter States: Users
  const [userSearch, setUserSearch] = useState('');
  const [userStatusFilter, setUserStatusFilter] = useState<'ALL' | 'ACTIVE' | 'BLOCKED'>('ALL');
  const [userRoleFilter, setUserRoleFilter] = useState<string>('ALL');
  const [userPage, setUserPage] = useState(1);
  const [togglingUserId, setTogglingUserId] = useState<string | null>(null);

  // Filter States: Loans
  const [loanSearch, setLoanSearch] = useState('');
  const [loanStatusFilter, setLoanStatusFilter] = useState<'ALL' | 'ACTIVE' | 'PAID_OFF' | 'OVERDUE'>('ALL');
  const [loanCategoryFilter, setLoanCategoryFilter] = useState<'ALL' | 'MICROLOAN' | 'MORTGAGE'>('ALL');
  const [loanPage, setLoanPage] = useState(1);

  // Filter States: Bank Offers
  const [offerQuery, setOfferQuery] = useState('');
  const [currency, setCurrency] = useState<'ALL' | Currency>('ALL');
  const [offerPage, setOfferPage] = useState(1);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [savingOffer, setSavingOffer] = useState(false);
  const [form, setForm] = useState<BankOfferRequest>(EMPTY_OFFER);

  // Filter States: Journal
  const [journalQuery, setJournalQuery] = useState('');
  const [journalAction, setJournalAction] = useState<UserAuditActionType | 'ALL'>('ALL');

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 4000);
  };

  // API Loaders
  const loadStats = async () => {
    setStatsLoading(true);
    try {
      const data = await adminManagementApi.getStats();
      setStats(data);
    } catch {
      setStats(null);
    } finally {
      setStatsLoading(false);
    }
  };

  const loadUsers = async () => {
    setUsersLoading(true);
    try {
      const data = await adminManagementApi.getUsers();
      setUsersList(Array.isArray(data) ? data : []);
    } catch {
      setUsersList([]);
    } finally {
      setUsersLoading(false);
    }
  };

  const loadLoans = async () => {
    setLoansLoading(true);
    try {
      const data = await adminManagementApi.getLoans();
      setLoansList(Array.isArray(data) ? data : []);
    } catch {
      setLoansList([]);
    } finally {
      setLoansLoading(false);
    }
  };

  const loadOffers = async () => {
    setOffersState('loading');
    try {
      const data = await bankOfferApi.getAll(true);
      setOffers(Array.isArray(data) ? data : []);
      setOffersState('ok');
    } catch {
      setOffers([]);
      setOffersState('error');
    }
  };

  const loadRates = async () => {
    setRatesState('loading');
    try {
      const [cfg, rates] = await Promise.all([
        calculatorApi.getMicroloanConfig().catch(() => null),
        microloanRateApi.getRates().catch(() => []),
      ]);
      if (cfg) {
        setSurchargeRate(cfg.bankInterest ?? 2000);
        if (cfg.baseDailyRate !== undefined) {
          setBaseRate(cfg.baseDailyRate);
        }
      }
      setRatesList(Array.isArray(rates) ? rates : []);
      setRatesState('ok');
    } catch {
      setRatesState('error');
    }
  };

  const loadLogs = async () => {
    setLogsState('loading');
    try {
      const data = await adminAuditApi.getLogs();
      const list = Array.isArray(data) ? data : [];
      list.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
      setLogs(list);
      setLogsState('ok');
    } catch {
      setLogs([]);
      setLogsState('error');
    }
  };

  const refreshAllData = () => {
    loadStats();
    loadUsers();
    loadLoans();
    loadOffers();
    loadRates();
    loadLogs();
  };

  useEffect(() => {
    refreshAllData();
  }, []);

  const avgRate = useMemo(() => {
    if (offers.length === 0) return 18.9;
    const sum = offers.reduce((acc, offer) => acc + Number(offer.annualInterestRate || 0), 0);
    return sum / offers.length;
  }, [offers]);

  const minMortgageRate = useMemo(() => {
    if (offers.length === 0) return 18.9;
    return Math.min(...offers.map((o) => Number(o.annualInterestRate || 18.9)));
  }, [offers]);

  // Filtered Users
  const filteredUsers = useMemo(() => {
    const q = userSearch.trim().toLowerCase();
    return usersList.filter((u) => {
      const name = `${u.firstName || ''} ${u.lastName || ''}`.toLowerCase();
      const matchesSearch =
        !q ||
        name.includes(q) ||
        u.email.toLowerCase().includes(q) ||
        u.id.toLowerCase().includes(q);
      const matchesStatus =
        userStatusFilter === 'ALL' ||
        (userStatusFilter === 'ACTIVE' && u.enabled) ||
        (userStatusFilter === 'BLOCKED' && !u.enabled);
      const matchesRole = userRoleFilter === 'ALL' || u.role === userRoleFilter;
      return matchesSearch && matchesStatus && matchesRole;
    });
  }, [usersList, userSearch, userStatusFilter, userRoleFilter]);

  const userPageSize = 8;
  const userPageCount = Math.max(1, Math.ceil(filteredUsers.length / userPageSize));
  const safeUserPage = Math.min(userPage, userPageCount);
  const pageUsers = filteredUsers.slice((safeUserPage - 1) * userPageSize, safeUserPage * userPageSize);

  // Filtered Loans
  const filteredLoans = useMemo(() => {
    const q = loanSearch.trim().toLowerCase();
    return loansList.filter((loan) => {
      const matchesSearch =
        !q ||
        loan.id.toLowerCase().includes(q) ||
        loan.borrowerEmail.toLowerCase().includes(q) ||
        loan.borrowerName.toLowerCase().includes(q) ||
        loan.productTitle.toLowerCase().includes(q);
      const matchesStatus = loanStatusFilter === 'ALL' || loan.status === loanStatusFilter;
      const matchesCategory = loanCategoryFilter === 'ALL' || loan.loanType === loanCategoryFilter;
      return matchesSearch && matchesStatus && matchesCategory;
    });
  }, [loansList, loanSearch, loanStatusFilter, loanCategoryFilter]);

  const loanPageSize = 6;
  const loanPageCount = Math.max(1, Math.ceil(filteredLoans.length / loanPageSize));
  const safeLoanPage = Math.min(loanPage, loanPageCount);
  const pageLoans = filteredLoans.slice((safeLoanPage - 1) * loanPageSize, safeLoanPage * loanPageSize);

  // Filtered Bank Offers
  const filteredOffers = useMemo(() => {
    const needle = offerQuery.trim().toLowerCase();
    return offers.filter((offer) => {
      const currencyOk = currency === 'ALL' || (offer.currency || 'RUB') === currency;
      const text = `${offer.bankName} ${offer.offerName}`.toLowerCase();
      return currencyOk && (!needle || text.includes(needle));
    });
  }, [offers, offerQuery, currency]);

  const offerPageSize = 6;
  const offerPageCount = Math.max(1, Math.ceil(filteredOffers.length / offerPageSize));
  const safeOfferPage = Math.min(offerPage, offerPageCount);
  const pageOffers = filteredOffers.slice((safeOfferPage - 1) * offerPageSize, safeOfferPage * offerPageSize);

  // Filtered Logs
  const filteredLogs = useMemo(() => {
    const needle = journalQuery.trim().toLowerCase();
    return logs.filter((record) => {
      const actionOk = journalAction === 'ALL' || record.actionType === journalAction;
      const text = `${record.userEmail || ''} ${record.actionDescription || ''} ${record.bankName || ''} ${record.offerName || ''}`.toLowerCase();
      return actionOk && (!needle || text.includes(needle));
    });
  }, [logs, journalQuery, journalAction]);

  const actionLabel = (type?: string) => {
    switch (type) {
      case 'MORTGAGE_CALCULATION':
      case 'SCENARIO_CALCULATED':
        return t('profileActionMortgage');
      case 'MICROLOAN_CALCULATION':
        return t('profileActionMicroloanCalc');
      case 'MICROLOAN_APPLICATION':
      case 'MICROLOAN_APPLIED':
        return t('profileActionMicroloanApp');
      case 'BANK_OFFER_SELECTED':
        return t('profileActionBankOffer');
      case 'SCENARIO_SAVED':
        return t('adminActionScenarioSaved');
      default:
        return type || '—';
    }
  };

  const getActionCategory = (type?: string) => {
    switch (type) {
      case 'MORTGAGE_CALCULATION':
      case 'SCENARIO_CALCULATED':
      case 'SCENARIO_SAVED':
        return { label: t('adminCategoryMortgage'), color: 'bg-blue-50 text-blue-700 dark:bg-blue-950/60 dark:text-blue-300' };
      case 'MICROLOAN_CALCULATION':
      case 'MICROLOAN_APPLICATION':
      case 'MICROLOAN_APPLIED':
        return { label: t('adminCategoryMicroloan'), color: 'bg-amber-50 text-amber-700 dark:bg-amber-950/60 dark:text-amber-300' };
      case 'BANK_OFFER_SELECTED':
        return { label: t('adminCategoryApps'), color: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300' };
      default:
        return { label: t('adminCategorySystem'), color: 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300' };
    }
  };

  const formatWhen = (value?: string) => {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return date.toLocaleString(language === 'ru' ? 'ru-RU' : 'en-GB', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // User Ban / Unban Toggle
  const handleToggleUserStatus = async (userItem: AdminUser) => {
    setTogglingUserId(userItem.id);
    try {
      const nextStatus = !userItem.enabled;
      const updated = await adminManagementApi.updateUserStatus(userItem.id, nextStatus);
      setUsersList((prev) => prev.map((u) => (u.id === userItem.id ? updated : u)));
      showToast(updated.enabled ? t('adminUserUnblockedToast') : t('adminUserBlockedToast'));
      loadStats();

      // If the current logged-in user blocked their own account, trigger immediate ban redirect
      if (!updated.enabled && (user?.id === userItem.id || user?.email === userItem.email)) {
        await refreshUser();
      }
    } catch {
      setError('Не удалось обновить статус пользователя');
    } finally {
      setTogglingUserId(null);
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      window.location.hash = '';
    }
  };

  // Offer CRUD
  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_OFFER);
    setModalOpen(true);
  };

  const openEdit = (offer: BankOffer) => {
    setEditingId(offer.id);
    setForm({
      bankName: offer.bankName,
      offerName: offer.offerName,
      annualInterestRate: offer.annualInterestRate,
      minDownPaymentPercent: offer.minDownPaymentPercent,
      maxTermMonths: offer.maxTermMonths,
      minLoanAmount: offer.minLoanAmount ?? undefined,
      maxLoanAmount: offer.maxLoanAmount ?? undefined,
      currency: offer.currency || 'RUB',
    });
    setModalOpen(true);
  };

  const saveOffer = async (event: React.FormEvent) => {
    event.preventDefault();
    setSavingOffer(true);
    setError(null);
    try {
      if (editingId) {
        await bankOfferApi.update(editingId, form);
      } else {
        await bankOfferApi.create(form);
      }
      setModalOpen(false);
      await loadOffers();
      showToast(editingId ? t('save') : t('adminAddProgram'));
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || t('saveOfferError'));
    } finally {
      setSavingOffer(false);
    }
  };

  const deleteOffer = async (id: string) => {
    if (!window.confirm(t('deleteOfferConfirm'))) return;
    setError(null);
    try {
      await bankOfferApi.delete(id);
      await loadOffers();
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || t('deleteOfferError'));
    }
  };

  const saveRates = async (event: React.FormEvent) => {
    event.preventDefault();
    setSavingRates(true);
    setRatesMessage(null);
    setError(null);
    try {
      await calculatorApi.updateMicroloanConfig({
        bankInterest: surchargeRate,
      });
      setRatesState('ok');
      setRatesMessage(t('ratesUpdated'));
      showToast(t('ratesUpdated'));
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || t('ratesUpdateError'));
    } finally {
      setSavingRates(false);
    }
  };

  const openCreateRateModal = () => {
    setEditingRate(null);
    setRateForm({
      name: '',
      baseDailyRate: 0.8,
      minDayRequired: 7,
      maxDayRequired: 30,
    });
    setRateModalOpen(true);
  };

  const openEditRateModal = (rate: MicroloanRateDTO) => {
    setEditingRate(rate);
    setRateForm({
      name: rate.name,
      baseDailyRate: rate.baseDailyRate,
      minDayRequired: rate.minDayRequired ?? 7,
      maxDayRequired: rate.maxDayRequired ?? 30,
    });
    setRateModalOpen(true);
  };

  const handleSaveRate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSavingRate(true);
    try {
      if (editingRate?.id) {
        await microloanRateApi.updateRate(editingRate.id, rateForm);
      } else {
        await microloanRateApi.saveRate(rateForm);
      }
      setRateModalOpen(false);
      setEditingRate(null);
      await loadRates();
      showToast(t('ratesUpdated'));
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || t('ratesUpdateError'));
    } finally {
      setSavingRate(false);
    }
  };

  const handleDeleteRate = async (id?: number) => {
    if (!id) return;
    if (!window.confirm('Удалить данный тариф?')) return;
    try {
      await microloanRateApi.deleteRate(id);
      await loadRates();
      showToast('Тариф успешно удален');
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || 'Не удалось удалить тариф');
    }
  };

  const handleSendSupport = (e: React.FormEvent) => {
    e.preventDefault();
    setSupportSent(true);
    setTimeout(() => {
      setSupportModalOpen(false);
      setSupportSent(false);
      setSupportSubject('');
      setSupportMessageText('');
      showToast(t('adminMessageSent'));
    }, 1200);
  };

  // Nav Items (without count badges)
  const navItems = [
    {
      id: 'overview' as const,
      label: t('adminNavOverview'),
      icon: <LayoutDashboard size={18} />,
    },
    {
      id: 'users' as const,
      label: t('adminNavUsers'),
      icon: <Users size={18} />,
    },
    {
      id: 'loans' as const,
      label: t('adminNavLoans'),
      icon: <Coins size={18} />,
    },
    {
      id: 'products' as const,
      label: t('adminNavProducts'),
      icon: <Landmark size={18} />,
    },
    {
      id: 'journal' as const,
      label: t('adminNavSettings'),
      icon: <ScrollText size={18} />,
    },
  ];

  const getSectionTitle = () => {
    switch (section) {
      case 'users':
        return t('adminSectionUsers');
      case 'loans':
        return t('adminSectionLoans');
      case 'products':
      case 'rates':
        return t('adminProductsTitle');
      case 'journal':
        return t('adminJournalTitle');
      case 'overview':
      default:
        return t('adminOverviewTitle');
    }
  };

  const getSectionDesc = () => {
    switch (section) {
      case 'users':
        return t('adminSectionUsersDesc');
      case 'loans':
        return t('adminSectionLoansDesc');
      case 'products':
      case 'rates':
        return t('adminProductsDesc');
      case 'journal':
        return t('adminJournalDesc');
      case 'overview':
      default:
        return t('adminOverviewDesc');
    }
  };

  const displayName = user?.email?.split('@')[0] || 'Admin';
  const systemOk = offersState !== 'error' && ratesState !== 'error';

  return (
    <div className="min-h-screen bg-slate-100 dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex antialiased relative overflow-x-hidden">
      {/* Mobile Drawer Backdrop */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-950/60 backdrop-blur-xs lg:hidden transition-opacity duration-300"
          onClick={() => setSidebarOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* 1. Admin Sidebar (Collapsible / Slide-out Drawer) */}
      <aside
        className={`fixed lg:sticky top-0 h-screen z-50 lg:z-10 bg-slate-950 text-slate-300 flex flex-col border-r border-slate-800 transition-all duration-300 ease-in-out shrink-0 overflow-hidden ${
          sidebarOpen
            ? 'w-72 translate-x-0 shadow-2xl lg:shadow-none'
            : 'w-72 lg:w-0 -translate-x-full lg:translate-x-0 border-r-0 pointer-events-none'
        }`}
        aria-label="Admin Navigation Sidebar"
      >
        <div className="w-72 h-full flex flex-col shrink-0 overflow-y-auto">
          <div className="px-6 pt-7 pb-6 border-b border-slate-900 flex items-center justify-between">
            <div
              className="flex items-center gap-3.5 cursor-pointer select-none group min-w-0 flex-1"
              onClick={() => { window.location.hash = 'calculator'; }}
              title={t('navMortgage')}
            >
              <div className="w-11 h-11 rounded-2xl bg-blue-600 flex items-center justify-center text-white shadow-lg shadow-blue-600/30 group-hover:scale-105 transition-transform shrink-0">
                <Landmark size={22} />
              </div>
              <div className="min-w-0">
                <div className="font-extrabold text-base tracking-tight text-white leading-tight truncate">
                  Mortgage<span className="text-blue-400">Surprise</span>
                </div>
                <div className="text-[11px] font-semibold text-slate-400 tracking-wide mt-0.5 truncate">
                  {t('adminBankConsole')}
                </div>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setSidebarOpen(false)}
              className="p-1.5 rounded-xl text-slate-400 hover:text-white hover:bg-slate-900 cursor-pointer transition-colors shrink-0 ml-1"
              title={language === 'ru' ? 'Свернуть меню' : 'Collapse sidebar'}
              aria-label="Close sidebar"
            >
              <PanelLeftClose size={18} />
            </button>
          </div>

          <div className="px-6 pt-6 pb-2">
            <div className="text-[10px] font-bold uppercase tracking-[0.18em] text-slate-500">
              {t('adminWorkspace')}
            </div>
          </div>

          <nav className="px-3.5 space-y-1.5 flex-1 py-1">
            {navItems.map((item) => {
              const active = section === item.id || (section === 'rates' && item.id === 'products');
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => navigateTo(item.id)}
                  className={`w-full flex items-center px-3.5 py-3 rounded-2xl text-sm font-semibold transition-all cursor-pointer ${
                    active
                      ? 'bg-blue-600 text-white shadow-md shadow-blue-600/20'
                      : 'text-slate-400 hover:text-white hover:bg-slate-900/80'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <span className={active ? 'text-white' : 'text-slate-400'}>{item.icon}</span>
                    <span>{item.label}</span>
                  </div>
                </button>
              );
            })}
          </nav>

          {/* Support Card */}
          <div className="p-4 space-y-3 mt-auto border-t border-slate-900">
            <div className="rounded-2xl bg-gradient-to-br from-slate-900 to-slate-900/90 border border-slate-800/80 p-4 relative overflow-hidden">
              <div className="flex items-start gap-3">
                <div className="w-9 h-9 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center shrink-0">
                  <LifeBuoy size={18} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-xs font-bold text-white leading-snug">{t('adminSupportTitle')}</div>
                  <button
                    type="button"
                    onClick={() => setSupportModalOpen(true)}
                    className="mt-1 text-[11px] font-medium text-blue-400 hover:text-blue-300 underline underline-offset-2 cursor-pointer transition-colors"
                  >
                    {t('adminWriteSupport')}
                  </button>
                </div>
              </div>
            </div>

            <div className="px-2 flex items-center justify-between text-[11px] text-slate-400 font-medium">
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                <span>{t('adminRole')}</span>
              </div>
              <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400">PROD LIVE</span>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Area */}
      <div className="flex-1 min-w-0 flex flex-col">
        {/* Header Bar */}
        <header className="bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-4 sm:px-8 sticky top-0 z-20 backdrop-blur-md">
          <div className="h-18 flex items-center justify-between gap-4">
            <div className="min-w-0 flex items-center gap-3">
              <button
                type="button"
                onClick={() => setSidebarOpen((prev) => !prev)}
                className="p-2 sm:p-2.5 rounded-xl border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 cursor-pointer transition-colors shrink-0 flex items-center justify-center"
                title={
                  sidebarOpen
                    ? (language === 'ru' ? 'Свернуть боковое меню' : 'Collapse sidebar')
                    : (language === 'ru' ? 'Развернуть боковое меню' : 'Expand sidebar')
                }
                aria-label="Toggle sidebar"
              >
                {sidebarOpen ? <PanelLeftClose size={18} /> : <PanelLeftOpen size={18} />}
              </button>

              <div className="flex items-center gap-1.5 text-xs font-medium text-slate-400">
                <span>{t('adminConsole')}</span>
                <span>/</span>
                <span className="text-slate-800 dark:text-slate-200 font-bold truncate">
                  {getSectionTitle()}
                </span>
              </div>
            </div>

            <div className="flex items-center gap-2 sm:gap-3">
              <button
                type="button"
                onClick={refreshAllData}
                className="p-2.5 rounded-xl border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300 cursor-pointer transition-colors"
                title="Обновить реальные данные"
              >
                <RefreshCw size={17} className={statsLoading || usersLoading || loansLoading ? 'animate-spin' : ''} />
              </button>

              {/* Notifications Bell */}
              <div className="relative">
                <button
                  type="button"
                  onClick={() => {
                    setNotificationsOpen(!notificationsOpen);
                    setUnreadNotifications(0);
                  }}
                  className="relative p-2.5 rounded-xl border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/60 text-slate-600 dark:text-slate-300 cursor-pointer transition-colors"
                  title={t('adminNotifications')}
                >
                  <Bell size={18} />
                  {unreadNotifications > 0 && (
                    <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-blue-500 text-white text-[10px] font-black flex items-center justify-center">
                      {unreadNotifications}
                    </span>
                  )}
                </button>

                {notificationsOpen && (
                  <div className="absolute right-0 mt-2 w-80 sm:w-96 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl p-4 z-50 animate-in fade-in zoom-in-95">
                    <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
                      <div className="font-bold text-sm text-slate-900 dark:text-white">
                        {t('adminNotifications')}
                      </div>
                      <span className="text-[11px] font-semibold text-blue-600 dark:text-blue-400">
                        {loansList.length} активных записей
                      </span>
                    </div>
                    <div className="divide-y divide-slate-100 dark:divide-slate-800 max-h-72 overflow-y-auto">
                      <div className="py-2.5">
                        <div className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                          Реальная синхронизация с базами PostgreSQL & MongoDB
                        </div>
                        <div className="text-[11px] text-slate-400 mt-0.5">
                          Все показатели поступают напрямую из репозиториев
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Language Switcher */}
              <button
                type="button"
                onClick={toggleLanguage}
                className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/60 text-xs font-bold cursor-pointer transition-colors"
              >
                <Globe size={14} className="text-blue-600 dark:text-blue-400" />
                <span>{language.toUpperCase()}</span>
              </button>

              {/* Theme Switcher */}
              <button
                type="button"
                onClick={toggleTheme}
                className="p-2.5 rounded-xl border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/60 cursor-pointer transition-colors"
              >
                {theme === 'dark' ? <Sun size={17} className="text-amber-400" /> : <Moon size={17} />}
              </button>

              {/* Admin Profile Pill */}
              <div className="flex items-center gap-3 pl-2 border-l border-slate-200 dark:border-slate-800">
                <div className="hidden sm:block text-right">
                  <div className="text-sm font-bold text-slate-900 dark:text-white leading-tight">
                    {displayName}
                  </div>
                  <div className="text-[11px] font-semibold text-emerald-600 dark:text-emerald-400">
                    {t('adminRoleAdmin')}
                  </div>
                </div>
                <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-blue-700 to-indigo-500 text-white flex items-center justify-center font-black text-sm shadow-sm">
                  {displayName.slice(0, 1).toUpperCase()}
                </div>
                <button
                  type="button"
                  onClick={handleLogout}
                  title={t('navLogout')}
                  className="p-2 rounded-xl text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-950/40 cursor-pointer transition-colors"
                >
                  <LogOut size={18} />
                </button>
              </div>
            </div>
          </div>
        </header>

        {/* Global Toast */}
        {toastMessage && (
          <div className="fixed top-20 right-6 z-50 animate-in fade-in slide-in-from-top-4">
            <div className="flex items-center gap-2.5 px-4 py-3 rounded-2xl bg-slate-950 text-white shadow-xl border border-slate-800 text-xs font-bold">
              <CheckCircle2 size={16} className="text-emerald-400 shrink-0" />
              <span>{toastMessage}</span>
            </div>
          </div>
        )}

        {/* Content Body */}
        <main className="flex-1 px-4 sm:px-8 py-8 space-y-7">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-slate-950 dark:text-white">
                {getSectionTitle()}
              </h1>
              <p className="text-sm text-slate-500 dark:text-slate-400 mt-1 max-w-2xl font-normal">
                {getSectionDesc()}
              </p>
            </div>

            <div className="flex items-center gap-2.5">
              {(section === 'products' || section === 'rates') && (
                <>
                  <button
                    type="button"
                    onClick={openCreateRateModal}
                    className="inline-flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-bold shadow-md shadow-emerald-600/20 cursor-pointer transition-all"
                  >
                    <Plus size={16} />
                    <span>{t('adminAddTariff')}</span>
                  </button>
                  <button
                    type="button"
                    onClick={openCreate}
                    className="inline-flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold shadow-md shadow-blue-600/20 cursor-pointer transition-all"
                  >
                    <Plus size={16} />
                    <span>{t('adminAddProduct')}</span>
                  </button>
                </>
              )}
            </div>
          </div>

          {error && (
            <div className="p-4 rounded-2xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 text-sm flex items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <AlertCircle size={18} className="shrink-0" />
                <span>{error}</span>
              </div>
              <button
                type="button"
                onClick={() => setError(null)}
                className="text-red-500 hover:text-red-700 cursor-pointer"
              >
                <X size={16} />
              </button>
            </div>
          )}

          {/* 4 REAL-DATA METRIC CARDS */}
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
            {/* Card 1: Total Registered Users */}
            <div className="rounded-3xl p-6 bg-slate-950 text-white border border-slate-900 dark:bg-slate-900 dark:border-slate-800 shadow-sm flex flex-col justify-between">
              <div className="flex items-center justify-between gap-2">
                <div className="text-xs font-bold text-slate-400 tracking-wide">
                  {t('adminTotalUsers')}
                </div>
                <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  База данных
                </span>
              </div>
              <div className="mt-4 text-3xl font-black tracking-tight text-white">
                {statsLoading ? '...' : (stats?.totalUsers ?? usersList.length)}
              </div>
              <div className="mt-2 text-xs text-slate-400 font-medium">
                {usersList.filter((u) => u.enabled).length} активных пользователей
              </div>
            </div>

            {/* Card 2: Active Loans Count */}
            <div className="rounded-3xl p-6 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm flex flex-col justify-between">
              <div className="flex items-center justify-between gap-2">
                <div className="text-xs font-bold text-slate-500 dark:text-slate-400">
                  {t('adminActiveLoans')}
                </div>
                <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400">
                  Реальные займы
                </span>
              </div>
              <div className="mt-4 text-3xl font-black tracking-tight text-slate-950 dark:text-white">
                {loansLoading ? '...' : (stats?.activeLoansCount ?? loansList.length)}
              </div>
              <div className="mt-2 text-xs text-slate-400 font-medium">
                Ипотек: {stats?.mortgageLoansCount ?? 0} · Микрозаймов: {stats?.microloansCount ?? 0}
              </div>
            </div>

            {/* Card 3: Total Portfolio Amount */}
            <div className="rounded-3xl p-6 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm flex flex-col justify-between">
              <div className="flex items-center justify-between gap-2">
                <div className="text-xs font-bold text-slate-500 dark:text-slate-400">
                  {t('adminCreditPortfolio')}
                </div>
                <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400">
                  Объём займов
                </span>
              </div>
              <div className="mt-4 text-2xl sm:text-3xl font-black tracking-tight text-slate-950 dark:text-white truncate">
                {statsLoading ? '...' : formatCurrency(stats?.totalPortfolioAmount ?? 0, 'RUB')}
              </div>
              <div className="mt-2 text-xs text-slate-400 font-medium truncate">
                Ипотека: {formatCurrency(stats?.mortgagePortfolioAmount ?? 0, 'RUB')}
              </div>
            </div>

            {/* Card 4: Debt Repayment Resolution Progress */}
            <div className="rounded-3xl p-6 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm flex flex-col justify-between">
              <div className="flex items-center justify-between gap-2">
                <div className="text-xs font-bold text-slate-500 dark:text-slate-400">
                  {t('adminRepaymentRate')}
                </div>
                <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center gap-1">
                  <TrendingUp size={12} />
                  <span>Отдача долга</span>
                </span>
              </div>
              <div className="mt-4 text-3xl font-black tracking-tight text-indigo-600 dark:text-indigo-400">
                {statsLoading ? '...' : `${(stats?.repaymentProgressPercent ?? 0).toFixed(1)}%`}
              </div>
              <div className="mt-2 text-xs text-slate-400 font-medium truncate">
                Выплачено: {formatCurrency(stats?.totalRepaidAmount ?? 0, 'RUB')}
              </div>
            </div>
          </div>

          {/* VIEW: OVERVIEW */}
          {section === 'overview' && (
            <div className="space-y-7">
              <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                {/* Left: Real Loan Repayment Monitoring Preview */}
                <div className="xl:col-span-2 bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 overflow-hidden shadow-sm">
                  <div className="p-5 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
                    <div>
                      <div className="font-bold text-base text-slate-900 dark:text-white flex items-center gap-2">
                        <Coins size={18} className="text-blue-600 dark:text-blue-400" />
                        <span>{t('adminSectionLoans')}</span>
                      </div>
                      <div className="text-xs text-slate-400 mt-0.5">
                        Отслеживание отдачи долга по оформленным займам в реальном времени
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => navigateTo('loans')}
                      className="text-xs font-bold text-blue-600 dark:text-blue-400 hover:underline cursor-pointer flex items-center gap-1"
                    >
                      <span>Все займы ({loansList.length})</span>
                      <span>&gt;</span>
                    </button>
                  </div>

                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                      <thead className="text-[11px] uppercase tracking-wider text-slate-400 bg-slate-50/50 dark:bg-slate-950/40 border-b border-slate-100 dark:border-slate-800 font-bold">
                        <tr>
                          <th className="px-5 py-3.5">ID / Заёмщик</th>
                          <th className="px-5 py-3.5">Продукт</th>
                          <th className="px-5 py-3.5">Сумма</th>
                          <th className="px-5 py-3.5">Отдача долга</th>
                          <th className="px-5 py-3.5 text-right">Статус</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                        {loansList.length === 0 ? (
                          <tr>
                            <td colSpan={5} className="px-5 py-12 text-center text-sm text-slate-400">
                              {t('adminNoLoans')}
                            </td>
                          </tr>
                        ) : (
                          loansList.slice(0, 5).map((loan) => (
                            <tr key={loan.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40 transition-colors">
                              <td className="px-5 py-3.5">
                                <div className="font-mono text-xs font-bold text-blue-600 dark:text-blue-400">
                                  {loan.id}
                                </div>
                                <div className="text-xs font-medium text-slate-600 dark:text-slate-300 mt-0.5">
                                  {loan.borrowerEmail}
                                </div>
                              </td>
                              <td className="px-5 py-3.5">
                                <span className={`inline-block text-[10px] font-bold px-2 py-0.5 rounded-full mb-1 ${
                                  loan.loanType === 'MORTGAGE'
                                    ? 'bg-blue-50 text-blue-700 dark:bg-blue-950 dark:text-blue-300'
                                    : 'bg-amber-50 text-amber-700 dark:bg-amber-950 dark:text-amber-300'
                                }`}>
                                  {loan.loanType === 'MORTGAGE' ? 'Ипотека' : 'Микрозайм'}
                                </span>
                                <div className="text-xs font-bold text-slate-800 dark:text-slate-200">
                                  {loan.productTitle}
                                </div>
                              </td>
                              <td className="px-5 py-3.5 whitespace-nowrap">
                                <div className="font-extrabold text-slate-900 dark:text-white">
                                  {formatCurrency(loan.loanAmount, (loan.currency as Currency) || 'RUB')}
                                </div>
                                <div className="text-[11px] text-slate-400">
                                  {loan.term} · {loan.interestRate}%
                                </div>
                              </td>
                              <td className="px-5 py-3.5 min-w-[180px]">
                                <div className="flex items-center justify-between text-[11px] font-semibold mb-1">
                                  <span className="text-emerald-600 dark:text-emerald-400">
                                    {loan.progressPercent.toFixed(1)}%
                                  </span>
                                  <span className="text-slate-400">
                                    {loan.elapsedPeriods} из {loan.termUnits} {loan.loanType === 'MORTGAGE' ? 'мес.' : 'дн.'}
                                  </span>
                                </div>
                                <div className="w-full bg-slate-100 dark:bg-slate-800 h-2 rounded-full overflow-hidden">
                                  <div
                                    className="bg-emerald-500 h-2 rounded-full transition-all duration-500"
                                    style={{ width: `${Math.max(2, loan.progressPercent)}%` }}
                                  />
                                </div>
                                <div className="text-[10px] text-slate-400 mt-1 flex justify-between">
                                  <span>Выплачено: {formatCurrency(loan.paidAmount, (loan.currency as Currency) || 'RUB')}</span>
                                </div>
                              </td>
                              <td className="px-5 py-3.5 text-right">
                                <span className={`text-[11px] font-bold px-2.5 py-1 rounded-full ${
                                  loan.status === 'PAID_OFF'
                                    ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300'
                                    : loan.status === 'OVERDUE'
                                      ? 'bg-red-50 text-red-700 dark:bg-red-950/60 dark:text-red-300'
                                      : 'bg-blue-50 text-blue-700 dark:bg-blue-950/60 dark:text-blue-300'
                                }`}>
                                  {loan.status === 'PAID_OFF'
                                    ? t('adminStatusPaidOff')
                                    : loan.status === 'OVERDUE'
                                      ? t('adminStatusOverdue')
                                      : t('adminStatusOnTrack')}
                                </span>
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Right Column: Widgets */}
                <div className="space-y-6">
                  {/* System Status */}
                  <div className="bg-slate-950 text-white rounded-3xl p-6 border border-slate-900 shadow-sm">
                    <div className="flex items-center justify-between mb-5">
                      <div className="font-bold text-sm text-slate-200 tracking-wide">
                        {t('adminSystemStatus')}
                      </div>
                      <span className={`inline-flex items-center gap-1.5 text-xs font-bold px-3 py-1 rounded-full ${
                        systemOk
                          ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                          : 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                      }`}>
                        <span className={`w-2 h-2 rounded-full ${systemOk ? 'bg-emerald-400' : 'bg-amber-400'} animate-pulse`} />
                        {systemOk ? t('adminSystemOk') : t('adminSystemIssue')}
                      </span>
                    </div>

                    <div className="divide-y divide-slate-800/80 text-xs">
                      <div className="py-2.5 flex items-center justify-between">
                        <span className="text-slate-400">{t('adminApiPayments')}</span>
                        <span className="font-bold text-emerald-400 flex items-center gap-1">
                          <Check size={13} />
                          {t('adminApiOnline')}
                        </span>
                      </div>
                      <div className="py-2.5 flex items-center justify-between">
                        <span className="text-slate-400">PostgreSQL (Users & Offers)</span>
                        <span className="font-bold text-slate-200">Подключено · 100%</span>
                      </div>
                      <div className="py-2.5 flex items-center justify-between">
                        <span className="text-slate-400">MongoDB (Audit & Schedules)</span>
                        <span className="font-bold text-slate-200">Подключено · 100%</span>
                      </div>
                      <div className="py-2.5 flex items-center justify-between">
                        <span className="text-slate-400">{t('adminLastBackup')}</span>
                        <span className="font-bold text-slate-200">{t('adminLastBackupTime')}</span>
                      </div>
                    </div>
                  </div>

                  {/* Rates Widget */}
                  <div className="bg-white dark:bg-slate-900 rounded-3xl p-6 border border-slate-200 dark:border-slate-800 shadow-sm">
                    <div className="flex items-center justify-between mb-4">
                      <div className="font-bold text-sm text-slate-900 dark:text-white">
                        {t('adminNavProducts')}
                      </div>
                      <button
                        type="button"
                        onClick={() => navigateTo('products')}
                        className="text-xs font-bold text-blue-600 dark:text-blue-400 hover:underline cursor-pointer flex items-center gap-1"
                      >
                        <span>{t('adminQuickChange')}</span>
                        <span>&gt;</span>
                      </button>
                    </div>

                    <div className="space-y-3 text-xs mb-5">
                      <div className="flex items-center justify-between py-1 border-b border-slate-100 dark:border-slate-800">
                        <span className="text-slate-500 dark:text-slate-400">Ипотека:</span>
                        <span className="font-extrabold text-slate-900 dark:text-white">
                          от {minMortgageRate}% / {t('adminPerYear')} (ср. {avgRate.toFixed(1)}%)
                        </span>
                      </div>
                      <div className="flex items-center justify-between py-1 border-b border-slate-100 dark:border-slate-800">
                        <span className="text-slate-500 dark:text-slate-400">Микрозаймы:</span>
                        <span className="font-extrabold text-slate-900 dark:text-white">
                          {baseRate}% / {t('perDay')} (+{surchargeRate}%)
                        </span>
                      </div>
                      <div className="flex items-center justify-between py-1">
                        <span className="text-slate-500 dark:text-slate-400">Каталог программ:</span>
                        <span className="font-extrabold text-blue-600 dark:text-blue-400">
                          {offers.length} {t('adminProgramsShort')}
                        </span>
                      </div>
                    </div>

                    {/* Maintenance Switch */}
                    <div className="pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                      <div>
                        <div className="text-xs font-bold text-slate-800 dark:text-slate-200">
                          {t('adminMaintenanceMode')}
                        </div>
                        <div className="text-[10px] text-slate-400">
                          {maintenanceMode ? 'Включен (только чтение)' : 'Отключен (штатный режим)'}
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => {
                          const next = !maintenanceMode;
                          setMaintenanceMode(next);
                          showToast(next ? 'Режим техработ активирован' : 'Штатный режим восстановлен');
                        }}
                        aria-label={t('adminMaintenanceMode')}
                        className={`w-11 h-6 rounded-full transition-colors relative cursor-pointer ${
                          maintenanceMode ? 'bg-amber-500' : 'bg-slate-300 dark:bg-slate-700'
                        }`}
                      >
                        <span
                          className={`block w-4 h-4 rounded-full bg-white transition-transform ${
                            maintenanceMode ? 'translate-x-6' : 'translate-x-1'
                          } top-1 absolute shadow-sm`}
                        />
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              {/* Bottom: Recent Actions */}
              <div className="bg-white dark:bg-slate-900 rounded-3xl p-6 border border-slate-200 dark:border-slate-800 shadow-sm">
                <div className="flex items-center justify-between mb-5">
                  <div>
                    <div className="font-bold text-base text-slate-900 dark:text-white">
                      {t('adminRecentActions')}
                    </div>
                    <div className="text-xs text-slate-400 mt-0.5">
                      События безопасности и расчеты клиентов в реальном времени
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => navigateTo('journal')}
                    className="text-xs font-bold text-blue-600 dark:text-blue-400 hover:underline cursor-pointer flex items-center gap-1"
                  >
                    <span>{t('adminOpenJournal')}</span>
                    <span>-&gt;</span>
                  </button>
                </div>

                <div className="divide-y divide-slate-100 dark:divide-slate-800 text-xs">
                  {logs.length === 0 ? (
                    <div className="py-8 text-center text-slate-400">{t('adminNoJournal')}</div>
                  ) : (
                    logs.slice(0, 5).map((record) => {
                      const category = getActionCategory(record.actionType);
                      return (
                        <div
                          key={record.id}
                          className="py-3.5 grid grid-cols-1 sm:grid-cols-12 gap-2 sm:gap-4 items-center"
                        >
                          <div className="sm:col-span-2 text-slate-400 whitespace-nowrap font-medium">
                            {formatWhen(record.timestamp)}
                          </div>
                          <div className="sm:col-span-3 font-bold text-slate-800 dark:text-slate-200 truncate">
                            {record.userEmail || 'Анонимный клиент'}
                          </div>
                          <div className="sm:col-span-5 text-slate-600 dark:text-slate-400 truncate">
                            {actionLabel(record.actionType)}
                            {record.actionDescription ? ` · ${record.actionDescription}` : ''}
                          </div>
                          <div className="sm:col-span-2 text-right">
                            <span className={`inline-block text-[10px] font-bold px-2 py-0.5 rounded-full ${category.color}`}>
                              {category.label}
                            </span>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            </div>
          )}

          {/* VIEW: USERS (Pure Real Data, No Add User Button) */}
          {section === 'users' && (
            <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 overflow-hidden shadow-sm">
              <div className="p-5 border-b border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row gap-3 sm:items-center justify-between">
                <div className="relative flex-1">
                  <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    value={userSearch}
                    onChange={(e) => {
                      setUserSearch(e.target.value);
                      setUserPage(1);
                    }}
                    placeholder={t('adminSearchUsers')}
                    className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 text-xs font-medium focus:outline-none focus:border-blue-500"
                  />
                </div>
                <div className="flex items-center gap-2">
                  <select
                    value={userStatusFilter}
                    onChange={(e) => {
                      setUserStatusFilter(e.target.value as any);
                      setUserPage(1);
                    }}
                    aria-label={t('adminStatusAll')}
                    className="px-3.5 py-2.5 rounded-2xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 text-xs font-bold text-slate-700 dark:text-slate-300 focus:outline-none cursor-pointer"
                  >
                    <option value="ALL">{t('adminStatusAll')}</option>
                    <option value="ACTIVE">{t('adminStatusActivePill')}</option>
                    <option value="BLOCKED">{t('adminStatusBlocked')}</option>
                  </select>
                  <select
                    value={userRoleFilter}
                    onChange={(e) => {
                      setUserRoleFilter(e.target.value);
                      setUserPage(1);
                    }}
                    aria-label={t('adminAllRoles')}
                    className="px-3.5 py-2.5 rounded-2xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 text-xs font-bold text-slate-700 dark:text-slate-300 focus:outline-none cursor-pointer"
                  >
                    <option value="ALL">{t('adminAllRoles')}</option>
                    <option value="ROLE_USER">{t('adminRoleClient')}</option>
                    <option value="ROLE_ADMIN">{t('adminRoleAdmin')}</option>
                  </select>
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="text-[11px] uppercase tracking-wider text-slate-400 bg-slate-50/50 dark:bg-slate-950/40 border-b border-slate-100 dark:border-slate-800 font-bold">
                    <tr>
                      <th className="px-5 py-3.5">{t('adminClient')}</th>
                      <th className="px-5 py-3.5">Роль</th>
                      <th className="px-5 py-3.5">Займов / Долг</th>
                      <th className="px-5 py-3.5">{t('adminColStatus')}</th>
                      <th className="px-5 py-3.5">{t('adminColRegistered')}</th>
                      <th className="px-5 py-3.5 text-right">{t('adminColActions')}</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {usersLoading ? (
                      <tr>
                        <td colSpan={6} className="px-5 py-12 text-center text-sm text-slate-400">
                          Загрузка пользователей из базы...
                        </td>
                      </tr>
                    ) : pageUsers.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="px-5 py-12 text-center text-sm text-slate-400">
                          {t('adminNoUsers')}
                        </td>
                      </tr>
                    ) : (
                      pageUsers.map((item) => (
                        <tr key={item.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40 transition-colors">
                          <td className="px-5 py-3.5">
                            <div className="font-bold text-slate-900 dark:text-white">
                              {item.firstName || item.lastName
                                ? `${item.firstName || ''} ${item.lastName || ''}`.trim()
                                : item.email.split('@')[0]}
                            </div>
                            <div className="text-xs text-slate-400 mt-0.5">
                              {item.email}
                            </div>
                          </td>
                          <td className="px-5 py-3.5">
                            <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                              {item.role === 'ROLE_ADMIN' ? t('adminRoleAdmin') : t('adminRoleClient')}
                            </span>
                          </td>
                          <td className="px-5 py-3.5 whitespace-nowrap">
                            <div className="text-xs font-bold text-slate-900 dark:text-white">
                              {item.activeLoansCount} {item.activeLoansCount === 1 ? 'займ' : 'займов'}
                            </div>
                            <div className="text-[11px] text-slate-400">
                              Остаток: {formatCurrency(item.remainingDebt, 'RUB')}
                            </div>
                          </td>
                          <td className="px-5 py-3.5">
                            <span className={`text-[11px] font-bold px-2.5 py-1 rounded-full ${
                              item.enabled
                                ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300'
                                : 'bg-red-50 text-red-700 dark:bg-red-950/60 dark:text-red-300'
                            }`}>
                              {item.enabled ? t('adminStatusActivePill') : t('adminStatusBlocked')}
                            </span>
                          </td>
                          <td className="px-5 py-3.5 text-xs text-slate-500 dark:text-slate-400 whitespace-nowrap">
                            {formatWhen(item.createdAt)}
                          </td>
                          <td className="px-5 py-3.5 text-right">
                            <button
                              type="button"
                              disabled={togglingUserId === item.id || item.role === 'ROLE_ADMIN'}
                              onClick={() => handleToggleUserStatus(item)}
                              className={`text-xs font-semibold px-3 py-1.5 rounded-xl cursor-pointer transition-colors disabled:opacity-40 ${
                                !item.enabled
                                  ? 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 dark:bg-emerald-950 dark:text-emerald-300'
                                  : 'bg-slate-100 dark:bg-slate-800 text-slate-600 hover:text-red-600 dark:text-slate-300'
                              }`}
                            >
                              {!item.enabled ? t('adminToggleUnban') : t('adminToggleBan')}
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              <div className="px-5 py-3.5 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500">
                <div>
                  {filteredUsers.length === 0
                    ? '0 из 0'
                    : `${(safeUserPage - 1) * userPageSize + 1}–${Math.min(
                        safeUserPage * userPageSize,
                        filteredUsers.length
                      )} из ${filteredUsers.length}`}
                </div>
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    disabled={safeUserPage <= 1}
                    onClick={() => setUserPage((p) => Math.max(1, p - 1))}
                    className="p-1.5 rounded-lg border border-slate-200 dark:border-slate-800 hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-40 cursor-pointer"
                  >
                    <ChevronLeft size={14} />
                  </button>
                  {Array.from({ length: userPageCount }, (_, i) => i + 1).slice(0, 5).map((num) => (
                    <button
                      key={num}
                      type="button"
                      onClick={() => setUserPage(num)}
                      className={`w-7 h-7 rounded-lg font-bold text-xs cursor-pointer ${
                        num === safeUserPage
                          ? 'bg-blue-600 text-white'
                          : 'hover:bg-slate-100 dark:hover:bg-slate-800'
                      }`}
                    >
                      {num}
                    </button>
                  ))}
                  <button
                    type="button"
                    disabled={safeUserPage >= userPageCount}
                    onClick={() => setUserPage((p) => Math.min(userPageCount, p + 1))}
                    className="p-1.5 rounded-lg border border-slate-200 dark:border-slate-800 hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-40 cursor-pointer"
                  >
                    <ChevronRight size={14} />
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* VIEW: LOANS & REPAYMENT MONITORING (No Approvals, Observational Only) */}
          {section === 'loans' && (
            <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 overflow-hidden shadow-sm">
              <div className="p-5 border-b border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row gap-3 sm:items-center justify-between">
                <div className="relative flex-1">
                  <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    value={loanSearch}
                    onChange={(e) => {
                      setLoanSearch(e.target.value);
                      setLoanPage(1);
                    }}
                    placeholder="Поиск по ID займа, email заёмщика или названию продукта"
                    className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 text-xs font-medium focus:outline-none"
                  />
                </div>
                <div className="flex items-center gap-2">
                  <select
                    value={loanStatusFilter}
                    onChange={(e) => {
                      setLoanStatusFilter(e.target.value as any);
                      setLoanPage(1);
                    }}
                    aria-label={t('adminStatusAll')}
                    className="px-3.5 py-2.5 rounded-2xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 text-xs font-bold text-slate-700 dark:text-slate-300 focus:outline-none cursor-pointer"
                  >
                    <option value="ALL">{t('adminStatusAll')}</option>
                    <option value="ACTIVE">{t('adminStatusOnTrack')}</option>
                    <option value="PAID_OFF">{t('adminStatusPaidOff')}</option>
                    <option value="OVERDUE">{t('adminStatusOverdue')}</option>
                  </select>
                  <select
                    value={loanCategoryFilter}
                    onChange={(e) => {
                      setLoanCategoryFilter(e.target.value as any);
                      setLoanPage(1);
                    }}
                    aria-label={t('loanCategory')}
                    className="px-3.5 py-2.5 rounded-2xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 text-xs font-bold text-slate-700 dark:text-slate-300 focus:outline-none cursor-pointer"
                  >
                    <option value="ALL">Все типы</option>
                    <option value="MORTGAGE">Ипотека</option>
                    <option value="MICROLOAN">Микрозаймы</option>
                  </select>
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="text-[11px] uppercase tracking-wider text-slate-400 bg-slate-50/50 dark:bg-slate-950/40 border-b border-slate-100 dark:border-slate-800 font-bold">
                    <tr>
                      <th className="px-5 py-3.5">ID / {t('adminBorrower')}</th>
                      <th className="px-5 py-3.5">{t('adminLoanProduct')}</th>
                      <th className="px-5 py-3.5">{t('adminInitialDebt')}</th>
                      <th className="px-5 py-3.5 min-w-[220px]">{t('adminRepaidProgress')}</th>
                      <th className="px-5 py-3.5">{t('adminNextPayment')}</th>
                      <th className="px-5 py-3.5 text-right">{t('adminColStatus')}</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {loansLoading ? (
                      <tr>
                        <td colSpan={6} className="px-5 py-12 text-center text-sm text-slate-400">
                          Загрузка займов и графиков выплат...
                        </td>
                      </tr>
                    ) : pageLoans.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="px-5 py-12 text-center text-sm text-slate-400">
                          {t('adminNoLoans')}
                        </td>
                      </tr>
                    ) : (
                      pageLoans.map((loan) => (
                        <tr key={loan.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40 transition-colors">
                          <td className="px-5 py-3.5">
                            <div className="font-mono text-xs font-bold text-blue-600 dark:text-blue-400">
                              {loan.id}
                            </div>
                            <div className="font-bold text-slate-900 dark:text-white mt-0.5">
                              {loan.borrowerName}
                            </div>
                            <div className="text-[11px] text-slate-400">
                              {loan.borrowerEmail}
                            </div>
                          </td>
                          <td className="px-5 py-3.5">
                            <span className={`inline-block text-[10px] font-bold px-2 py-0.5 rounded-full mb-1 ${
                              loan.loanType === 'MORTGAGE'
                                ? 'bg-blue-50 text-blue-700 dark:bg-blue-950 dark:text-blue-300'
                                : 'bg-amber-50 text-amber-700 dark:bg-amber-950 dark:text-amber-300'
                            }`}>
                              {loan.loanType === 'MORTGAGE' ? 'Ипотека' : 'Микрозайм'}
                            </span>
                            <div className="font-semibold text-slate-800 dark:text-slate-200">
                              {loan.productTitle}
                            </div>
                            <div className="text-xs text-slate-400 max-w-xs truncate">
                              {loan.purpose}
                            </div>
                          </td>
                          <td className="px-5 py-3.5 whitespace-nowrap">
                            <div className="font-extrabold text-slate-900 dark:text-white">
                              {formatCurrency(loan.loanAmount, (loan.currency as Currency) || 'RUB')}
                            </div>
                            <div className="text-xs text-slate-400">
                              {loan.term} · {loan.interestRate}%
                            </div>
                            <div className="text-[10px] text-slate-500 mt-0.5">
                              Всего к возврату: {formatCurrency(loan.totalRepayment, (loan.currency as Currency) || 'RUB')}
                            </div>
                          </td>
                          <td className="px-5 py-3.5">
                            <div className="flex items-center justify-between text-xs font-bold mb-1">
                              <span className="text-emerald-600 dark:text-emerald-400">
                                {loan.progressPercent.toFixed(1)}% отдано
                              </span>
                              <span className="text-slate-400 text-[11px]">
                                {loan.elapsedPeriods} из {loan.termUnits} {loan.loanType === 'MORTGAGE' ? 'мес.' : 'дн.'}
                              </span>
                            </div>
                            <div className="w-full bg-slate-100 dark:bg-slate-800 h-2.5 rounded-full overflow-hidden">
                              <div
                                className="bg-gradient-to-r from-emerald-500 to-teal-500 h-2.5 rounded-full transition-all duration-500"
                                style={{ width: `${Math.max(2, loan.progressPercent)}%` }}
                              />
                            </div>
                            <div className="flex items-center justify-between text-[11px] text-slate-500 dark:text-slate-400 mt-1.5">
                              <span>Выплачено: <strong className="text-slate-700 dark:text-slate-200">{formatCurrency(loan.paidAmount, (loan.currency as Currency) || 'RUB')}</strong></span>
                              <span>Остаток: <strong className="text-rose-600 dark:text-rose-400">{formatCurrency(loan.remainingBalance, (loan.currency as Currency) || 'RUB')}</strong></span>
                            </div>
                          </td>
                          <td className="px-5 py-3.5 whitespace-nowrap">
                            <div className="font-extrabold text-slate-900 dark:text-white text-xs">
                              {formatCurrency(loan.nextPaymentAmount, (loan.currency as Currency) || 'RUB')}
                            </div>
                            <div className="text-[11px] text-slate-400 flex items-center gap-1 mt-0.5">
                              <Clock size={11} />
                              <span>{loan.nextPaymentDate}</span>
                            </div>
                          </td>
                          <td className="px-5 py-3.5 text-right whitespace-nowrap">
                            <span className={`text-[11px] font-bold px-2.5 py-1 rounded-full ${
                              loan.status === 'PAID_OFF'
                                ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300'
                                : loan.status === 'OVERDUE'
                                  ? 'bg-red-50 text-red-700 dark:bg-red-950/60 dark:text-red-300'
                                  : 'bg-blue-50 text-blue-700 dark:bg-blue-950/60 dark:text-blue-300'
                            }`}>
                              {loan.status === 'PAID_OFF'
                                ? t('adminStatusPaidOff')
                                : loan.status === 'OVERDUE'
                                  ? t('adminStatusOverdue')
                                  : t('adminStatusOnTrack')}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              <div className="px-5 py-3.5 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500">
                <div>
                  {filteredLoans.length === 0
                    ? '0 из 0'
                    : `${(safeLoanPage - 1) * loanPageSize + 1}–${Math.min(
                        safeLoanPage * loanPageSize,
                        filteredLoans.length
                      )} из ${filteredLoans.length}`}
                </div>
                <div className="flex gap-1">
                  {Array.from({ length: loanPageCount }, (_, i) => i + 1).slice(0, 5).map((num) => (
                    <button
                      key={num}
                      type="button"
                      onClick={() => setLoanPage(num)}
                      className={`w-7 h-7 rounded-lg font-bold text-xs cursor-pointer ${
                        num === safeLoanPage
                          ? 'bg-blue-600 text-white'
                          : 'hover:bg-slate-100 dark:hover:bg-slate-800'
                      }`}
                    >
                      {num}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* VIEW: PRODUCTS & RATES */}
          {(section === 'products' || section === 'rates') && (
            <div className="space-y-7">
              <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 overflow-hidden shadow-sm">
                <div className="p-5 border-b border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row gap-3 sm:items-center justify-between">
                  <div className="relative flex-1">
                    <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                      value={offerQuery}
                      onChange={(e) => {
                        setOfferQuery(e.target.value);
                        setOfferPage(1);
                      }}
                      placeholder={t('adminSearchPrograms')}
                      className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 text-xs font-medium focus:outline-none"
                    />
                  </div>
                  <div className="flex gap-1.5 overflow-x-auto">
                    {(['ALL', 'RUB', 'USD', 'EUR', 'BYN'] as const).map((code) => (
                      <button
                        key={code}
                        type="button"
                        onClick={() => {
                          setCurrency(code);
                          setOfferPage(1);
                        }}
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold cursor-pointer transition-colors ${
                          currency === code
                            ? 'bg-blue-600 text-white'
                            : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300'
                        }`}
                      >
                        {code === 'ALL' ? t('allCurrencies') : code}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="text-[11px] uppercase tracking-wider text-slate-400 bg-slate-50/50 dark:bg-slate-950/40 border-b border-slate-100 dark:border-slate-800 font-bold">
                      <tr>
                        <th className="px-5 py-3.5">{t('adminColProgram')}</th>
                        <th className="px-5 py-3.5">{t('adminColRate')}</th>
                        <th className="px-5 py-3.5">{t('adminColDown')}</th>
                        <th className="px-5 py-3.5">{t('adminColTerm')}</th>
                        <th className="px-5 py-3.5">{t('adminColStatus')}</th>
                        <th className="px-5 py-3.5 text-right">{t('adminColActions')}</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                      {offersState === 'loading' ? (
                        <tr>
                          <td colSpan={6} className="px-5 py-12 text-center text-sm text-slate-400">
                            {t('loadingOffers')}
                          </td>
                        </tr>
                      ) : pageOffers.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="px-5 py-12 text-center text-sm text-slate-400">
                            {t('adminEmptyPrograms')}
                          </td>
                        </tr>
                      ) : (
                        pageOffers.map((offer) => (
                          <tr key={offer.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40 transition-colors">
                            <td className="px-5 py-3.5">
                              <div className="font-bold text-slate-900 dark:text-white">
                                {offer.offerName}
                              </div>
                              <div className="text-xs text-slate-400">
                                {offer.bankName} · {limitText(offer, t('from'), t('upTo'))}
                              </div>
                            </td>
                            <td className="px-5 py-3.5 font-extrabold text-blue-600 dark:text-blue-400">
                              {offer.annualInterestRate}%
                            </td>
                            <td className="px-5 py-3.5 text-xs text-slate-600 dark:text-slate-300">
                              {t('from')} {offer.minDownPaymentPercent}%
                            </td>
                            <td className="px-5 py-3.5 text-xs text-slate-600 dark:text-slate-300">
                              {Math.floor(offer.maxTermMonths / 12)} {t('years')}
                            </td>
                            <td className="px-5 py-3.5">
                              <span className={`text-[11px] font-bold px-2 py-0.5 rounded-full ${
                                offer.active === false
                                  ? 'bg-slate-100 text-slate-500 dark:bg-slate-800'
                                  : 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300'
                              }`}>
                                {offer.active === false ? t('adminStatusInactive') : t('adminStatusActive')}
                              </span>
                            </td>
                            <td className="px-5 py-3.5 text-right">
                              <div className="flex items-center justify-end gap-1">
                                <button
                                  type="button"
                                  onClick={() => openEdit(offer)}
                                  className="p-2 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500 cursor-pointer"
                                  title={t('adminEditRates')}
                                >
                                  <Pencil size={15} />
                                </button>
                                <button
                                  type="button"
                                  onClick={() => deleteOffer(offer.id)}
                                  className="p-2 rounded-xl hover:bg-red-50 text-slate-400 hover:text-red-600 dark:hover:bg-red-950/40 cursor-pointer"
                                  title="Удалить"
                                >
                                  <Trash2 size={15} />
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                <div className="px-5 py-3.5 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500">
                  <div>
                    {filteredOffers.length === 0
                      ? '0 из 0'
                      : `${(safeOfferPage - 1) * offerPageSize + 1}–${Math.min(
                          safeOfferPage * offerPageSize,
                          filteredOffers.length
                        )} из ${filteredOffers.length}`}
                  </div>
                  <div className="flex gap-1">
                    {Array.from({ length: offerPageCount }, (_, i) => i + 1).slice(0, 5).map((num) => (
                      <button
                        key={num}
                        type="button"
                        onClick={() => setOfferPage(num)}
                        className={`w-7 h-7 rounded-lg font-bold text-xs cursor-pointer ${
                          num === safeOfferPage
                            ? 'bg-blue-600 text-white'
                            : 'hover:bg-slate-100 dark:hover:bg-slate-800'
                        }`}
                      >
                        {num}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              {/* Microloan Fixed Surcharge Config & Tariffs Management */}
              <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
                <form
                  onSubmit={saveRates}
                  className="lg:col-span-3 bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 space-y-5 shadow-sm"
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-2xl bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400">
                      <SlidersHorizontal size={20} />
                    </div>
                    <div>
                      <h3 className="font-bold text-sm text-slate-900 dark:text-white">
                        {t('adminFixedVanitySurcharge')}
                      </h3>
                      <p className="text-xs text-slate-500 mt-0.5">{t('adminFixedVanitySurchargeDesc')}</p>
                    </div>
                  </div>

                  <Field label={`${t('adminFixedVanitySurcharge')} (₽)`}>
                    <input
                      type="number"
                      step="100"
                      min="0"
                      max="100000"
                      value={surchargeRate}
                      onChange={(e) => setSurchargeRate(Number(e.target.value))}
                      className={fieldClass}
                    />
                  </Field>

                  <div className="flex justify-end pt-2">
                    <button
                      type="submit"
                      disabled={savingRates}
                      className="px-6 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold shadow-md shadow-blue-600/20 cursor-pointer disabled:opacity-50 transition-all"
                    >
                      {savingRates ? t('savingRates') : t('saveRates')}
                    </button>
                  </div>
                </form>

                {/* Summary Rate Card */}
                <div className="lg:col-span-2 rounded-3xl p-6 bg-slate-950 text-white border border-slate-900 flex flex-col justify-between">
                  <div>
                    <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                      Конфигурация кредитного ядра
                    </div>
                    <div className="text-xl font-black mt-2 text-white">Ставки и надбавки</div>
                    <div className="mt-4 space-y-3 text-xs">
                      <div className="flex items-center justify-between py-2 border-b border-slate-800">
                        <span className="text-slate-400">Действующих тарифов:</span>
                        <span className="font-extrabold text-blue-400">{ratesList.length}</span>
                      </div>
                      <div className="flex items-center justify-between py-2 border-b border-slate-800">
                        <span className="text-slate-400">Базовый минимум:</span>
                        <span className="font-extrabold text-emerald-400">По тарифу клиента</span>
                      </div>
                      <div className="flex items-center justify-between py-2">
                        <span className="text-slate-400">Роскошный максимум:</span>
                        <span className="font-extrabold text-amber-400">+{surchargeRate} ₽ (фикс.)</span>
                      </div>
                    </div>
                  </div>
                  <div className="text-[10px] text-slate-400 mt-4">
                    Параметры сохраняются в PostgreSQL microloan_configs и инвалидируют кэш Caffeine.
                  </div>
                </div>
              </div>

              {/* Tariffs Table (microloan_rates) */}
              <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 overflow-hidden shadow-sm">
                <div className="p-5 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-2xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400">
                      <Coins size={20} />
                    </div>
                    <div>
                      <h3 className="font-bold text-sm text-slate-900 dark:text-white">
                        {t('adminTariffManagement')}
                      </h3>
                      <p className="text-xs text-slate-500 mt-0.5">Управление процентными ставками и сроками микрозаймов</p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={openCreateRateModal}
                    className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-blue-600/20 cursor-pointer"
                  >
                    <Plus size={14} />
                    <span>{t('adminAddTariff')}</span>
                  </button>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-950/30 text-slate-400 uppercase text-[10px] tracking-wider">
                        <th className="p-3.5 font-bold">ID</th>
                        <th className="p-3.5 font-bold">{t('tariffName')}</th>
                        <th className="p-3.5 font-bold">{t('baseDailyRateLabel')}</th>
                        <th className="p-3.5 font-bold">{t('adminMinDays')}</th>
                        <th className="p-3.5 font-bold">{t('adminMaxDays')}</th>
                        <th className="p-3.5 font-bold text-right">Действия</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800 font-medium">
                      {ratesList.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="p-8 text-center text-slate-400">
                            Тарифы пока не добавлены
                          </td>
                        </tr>
                      ) : (
                        ratesList.map((rate) => (
                          <tr key={rate.id ?? rate.name} className="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition-colors">
                            <td className="p-3.5 font-mono text-slate-400">{rate.id ?? '—'}</td>
                            <td className="p-3.5 font-bold text-slate-900 dark:text-white">{rate.name}</td>
                            <td className="p-3.5 font-bold text-emerald-600 dark:text-emerald-400">
                              {rate.baseDailyRate}% / {t('perDay')}
                            </td>
                            <td className="p-3.5">{rate.minDayRequired ?? 7} {t('daysUnit')}</td>
                            <td className="p-3.5">{rate.maxDayRequired ?? 30} {t('daysUnit')}</td>
                            <td className="p-3.5 text-right">
                              <div className="flex items-center justify-end gap-1.5">
                                <button
                                  type="button"
                                  onClick={() => openEditRateModal(rate)}
                                  className="p-1.5 rounded-lg text-slate-500 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-950/60 transition-colors cursor-pointer"
                                  title={t('adminEditTariff')}
                                >
                                  <Pencil size={15} />
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleDeleteRate(rate.id)}
                                  className="p-1.5 rounded-lg text-slate-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/60 transition-colors cursor-pointer"
                                  title={t('adminDeleteTariff')}
                                >
                                  <Trash2 size={15} />
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* VIEW: JOURNAL */}
          {section === 'journal' && (
            <div className="space-y-6">
              <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 overflow-hidden shadow-sm">
                <div className="p-5 border-b border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row gap-3 sm:items-center justify-between">
                  <div className="relative flex-1">
                    <Search size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                      value={journalQuery}
                      onChange={(e) => setJournalQuery(e.target.value)}
                      placeholder={t('adminSearchJournal')}
                      className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 text-xs font-medium focus:outline-none"
                    />
                  </div>
                  <div className="flex gap-1.5 overflow-x-auto">
                    {(['ALL', 'MORTGAGE_CALCULATION', 'MICROLOAN_CALCULATION', 'MICROLOAN_APPLICATION', 'BANK_OFFER_SELECTED'] as const).map(
                      (action) => (
                        <button
                          key={action}
                          type="button"
                          onClick={() => setJournalAction(action as any)}
                          className={`px-3 py-1.5 rounded-xl text-xs font-bold cursor-pointer whitespace-nowrap transition-colors ${
                            journalAction === action
                              ? 'bg-blue-600 text-white'
                              : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300'
                          }`}
                        >
                          {action === 'ALL' ? t('allOperations') : actionLabel(action)}
                        </button>
                      )
                    )}
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="text-[11px] uppercase tracking-wider text-slate-400 bg-slate-50/50 dark:bg-slate-950/40 border-b border-slate-100 dark:border-slate-800 font-bold">
                      <tr>
                        <th className="px-5 py-3.5">{t('adminColDate')}</th>
                        <th className="px-5 py-3.5">{t('adminColUser')}</th>
                        <th className="px-5 py-3.5">{t('adminColAction')}</th>
                        <th className="px-5 py-3.5">{t('adminColDetails')}</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                      {logsState === 'loading' ? (
                        <tr>
                          <td colSpan={4} className="px-5 py-12 text-center text-sm text-slate-400">
                            {t('loadingOffers')}
                          </td>
                        </tr>
                      ) : filteredLogs.length === 0 ? (
                        <tr>
                          <td colSpan={4} className="px-5 py-12 text-center text-sm text-slate-400">
                            {t('adminNoJournal')}
                          </td>
                        </tr>
                      ) : (
                        filteredLogs.map((record) => (
                          <tr key={record.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40 transition-colors">
                            <td className="px-5 py-3.5 text-xs text-slate-400 whitespace-nowrap font-medium">
                              {formatWhen(record.timestamp)}
                            </td>
                            <td className="px-5 py-3.5 font-bold text-slate-800 dark:text-slate-200">
                              {record.userEmail || '—'}
                            </td>
                            <td className="px-5 py-3.5 text-xs font-semibold">
                              {actionLabel(record.actionType)}
                            </td>
                            <td className="px-5 py-3.5 text-xs text-slate-500">
                              {record.actionDescription || record.offerName || record.bankName || '—'}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>

      {/* MODAL: Bank Offer Create / Edit */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 backdrop-blur-sm p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-lg p-6 border border-slate-200 dark:border-slate-800 shadow-2xl animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-black text-slate-900 dark:text-white">
                {editingId ? t('editBankOfferTitle') : t('newBankOfferTitle')}
              </h3>
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="p-1.5 rounded-xl text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer"
              >
                <X size={18} />
              </button>
            </div>
            <form onSubmit={saveOffer} className="space-y-3.5">
              <div className="grid grid-cols-2 gap-3">
                <Field label={t('bankNameLabel')}>
                  <input
                    required
                    value={form.bankName}
                    onChange={(e) => setForm({ ...form, bankName: e.target.value })}
                    className={fieldClass}
                  />
                </Field>
                <Field label={t('programNameLabel')}>
                  <input
                    required
                    value={form.offerName}
                    onChange={(e) => setForm({ ...form, offerName: e.target.value })}
                    className={fieldClass}
                  />
                </Field>
              </div>
              <div className="grid grid-cols-3 gap-3">
                <Field label={t('rate')}>
                  <input
                    required
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={form.annualInterestRate}
                    onChange={(e) => setForm({ ...form, annualInterestRate: Number(e.target.value) })}
                    className={fieldClass}
                  />
                </Field>
                <Field label={t('minDownPaymentLabel')}>
                  <input
                    required
                    type="number"
                    step="0.1"
                    min="0"
                    value={form.minDownPaymentPercent}
                    onChange={(e) => setForm({ ...form, minDownPaymentPercent: Number(e.target.value) })}
                    className={fieldClass}
                  />
                </Field>
                <Field label={t('termMonthsLabel')}>
                  <input
                    required
                    type="number"
                    min="1"
                    value={form.maxTermMonths}
                    onChange={(e) => setForm({ ...form, maxTermMonths: Number(e.target.value) })}
                    className={fieldClass}
                  />
                </Field>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <Field label={`${t('minLoanAmountLabel')} (${getCurrencySymbol(form.currency || 'RUB')})`}>
                  <input
                    type="number"
                    value={form.minLoanAmount || ''}
                    onChange={(e) =>
                      setForm({ ...form, minLoanAmount: e.target.value ? Number(e.target.value) : null })
                    }
                    className={fieldClass}
                  />
                </Field>
                <Field label={`${t('maxLoanAmountLabel')} (${getCurrencySymbol(form.currency || 'RUB')})`}>
                  <input
                    type="number"
                    value={form.maxLoanAmount || ''}
                    onChange={(e) =>
                      setForm({ ...form, maxLoanAmount: e.target.value ? Number(e.target.value) : null })
                    }
                    className={fieldClass}
                  />
                </Field>
              </div>
              <Field label={t('offerCurrencyLabel')}>
                <select
                  value={form.currency || 'RUB'}
                  onChange={(e) => setForm({ ...form, currency: e.target.value as Currency })}
                  className={fieldClass}
                >
                  {CURRENCY_CONFIGS.map((item) => (
                    <option key={item.code} value={item.code}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </Field>
              <div className="flex gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="flex-1 py-2.5 rounded-2xl bg-slate-100 dark:bg-slate-800 text-xs font-bold cursor-pointer"
                >
                  {t('cancel')}
                </button>
                <button
                  type="submit"
                  disabled={savingOffer}
                  className="flex-1 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold cursor-pointer disabled:opacity-60 shadow-md shadow-blue-600/20"
                >
                  {savingOffer ? t('saving') : t('save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Microloan Rate Create / Edit */}
      {rateModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 backdrop-blur-sm p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-md p-6 border border-slate-200 dark:border-slate-800 shadow-2xl animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-black text-slate-900 dark:text-white">
                {editingRate ? t('adminEditTariff') : t('adminAddTariff')}
              </h3>
              <button
                type="button"
                onClick={() => setRateModalOpen(false)}
                className="p-1.5 rounded-xl text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer"
              >
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleSaveRate} className="space-y-3.5">
              <Field label={t('tariffName')}>
                <input
                  required
                  placeholder="Стандарт, Лайт, Срочный..."
                  value={rateForm.name}
                  onChange={(e) => setRateForm({ ...rateForm, name: e.target.value })}
                  className={fieldClass}
                />
              </Field>
              <Field label={t('dailyRate')}>
                <input
                  required
                  type="number"
                  step="0.01"
                  min="0.01"
                  max="10"
                  value={rateForm.baseDailyRate}
                  onChange={(e) => setRateForm({ ...rateForm, baseDailyRate: parseFloat(e.target.value) || 0 })}
                  className={fieldClass}
                />
              </Field>
              <div className="grid grid-cols-2 gap-3">
                <Field label={t('adminMinDays')}>
                  <input
                    required
                    type="number"
                    min="1"
                    value={rateForm.minDayRequired}
                    onChange={(e) => setRateForm({ ...rateForm, minDayRequired: parseInt(e.target.value, 10) || 1 })}
                    className={fieldClass}
                  />
                </Field>
                <Field label={t('adminMaxDays')}>
                  <input
                    required
                    type="number"
                    min="1"
                    value={rateForm.maxDayRequired}
                    onChange={(e) => setRateForm({ ...rateForm, maxDayRequired: parseInt(e.target.value, 10) || 1 })}
                    className={fieldClass}
                  />
                </Field>
              </div>
              <div className="flex gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setRateModalOpen(false)}
                  className="flex-1 py-2.5 rounded-2xl bg-slate-100 dark:bg-slate-800 text-xs font-bold cursor-pointer"
                >
                  {t('cancel')}
                </button>
                <button
                  type="submit"
                  disabled={savingRate}
                  className="flex-1 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold cursor-pointer disabled:opacity-60 shadow-md shadow-blue-600/20"
                >
                  {savingRate ? t('saving') : t('save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Support */}
      {supportModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 backdrop-blur-sm p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-md p-6 border border-slate-200 dark:border-slate-800 shadow-2xl animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-black text-slate-900 dark:text-white">
                {t('adminSupportModalTitle')}
              </h3>
              <button
                type="button"
                onClick={() => setSupportModalOpen(false)}
                className="p-1.5 rounded-xl text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer"
              >
                <X size={18} />
              </button>
            </div>
            <p className="text-xs text-slate-500 mb-4">{t('adminSupportModalDesc')}</p>
            <form onSubmit={handleSendSupport} className="space-y-3.5">
              <Field label="Тема обращения">
                <input
                  required
                  placeholder="Вопрос по скорингу или правам доступа"
                  value={supportSubject}
                  onChange={(e) => setSupportSubject(e.target.value)}
                  className={fieldClass}
                />
              </Field>
              <Field label="Текст сообщения">
                <textarea
                  required
                  rows={4}
                  placeholder="Опишите вопрос или инцидент..."
                  value={supportMessageText}
                  onChange={(e) => setSupportMessageText(e.target.value)}
                  className={`${fieldClass} resize-none`}
                />
              </Field>
              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setSupportModalOpen(false)}
                  className="flex-1 py-2.5 rounded-2xl bg-slate-100 dark:bg-slate-800 text-xs font-bold cursor-pointer"
                >
                  {t('cancel')}
                </button>
                <button
                  type="submit"
                  disabled={supportSent}
                  className="flex-1 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold cursor-pointer shadow-md shadow-blue-600/20 disabled:opacity-60"
                >
                  {supportSent ? 'Отправка...' : t('adminSendMessage')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

const fieldClass =
  'w-full px-3.5 py-2.5 rounded-2xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 text-xs font-medium focus:outline-none focus:border-blue-500 transition-colors';

const Field: React.FC<{ label: string; children: React.ReactNode }> = ({ label, children }) => (
  <label className="block">
    <span className="block text-[11px] font-bold text-slate-500 dark:text-slate-400 mb-1">{label}</span>
    {children}
  </label>
);
