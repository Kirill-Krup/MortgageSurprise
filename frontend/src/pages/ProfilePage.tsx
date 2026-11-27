import React, { useState, useEffect, useMemo } from 'react';
import { profileApi, scenarioApi, calculatorApi, passportApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { getApiErrorMessage } from '../utils/errors';
import type {
  UserActivityRecord,
  ScenarioResponse,
  PaymentScheduleItem,
  DailyScheduleItem,
  Currency,
  ScenarioComparisonResponse,
  UserAuditActionType,
  PassportData,
} from '../types';
import { formatCurrency, getCurrencySymbol } from '../utils/format';
import {
  User as UserIcon,
  CreditCard,
  FileText,
  FolderBookmark,
  History,
  Trash2,
  FileDown,
  Table,
  X,
  AlertCircle,
  Sparkles,
  ShieldCheck,
  Banknote,
  TrendingUp,
  Zap,
  Home,
  GitCompare,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  Clock,
  Calendar,
  Check,
  ArrowRight,
  Globe,
  MapPin,
  Users,
} from 'lucide-react';

type ProfileTab = 'overview' | 'microloans' | 'scenarios' | 'history';
type LoanFilter = 'ALL' | 'ACTIVE' | 'REPAID';

interface ProfilePageProps {
  onOpenAuth: () => void;
  onNavigateToCalculator: () => void;
  onNavigateToMicroloan: () => void;
  onNavigateToCompare?: () => void;
}

export const ProfilePage: React.FC<ProfilePageProps> = ({
  onOpenAuth,
  onNavigateToCalculator,
  onNavigateToMicroloan,
  onNavigateToCompare,
}) => {
  const { user, loading: authLoading } = useAuth();
  const { t, language } = useLanguage();
  const isAdmin = user?.role === 'ROLE_ADMIN';

  const [activeTab, setActiveTab] = useState<ProfileTab>('overview');
  const [activity, setActivity] = useState<UserActivityRecord[]>([]);
  const [scenarios, setScenarios] = useState<ScenarioResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pdfToast, setPdfToast] = useState<string | null>(null);

  // Repaid microloans tracking (stored in localStorage per user)
  const [repaidLoanIds, setRepaidLoanIds] = useState<string[]>(() => {
    if (!user?.id) return [];
    try {
      const saved = localStorage.getItem(`repaid_loans_${user.id}`);
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  // Keep repaidLoanIds synchronized when user.id is resolved
  useEffect(() => {
    if (!user?.id) {
      setRepaidLoanIds([]);
      return;
    }
    try {
      const saved = localStorage.getItem(`repaid_loans_${user.id}`);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) {
          setRepaidLoanIds(parsed);
        }
      }
    } catch {
      // ignore
    }
  }, [user?.id]);

  const [loanFilter, setLoanFilter] = useState<LoanFilter>('ALL');

  // Repay confirmation modal
  const [repayingLoan, setRepayingLoan] = useState<UserActivityRecord | null>(null);

  // Microloan daily schedule modal
  const [microloanScheduleModal, setMicroloanScheduleModal] = useState<{
    loan: UserActivityRecord;
    schedule: DailyScheduleItem[];
  } | null>(null);

  // Mortgage schedule viewer modal
  const [selectedSchedule, setSelectedSchedule] = useState<{
    title: string;
    currency?: Currency;
    items: PaymentScheduleItem[];
  } | null>(null);
  const [loadingSchedule, setLoadingSchedule] = useState(false);

  // Expanded microloan details
  const [expandedLoanId, setExpandedLoanId] = useState<string | null>(null);

  // Scenario selection for comparison inside Profile
  const [selectedScenarioIds, setSelectedScenarioIds] = useState<string[]>([]);
  const [comparisonResult, setComparisonResult] = useState<ScenarioComparisonResponse | null>(null);
  const [isComparing, setIsComparing] = useState(false);
  const [showComparison, setShowComparison] = useState(false);

  // History action filter
  const [historyFilter, setHistoryFilter] = useState<UserAuditActionType | 'ALL'>('ALL');

  // Passport details state (AES-256 encrypted at rest on backend)
  const [passportData, setPassportData] = useState<PassportData | null>(null);
  const [passportModalOpen, setPassportModalOpen] = useState(false);
  const [passportForm, setPassportForm] = useState<PassportData>({
    fullName: '',
    seriesNumber: '',
    issueDate: '',
    departmentCode: '',
    issuedBy: '',
  });
  const [savingPassport, setSavingPassport] = useState(false);
  const [passportNotice, setPassportNotice] = useState<string | null>(null);
  const [passportError, setPassportError] = useState<string | null>(null);

  const openPassportModal = () => {
    setPassportError(null);
    if (passportData) {
      setPassportForm({
        fullName: passportData.fullName || '',
        seriesNumber: passportData.seriesNumber || '',
        issueDate: passportData.issueDate || '',
        departmentCode: passportData.departmentCode || '',
        issuedBy: passportData.issuedBy || '',
        loanApplicationRequest: passportData.loanApplicationRequest
          ? {
              age: passportData.loanApplicationRequest.age,
              country: passportData.loanApplicationRequest.country || '',
              city: passportData.loanApplicationRequest.city || '',
              gender: passportData.loanApplicationRequest.gender || 'Мужской',
            }
          : {
              age: 25,
              country: 'Россия',
              city: 'Москва',
              gender: 'Мужской',
            },
      });
    } else {
      setPassportForm({
        fullName: user?.email ? user.email.split('@')[0].replace(/[._]/g, ' ').toUpperCase() : '',
        seriesNumber: '',
        issueDate: '',
        departmentCode: '',
        issuedBy: '',
        loanApplicationRequest: {
          age: 25,
          country: 'Россия',
          city: 'Москва',
          gender: 'Мужской',
        },
      });
    }
    setPassportModalOpen(true);
  };

  const handleSavePassport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!passportForm.fullName.trim() || !passportForm.seriesNumber.trim()) {
      return;
    }
    setSavingPassport(true);
    try {
      const saved = await passportApi.save({
        fullName: passportForm.fullName.trim(),
        seriesNumber: passportForm.seriesNumber.trim(),
        issueDate: passportForm.issueDate || undefined,
        departmentCode: passportForm.departmentCode || undefined,
        issuedBy: passportForm.issuedBy || undefined,
        loanApplicationRequest: passportForm.loanApplicationRequest?.age
          ? {
              age: Number(passportForm.loanApplicationRequest.age),
              country: passportForm.loanApplicationRequest.country?.trim() || '',
              city: passportForm.loanApplicationRequest.city?.trim() || '',
              gender: passportForm.loanApplicationRequest.gender?.trim() || 'Мужской',
            }
          : undefined,
      });
      setPassportData(saved);
      setPassportModalOpen(false);
      setPassportNotice(t('passportSavedSuccess'));
      setTimeout(() => setPassportNotice(null), 4000);
    } catch (err: unknown) {
      setPassportError(getApiErrorMessage(err, 'Не удалось сохранить паспортные данные'));
    } finally {
      setSavingPassport(false);
    }
  };

  const handleDeletePassport = async () => {
    if (!window.confirm(t('passportDeleteConfirm'))) return;
    try {
      await passportApi.delete();
      setPassportData(null);
      setPassportNotice(t('passportDeletedSuccess'));
      setTimeout(() => setPassportNotice(null), 4000);
    } catch {
      alert('Не удалось удалить паспортные данные');
    }
  };

  useEffect(() => {
    if (authLoading) return;
    if (!user || isAdmin) {
      setLoading(false);
      return;
    }

    let isMounted = true;

    const loadData = async () => {
      setLoading(true);
      setError(null);
      try {
        const [activityRes, scenarioRes, passportRes] = await Promise.allSettled([
          profileApi.getActivity(),
          scenarioApi.getAll(),
          passportApi.get(),
        ]);

        if (!isMounted) return;

        if (activityRes.status === 'fulfilled' && Array.isArray(activityRes.value)) {
          setActivity(activityRes.value);
        } else {
          setActivity([]);
        }

        if (scenarioRes.status === 'fulfilled' && Array.isArray(scenarioRes.value)) {
          setScenarios(scenarioRes.value);
        } else {
          setScenarios([]);
        }

        if (passportRes.status === 'fulfilled') {
          setPassportData(passportRes.value);
        }

        // Only show an error notice if both network requests failed completely
        if (activityRes.status === 'rejected' && scenarioRes.status === 'rejected') {
          setError(t('profileLoadError'));
        }
      } catch {
        if (isMounted) {
          setError(t('profileLoadError'));
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    loadData();

    return () => {
      isMounted = false;
    };
  }, [user, isAdmin, authLoading, t]);

  const saveRepaidLoans = (newRepaid: string[]) => {
    setRepaidLoanIds(newRepaid);
    if (user?.id) {
      try {
        localStorage.setItem(`repaid_loans_${user.id}`, JSON.stringify(newRepaid));
      } catch {
        // ignore
      }
    }
  };

  const handleConfirmRepay = (loan: UserActivityRecord) => {
    if (!loan?.id) return;
    const updated = [...repaidLoanIds, loan.id];
    saveRepaidLoans(updated);
    setRepayingLoan(null);
    setPdfToast(t('profileRepaySuccess'));
    setTimeout(() => setPdfToast(null), 5000);
  };

  // Safe derived data
  const safeActivity = useMemo(() => (Array.isArray(activity) ? activity : []), [activity]);
  const safeScenarios = useMemo(() => (Array.isArray(scenarios) ? scenarios : []), [scenarios]);

  const microloanApplications = useMemo(
    () => safeActivity.filter((a) => a && a.actionType === 'MICROLOAN_APPLICATION'),
    [safeActivity]
  );

  const activeMicroloans = useMemo(
    () => microloanApplications.filter((a) => a && a.id && !repaidLoanIds.includes(a.id)),
    [microloanApplications, repaidLoanIds]
  );

  const filteredMicroloans = useMemo(() => {
    if (loanFilter === 'ACTIVE') {
      return microloanApplications.filter((a) => a && a.id && !repaidLoanIds.includes(a.id));
    }
    if (loanFilter === 'REPAID') {
      return microloanApplications.filter((a) => a && a.id && repaidLoanIds.includes(a.id));
    }
    return microloanApplications;
  }, [microloanApplications, loanFilter, repaidLoanIds]);

  const microloanCalculations = useMemo(
    () => safeActivity.filter((a) => a && a.actionType === 'MICROLOAN_CALCULATION'),
    [safeActivity]
  );

  const mortgageCalculations = useMemo(
    () => safeActivity.filter((a) => a && a.actionType === 'MORTGAGE_CALCULATION'),
    [safeActivity]
  );

  const totalActiveMicroloanDebt = useMemo(() => {
    return activeMicroloans.reduce((sum, a) => sum + (Number(a?.totalPayment) || 0), 0);
  }, [activeMicroloans]);

  const totalMicroloanBorrowed = useMemo(() => {
    return microloanApplications.reduce((sum, a) => sum + (Number(a?.loanAmount) || 0), 0);
  }, [microloanApplications]);

  const calculateDueInfo = (timestamp?: string | null, termDays: number = 14) => {
    const start = timestamp ? new Date(timestamp) : new Date();
    const validStart = isNaN(start.getTime()) ? new Date() : start;
    const due = new Date(validStart.getTime() + (Number(termDays) || 14) * 24 * 60 * 60 * 1000);
    const now = new Date();
    const diffDays = Math.ceil((due.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    return { due, diffDays };
  };

  const nearestDueLoan = useMemo(() => {
    if (activeMicroloans.length === 0) return null;
    let minLoan = activeMicroloans[0];
    if (!minLoan) return null;
    let minDays = calculateDueInfo(minLoan.timestamp, minLoan.termDays ?? 14).diffDays;
    for (const loan of activeMicroloans) {
      if (!loan) continue;
      const { diffDays } = calculateDueInfo(loan.timestamp, loan.termDays ?? 14);
      if (diffDays < minDays) {
        minDays = diffDays;
        minLoan = loan;
      }
    }
    return { loan: minLoan, diffDays: minDays };
  }, [activeMicroloans]);

  const formatDate = (iso?: string | null) => {
    if (!iso) return '—';
    const dateObj = new Date(iso);
    if (isNaN(dateObj.getTime())) return '—';
    return dateObj.toLocaleDateString(language === 'en' ? 'en-US' : 'ru-RU', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm(t('confirmDeleteScenario'))) return;
    try {
      await scenarioApi.delete(id);
      setScenarios((prev) => prev.filter((s) => s.id !== id));
      setSelectedScenarioIds((prev) => prev.filter((item) => item !== id));
    } catch {
      setError(t('scenarioDeleteError'));
    }
  };

  const handleExportPdf = async (id: string) => {
    try {
      const res = await scenarioApi.exportPdf(id);
      setPdfToast(res.message || t('pdfExportStartedToast'));
      setTimeout(() => setPdfToast(null), 5000);
    } catch {
      setError(t('pdfExportErrorToast'));
    }
  };

  const handleViewSchedule = async (scenario: ScenarioResponse) => {
    if (!scenario?.mongoScheduleId) {
      setError(t('noScheduleFoundError'));
      return;
    }
    setLoadingSchedule(true);
    try {
      const items = await calculatorApi.getSchedule(scenario.mongoScheduleId);
      setSelectedSchedule({
        title: scenario.title,
        currency: scenario.currency,
        items,
      });
    } catch {
      setError(t('scheduleLoadError'));
    } finally {
      setLoadingSchedule(false);
    }
  };

  const handleViewMicroloanSchedule = (loan: UserActivityRecord) => {
    const safeAmount = Number(loan?.loanAmount) || 15000;
    const safeTerm = Number(loan?.termDays) || 14;
    const isVanity = loan?.microloanType === 'VANITY';
    const baseDailyRate = 0.8;
    const dailyInterest = Math.round((safeAmount * (baseDailyRate / 100)) * 100) / 100;
    const vanitySurcharge = isVanity ? Math.round((safeAmount * 0.05) * 100) / 100 : 0;

    const start = loan?.timestamp ? new Date(loan.timestamp) : new Date();
    const validStart = isNaN(start.getTime()) ? new Date() : start;
    const schedule: DailyScheduleItem[] = [];
    let accumulated = 0;

    for (let d = 1; d <= safeTerm; d++) {
      accumulated = Math.round((accumulated + dailyInterest) * 100) / 100;
      const dayDate = new Date(validStart);
      dayDate.setDate(dayDate.getDate() + d);
      const dayExtra = d === safeTerm ? vanitySurcharge : 0;
      schedule.push({
        dayNumber: d,
        date: dayDate.toISOString().split('T')[0],
        principalBalance: safeAmount,
        dailyInterest: dailyInterest + dayExtra,
        accumulatedInterest: accumulated + (d === safeTerm ? vanitySurcharge : 0),
        totalDue: safeAmount + accumulated + (d === safeTerm ? vanitySurcharge : 0),
      });
    }

    setMicroloanScheduleModal({ loan, schedule });
  };

  // Toggle selection for scenario comparison
  const toggleScenarioSelect = (id: string) => {
    if (selectedScenarioIds.includes(id)) {
      setSelectedScenarioIds(selectedScenarioIds.filter((item) => item !== id));
    } else {
      if (selectedScenarioIds.length >= 3) {
        setError(t('maxCompareLimitError'));
        return;
      }
      setError(null);
      setSelectedScenarioIds([...selectedScenarioIds, id]);
    }
  };

  // Compare selected scenarios
  const handleRunComparison = async () => {
    if (selectedScenarioIds.length < 2 || selectedScenarioIds.length > 3) {
      setError(t('selectScenariosError'));
      return;
    }
    setIsComparing(true);
    setError(null);
    try {
      const res = await scenarioApi.compare(selectedScenarioIds);
      setComparisonResult(res);
      setShowComparison(true);
    } catch {
      setError(t('compareErrorDefault'));
    } finally {
      setIsComparing(false);
    }
  };

  const filteredHistory = useMemo(() => {
    if (historyFilter === 'ALL') return safeActivity;
    return safeActivity.filter((a) => a && a.actionType === historyFilter);
  }, [safeActivity, historyFilter]);

  // Clean skeleton loading state while auth is determining session
  if (authLoading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-in fade-in duration-200">
        <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 sm:p-8 shadow-xs mb-8 animate-pulse">
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-5">
            <div className="w-16 h-16 rounded-2xl bg-slate-200 dark:bg-slate-800" />
            <div className="flex-1 space-y-2.5">
              <div className="h-6 w-48 bg-slate-200 dark:bg-slate-800 rounded-lg" />
              <div className="h-4 w-32 bg-slate-100 dark:bg-slate-800/60 rounded-md" />
            </div>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6 pt-6 border-t border-slate-100 dark:border-slate-800">
            {[1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="bg-slate-50 dark:bg-slate-950 rounded-2xl p-4 border border-slate-100 dark:border-slate-800 space-y-2"
              >
                <div className="h-3 w-20 bg-slate-200 dark:bg-slate-800 rounded" />
                <div className="h-6 w-16 bg-slate-200 dark:bg-slate-800 rounded" />
              </div>
            ))}
          </div>
        </div>
        <div className="py-16 text-center text-slate-400 dark:text-slate-500">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-3 border-blue-600 border-t-transparent" />
          <p className="mt-3 text-sm font-medium">{t('loadingScenarios')}</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center animate-in fade-in">
        <div className="w-16 h-16 bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 rounded-3xl flex items-center justify-center mx-auto mb-4 border border-blue-200 dark:border-blue-800">
          <UserIcon size={32} />
        </div>
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white mb-2">
          {t('profileTitle')}
        </h2>
        <p className="text-slate-600 dark:text-slate-400 max-w-md mx-auto mb-6 text-sm">
          {t('profileAuthRequired')}
        </p>
        <button
          onClick={onOpenAuth}
          className="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-2xl shadow-lg shadow-blue-500/20 transition-all cursor-pointer"
        >
          {t('navLogin')}
        </button>
      </div>
    );
  }

  if (isAdmin) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center animate-in fade-in">
        <div className="w-16 h-16 bg-amber-50 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400 rounded-3xl flex items-center justify-center mx-auto mb-4 border border-amber-200 dark:border-amber-800">
          <ShieldCheck size={32} />
        </div>
        <h2 className="text-2xl font-extrabold text-slate-900 dark:text-white mb-2">
          {t('adminConfigModeAlert')}
        </h2>
        <p className="text-slate-600 dark:text-slate-400 max-w-lg mx-auto text-sm">
          {t('adminScenariosNotice')}
        </p>
      </div>
    );
  }

  const profileTabs: { id: ProfileTab; label: string; icon: React.ReactNode; count?: number }[] = [
    { id: 'overview', label: t('profileOverview'), icon: <UserIcon size={17} /> },
    {
      id: 'microloans',
      label: t('profileMyMicroloans'),
      icon: <Zap size={17} />,
      count: activeMicroloans.length,
    },
    {
      id: 'scenarios',
      label: t('navScenarios'),
      icon: <FolderBookmark size={17} />,
      count: safeScenarios.length,
    },
    {
      id: 'history',
      label: t('profileHistory'),
      icon: <History size={17} />,
      count: safeActivity.length,
    },
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-in fade-in duration-300">
      {/* Profile Header */}
      <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 sm:p-8 shadow-xs mb-8">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-5">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-blue-600 to-indigo-700 flex items-center justify-center text-white text-2xl font-black shadow-lg shadow-blue-600/20 border border-blue-500/30">
            {(user?.email?.[0] || 'U').toUpperCase()}
          </div>
          <div className="flex-1 min-w-0">
            <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white truncate">
              {user?.email || 'User'}
            </h1>
            <div className="flex flex-wrap items-center gap-3 mt-2">
              <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                ID: {user?.id ? `${user.id.slice(0, 8)}…` : '—'}
              </span>
              <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-emerald-50 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
                {t('profileStatusActive')}
              </span>
            </div>
          </div>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6 pt-6 border-t border-slate-100 dark:border-slate-800">
          <div className="bg-slate-50 dark:bg-slate-950 rounded-2xl p-4 border border-slate-100 dark:border-slate-800">
            <div className="text-xs text-slate-500 dark:text-slate-400 font-semibold flex items-center gap-1.5 mb-1">
              <Zap size={13} className="text-amber-500" />
              {t('profileActiveMicroloans')}
            </div>
            <div className="text-2xl font-black text-slate-900 dark:text-white">
              {activeMicroloans.length}
            </div>
          </div>

          <div className="bg-slate-50 dark:bg-slate-950 rounded-2xl p-4 border border-slate-100 dark:border-slate-800">
            <div className="text-xs text-slate-500 dark:text-slate-400 font-semibold flex items-center gap-1.5 mb-1">
              <Banknote size={13} className="text-amber-600" />
              {t('profileTotalToRepay')}
            </div>
            <div className="text-2xl font-black text-amber-600 dark:text-amber-400">
              {formatCurrency(totalActiveMicroloanDebt, 'RUB')}
            </div>
          </div>

          <div className="bg-slate-50 dark:bg-slate-950 rounded-2xl p-4 border border-slate-100 dark:border-slate-800">
            <div className="text-xs text-slate-500 dark:text-slate-400 font-semibold flex items-center gap-1.5 mb-1">
              <FolderBookmark size={13} className="text-indigo-500" />
              {t('profileSavedScenarios')}
            </div>
            <div className="text-2xl font-black text-slate-900 dark:text-white">
              {safeScenarios.length}
            </div>
          </div>

          <div className="bg-slate-50 dark:bg-slate-950 rounded-2xl p-4 border border-slate-100 dark:border-slate-800">
            <div className="text-xs text-slate-500 dark:text-slate-400 font-semibold flex items-center gap-1.5 mb-1">
              <TrendingUp size={13} className="text-emerald-500" />
              {t('profileTotalCalculations')}
            </div>
            <div className="text-2xl font-black text-slate-900 dark:text-white">
              {mortgageCalculations.length + microloanCalculations.length}
            </div>
          </div>
        </div>
      </div>

      {/* Tabs Navigation */}
      <div className="flex flex-wrap gap-2 mb-6 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-1.5 shadow-xs">
        {profileTabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all cursor-pointer ${
              activeTab === tab.id
                ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-50 dark:hover:bg-slate-800'
            }`}
          >
            {tab.icon}
            <span>{tab.label}</span>
            {tab.count !== undefined && tab.count > 0 && (
              <span
                className={`text-[10px] font-bold px-1.5 py-0.5 rounded-full ${
                  activeTab === tab.id
                    ? 'bg-blue-200 dark:bg-blue-900 text-blue-800 dark:text-blue-200'
                    : 'bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300'
                }`}
              >
                {tab.count}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* Toast notifications */}
      {pdfToast && (
        <div className="mb-6 p-4 bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-200 dark:border-emerald-800 rounded-2xl text-emerald-800 dark:text-emerald-300 text-sm font-semibold flex items-center gap-2 animate-in fade-in">
          <Sparkles size={18} className="text-emerald-600 dark:text-emerald-400" />
          <span>{pdfToast}</span>
        </div>
      )}

      {error && (
        <div className="mb-6 p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 rounded-2xl text-red-700 dark:text-red-300 text-sm flex items-start gap-2 animate-in fade-in">
          <AlertCircle size={18} className="shrink-0 mt-0.5" />
          <span>{error}</span>
          <button
            onClick={() => setError(null)}
            className="ml-auto text-red-400 hover:text-red-600 cursor-pointer"
          >
            <X size={16} />
          </button>
        </div>
      )}

      {loading ? (
        <div className="py-20 text-center text-slate-400 dark:text-slate-500">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-3 border-blue-600 border-t-transparent" />
          <p className="mt-2 text-sm">{t('loadingScenarios')}</p>
        </div>
      ) : (
        <>
          {/* ===== OVERVIEW TAB ===== */}
          {activeTab === 'overview' && (
            <div className="space-y-6 animate-in fade-in">
              {/* Urgent Microloan Banner if active loans exist */}
              {activeMicroloans.length > 0 && nearestDueLoan && (
                <div className="p-5 bg-gradient-to-r from-amber-500/10 via-amber-500/5 to-transparent border border-amber-300 dark:border-amber-800/80 rounded-3xl flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div className="flex items-start gap-3.5">
                    <div className="p-3 bg-amber-500 text-white rounded-2xl shadow-md shadow-amber-500/20 shrink-0">
                      <Clock size={22} />
                    </div>
                    <div>
                      <h3 className="font-extrabold text-base text-slate-900 dark:text-white">
                        {t('profileUpcomingDue')}{' '}
                        <span className="text-amber-600 dark:text-amber-400">
                          {nearestDueLoan.diffDays > 0
                            ? `${nearestDueLoan.diffDays} ${t('daysUnit')}`
                            : nearestDueLoan.diffDays === 0
                            ? t('profileDueToday')
                            : `${Math.abs(nearestDueLoan.diffDays)} ${t('daysUnit')}`}
                        </span>
                      </h3>
                      <p className="text-xs text-slate-600 dark:text-slate-400 mt-1">
                        {t('profileTotalToRepay')}:{' '}
                        <span className="font-bold text-slate-900 dark:text-white">
                          {formatCurrency(totalActiveMicroloanDebt, 'RUB')}
                        </span>
                        {' '}&bull; {activeMicroloans.length} {t('daysUnit') === 'дн.' ? 'активных займа' : 'active loans'}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => setActiveTab('microloans')}
                    className="px-5 py-2.5 bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs rounded-xl shadow-md shadow-amber-600/20 transition-all flex items-center gap-1.5 self-start md:self-auto cursor-pointer"
                  >
                    <span>{t('profileMyMicroloans')}</span>
                    <ArrowRight size={14} />
                  </button>
                </div>
              )}

              {/* Passport & Identity Card (AES-256 Encrypted) */}
              <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-xs">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
                  <div className="flex items-center gap-3">
                    <div className="p-3 bg-blue-50 dark:bg-blue-950 text-blue-600 dark:text-blue-400 rounded-2xl shrink-0">
                      <ShieldCheck size={22} />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h2 className="text-base font-bold text-slate-900 dark:text-white">
                          {t('passportSectionTitle')}
                        </h2>
                        <span className="text-[10px] font-mono font-bold bg-blue-100 dark:bg-blue-900/60 text-blue-700 dark:text-blue-300 px-2 py-0.5 rounded-full border border-blue-200 dark:border-blue-800">
                          AES-256
                        </span>
                      </div>
                      <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                        {t('passportSectionDesc')}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 self-start sm:self-auto">
                    {passportData ? (
                      <>
                        <button
                          type="button"
                          onClick={openPassportModal}
                          className="px-3.5 py-2 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-bold rounded-xl transition-all cursor-pointer"
                        >
                          {t('passportEditBtn')}
                        </button>
                        <button
                          type="button"
                          onClick={handleDeletePassport}
                          className="px-3.5 py-2 bg-red-50 dark:bg-red-950/40 hover:bg-red-100 dark:hover:bg-red-900/40 text-red-600 dark:text-red-400 text-xs font-bold rounded-xl transition-all cursor-pointer"
                        >
                          {t('passportDeleteBtn')}
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        onClick={openPassportModal}
                        className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-xl shadow-md shadow-blue-600/20 transition-all cursor-pointer"
                      >
                        {t('passportAddBtn')}
                      </button>
                    )}
                  </div>
                </div>

                {passportNotice && (
                  <div className="mb-4 p-3 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 rounded-xl text-emerald-700 dark:text-emerald-300 text-xs flex items-center gap-2">
                    <CheckCircle2 size={16} />
                    <span>{passportNotice}</span>
                  </div>
                )}

                {passportData ? (
                  <div className="p-4 bg-slate-50 dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-800 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-xs">
                    <div>
                      <span className="text-slate-400 block mb-0.5">{t('fullName')}</span>
                      <span className="font-bold text-slate-900 dark:text-white uppercase">{passportData.fullName}</span>
                    </div>
                    <div>
                      <span className="text-slate-400 block mb-0.5">{t('passportSeriesNumber')}</span>
                      <span className="font-mono font-bold text-slate-900 dark:text-white">
                        {passportData.seriesNumber}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block mb-0.5">{t('issueDate')}</span>
                      <span className="font-medium text-slate-700 dark:text-slate-300">
                        {passportData.issueDate || '—'}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block mb-0.5">{t('departmentCode')}</span>
                      <span className="font-mono font-medium text-slate-700 dark:text-slate-300">
                        {passportData.departmentCode || '—'}
                      </span>
                    </div>
                    {passportData.issuedBy && (
                      <div className="sm:col-span-2 lg:col-span-4 pt-2 border-t border-slate-200/60 dark:border-slate-800/60">
                        <span className="text-slate-400 block mb-0.5">{t('issuedBy')}</span>
                        <span className="text-slate-700 dark:text-slate-300">{passportData.issuedBy}</span>
                      </div>
                    )}
                    {passportData.loanApplicationRequest && (
                      <div className="sm:col-span-2 lg:col-span-4 pt-3 border-t border-slate-200/60 dark:border-slate-800/60 grid grid-cols-2 sm:grid-cols-4 gap-3">
                        <div>
                          <span className="text-slate-400 block mb-0.5 flex items-center gap-1">
                            <Calendar size={12} className="text-blue-500" />
                            {language === 'ru' ? 'Возраст' : 'Age'}
                          </span>
                          <span className="font-semibold text-slate-800 dark:text-slate-200">
                            {passportData.loanApplicationRequest.age ? `${passportData.loanApplicationRequest.age} ${language === 'ru' ? 'лет' : 'y/o'}` : '—'}
                          </span>
                        </div>
                        <div>
                          <span className="text-slate-400 block mb-0.5 flex items-center gap-1">
                            <Users size={12} className="text-blue-500" />
                            {language === 'ru' ? 'Пол' : 'Gender'}
                          </span>
                          <span className="font-semibold text-slate-800 dark:text-slate-200">
                            {passportData.loanApplicationRequest.gender || '—'}
                          </span>
                        </div>
                        <div>
                          <span className="text-slate-400 block mb-0.5 flex items-center gap-1">
                            <Globe size={12} className="text-blue-500" />
                            {language === 'ru' ? 'Страна' : 'Country'}
                          </span>
                          <span className="font-semibold text-slate-800 dark:text-slate-200">
                            {passportData.loanApplicationRequest.country || '—'}
                          </span>
                        </div>
                        <div>
                          <span className="text-slate-400 block mb-0.5 flex items-center gap-1">
                            <MapPin size={12} className="text-blue-500" />
                            {language === 'ru' ? 'Город' : 'City'}
                          </span>
                          <span className="font-semibold text-slate-800 dark:text-slate-200">
                            {passportData.loanApplicationRequest.city || '—'}
                          </span>
                        </div>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="p-6 bg-slate-50 dark:bg-slate-950 rounded-2xl border border-dashed border-slate-200 dark:border-slate-800 text-center">
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {t('passportNotProvided')}
                    </p>
                  </div>
                )}
              </div>

              {/* Active Microloans Summary List */}
              {microloanApplications.length > 0 && (
                <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-xs">
                  <div className="flex items-center justify-between mb-4">
                    <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
                      <Zap size={20} className="text-amber-500" />
                      {t('profileMyMicroloans')}
                    </h2>
                    <button
                      onClick={() => setActiveTab('microloans')}
                      className="text-xs font-semibold text-blue-600 dark:text-blue-400 hover:underline cursor-pointer"
                    >
                      {t('profileViewAll')} ({microloanApplications.length})
                    </button>
                  </div>
                  <div className="space-y-3">
                    {microloanApplications.slice(0, 3).map((loan) => {
                      const isRepaid = loan.id ? repaidLoanIds.includes(loan.id) : false;
                      const { due, diffDays } = calculateDueInfo(loan.timestamp, loan.termDays ?? 14);

                      return (
                        <div
                          key={loan.id}
                          className="p-4 bg-slate-50 dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                        >
                          <div className="flex items-center gap-3 min-w-0">
                            <div
                              className={`p-2 rounded-xl shrink-0 ${
                                isRepaid
                                  ? 'bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300'
                                  : 'bg-amber-100 dark:bg-amber-900/60 text-amber-700 dark:text-amber-300'
                              }`}
                            >
                              <CreditCard size={18} />
                            </div>
                            <div className="min-w-0">
                              <div className="flex items-center gap-2">
                                <span className="font-bold text-sm text-slate-900 dark:text-white truncate">
                                  {loan.applicationId ?? 'MS-…'}
                                </span>
                                <span
                                  className={`text-[10px] font-bold px-1.5 py-0.5 rounded-full ${
                                    isRepaid
                                      ? 'bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300 border border-emerald-300'
                                      : 'bg-amber-100 dark:bg-amber-950 text-amber-800 dark:text-amber-300 border border-amber-300'
                                  }`}
                                >
                                  {isRepaid ? t('profileStatusRepaid') : t('profileStatusApproved')}
                                </span>
                              </div>
                              <div className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 flex flex-wrap items-center gap-2">
                                <span>{t('profileDueDate')}: {due.toLocaleDateString(language === 'en' ? 'en-US' : 'ru-RU')}</span>
                                {!isRepaid && (
                                  <span className="font-bold text-amber-600 dark:text-amber-400">
                                    ({diffDays > 0 ? `${t('profileDaysLeft')} ${diffDays}` : t('profileDueToday')})
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                          <div className="flex items-center gap-4">
                            <div className="text-right">
                              <div className="text-[11px] text-slate-500 dark:text-slate-400">{t('loanAmount')}</div>
                              <div className="font-bold text-sm text-slate-900 dark:text-white">
                                {formatCurrency(loan.loanAmount, loan.currency || 'RUB')}
                              </div>
                            </div>
                            <div className="text-right">
                              <div className="text-[11px] text-slate-500 dark:text-slate-400">{t('totalRepayment')}</div>
                              <div className={`font-bold text-sm ${isRepaid ? 'text-emerald-600 dark:text-emerald-400 line-through' : 'text-amber-600 dark:text-amber-400'}`}>
                                {formatCurrency(loan.totalPayment, loan.currency || 'RUB')}
                              </div>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Recent Scenarios */}
              {safeScenarios.length > 0 && (
                <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-xs">
                  <div className="flex items-center justify-between mb-4">
                    <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
                      <FolderBookmark size={20} className="text-indigo-500" />
                      {t('profileRecentScenarios')}
                    </h2>
                    <button
                      onClick={() => setActiveTab('scenarios')}
                      className="text-xs font-semibold text-blue-600 dark:text-blue-400 hover:underline cursor-pointer"
                    >
                      {t('profileViewAll')} ({safeScenarios.length})
                    </button>
                  </div>
                  <div className="space-y-3">
                    {safeScenarios.slice(0, 3).map((sc) => (
                      <div
                        key={sc.id}
                        className="p-4 bg-slate-50 dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                      >
                        <div className="min-w-0">
                          <div className="font-bold text-sm text-slate-900 dark:text-white truncate">
                            {sc.title}
                          </div>
                          <div className="text-xs text-slate-500 dark:text-slate-400">
                            {sc.annualInterestRate ?? 0}% • {Math.floor((sc.termMonths ?? 0) / 12)} {t('years')} •{' '}
                            {sc.paymentType === 'ANNUITY' ? t('annuity') : t('differentiated')}
                          </div>
                        </div>
                        <div className="flex items-center gap-4">
                          <div className="text-right">
                            <div className="text-[11px] text-slate-500 dark:text-slate-400">{t('monthlyPayment')}</div>
                            <div className="font-bold text-sm text-slate-900 dark:text-white">
                              {formatCurrency(sc.monthlyPayment, sc.currency || 'RUB')}
                            </div>
                          </div>
                          <div className="text-right">
                            <div className="text-[11px] text-slate-500 dark:text-slate-400">{t('overpaymentInterest')}</div>
                            <div className="font-bold text-sm text-amber-600 dark:text-amber-400">
                              {formatCurrency(sc.totalInterest, sc.currency || 'RUB')}
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Quick Actions */}
              <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 shadow-xs">
                <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2 mb-4">
                  {t('profileQuickActions')}
                </h2>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <button
                    onClick={onNavigateToCalculator}
                    className="p-4 bg-blue-50 dark:bg-blue-950/40 hover:bg-blue-100 dark:hover:bg-blue-900/50 border border-blue-200 dark:border-blue-800 rounded-2xl flex items-center gap-3 transition-colors cursor-pointer"
                  >
                    <div className="p-2 rounded-xl bg-blue-100 dark:bg-blue-900/60 text-blue-700 dark:text-blue-300">
                      <Home size={20} />
                    </div>
                    <div className="text-left">
                      <div className="font-bold text-sm text-blue-900 dark:text-blue-200">{t('profileNewMortgage')}</div>
                      <div className="text-xs text-blue-600 dark:text-blue-400">{t('navMortgageDesc')}</div>
                    </div>
                  </button>
                  <button
                    onClick={onNavigateToMicroloan}
                    className="p-4 bg-amber-50 dark:bg-amber-950/40 hover:bg-amber-100 dark:hover:bg-amber-900/50 border border-amber-200 dark:border-amber-800 rounded-2xl flex items-center gap-3 transition-colors cursor-pointer"
                  >
                    <div className="p-2 rounded-xl bg-amber-100 dark:bg-amber-900/60 text-amber-700 dark:text-amber-300">
                      <Zap size={20} />
                    </div>
                    <div className="text-left">
                      <div className="font-bold text-sm text-amber-900 dark:text-amber-200">{t('profileNewMicroloan')}</div>
                      <div className="text-xs text-amber-600 dark:text-amber-400">{t('navMicroloanDesc')}</div>
                    </div>
                  </button>
                  {onNavigateToCompare && (
                    <button
                      onClick={onNavigateToCompare}
                      className="p-4 bg-indigo-50 dark:bg-indigo-950/40 hover:bg-indigo-100 dark:hover:bg-indigo-900/50 border border-indigo-200 dark:border-indigo-800 rounded-2xl flex items-center gap-3 transition-colors cursor-pointer"
                    >
                      <div className="p-2 rounded-xl bg-indigo-100 dark:bg-indigo-900/60 text-indigo-700 dark:text-indigo-300">
                        <GitCompare size={20} />
                      </div>
                      <div className="text-left">
                        <div className="font-bold text-sm text-indigo-900 dark:text-indigo-200">{t('profileCompareBtn')}</div>
                        <div className="text-xs text-indigo-600 dark:text-indigo-400">{t('compareSubtitle')}</div>
                      </div>
                    </button>
                  )}
                </div>
              </div>

              {/* Empty state if no data at all */}
              {microloanApplications.length === 0 && safeScenarios.length === 0 && (
                <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-12 text-center shadow-xs">
                  <FileText size={36} className="mx-auto text-slate-400 mb-3" />
                  <h3 className="text-base font-bold text-slate-800 dark:text-white">{t('profileEmptyTitle')}</h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-md mx-auto">
                    {t('profileEmptyDesc')}
                  </p>
                </div>
              )}
            </div>
          )}

          {/* ===== MICROLOANS TAB ===== */}
          {activeTab === 'microloans' && (
            <div className="space-y-6 animate-in fade-in">
              {/* Summary cards */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-5 shadow-xs">
                  <div className="text-xs text-slate-500 dark:text-slate-400 font-semibold mb-1">{t('profileTotalLoans')}</div>
                  <div className="text-3xl font-black text-slate-900 dark:text-white">{microloanApplications.length}</div>
                </div>
                <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-5 shadow-xs">
                  <div className="text-xs text-slate-500 dark:text-slate-400 font-semibold mb-1">{t('profileTotalBorrowed')}</div>
                  <div className="text-3xl font-black text-slate-900 dark:text-white">{formatCurrency(totalMicroloanBorrowed, 'RUB')}</div>
                </div>
                <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-5 shadow-xs">
                  <div className="text-xs text-slate-500 dark:text-slate-400 font-semibold mb-1">{t('profileTotalToRepay')}</div>
                  <div className="text-3xl font-black text-amber-600 dark:text-amber-400">{formatCurrency(totalActiveMicroloanDebt, 'RUB')}</div>
                </div>
              </div>

              {/* Filter Tabs */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex bg-slate-100 dark:bg-slate-800 p-1 rounded-xl">
                  <button
                    onClick={() => setLoanFilter('ALL')}
                    className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-colors cursor-pointer ${
                      loanFilter === 'ALL'
                        ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs'
                        : 'text-slate-600 dark:text-slate-400'
                    }`}
                  >
                    {t('profileFilterAll')} ({microloanApplications.length})
                  </button>
                  <button
                    onClick={() => setLoanFilter('ACTIVE')}
                    className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-colors cursor-pointer ${
                      loanFilter === 'ACTIVE'
                        ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs'
                        : 'text-slate-600 dark:text-slate-400'
                    }`}
                  >
                    {t('profileFilterActive')} ({activeMicroloans.length})
                  </button>
                  <button
                    onClick={() => setLoanFilter('REPAID')}
                    className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-colors cursor-pointer ${
                      loanFilter === 'REPAID'
                        ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs'
                        : 'text-slate-600 dark:text-slate-400'
                    }`}
                  >
                    {t('profileFilterRepaid')} ({repaidLoanIds.length})
                  </button>
                </div>

                <button
                  onClick={onNavigateToMicroloan}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-xl shadow-sm transition-all self-start sm:self-auto cursor-pointer"
                >
                  + {t('profileNewMicroloan')}
                </button>
              </div>

              {filteredMicroloans.length === 0 ? (
                <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-12 text-center shadow-xs">
                  <Zap size={36} className="mx-auto text-slate-400 mb-3" />
                  <h3 className="text-base font-bold text-slate-800 dark:text-white">{t('profileNoMicroloans')}</h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 mb-5">{t('profileNoMicroloansDesc')}</p>
                  <button
                    onClick={onNavigateToMicroloan}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-xl shadow-sm cursor-pointer"
                  >
                    {t('profileNewMicroloan')}
                  </button>
                </div>
              ) : (
                <div className="space-y-4">
                  {filteredMicroloans.map((loan) => {
                    const isExpanded = expandedLoanId === loan.id;
                    const isRepaid = loan.id ? repaidLoanIds.includes(loan.id) : false;
                    const { due, diffDays } = calculateDueInfo(loan.timestamp, loan.termDays ?? 14);

                    return (
                      <div
                        key={loan.id}
                        className={`bg-white dark:bg-slate-900 rounded-3xl border shadow-xs overflow-hidden transition-all ${
                          isRepaid
                            ? 'border-emerald-200 dark:border-emerald-950/60 opacity-90'
                            : 'border-slate-200 dark:border-slate-800'
                        }`}
                      >
                        <div className="p-6">
                          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                            <div className="flex items-start gap-3.5 min-w-0">
                              <div
                                className={`p-3 rounded-2xl shrink-0 mt-0.5 ${
                                  isRepaid
                                    ? 'bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300'
                                    : loan.microloanType === 'VANITY'
                                    ? 'bg-amber-100 dark:bg-amber-900/60 text-amber-700 dark:text-amber-300'
                                    : 'bg-blue-100 dark:bg-blue-900/60 text-blue-700 dark:text-blue-300'
                                }`}
                              >
                                <CreditCard size={22} />
                              </div>
                              <div className="min-w-0">
                                <div className="flex flex-wrap items-center gap-2">
                                  <span className="font-extrabold text-slate-900 dark:text-white text-base">
                                    {loan.applicationId ?? 'MS-…'}
                                  </span>
                                  <span
                                    className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                                      loan.microloanType === 'VANITY'
                                        ? 'bg-amber-100 dark:bg-amber-950 text-amber-800 dark:text-amber-200 border border-amber-300 dark:border-amber-800'
                                        : 'bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800'
                                    }`}
                                  >
                                    {loan.microloanType === 'VANITY' ? t('vanityType') : t('basicNeedsType')}
                                  </span>
                                  {isRepaid ? (
                                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800 flex items-center gap-1">
                                      <Check size={11} />
                                      {t('profileStatusRepaid')}
                                    </span>
                                  ) : (
                                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                                      {t('profileStatusApproved')}
                                    </span>
                                  )}
                                </div>

                                <div className="text-xs text-slate-500 dark:text-slate-400 mt-1 flex flex-wrap items-center gap-3">
                                  <span>{t('startDate')}: {formatDate(loan.timestamp)}</span>
                                  <span>&bull;</span>
                                  <span>{t('loanTermDays')}: {loan.termDays ?? 14} {t('daysUnit')}</span>
                                  <span>&bull;</span>
                                  <span className="font-semibold text-slate-800 dark:text-slate-200">
                                    {t('profileDueDate')}: {due.toLocaleDateString(language === 'en' ? 'en-US' : 'ru-RU')}
                                  </span>
                                </div>
                              </div>
                            </div>

                            {/* Repayment amount highlight */}
                            <div className="flex items-center gap-4 sm:gap-6">
                              <div className="text-left md:text-right">
                                <div className="text-xs text-slate-500 dark:text-slate-400 font-semibold">{t('loanAmount')}</div>
                                <div className="text-base font-bold text-slate-900 dark:text-white">
                                  {formatCurrency(loan.loanAmount, loan.currency || 'RUB')}
                                </div>
                              </div>

                              <div className="text-left md:text-right">
                                <div className="text-xs text-slate-500 dark:text-slate-400 font-semibold">{t('totalRepayment')}</div>
                                <div
                                  className={`text-xl font-black ${
                                    isRepaid
                                      ? 'text-emerald-600 dark:text-emerald-400 line-through'
                                      : 'text-amber-600 dark:text-amber-400'
                                  }`}
                                >
                                  {formatCurrency(loan.totalPayment, loan.currency || 'RUB')}
                                </div>
                              </div>
                            </div>
                          </div>

                          {/* Days status badge & Actions */}
                          <div className="mt-5 pt-4 border-t border-slate-100 dark:border-slate-800 flex flex-wrap items-center justify-between gap-3">
                            <div className="flex items-center gap-2">
                              {!isRepaid ? (
                                <div
                                  className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-bold ${
                                    diffDays > 3
                                      ? 'bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800'
                                      : diffDays >= 0
                                      ? 'bg-red-50 dark:bg-red-950/60 text-red-700 dark:text-red-300 border border-red-200 dark:border-red-800 animate-pulse'
                                      : 'bg-red-100 dark:bg-red-900 text-red-900 dark:text-red-100'
                                  }`}
                                >
                                  <Clock size={13} />
                                  <span>
                                    {diffDays > 0
                                      ? `${t('profileDaysLeft')} ${diffDays} ${t('daysUnit')}`
                                      : diffDays === 0
                                      ? t('profileDueToday')
                                      : `${t('profileDaysOverdue')} ${Math.abs(diffDays)} ${t('daysUnit')}`}
                                  </span>
                                </div>
                              ) : (
                                <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-bold bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
                                  <CheckCircle2 size={13} />
                                  <span>{t('profileRepaySuccess')}</span>
                                </div>
                              )}
                            </div>

                            <div className="flex items-center gap-2">
                              <button
                                onClick={() => handleViewMicroloanSchedule(loan)}
                                className="px-3 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-semibold rounded-xl transition-colors flex items-center gap-1.5 cursor-pointer"
                              >
                                <Table size={14} />
                                <span>{t('profileDailyScheduleModal')}</span>
                              </button>

                              {!isRepaid && (
                                <button
                                  onClick={() => setRepayingLoan(loan)}
                                  className="px-3.5 py-1.5 bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold rounded-xl shadow-xs transition-colors cursor-pointer"
                                >
                                  {t('profileRepayBtn')}
                                </button>
                              )}

                              <button
                                onClick={() => setExpandedLoanId(isExpanded ? null : loan.id)}
                                className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
                                title="Подробнее"
                              >
                                {isExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                              </button>
                            </div>
                          </div>

                          {/* Expandable detailed drawer */}
                          {isExpanded && (
                            <div className="mt-4 pt-4 border-t border-dashed border-slate-200 dark:border-slate-800 animate-in fade-in">
                              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs">
                                <div>
                                  <div className="text-slate-500 dark:text-slate-400 font-semibold">{t('loanPurpose')}</div>
                                  <div className="font-bold text-slate-900 dark:text-white mt-0.5">
                                    {loan.loanPurpose ?? '—'}
                                  </div>
                                </div>
                                <div>
                                  <div className="text-slate-500 dark:text-slate-400 font-semibold">{t('totalInterest')}</div>
                                  <div className="font-bold text-amber-600 dark:text-amber-400 mt-0.5">
                                    {loan.totalPayment != null && loan.loanAmount != null
                                      ? formatCurrency(loan.totalPayment - loan.loanAmount, loan.currency || 'RUB')
                                      : '—'}
                                  </div>
                                </div>
                                <div>
                                  <div className="text-slate-500 dark:text-slate-400 font-semibold">{t('baseRate')}</div>
                                  <div className="font-bold text-slate-900 dark:text-white mt-0.5">
                                    0.8% {t('perDay')}
                                  </div>
                                </div>
                                <div>
                                  <div className="text-slate-500 dark:text-slate-400 font-semibold">{t('calcCurrency')}</div>
                                  <div className="font-bold text-slate-900 dark:text-white mt-0.5">
                                    {loan.currency || 'RUB'} ({getCurrencySymbol(loan.currency || 'RUB')})
                                  </div>
                                </div>
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {/* ===== SCENARIOS TAB ===== */}
          {activeTab === 'scenarios' && (
            <div className="space-y-6 animate-in fade-in">
              {/* Header & Comparison Action Bar */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2">
                <div>
                  <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
                    <FolderBookmark size={20} className="text-indigo-500" />
                    {t('navScenarios')}
                  </h2>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                    {t('myScenariosDesc')}
                  </p>
                </div>

                <div className="flex items-center gap-3">
                  {selectedScenarioIds.length > 0 && (
                    <button
                      onClick={() => {
                        setSelectedScenarioIds([]);
                        setShowComparison(false);
                      }}
                      className="text-xs text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 underline cursor-pointer"
                    >
                      {t('profileClearSelection')}
                    </button>
                  )}

                  <button
                    onClick={handleRunComparison}
                    disabled={selectedScenarioIds.length < 2 || isComparing}
                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs font-bold rounded-xl shadow-sm transition-all flex items-center gap-1.5 cursor-pointer"
                  >
                    <GitCompare size={14} />
                    <span>
                      {selectedScenarioIds.length >= 2
                        ? `${t('profileCompareSelected')} (${selectedScenarioIds.length})`
                        : t('profileCompareBtn')}
                    </span>
                  </button>

                  <button
                    onClick={onNavigateToCalculator}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-xl shadow-sm transition-all cursor-pointer"
                  >
                    + {t('newCalcInCalculator')}
                  </button>
                </div>
              </div>

              {/* Selection banner hint if selecting */}
              {safeScenarios.length >= 2 && (
                <div className="p-3 bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-200 dark:border-indigo-800 rounded-2xl flex items-center justify-between text-xs text-indigo-900 dark:text-indigo-200">
                  <div className="flex items-center gap-2">
                    <Sparkles size={16} className="text-indigo-600 dark:text-indigo-400 shrink-0" />
                    <span>
                      {t('profileSelectedScenariosCount').replace('{count}', String(selectedScenarioIds.length))}
                    </span>
                  </div>
                  {onNavigateToCompare && (
                    <button
                      onClick={onNavigateToCompare}
                      className="font-bold underline text-indigo-700 dark:text-indigo-300 hover:text-indigo-900 cursor-pointer"
                    >
                      {t('profileOpenFullCompare')} &rarr;
                    </button>
                  )}
                </div>
              )}

              {/* Inline Comparison View if triggered */}
              {showComparison && comparisonResult && (
                <div className="bg-white dark:bg-slate-900 rounded-3xl border-2 border-indigo-500/40 p-6 shadow-md animate-in fade-in">
                  <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800 mb-4">
                    <div className="flex items-center gap-2">
                      <GitCompare size={20} className="text-indigo-600 dark:text-indigo-400" />
                      <h3 className="font-extrabold text-base text-slate-900 dark:text-white">
                        {t('compareResults')}
                      </h3>
                    </div>
                    <div className="flex items-center gap-3">
                      {onNavigateToCompare && (
                        <button
                          onClick={onNavigateToCompare}
                          className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:underline cursor-pointer"
                        >
                          {t('profileOpenFullCompare')}
                        </button>
                      )}
                      <button
                        onClick={() => setShowComparison(false)}
                        className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer"
                      >
                        <X size={18} />
                      </button>
                    </div>
                  </div>

                  <div className="overflow-x-auto">
                    <table className="w-full text-xs border-collapse">
                      <thead>
                        <tr className="border-b border-slate-200 dark:border-slate-800 text-left">
                          <th className="p-3 text-slate-500 font-semibold">{t('scenarioName')}</th>
                          {(comparisonResult.scenarios || []).map((sc) => (
                            <th key={sc.id} className="p-3 font-bold text-slate-900 dark:text-white">
                              {sc.title}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                        <tr>
                          <td className="p-3 text-slate-500 font-semibold">{t('monthlyPayment')}</td>
                          {(comparisonResult.scenarios || []).map((sc) => {
                            const isLowest = sc.id === comparisonResult.lowestMonthlyPaymentScenarioId;
                            return (
                              <td key={sc.id} className="p-3 font-bold text-slate-900 dark:text-white">
                                <span>{formatCurrency(sc.monthlyPayment, sc.currency || 'RUB')}</span>
                                {isLowest && (
                                  <span className="ml-2 text-[10px] font-bold px-1.5 py-0.5 rounded bg-emerald-100 dark:bg-emerald-950 text-emerald-800 dark:text-emerald-200">
                                    {t('minPaymentBadge')}
                                  </span>
                                )}
                              </td>
                            );
                          })}
                        </tr>
                        <tr>
                          <td className="p-3 text-slate-500 font-semibold">{t('overpaymentInterest')}</td>
                          {(comparisonResult.scenarios || []).map((sc) => {
                            const isLowest = sc.id === comparisonResult.lowestOverpaymentScenarioId;
                            return (
                              <td key={sc.id} className="p-3 font-bold text-amber-600 dark:text-amber-400">
                                <span>{formatCurrency(sc.totalInterest, sc.currency || 'RUB')}</span>
                                {isLowest && (
                                  <span className="ml-2 text-[10px] font-bold px-1.5 py-0.5 rounded bg-emerald-100 dark:bg-emerald-950 text-emerald-800 dark:text-emerald-200">
                                    {t('minOverpaymentBadge')}
                                  </span>
                                )}
                              </td>
                            );
                          })}
                        </tr>
                        <tr>
                          <td className="p-3 text-slate-500 font-semibold">{t('totalPaymentsSum')}</td>
                          {(comparisonResult.scenarios || []).map((sc) => (
                            <td key={sc.id} className="p-3 font-bold text-slate-900 dark:text-white">
                              {formatCurrency(sc.totalPayment, sc.currency || 'RUB')}
                            </td>
                          ))}
                        </tr>
                        <tr>
                          <td className="p-3 text-slate-500 font-semibold">{t('rate')} / {t('loanTerm')}</td>
                          {(comparisonResult.scenarios || []).map((sc) => (
                            <td key={sc.id} className="p-3 text-slate-700 dark:text-slate-300">
                              {sc.annualInterestRate ?? 0}% / {Math.floor((sc.termMonths ?? 0) / 12)} {t('years')}
                            </td>
                          ))}
                        </tr>
                        <tr>
                          <td className="p-3 text-slate-500 font-semibold">{t('paymentType')}</td>
                          {(comparisonResult.scenarios || []).map((sc) => (
                            <td key={sc.id} className="p-3 text-slate-700 dark:text-slate-300">
                              {sc.paymentType === 'ANNUITY' ? t('annuity') : t('differentiated')}
                            </td>
                          ))}
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* Scenarios Grid */}
              {safeScenarios.length === 0 ? (
                <div className="bg-white dark:bg-slate-900 rounded-3xl p-12 text-center border border-slate-200 dark:border-slate-800 shadow-xs">
                  <FolderBookmark size={36} className="mx-auto text-slate-400 mb-3" />
                  <h3 className="text-base font-bold text-slate-800 dark:text-white">{t('emptyScenariosTitle')}</h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mb-5 mt-1">{t('emptyScenariosDesc')}</p>
                  <button
                    onClick={onNavigateToCalculator}
                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-xl shadow-sm cursor-pointer"
                  >
                    {t('goToCalculator')}
                  </button>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {safeScenarios.map((sc) => {
                    const isSelected = selectedScenarioIds.includes(sc.id);

                    return (
                      <div
                        key={sc.id}
                        className={`bg-white dark:bg-slate-900 rounded-3xl p-6 border shadow-sm hover:shadow-md transition-all flex flex-col justify-between ${
                          isSelected
                            ? 'border-indigo-500 ring-2 ring-indigo-500/20'
                            : 'border-slate-200 dark:border-slate-800'
                        }`}
                      >
                        <div>
                          {/* Selection Checkbox & Header */}
                          <div className="flex items-start justify-between gap-3 mb-3">
                            <label className="flex items-center gap-2 cursor-pointer select-none">
                              <input
                                type="checkbox"
                                checked={isSelected}
                                onChange={() => toggleScenarioSelect(sc.id)}
                                className="w-4 h-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 cursor-pointer"
                              />
                              <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                                {sc.currency || 'RUB'} {getCurrencySymbol(sc.currency || 'RUB')}
                              </span>
                            </label>

                            <button
                              onClick={() => handleDelete(sc.id)}
                              className="p-1 text-slate-400 hover:text-red-500 rounded-lg hover:bg-red-50 dark:hover:bg-red-950/40 transition-colors shrink-0 cursor-pointer"
                              title={t('deleteScenarioTooltip')}
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>

                          <h3 className="font-extrabold text-slate-900 dark:text-white text-base line-clamp-1 mb-3">
                            {sc.title}
                          </h3>

                          {/* Numbers card */}
                          <div className="p-4 bg-slate-50 dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-800 mb-4 space-y-2">
                            <div className="flex justify-between items-baseline">
                              <span className="text-xs text-slate-500 dark:text-slate-400">{t('monthlyPayment')}:</span>
                              <span className="text-lg font-black text-slate-900 dark:text-white">
                                {formatCurrency(sc.monthlyPayment, sc.currency || 'RUB')}
                              </span>
                            </div>
                            <div className="flex justify-between items-baseline">
                              <span className="text-xs text-slate-500 dark:text-slate-400">{t('overpaymentInterest')}:</span>
                              <span className="text-sm font-bold text-amber-600 dark:text-amber-400">
                                {formatCurrency(sc.totalInterest, sc.currency || 'RUB')}
                              </span>
                            </div>
                          </div>

                          {/* Parameters breakdown */}
                          <div className="space-y-2 text-xs text-slate-600 dark:text-slate-300 pb-2">
                            <div className="flex justify-between">
                              <span className="text-slate-400 dark:text-slate-500">{t('propertyPrice')}:</span>
                              <span className="font-semibold text-slate-800 dark:text-slate-200">
                                {formatCurrency(sc.propertyPrice, sc.currency || 'RUB')}
                              </span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-slate-400 dark:text-slate-500">{t('loanAmountTitle')}:</span>
                              <span className="font-semibold text-slate-800 dark:text-slate-200">
                                {formatCurrency(sc.loanAmount, sc.currency || 'RUB')}
                              </span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-slate-400 dark:text-slate-500">{t('rate')} / {t('loanTerm')}:</span>
                              <span className="font-semibold text-slate-800 dark:text-slate-200">
                                {sc.annualInterestRate ?? 0}% / {Math.floor((sc.termMonths ?? 0) / 12)} {t('years')}
                              </span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-slate-400 dark:text-slate-500">{t('paymentType')}:</span>
                              <span className="font-semibold text-slate-800 dark:text-slate-200">
                                {sc.paymentType === 'ANNUITY' ? t('annuity') : t('differentiated')}
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* Action buttons */}
                        <div className="mt-5 space-y-2 pt-3 border-t border-slate-100 dark:border-slate-800">
                          <button
                            onClick={() => handleViewSchedule(sc)}
                            disabled={loadingSchedule}
                            className="w-full py-2 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-semibold text-xs rounded-xl transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
                          >
                            <Table size={14} />
                            <span>{t('paymentScheduleModal')}</span>
                          </button>
                          <button
                            onClick={() => handleExportPdf(sc.id)}
                            className="w-full py-2 bg-blue-50 dark:bg-blue-950/60 hover:bg-blue-100 dark:hover:bg-blue-900/60 text-blue-700 dark:text-blue-300 font-semibold text-xs rounded-xl transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
                          >
                            <FileDown size={14} />
                            <span>{t('exportPdfBtn')}</span>
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {/* ===== HISTORY TAB ===== */}
          {activeTab === 'history' && (
            <div className="space-y-4 animate-in fade-in">
              {/* History Filter Chips */}
              <div className="flex flex-wrap items-center gap-2">
                <button
                  onClick={() => setHistoryFilter('ALL')}
                  className={`px-3 py-1.5 text-xs font-bold rounded-xl transition-colors cursor-pointer ${
                    historyFilter === 'ALL'
                      ? 'bg-blue-600 text-white'
                      : 'bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400'
                  }`}
                >
                  {t('profileFilterAll')} ({safeActivity.length})
                </button>
                <button
                  onClick={() => setHistoryFilter('MICROLOAN_APPLICATION')}
                  className={`px-3 py-1.5 text-xs font-bold rounded-xl transition-colors cursor-pointer ${
                    historyFilter === 'MICROLOAN_APPLICATION'
                      ? 'bg-blue-600 text-white'
                      : 'bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400'
                  }`}
                >
                  {t('profileActionMicroloanApp')}
                </button>
                <button
                  onClick={() => setHistoryFilter('MORTGAGE_CALCULATION')}
                  className={`px-3 py-1.5 text-xs font-bold rounded-xl transition-colors cursor-pointer ${
                    historyFilter === 'MORTGAGE_CALCULATION'
                      ? 'bg-blue-600 text-white'
                      : 'bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400'
                  }`}
                >
                  {t('profileActionMortgage')}
                </button>
                <button
                  onClick={() => setHistoryFilter('MICROLOAN_CALCULATION')}
                  className={`px-3 py-1.5 text-xs font-bold rounded-xl transition-colors cursor-pointer ${
                    historyFilter === 'MICROLOAN_CALCULATION'
                      ? 'bg-blue-600 text-white'
                      : 'bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400'
                  }`}
                >
                  {t('profileActionMicroloanCalc')}
                </button>
                <button
                  onClick={() => setHistoryFilter('BANK_OFFER_SELECTED')}
                  className={`px-3 py-1.5 text-xs font-bold rounded-xl transition-colors cursor-pointer ${
                    historyFilter === 'BANK_OFFER_SELECTED'
                      ? 'bg-blue-600 text-white'
                      : 'bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400'
                  }`}
                >
                  {t('profileActionBankOffer')}
                </button>
              </div>

              {filteredHistory.length === 0 ? (
                <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-12 text-center shadow-xs">
                  <History size={36} className="mx-auto text-slate-400 mb-3" />
                  <h3 className="text-base font-bold text-slate-800 dark:text-white">{t('profileNoHistory')}</h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">{t('profileNoHistoryDesc')}</p>
                </div>
              ) : (
                <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-xs overflow-hidden">
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs border-collapse">
                      <thead>
                        <tr className="bg-slate-50 dark:bg-slate-950 text-slate-600 dark:text-slate-300 font-bold border-b border-slate-100 dark:border-slate-800">
                          <th className="p-3.5">{t('profileHistoryDate')}</th>
                          <th className="p-3.5">{t('profileHistoryAction')}</th>
                          <th className="p-3.5">{t('profileHistoryDetails')}</th>
                          <th className="p-3.5 text-right">{t('loanAmount')}</th>
                          <th className="p-3.5 text-right">{t('calcCurrency')}</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                        {filteredHistory.map((record) => {
                          let actionIcon: React.ReactNode;
                          let actionColor: string;
                          let actionLabel: string;

                          switch (record.actionType) {
                            case 'MORTGAGE_CALCULATION':
                              actionIcon = <Home size={14} />;
                              actionColor = 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/60';
                              actionLabel = t('profileActionMortgage');
                              break;
                            case 'MICROLOAN_CALCULATION':
                              actionIcon = <Zap size={14} />;
                              actionColor = 'text-purple-600 dark:text-purple-400 bg-purple-50 dark:bg-purple-950/60';
                              actionLabel = t('profileActionMicroloanCalc');
                              break;
                            case 'MICROLOAN_APPLICATION':
                              actionIcon = <CreditCard size={14} />;
                              actionColor = 'text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60';
                              actionLabel = t('profileActionMicroloanApp');
                              break;
                            case 'BANK_OFFER_SELECTED':
                              actionIcon = <GitCompare size={14} />;
                              actionColor = 'text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/60';
                              actionLabel = t('profileActionBankOffer');
                              break;
                            default:
                              actionIcon = <History size={14} />;
                              actionColor = 'text-slate-600 dark:text-slate-400 bg-slate-50 dark:bg-slate-800';
                              actionLabel = (record as any)?.actionType || 'Action';
                              break;
                          }

                          return (
                            <tr
                              key={record.id}
                              className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition-colors"
                            >
                              <td className="p-3.5 font-medium text-slate-700 dark:text-slate-300 whitespace-nowrap">
                                {formatDate(record.timestamp)}
                              </td>
                              <td className="p-3.5">
                                <span className={`inline-flex items-center gap-1.5 text-[11px] font-bold px-2 py-1 rounded-lg ${actionColor}`}>
                                  {actionIcon}
                                  {actionLabel}
                                </span>
                              </td>
                              <td className="p-3.5 text-slate-600 dark:text-slate-400 max-w-xs truncate">
                                {record.loanPurpose
                                  ? record.loanPurpose
                                  : record.bankName
                                  ? `${record.bankName}${record.offerName ? ` — ${record.offerName}` : ''}`
                                  : record.termMonths
                                  ? `${record.termMonths} ${t('months')}`
                                  : record.termDays
                                  ? `${record.termDays} ${t('daysUnit')}`
                                  : '—'}
                              </td>
                              <td className="p-3.5 text-right font-bold text-slate-900 dark:text-white whitespace-nowrap">
                                {record.loanAmount != null
                                  ? formatCurrency(record.loanAmount, record.currency || 'RUB')
                                  : '—'}
                              </td>
                              <td className="p-3.5 text-right text-slate-500 dark:text-slate-400 whitespace-nowrap">
                                {record.currency || 'RUB'}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          )}
        </>
      )}

      {/* Repay Loan Confirmation Modal */}
      {repayingLoan && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl w-full max-w-md p-6 border border-slate-100 dark:border-slate-800">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800 mb-4">
              <div className="flex items-center gap-2">
                <CheckCircle2 size={20} className="text-amber-500" />
                <h3 className="font-extrabold text-base text-slate-900 dark:text-white">
                  {t('profileRepayModalTitle')}
                </h3>
              </div>
              <button
                onClick={() => setRepayingLoan(null)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-lg"
              >
                <X size={18} />
              </button>
            </div>

            <p className="text-xs text-slate-600 dark:text-slate-400 mb-5 leading-relaxed">
              {t('profileRepayModalDesc')}
            </p>

            <div className="p-4 bg-slate-50 dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-800 mb-6 space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="text-slate-500">{t('loanAmount')}:</span>
                <span className="font-bold text-slate-900 dark:text-white">
                  {formatCurrency(repayingLoan.loanAmount, repayingLoan.currency || 'RUB')}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">{t('totalInterest')}:</span>
                <span className="font-bold text-amber-600">
                  {repayingLoan.totalPayment != null && repayingLoan.loanAmount != null
                    ? formatCurrency(repayingLoan.totalPayment - repayingLoan.loanAmount, repayingLoan.currency || 'RUB')
                    : '—'}
                </span>
              </div>
              <div className="flex justify-between pt-2 border-t border-slate-200 dark:border-slate-800 font-extrabold text-sm">
                <span className="text-slate-900 dark:text-white">{t('totalRepayment')}:</span>
                <span className="text-amber-600 dark:text-amber-400">
                  {formatCurrency(repayingLoan.totalPayment, repayingLoan.currency || 'RUB')}
                </span>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3">
              <button
                onClick={() => setRepayingLoan(null)}
                className="px-4 py-2 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-semibold text-xs rounded-xl cursor-pointer"
              >
                {t('cancel')}
              </button>
              <button
                onClick={() => handleConfirmRepay(repayingLoan)}
                className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-sm cursor-pointer"
              >
                {t('profileRepayConfirm')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Microloan Daily Schedule Modal */}
      {microloanScheduleModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl w-full max-w-3xl max-h-[85vh] flex flex-col overflow-hidden border border-slate-100 dark:border-slate-800">
            <div className="flex items-center justify-between p-6 border-b border-slate-100 dark:border-slate-800">
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <Calendar size={18} className="text-amber-500" />
                  {t('profileDailyScheduleModal')}
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  {microloanScheduleModal.loan.applicationId ?? 'MS-…'} &bull; {microloanScheduleModal.loan.loanPurpose ?? t('basicNeedsType')}
                </p>
              </div>
              <button
                onClick={() => setMicroloanScheduleModal(null)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
              >
                <X size={20} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6">
              <div className="overflow-x-auto rounded-xl border border-slate-100 dark:border-slate-800">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-slate-50 dark:bg-slate-950 text-slate-600 dark:text-slate-300 font-bold border-b border-slate-100 dark:border-slate-800 sticky top-0">
                      <th className="p-3 text-center">{t('tableDay')}</th>
                      <th className="p-3">{t('tableDate')}</th>
                      <th className="p-3 text-right">{t('tablePrincipal')}</th>
                      <th className="p-3 text-right">{t('tableDailyInterest')}</th>
                      <th className="p-3 text-right">{t('tableAccumulated')}</th>
                      <th className="p-3 text-right">{t('tableTotalDue')}</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {microloanScheduleModal.schedule.map((row) => (
                      <tr key={row.dayNumber} className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition-colors">
                        <td className="p-2.5 text-center font-medium text-slate-400">{row.dayNumber}</td>
                        <td className="p-2.5 font-medium text-slate-700 dark:text-slate-300 whitespace-nowrap">
                          {new Date(row.date).toLocaleDateString(language === 'en' ? 'en-US' : 'ru-RU', {
                            day: 'numeric',
                            month: 'short',
                          })}
                        </td>
                        <td className="p-2.5 text-right font-medium text-slate-600 dark:text-slate-400 whitespace-nowrap">
                          {formatCurrency(row.principalBalance, microloanScheduleModal.loan.currency || 'RUB')}
                        </td>
                        <td className="p-2.5 text-right text-amber-600 dark:text-amber-400 font-medium whitespace-nowrap">
                          +{formatCurrency(row.dailyInterest, microloanScheduleModal.loan.currency || 'RUB')}
                        </td>
                        <td className="p-2.5 text-right text-amber-700 dark:text-amber-300 font-semibold whitespace-nowrap">
                          {formatCurrency(row.accumulatedInterest, microloanScheduleModal.loan.currency || 'RUB')}
                        </td>
                        <td className="p-2.5 text-right font-bold text-blue-600 dark:text-blue-400 whitespace-nowrap">
                          {formatCurrency(row.totalDue, microloanScheduleModal.loan.currency || 'RUB')}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="p-4 border-t border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 flex justify-end">
              <button
                onClick={() => setMicroloanScheduleModal(null)}
                className="px-5 py-2 bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-semibold text-xs rounded-xl transition-colors cursor-pointer"
              >
                {t('closeModal')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Mortgage Schedule Viewer Modal */}
      {selectedSchedule && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl w-full max-w-4xl max-h-[85vh] flex flex-col overflow-hidden border border-slate-100 dark:border-slate-800">
            <div className="flex items-center justify-between p-6 border-b border-slate-100 dark:border-slate-800">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-lg font-bold text-slate-900 dark:text-white">{t('paymentScheduleModal')}</h3>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                    {selectedSchedule.currency || 'RUB'} {getCurrencySymbol(selectedSchedule.currency || 'RUB')}
                  </span>
                </div>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">{selectedSchedule.title}</p>
              </div>
              <button
                onClick={() => setSelectedSchedule(null)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
              >
                <X size={20} />
              </button>
            </div>
            <div className="flex-1 overflow-y-auto p-6">
              <div className="overflow-x-auto rounded-xl border border-slate-100 dark:border-slate-800">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-slate-50 dark:bg-slate-950 text-slate-600 dark:text-slate-300 font-bold border-b border-slate-100 dark:border-slate-800 sticky top-0">
                      <th className="p-3 text-center">{t('tableMonth')}</th>
                      <th className="p-3">{t('tableDate')}</th>
                      <th className="p-3 text-right">{t('tableBalance')}</th>
                      <th className="p-3 text-right">{t('tablePrincipalPart')}</th>
                      <th className="p-3 text-right">{t('tableInterestPart')}</th>
                      <th className="p-3 text-right">{t('tableEarly')}</th>
                      <th className="p-3 text-right">{t('tableTotalMonthly')}</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {selectedSchedule.items.map((row) => (
                      <tr key={row.monthNumber} className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition-colors">
                        <td className="p-2.5 text-center font-medium text-slate-400">{row.monthNumber}</td>
                        <td className="p-2.5 font-medium text-slate-700 dark:text-slate-300 whitespace-nowrap">
                          {new Date(row.paymentDate).toLocaleDateString(language === 'en' ? 'en-US' : 'ru-RU', {
                            month: 'short',
                            year: 'numeric',
                          })}
                        </td>
                        <td className="p-2.5 text-right font-medium text-slate-600 dark:text-slate-400 whitespace-nowrap">
                          {formatCurrency(row.openingBalance, selectedSchedule.currency || 'RUB')}
                        </td>
                        <td className="p-2.5 text-right font-semibold text-slate-800 dark:text-slate-200 whitespace-nowrap">
                          {formatCurrency(row.principalPayment, selectedSchedule.currency || 'RUB')}
                        </td>
                        <td className="p-2.5 text-right text-amber-600 dark:text-amber-400 font-medium whitespace-nowrap">
                          {formatCurrency(row.interestPayment, selectedSchedule.currency || 'RUB')}
                        </td>
                        <td className="p-2.5 text-right text-emerald-600 dark:text-emerald-400 font-semibold whitespace-nowrap">
                          {row.earlyPayment > 0 ? formatCurrency(row.earlyPayment, selectedSchedule.currency || 'RUB') : '—'}
                        </td>
                        <td className="p-2.5 text-right font-bold text-blue-600 dark:text-blue-400 whitespace-nowrap">
                          {formatCurrency(row.totalMonthlyPayment, selectedSchedule.currency || 'RUB')}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
            <div className="p-4 border-t border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-950 flex justify-end">
              <button
                onClick={() => setSelectedSchedule(null)}
                className="px-5 py-2 bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-semibold text-xs rounded-xl transition-colors cursor-pointer"
              >
                {t('closeModal')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Passport Create / Edit */}
      {passportModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 backdrop-blur-sm p-4">
          <div className="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-md p-6 border border-slate-200 dark:border-slate-800 shadow-2xl animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <ShieldCheck size={20} className="text-blue-600" />
                <h3 className="text-base font-extrabold text-slate-900 dark:text-white">
                  {passportData ? t('passportEditBtn') : t('passportAddBtn')}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setPassportModalOpen(false)}
                className="p-1.5 rounded-xl text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer"
              >
                <X size={18} />
              </button>
            </div>
            <p className="text-xs text-slate-500 mb-4">{t('passportMaskedNotice')}</p>
            {passportError && (
              <div className="p-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900/40 text-rose-600 dark:text-rose-400 text-xs font-semibold mb-3 animate-in fade-in">
                {passportError}
              </div>
            )}
            <form onSubmit={handleSavePassport} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                  {t('fullName')} *
                </label>
                <input
                  required
                  placeholder={t('placeholderFullName')}
                  value={passportForm.fullName}
                  onChange={(e) => setPassportForm({ ...passportForm, fullName: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 uppercase"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('passportSeriesNumber')} *
                  </label>
                  <input
                    required
                    maxLength={11}
                    placeholder="4515 123456"
                    value={passportForm.seriesNumber}
                    onChange={(e) => setPassportForm({ ...passportForm, seriesNumber: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 font-mono"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('issueDate')}
                  </label>
                  <input
                    type="date"
                    value={passportForm.issueDate || ''}
                    onChange={(e) => setPassportForm({ ...passportForm, issueDate: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('departmentCode')}
                  </label>
                  <input
                    maxLength={7}
                    placeholder="770-001"
                    value={passportForm.departmentCode || ''}
                    onChange={(e) => setPassportForm({ ...passportForm, departmentCode: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 font-mono"
                  />
                </div>
                <div className="col-span-2">
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('issuedBy')}
                  </label>
                  <input
                    placeholder={t('placeholderIssuedBy')}
                    value={passportForm.issuedBy || ''}
                    onChange={(e) => setPassportForm({ ...passportForm, issuedBy: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              {/* Questionnaire details (age, gender, country, city) */}
              <div className="pt-2 border-t border-slate-200/60 dark:border-slate-800/60">
                <div className="text-[11px] font-bold text-slate-400 dark:text-slate-500 uppercase tracking-wider mb-2">
                  {language === 'ru' ? 'Анкета заемщика' : 'Borrower Questionnaire'}
                </div>
                <div className="grid grid-cols-2 gap-3 mb-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1 flex items-center gap-1">
                      <Calendar size={12} className="text-blue-500" />
                      {language === 'ru' ? 'Возраст' : 'Age'}
                    </label>
                    <input
                      type="number"
                      min={18}
                      max={120}
                      placeholder="25"
                      value={passportForm.loanApplicationRequest?.age ?? ''}
                      onChange={(e) =>
                        setPassportForm({
                          ...passportForm,
                          loanApplicationRequest: {
                            age: e.target.value ? Number(e.target.value) : 18,
                            country: passportForm.loanApplicationRequest?.country || '',
                            city: passportForm.loanApplicationRequest?.city || '',
                            gender: passportForm.loanApplicationRequest?.gender || 'Мужской',
                          },
                        })
                      }
                      className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1 flex items-center gap-1">
                      <Users size={12} className="text-blue-500" />
                      {language === 'ru' ? 'Пол' : 'Gender'}
                    </label>
                    <select
                      value={passportForm.loanApplicationRequest?.gender || 'Мужской'}
                      onChange={(e) =>
                        setPassportForm({
                          ...passportForm,
                          loanApplicationRequest: {
                            age: passportForm.loanApplicationRequest?.age || 18,
                            country: passportForm.loanApplicationRequest?.country || '',
                            city: passportForm.loanApplicationRequest?.city || '',
                            gender: e.target.value,
                          },
                        })
                      }
                      className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                    >
                      <option value="Мужской">{language === 'ru' ? 'Мужской' : 'Male'}</option>
                      <option value="Женский">{language === 'ru' ? 'Женский' : 'Female'}</option>
                      <option value="Другой">{language === 'ru' ? 'Другой' : 'Other'}</option>
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1 flex items-center gap-1">
                      <Globe size={12} className="text-blue-500" />
                      {language === 'ru' ? 'Страна' : 'Country'}
                    </label>
                    <input
                      placeholder={language === 'ru' ? 'Россия' : 'Country'}
                      value={passportForm.loanApplicationRequest?.country || ''}
                      onChange={(e) =>
                        setPassportForm({
                          ...passportForm,
                          loanApplicationRequest: {
                            age: passportForm.loanApplicationRequest?.age || 18,
                            country: e.target.value,
                            city: passportForm.loanApplicationRequest?.city || '',
                            gender: passportForm.loanApplicationRequest?.gender || 'Мужской',
                          },
                        })
                      }
                      className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1 flex items-center gap-1">
                      <MapPin size={12} className="text-blue-500" />
                      {language === 'ru' ? 'Город' : 'City'}
                    </label>
                    <input
                      placeholder={language === 'ru' ? 'Москва' : 'City'}
                      value={passportForm.loanApplicationRequest?.city || ''}
                      onChange={(e) =>
                        setPassportForm({
                          ...passportForm,
                          loanApplicationRequest: {
                            age: passportForm.loanApplicationRequest?.age || 18,
                            country: passportForm.loanApplicationRequest?.country || '',
                            city: e.target.value,
                            gender: passportForm.loanApplicationRequest?.gender || 'Мужской',
                          },
                        })
                      }
                      className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                </div>
              </div>
              <div className="flex gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setPassportModalOpen(false)}
                  className="flex-1 py-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 text-xs font-bold cursor-pointer"
                >
                  {t('cancel')}
                </button>
                <button
                  type="submit"
                  disabled={savingPassport}
                  className="flex-1 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold cursor-pointer disabled:opacity-60 shadow-md shadow-blue-600/20"
                >
                  {savingPassport ? t('saving') : t('save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
