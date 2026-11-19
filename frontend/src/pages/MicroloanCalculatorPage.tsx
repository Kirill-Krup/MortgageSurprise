import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { calculatorApi, microloanRateApi, passportApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import type { TranslationKey } from '../i18n/translations';
import type {
  Currency,
  MicroloanCalculationRequest,
  MicroloanCalculationResponse,
  MicroloanConfig,
  MicroloanRateDTO,
  MicroloanType,
  PassportData,
} from '../types';
import {
  Banknote,
  Calendar,
  Clock,
  ShieldCheck,
  Coins,
  ChevronDown,
  ChevronRight,
  Sparkles,
  HeartHandshake,
  CheckCircle2,
  AlertCircle,
  FileText,
  UserCheck,
  X,
  CreditCard,
  Percent,
  TrendingDown,
} from 'lucide-react';
import { CURRENCY_CONFIGS, formatCurrency, getCurrencySymbol } from '../utils/format';
import { useExperiment } from '../hooks/useExperiment';
import { BUTTON_COLOR_VARIANTS, getMicroloanApplyButtonClasses } from '../utils/abTesting';

interface MicroloanCalculatorPageProps {
  onOpenAuth: () => void;
  onNavigateToProfile?: () => void;
}

const QUICK_AMOUNTS = [5_000, 15_000, 30_000, 50_000, 100_000];
const QUICK_TERMS = [7, 10, 14, 21, 30];

interface PurposeOption {
  id: string;
  translationKey: TranslationKey;
  category: 'BASIC' | 'VANITY' | 'CUSTOM';
}

const PURPOSE_OPTIONS: PurposeOption[] = [
  // Basic needs
  { id: 'groceries', translationKey: 'purposeGroceries', category: 'BASIC' },
  { id: 'repair', translationKey: 'purposeRepair', category: 'BASIC' },
  { id: 'medical', translationKey: 'purposeMedical', category: 'BASIC' },
  { id: 'utilities', translationKey: 'purposeUtilities', category: 'BASIC' },
  { id: 'family', translationKey: 'purposeFamily', category: 'BASIC' },
  { id: 'clothing', translationKey: 'purposeClothing', category: 'BASIC' },
  // Luxury / Vanity ("на понты")
  { id: 'iphone', translationKey: 'purposeIphone', category: 'VANITY' },
  { id: 'tuning', translationKey: 'purposeTuning', category: 'VANITY' },
  { id: 'luxury_wear', translationKey: 'purposeLuxuryWear', category: 'VANITY' },
  { id: 'parties', translationKey: 'purposeParties', category: 'VANITY' },
  { id: 'vacation', translationKey: 'purposeVacation', category: 'VANITY' },
  // Custom
  { id: 'custom', translationKey: 'purposeCustom', category: 'CUSTOM' },
];

const VANITY_KEYWORDS = [
  'айфон',
  'iphone',
  'apple',
  'машин',
  'авто',
  'тюнинг',
  'tuning',
  'диск',
  'клуб',
  'вечеринк',
  'бар',
  'ресторан',
  'бренд',
  'часы',
  'gucci',
  'прада',
  'prada',
  'louis',
  'луи',
  'понты',
  'статус',
  'luxur',
  'премиум',
  'золот',
  'ювелир',
  'украшен',
  'vip',
  'вип',
  'отдых',
  'развлечен',
  'гаджет',
];

export const MicroloanCalculatorPage: React.FC<MicroloanCalculatorPageProps> = ({
  onOpenAuth,
  onNavigateToProfile,
}) => {
  const { user } = useAuth();
  const { t, language } = useLanguage();
  const isAdmin = user?.role === 'ROLE_ADMIN';

  // AB test button color for Microloan Application (persisted permanently in DB and localStorage)
  const applyBtnColorVariant = useExperiment('apply_microloan_button', BUTTON_COLOR_VARIANTS);
  const applyBtnClasses = getMicroloanApplyButtonClasses(applyBtnColorVariant);

  // Bank config (admin-controlled rates)
  const [config, setConfig] = useState<MicroloanConfig | null>(null);
  const [exchangeRates, setExchangeRates] = useState<Record<string, number>>({});

  // Tariffs loaded from DB (Requirement 3 & 4)
  const [rates, setRates] = useState<MicroloanRateDTO[]>([]);
  const [selectedRate, setSelectedRate] = useState<MicroloanRateDTO | null>(null);
  const [isTariffDropdownOpen, setIsTariffDropdownOpen] = useState<boolean>(false);

  // Form State
  const [currency, setCurrency] = useState<Currency>('RUB');
  const [amount, setAmount] = useState<number>(15_000);
  const [termDays, setTermDays] = useState<number>(14);
  const [selectedPurposeId, setSelectedPurposeId] = useState<string>('groceries');
  const [customPurposeText, setCustomPurposeText] = useState<string>('');
  const [startDate, setStartDate] = useState<string>(() => {
    return new Date().toISOString().split('T')[0];
  });

  const [loading, setLoading] = useState<boolean>(false);
  const [result, setResult] = useState<MicroloanCalculationResponse | null>(null);

  // Passport modal state (Mock identity form)
  const [isPassportModalOpen, setIsPassportModalOpen] = useState<boolean>(false);
  const [passportData, setPassportData] = useState({
    fullName: '',
    seriesNumber: '',
    issueDate: '',
    departmentCode: '',
    issuedBy: '',
  });
  const [applicationSuccess, setApplicationSuccess] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  // User saved passport profile state (AES-256 backed)
  const [savedPassport, setSavedPassport] = useState<PassportData | null>(null);
  const [saveToProfile, setSaveToProfile] = useState<boolean>(true);

  // Fetch user passport from profile if logged in
  useEffect(() => {
    if (!user) {
      setSavedPassport(null);
      return;
    }
    let isMounted = true;
    passportApi
      .get()
      .then((data) => {
        if (!isMounted) return;
        setSavedPassport(data);
        if (data) {
          setPassportData({
            fullName: data.fullName || '',
            seriesNumber: data.seriesNumber || '',
            issueDate: data.issueDate || '',
            departmentCode: data.departmentCode || '',
            issuedBy: data.issuedBy || '',
          });
        }
      })
      .catch((err) => {
        console.warn('Could not load profile passport:', err);
      });
    return () => {
      isMounted = false;
    };
  }, [user]);

  // Fetch admin-configured rates, tariffs, and exchange rates on mount
  useEffect(() => {
    let isMounted = true;
    const loadData = async () => {
      try {
        const [cfg, rateList, liveRates] = await Promise.all([
          calculatorApi.getMicroloanConfig().catch(() => null),
          microloanRateApi.getRates().catch(() => []),
          calculatorApi.getExchangeRates().catch(() => ({})),
        ]);
        if (!isMounted) return;
        if (cfg) {
          setConfig(cfg);
        }
        if (liveRates && Object.keys(liveRates).length > 0) {
          setExchangeRates(liveRates);
        }
        if (Array.isArray(rateList) && rateList.length > 0) {
          setRates(rateList);
          setSelectedRate(rateList[0]);
        } else {
          const fallbackRate: MicroloanRateDTO = {
            id: 1,
            name: 'Стандарт',
            baseDailyRate: 0.8,
            minDayRequired: 7,
            maxDayRequired: 30,
          };
          setRates([fallbackRate]);
          setSelectedRate(fallbackRate);
        }
      } catch (err) {
        console.warn('Could not fetch microloan config/rates/exchangeRates:', err);
      }
    };
    loadData();
    return () => {
      isMounted = false;
    };
  }, []);

  const baseRate = selectedRate?.baseDailyRate ?? config?.baseDailyRate ?? 0.8;
  // Fixed surcharge amount for Luxury Maximum configured by admin in RUB, adapted to selected currency
  const rawBankInterest = config?.bankInterest ?? 2000;
  const fixedVanitySurcharge = useMemo(() => {
    if (currency === 'RUB' || !exchangeRates[currency] || exchangeRates[currency] <= 0) {
      return rawBankInterest;
    }
    return Math.round((rawBankInterest / exchangeRates[currency]) * 100) / 100;
  }, [rawBankInterest, currency, exchangeRates]);

  // Determine effective purpose text
  const currentPurpose = useMemo(() => {
    if (selectedPurposeId === 'custom') {
      return customPurposeText.trim() || t('purposeCustom');
    }
    const found = PURPOSE_OPTIONS.find((o) => o.id === selectedPurposeId);
    return found ? t(found.translationKey) : t('purposeGroceries');
  }, [selectedPurposeId, customPurposeText, t]);

  // Automated category classification: user cannot manually choose, it is derived from purpose (Requirement 4)
  const detectedType: MicroloanType = useMemo(() => {
    if (selectedPurposeId === 'custom') {
      const lower = customPurposeText.toLowerCase().trim();
      return VANITY_KEYWORDS.some((kw) => lower.includes(kw)) ? 'VANITY' : 'BASIC_NEEDS';
    }
    const found = PURPOSE_OPTIONS.find((o) => o.id === selectedPurposeId);
    return found?.category === 'VANITY' ? 'VANITY' : 'BASIC_NEEDS';
  }, [selectedPurposeId, customPurposeText]);

  const calculateLoan = useCallback(async () => {
    setLoading(true);

    const safeAmount = Math.max(1000, Math.min(100000, amount || 15000));
    const minDays = selectedRate?.minDayRequired ?? 7;
    const maxDays = selectedRate?.maxDayRequired ?? 30;
    const safeTerm = Math.max(minDays, Math.min(maxDays, termDays || 14));

    try {
      const req: MicroloanCalculationRequest = {
        amount: safeAmount,
        termDays: safeTerm,
        rateName: selectedRate?.name || 'Стандарт',
        type: detectedType,
        purpose: currentPurpose,
        startDate,
        currency,
      };
      const res = await calculatorApi.calculateMicroloan(req);
      setResult(res);
    } catch {
      // Robust client-side fallback matching backend exact fixed surcharge formula (Requirement 5)
      const dailyInterest = Math.round((safeAmount * (baseRate / 100)) * 100) / 100;
      const baseTotalInterest = Math.round((dailyInterest * safeTerm) * 100) / 100;

      // Fixed surcharge applied flat (monetary amount, NOT a percentage)
      const surchargeAmount = detectedType === 'VANITY' ? fixedVanitySurcharge : 0;

      const totalInterest = baseTotalInterest + surchargeAmount;
      const totalRepayment = safeAmount + totalInterest;

      const start = new Date(startDate || new Date().toISOString().split('T')[0]);
      const due = new Date(start);
      due.setDate(due.getDate() + safeTerm);

      const dailySchedule = [];
      let accumulatedInterest = 0;
      for (let d = 1; d <= safeTerm; d++) {
        accumulatedInterest = Math.round((accumulatedInterest + dailyInterest) * 100) / 100;
        const dayDate = new Date(start);
        dayDate.setDate(dayDate.getDate() + d);
        dailySchedule.push({
          dayNumber: d,
          date: dayDate.toISOString().split('T')[0],
          principalBalance: safeAmount,
          dailyInterest,
          accumulatedInterest,
          totalDue: safeAmount + accumulatedInterest,
        });
      }

      setResult({
        scheduleId: 'local-calc',
        loanAmount: safeAmount,
        termDays: safeTerm,
        microloanType: detectedType,
        purpose: currentPurpose,
        baseDailyRate: baseRate,
        vanitySurchargeRate: 0,
        vanitySurchargeAmount: surchargeAmount,
        effectiveDailyRate: baseRate,
        annualEquivalentRate: Math.round(baseRate * 365 * 100) / 100,
        totalInterest,
        totalRepayment,
        startDate,
        dueDate: due.toISOString().split('T')[0],
        currency,
        dailySchedule,
      });
    } finally {
      setLoading(false);
    }
  }, [amount, termDays, detectedType, currentPurpose, startDate, currency, baseRate, fixedVanitySurcharge, selectedRate]);

  useEffect(() => {
    calculateLoan();
  }, [calculateLoan]);

  // Click on "Оформить онлайн-заявку"
  const handleStartApplication = () => {
    if (!user) {
      onOpenAuth();
      return;
    }
    if (isAdmin) {
      alert(t('adminActionBlocked'));
      return;
    }

    // Auto-fill from profile if already filled, or keep draft
    if (savedPassport) {
      setPassportData({
        fullName: savedPassport.fullName || '',
        seriesNumber: savedPassport.seriesNumber || '',
        issueDate: savedPassport.issueDate || '',
        departmentCode: savedPassport.departmentCode || '',
        issuedBy: savedPassport.issuedBy || '',
      });
      setSaveToProfile(false);
    } else {
      if (!passportData.fullName && user.email) {
        const emailPrefix = user.email.split('@')[0];
        setPassportData((prev) => ({
          ...prev,
          fullName: emailPrefix.replace(/[._]/g, ' ').toUpperCase(),
        }));
      }
      setSaveToProfile(true);
    }
    setFormError(null);
    setIsPassportModalOpen(true);
  };

  // Submit Mock Passport Form
  const handleSubmitPassport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!passportData.fullName.trim() || !passportData.seriesNumber.trim()) {
      setFormError(t('passportFormFillError'));
      return;
    }

    // Optionally persist passport data to user profile
    if (user && !isAdmin && saveToProfile) {
      try {
        const saved = await passportApi.save({
          fullName: passportData.fullName.trim(),
          seriesNumber: passportData.seriesNumber.trim(),
          issueDate: passportData.issueDate || undefined,
          departmentCode: passportData.departmentCode || undefined,
          issuedBy: passportData.issuedBy || undefined,
        });
        setSavedPassport(saved);
      } catch (err) {
        console.warn('Could not save passport data to profile:', err);
      }
    }

    const randomId = `MS-${Math.floor(10000 + Math.random() * 90000)}`;
    setIsPassportModalOpen(false);
    setApplicationSuccess(randomId);

    if (user && !isAdmin) {
      const safeAmount = Math.max(1000, Math.min(100000, amount || 15000));
      const safeTerm = Math.max(7, Math.min(30, termDays || 14));
      const totalRepay = result?.totalRepayment ?? safeAmount;
      try {
        await calculatorApi.applyMicroloan({
          amount: safeAmount,
          termDays: safeTerm,
          purpose: currentPurpose,
          microloanType: detectedType,
          totalRepayment: totalRepay,
          scheduleId: result?.scheduleId,
          applicationId: randomId,
          applicantFullName: passportData.fullName,
          currency,
        });
      } catch (err) {
        console.warn('Microloan application audit error:', err);
      }
    }

    setTimeout(() => {
      setApplicationSuccess(null);
    }, 9000);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-in fade-in duration-300">
      {/* Header section */}
      <div className="mb-6">
        <div className="relative inline-block text-left mb-3">
          <button
            type="button"
            onClick={() => setIsTariffDropdownOpen((prev) => !prev)}
            className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-semibold bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800 hover:bg-blue-100 dark:hover:bg-blue-900/60 transition-all cursor-pointer shadow-xs"
          >
            <Sparkles size={14} className="text-blue-500" />
            <span>Микрозайм: {selectedRate ? `${selectedRate.name} (${selectedRate.baseDailyRate}%/дн.)` : t('selectTariff')}</span>
            <ChevronDown size={14} className={`transition-transform duration-200 ${isTariffDropdownOpen ? 'rotate-180' : ''}`} />
          </button>

          {isTariffDropdownOpen && (
            <div className="absolute left-0 mt-2 w-72 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-xl z-30 p-2 space-y-1 animate-in fade-in">
              <div className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                {t('tariffDropdownLabel')} ({rates.length})
              </div>
              {rates.map((rate) => {
                const isSelected = selectedRate?.name === rate.name;
                return (
                  <button
                    key={rate.id ?? rate.name}
                    type="button"
                    onClick={() => {
                      setSelectedRate(rate);
                      setIsTariffDropdownOpen(false);
                    }}
                    className={`w-full text-left px-3 py-2.5 rounded-xl text-xs flex items-center justify-between transition-colors cursor-pointer ${
                      isSelected
                        ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 font-bold'
                        : 'text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                    }`}
                  >
                    <div>
                      <div className="font-extrabold">{rate.name}</div>
                      <div className="text-[11px] opacity-75">
                        {rate.minDayRequired ?? 7}–{rate.maxDayRequired ?? 30} {t('daysUnit')}
                      </div>
                    </div>
                    <span className="px-2 py-0.5 rounded-md bg-emerald-100 dark:bg-emerald-950 text-emerald-800 dark:text-emerald-300 text-[11px] font-black">
                      {rate.baseDailyRate}%/{t('perDay')}
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </div>
        <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight">
          {t('microloanTitle')}
        </h1>
        <p className="mt-2 text-sm sm:text-base text-slate-600 dark:text-slate-400 max-w-3xl">
          {t('microloanSubtitle')}
        </p>
      </div>

      {/* Admin Warning Banner if role is ADMIN */}
      {isAdmin && (
        <div className="mb-6 p-4 bg-amber-50 dark:bg-amber-950/40 border border-amber-300 dark:border-amber-800 rounded-2xl flex items-start gap-3 text-amber-900 dark:text-amber-200 text-sm animate-in fade-in">
          <ShieldCheck size={20} className="text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
          <div>
            <div className="font-bold">{t('adminConfigModeAlert')}</div>
          </div>
        </div>
      )}

      {/* Success Notification after application */}
      {applicationSuccess && (
        <div className="mb-6 p-5 bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-300 dark:border-emerald-800 rounded-3xl flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-emerald-950 dark:text-emerald-200 shadow-md animate-in fade-in">
          <div className="flex items-start gap-3.5">
            <CheckCircle2 size={24} className="text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
            <div>
              <h3 className="font-extrabold text-base text-emerald-900 dark:text-emerald-100">
                {t('applicationSuccessTitle')}
              </h3>
              <p className="text-xs sm:text-sm text-emerald-800 dark:text-emerald-300 mt-1">
                {t('applicationSuccessDesc')}
                <span className="font-mono font-bold ml-1">{applicationSuccess}</span>. {t('passportVerifiedNotice')}
              </p>
            </div>
          </div>
          {onNavigateToProfile && user && !isAdmin && (
            <button
              type="button"
              onClick={onNavigateToProfile}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs rounded-xl shadow-sm transition-all shrink-0 cursor-pointer flex items-center gap-1.5 self-start sm:self-auto"
            >
              <span>{t('profileGoToProfileToTrack')}</span>
              <ChevronRight size={14} />
            </button>
          )}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left column: Controls and inputs */}
        <div className="lg:col-span-7 space-y-6">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 sm:p-7 shadow-xs transition-colors">
            {/* Currency selector */}
            <div className="flex items-center justify-between pb-5 mb-5 border-b border-slate-100 dark:border-slate-800">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
                <Coins size={15} />
                {t('calcCurrency')}
              </span>
              <div className="flex bg-slate-100 dark:bg-slate-950 p-1 rounded-xl border border-transparent dark:border-slate-800">
                {CURRENCY_CONFIGS.map((c) => (
                  <button
                    key={c.code}
                    onClick={() => setCurrency(c.code)}
                    className={`px-3 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                      currency === c.code
                        ? 'bg-white dark:bg-slate-800 text-blue-700 dark:text-blue-400 shadow-xs'
                        : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                    }`}
                  >
                    {c.symbol} {c.code}
                  </button>
                ))}
              </div>
            </div>

            {Object.keys(exchangeRates).length > 0 && (
              <div className="mb-5 px-3 py-2 bg-blue-50/60 dark:bg-blue-950/30 rounded-xl border border-blue-100 dark:border-blue-900/50 flex items-center gap-2 text-[11px] text-blue-700 dark:text-blue-300 font-medium">
                <TrendingDown size={13} className="shrink-0 opacity-70" />
                <span>
                  {t('liveRates') || 'Курсы'}:{' '}
                  {CURRENCY_CONFIGS.filter(c => c.code !== 'RUB' && exchangeRates[c.code])
                    .map(c => `1 ${c.symbol} = ${exchangeRates[c.code]?.toFixed(2) || '—'} ₽`)
                    .join(' · ')}
                </span>
              </div>
            )}

            {/* Tariff Selection (Requirement 3 & 4) */}
            <div className="mb-6 space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                  <Coins size={17} className="text-blue-600 dark:text-blue-400" />
                  <span>{t('tariffDropdownLabel')}</span>
                </label>
                {selectedRate && (
                  <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400">
                    {selectedRate.baseDailyRate}% / {t('perDay')}
                  </span>
                )}
              </div>
              <div className="relative">
                <select
                  value={selectedRate?.name || ''}
                  onChange={(e) => {
                    const found = rates.find((r) => r.name === e.target.value);
                    if (found) setSelectedRate(found);
                  }}
                  className="w-full px-3.5 py-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-2xl text-xs sm:text-sm font-semibold text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer transition-colors"
                >
                  {rates.map((r) => (
                    <option key={r.id ?? r.name} value={r.name}>
                      {r.name} — {r.baseDailyRate}% в день ({r.minDayRequired ?? 7}–{r.maxDayRequired ?? 30} {t('daysUnit')})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Purpose Selection: Dropdown with reasons (Requirement 3) */}
            <div className="mb-6 space-y-3">
              <label className="block text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center justify-between">
                <span className="flex items-center gap-2">
                  <FileText size={17} className="text-blue-600 dark:text-blue-400" />
                  {t('purposeDropdownLabel')}
                </span>
              </label>

              {/* Native Dropdown */}
              <div className="relative">
                <select
                  value={selectedPurposeId}
                  onChange={(e) => setSelectedPurposeId(e.target.value)}
                  className="w-full px-3.5 py-3 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-2xl text-xs sm:text-sm font-semibold text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer transition-colors"
                >
                  <optgroup label={t('optgroupBasic')}>
                    {PURPOSE_OPTIONS.filter((o) => o.category === 'BASIC').map((opt) => (
                      <option key={opt.id} value={opt.id}>
                        {t(opt.translationKey)}
                      </option>
                    ))}
                  </optgroup>
                  <optgroup label={t('optgroupVanity')}>
                    {PURPOSE_OPTIONS.filter((o) => o.category === 'VANITY').map((opt) => (
                      <option key={opt.id} value={opt.id}>
                        {t(opt.translationKey)}
                      </option>
                    ))}
                  </optgroup>
                  <optgroup label={t('optgroupOther')}>
                    <option value="custom">{t('purposeCustom')}</option>
                  </optgroup>
                </select>
              </div>

              {/* Custom purpose input field if "custom" selected */}
              {selectedPurposeId === 'custom' && (
                <div className="pt-2 animate-in fade-in">
                  <input
                    type="text"
                    value={customPurposeText}
                    onChange={(e) => setCustomPurposeText(e.target.value)}
                    placeholder={t('customPurposePlaceholder')}
                    className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-semibold text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1">
                    {t('scoringBannerDesc')}
                  </p>
                </div>
              )}

              {/* Automated classification notification banner */}
              <div
                className={`p-3 rounded-2xl border flex items-center justify-between gap-3 text-xs transition-colors ${
                  detectedType === 'VANITY'
                    ? 'bg-amber-50 dark:bg-amber-950/40 border-amber-200 dark:border-amber-800 text-amber-900 dark:text-amber-200'
                    : 'bg-blue-50 dark:bg-blue-950/40 border-blue-200 dark:border-blue-800 text-blue-900 dark:text-blue-200'
                }`}
              >
                <div className="flex items-center gap-2 min-w-0">
                  {detectedType === 'VANITY' ? (
                    <Sparkles size={16} className="text-amber-500 shrink-0" />
                  ) : (
                    <HeartHandshake size={16} className="text-blue-500 shrink-0" />
                  )}
                  <div className="truncate">
                    <span className="font-bold block sm:inline">
                      {detectedType === 'VANITY' ? t('autoClassifiedVanity') : t('autoClassifiedBasic')}
                    </span>
                    <span className="text-[11px] opacity-75 hidden sm:inline ml-1.5">
                      &bull; {t('autoClassifiedBanner')}
                    </span>
                  </div>
                </div>

                <span
                  className={`text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded-full shrink-0 ${
                    detectedType === 'VANITY'
                      ? 'bg-amber-200/80 dark:bg-amber-900 text-amber-950 dark:text-amber-200'
                      : 'bg-blue-200/80 dark:bg-blue-900 text-blue-950 dark:text-blue-200'
                  }`}
                >
                  {detectedType === 'VANITY' ? t('vanityType') : t('basicNeedsType')}
                </span>
              </div>
            </div>

            {/* Amount input */}
            <div className="space-y-3 mb-6 pt-4 border-t border-slate-100 dark:border-slate-800">
              <div className="flex items-center justify-between">
                <label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                  <Banknote size={17} className="text-blue-600 dark:text-blue-400" />
                  {t('loanAmount')}
                </label>
                <div className="flex items-center gap-1 bg-slate-50 dark:bg-slate-950 px-3 py-1 rounded-xl border border-slate-200 dark:border-slate-800">
                  <input
                    type="number"
                    min={1000}
                    max={100000}
                    step={1000}
                    value={amount}
                    onChange={(e) => setAmount(Number(e.target.value))}
                    className="w-28 text-right font-bold text-slate-900 dark:text-white bg-transparent focus:outline-none"
                  />
                  <span className="font-semibold text-slate-500 dark:text-slate-400 text-sm">
                    {getCurrencySymbol(currency)}
                  </span>
                </div>
              </div>

              <input
                type="range"
                min={1000}
                max={100000}
                step={1000}
                value={amount}
                onChange={(e) => setAmount(Number(e.target.value))}
                className="w-full h-2 bg-slate-200 dark:bg-slate-800 rounded-lg appearance-none cursor-pointer accent-blue-600"
              />

              <div className="flex items-center justify-between text-xs text-slate-400 dark:text-slate-500 font-medium">
                <span>{t('minAmountHint')}</span>
                <span>{t('maxAmountHint')}</span>
              </div>

              {/* Quick amount chips */}
              <div className="flex flex-wrap gap-2 pt-1">
                {QUICK_AMOUNTS.map((val) => (
                  <button
                    key={val}
                    onClick={() => setAmount(val)}
                    className={`px-2.5 py-1 text-xs font-medium rounded-lg border transition-all cursor-pointer ${
                      amount === val
                        ? 'bg-blue-50 dark:bg-blue-950/80 border-blue-500 text-blue-700 dark:text-blue-300 font-semibold'
                        : 'bg-slate-50 dark:bg-slate-950/80 border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 hover:border-slate-300 dark:hover:border-slate-700'
                    }`}
                  >
                    {val.toLocaleString()} {getCurrencySymbol(currency)}
                  </button>
                ))}
              </div>
            </div>

            {/* Term in days */}
            <div className="space-y-3 mb-6 pt-4 border-t border-slate-100 dark:border-slate-800">
              <div className="flex items-center justify-between">
                <label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                  <Clock size={17} className="text-blue-600 dark:text-blue-400" />
                  {t('loanTermDays')}
                </label>
                <div className="flex items-center gap-1 bg-slate-50 dark:bg-slate-950 px-3 py-1 rounded-xl border border-slate-200 dark:border-slate-800">
                  <input
                    type="number"
                    min={7}
                    max={30}
                    value={termDays}
                    onChange={(e) => setTermDays(Number(e.target.value))}
                    className="w-16 text-right font-bold text-slate-900 dark:text-white bg-transparent focus:outline-none"
                  />
                  <span className="font-semibold text-slate-500 dark:text-slate-400 text-sm">{t('daysUnit')}</span>
                </div>
              </div>

              <input
                type="range"
                min={7}
                max={30}
                step={1}
                value={termDays}
                onChange={(e) => setTermDays(Number(e.target.value))}
                className="w-full h-2 bg-slate-200 dark:bg-slate-800 rounded-lg appearance-none cursor-pointer accent-blue-600"
              />

              <div className="flex items-center justify-between text-xs text-slate-400 dark:text-slate-500 font-medium">
                <span>{t('minTermHint')}</span>
                <span>{t('maxTermHint')}</span>
              </div>

              {/* Quick term chips */}
              <div className="flex flex-wrap gap-2 pt-1">
                {QUICK_TERMS.map((d) => (
                  <button
                    key={d}
                    onClick={() => setTermDays(d)}
                    className={`px-3 py-1 text-xs font-medium rounded-lg border transition-all cursor-pointer ${
                      termDays === d
                        ? 'bg-blue-50 dark:bg-blue-950/80 border-blue-500 text-blue-700 dark:text-blue-300 font-semibold'
                        : 'bg-slate-50 dark:bg-slate-950/80 border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 hover:border-slate-300 dark:hover:border-slate-700'
                    }`}
                  >
                    {d} {t('daysUnit')}
                  </button>
                ))}
              </div>
            </div>

            {/* Fixed Bank Rates Display (Admin-controlled, not user editable) */}
            <div className="pt-4 border-t border-slate-100 dark:border-slate-800">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 flex items-center gap-1.5">
                  <Percent size={14} className="text-blue-600 dark:text-blue-400" />
                  {t('adminRateNotice')}
                </span>
                <span className="text-[11px] font-bold text-slate-400 dark:text-slate-500">
                  {t('cbrfRateLimit')}
                </span>
              </div>

              <div className="p-3 bg-slate-50 dark:bg-slate-950 rounded-2xl border border-slate-200 dark:border-slate-800 grid grid-cols-2 gap-3 text-xs">
                <div>
                  <span className="text-slate-500 dark:text-slate-400 block">{t('baseRate')}:</span>
                  <span className="text-base font-bold text-slate-900 dark:text-white">
                    {baseRate}% {t('perDay')}
                  </span>
                </div>
                <div>
                  <span className="text-slate-500 dark:text-slate-400 block">{t('vanitySurcharge')}:</span>
                  <span className="text-base font-bold text-amber-600 dark:text-amber-400">
                    +{formatCurrency(fixedVanitySurcharge, currency)} ({t('fixedFeeToLoan')})
                  </span>
                </div>
              </div>
            </div>

            {/* Start Date */}
            <div className="pt-4 mt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
              <label className="text-xs font-semibold text-slate-600 dark:text-slate-400 flex items-center gap-1.5">
                <Calendar size={15} />
                {t('startDate')}
              </label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl px-3 py-1.5 text-xs font-bold text-slate-900 dark:text-white focus:outline-none"
              />
            </div>
          </div>
        </div>

        {/* Right column: Results & Amortization Card */}
        <div className="lg:col-span-5 space-y-6">
          {result && (
            <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 sm:p-7 shadow-xs transition-colors">
              <div className="pb-5 border-b border-slate-100 dark:border-slate-800">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    {t('financialResult')} &bull; {t('totalRepayment')}
                  </span>
                  <span
                    className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                      detectedType === 'VANITY'
                        ? 'bg-amber-100 dark:bg-amber-950 text-amber-800 dark:text-amber-300 border border-amber-300 dark:border-amber-800'
                        : 'bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800'
                    }`}
                  >
                    {detectedType === 'VANITY' ? t('vanityType') : t('basicNeedsType')}
                  </span>
                </div>
                <div className="text-3xl sm:text-4xl font-black text-slate-900 dark:text-white mt-1.5 flex items-center gap-2">
                  <span>{formatCurrency(result.totalRepayment, currency)}</span>
                  {loading && (
                    <span className="inline-block animate-spin rounded-full h-4 w-4 border-2 border-blue-600 border-t-transparent" />
                  )}
                </div>
                <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400 mt-1">
                  <span>{t('dueUntil')}</span>
                  <span className="font-bold text-slate-800 dark:text-slate-200">
                    {new Date(result.dueDate).toLocaleDateString(language === 'en' ? 'en-US' : 'ru-RU', {
                      day: 'numeric',
                      month: 'long',
                      year: 'numeric',
                    })}
                  </span>
                </div>
              </div>

              {/* Metrics Grid */}
              <div className="py-5 border-b border-slate-100 dark:border-slate-800 space-y-3">
                <div className="flex justify-between items-center text-xs">
                  <span className="text-slate-500 dark:text-slate-400">{t('totalInterest')}:</span>
                  <span className="font-bold text-amber-600 dark:text-amber-400 text-sm">
                    {formatCurrency(result.totalInterest, currency)}
                  </span>
                </div>

                {/* Vanity fixed surcharge breakdown (Requirement 2 & 6) */}
                {detectedType === 'VANITY' && (result.vanitySurchargeAmount ?? 0) > 0 && (
                  <div className="p-3 bg-amber-50 dark:bg-amber-950/40 rounded-xl border border-amber-200 dark:border-amber-800/60 flex items-center justify-between text-xs text-amber-900 dark:text-amber-300">
                    <span className="flex items-center gap-1.5 font-medium">
                      <Sparkles size={14} className="text-amber-500 shrink-0" />
                      {t('vanitySurchargeAmountLabel')}:
                    </span>
                    <span className="font-extrabold text-amber-700 dark:text-amber-200">
                      +{formatCurrency(result.vanitySurchargeAmount ?? 0, currency)} ({t('fixedSurchargeBadge')})
                    </span>
                  </div>
                )}

                <div className="flex justify-between items-center text-xs">
                  <span className="text-slate-500 dark:text-slate-400">{t('baseRate')}:</span>
                  <span className="font-bold text-slate-800 dark:text-white">
                    {result.baseDailyRate ?? baseRate}% {t('perDay')}
                  </span>
                </div>

                <div className="flex justify-between items-center text-xs">
                  <span className="text-slate-500 dark:text-slate-400">{t('aprRate')}:</span>
                  <span className="font-bold text-slate-800 dark:text-white">
                    {result.annualEquivalentRate}%
                  </span>
                </div>
              </div>

              {/* Action Application Button */}
              <div className="pt-5 space-y-3">
                <button
                  type="button"
                  onClick={handleStartApplication}
                  disabled={isAdmin}
                  className={`w-full py-3.5 ${applyBtnClasses} text-white font-bold rounded-xl shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed`}
                >
                  <CreditCard size={18} />
                  <span>{t('applyBtn')}</span>
                  <ChevronRight size={16} />
                </button>

                {!user && (
                  <p className="text-[11px] text-center text-slate-500 dark:text-slate-400">
                    {t('authRequiredToApply')}
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Daily Repayment Schedule */}
      {result && result.dailySchedule && result.dailySchedule.length > 0 && (
        <div className="mt-10 bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 sm:p-7 shadow-xs transition-colors">
          <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800 mb-4">
            <div>
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                {t('dailyScheduleTitle')}
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                {t('dailyBreakdownDesc')}
              </p>
            </div>
            <span className="text-xs font-bold px-3 py-1 bg-slate-100 dark:bg-slate-950 text-slate-700 dark:text-slate-300 rounded-xl border border-slate-200 dark:border-slate-800">
              {result.dailySchedule.length} {t('daysAccrued')}
            </span>
          </div>

          <div className="overflow-x-auto rounded-2xl border border-slate-100 dark:border-slate-800">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-slate-50 dark:bg-slate-950 text-slate-600 dark:text-slate-300 font-bold border-b border-slate-100 dark:border-slate-800">
                  <th className="p-3 text-center">{t('tableDay')}</th>
                  <th className="p-3">{t('tableDate')}</th>
                  <th className="p-3 text-right">{t('tablePrincipal')}</th>
                  <th className="p-3 text-right">{t('tableDailyInterest')}</th>
                  <th className="p-3 text-right">{t('tableAccumulated')}</th>
                  <th className="p-3 text-right">{t('tableTotalDue')}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {result.dailySchedule.map((row) => (
                  <tr
                    key={row.dayNumber}
                    className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition-colors"
                  >
                    <td className="p-3 text-center font-medium text-slate-400">
                      {row.dayNumber}
                    </td>
                    <td className="p-3 font-semibold text-slate-700 dark:text-slate-300 whitespace-nowrap">
                      {new Date(row.date).toLocaleDateString(language === 'en' ? 'en-US' : 'ru-RU', {
                        day: 'numeric',
                        month: 'short',
                        year: 'numeric',
                      })}
                    </td>
                    <td className="p-3 text-right font-medium text-slate-600 dark:text-slate-400 whitespace-nowrap">
                      {formatCurrency(row.principalBalance, currency)}
                    </td>
                    <td className="p-3 text-right text-amber-600 dark:text-amber-400 font-medium whitespace-nowrap">
                      +{formatCurrency(row.dailyInterest, currency)}
                    </td>
                    <td className="p-3 text-right text-amber-700 dark:text-amber-300 font-semibold whitespace-nowrap">
                      {formatCurrency(row.accumulatedInterest, currency)}
                    </td>
                    <td className="p-3 text-right font-black text-blue-700 dark:text-blue-400 whitespace-nowrap">
                      {formatCurrency(row.totalDue, currency)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Mock Passport Application Modal */}
      {isPassportModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl w-full max-w-lg p-6 sm:p-7 border border-slate-100 dark:border-slate-800 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800 mb-5">
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <UserCheck size={20} className="text-blue-600 dark:text-blue-400" />
                  {t('passportModalTitle')}
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  {t('passportModalDesc')}
                </p>
              </div>
              <button
                onClick={() => setIsPassportModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
              >
                <X size={20} />
              </button>
            </div>

            {/* Summary preview */}
            <div className="mb-5 p-3.5 bg-blue-50 dark:bg-slate-950 rounded-2xl border border-blue-200 dark:border-slate-800 flex items-center justify-between text-xs">
              <div>
                <span className="text-slate-500 dark:text-slate-400 block">{t('loanAmount')}:</span>
                <span className="font-extrabold text-slate-900 dark:text-white text-sm">
                  {formatCurrency(amount, currency)}
                </span>
              </div>
              <div className="text-right">
                <span className="text-slate-500 dark:text-slate-400 block">{t('categoryTermLabel')}</span>
                <span className="font-bold text-blue-700 dark:text-blue-300">
                  {detectedType === 'VANITY' ? t('vanityType') : t('basicNeedsType')} &bull; {termDays} {t('daysUnit')}
                </span>
              </div>
            </div>

            {formError && (
              <div className="mb-4 p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 rounded-xl text-red-700 dark:text-red-300 text-xs flex items-center gap-2">
                <AlertCircle size={15} />
                <span>{formError}</span>
              </div>
            )}

            {savedPassport && (
              <div className="mb-4 p-2.5 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 rounded-xl text-emerald-700 dark:text-emerald-300 text-xs flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <ShieldCheck size={16} className="text-emerald-600 dark:text-emerald-400" />
                  <span className="font-semibold">{t('passportAutoFilledBadge')}</span>
                </div>
                <span className="text-[10px] bg-emerald-100 dark:bg-emerald-900/60 px-2 py-0.5 rounded-full font-mono font-bold">AES-256</span>
              </div>
            )}

            <form onSubmit={handleSubmitPassport} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                  {t('fullName')} *
                </label>
                <input
                  type="text"
                  required
                  placeholder={t('placeholderFullName')}
                  value={passportData.fullName}
                  onChange={(e) => setPassportData({ ...passportData, fullName: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 uppercase"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('passportSeriesNumber')} *
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={11}
                    placeholder="4515 123456"
                    value={passportData.seriesNumber}
                    onChange={(e) => setPassportData({ ...passportData, seriesNumber: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 font-mono"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('issueDate')}
                  </label>
                  <input
                    type="date"
                    value={passportData.issueDate}
                    onChange={(e) => setPassportData({ ...passportData, issueDate: e.target.value })}
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
                    type="text"
                    maxLength={7}
                    placeholder="770-001"
                    value={passportData.departmentCode}
                    onChange={(e) => setPassportData({ ...passportData, departmentCode: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 font-mono"
                  />
                </div>
                <div className="col-span-2">
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('issuedBy')}
                  </label>
                  <input
                    type="text"
                    placeholder={t('placeholderIssuedBy')}
                    value={passportData.issuedBy}
                    onChange={(e) => setPassportData({ ...passportData, issuedBy: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                  {t('confirmedPurposeLabel')} {detectedType === 'VANITY' ? t('vanityType') : t('basicNeedsType')})
                </label>
                <input
                  type="text"
                  readOnly
                  value={currentPurpose}
                  className="w-full px-3 py-2 bg-slate-100 dark:bg-slate-950/80 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300 cursor-not-allowed"
                />
              </div>

              <div className="pt-1">
                <label className="flex items-center gap-2.5 cursor-pointer select-none text-xs text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white transition-colors">
                  <input
                    type="checkbox"
                    checked={saveToProfile}
                    onChange={(e) => setSaveToProfile(e.target.checked)}
                    className="w-4 h-4 rounded border-slate-300 dark:border-slate-700 text-blue-600 focus:ring-blue-500 cursor-pointer"
                  />
                  <span className="font-medium">{t('savePassportToProfileCheckbox')}</span>
                </label>
              </div>

              <div className="p-3 bg-slate-50 dark:bg-slate-950 rounded-xl border border-slate-200 dark:border-slate-800 text-[11px] text-slate-500 dark:text-slate-400">
                {t('passportSecurityNote')}
              </div>

              <div className="flex gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setIsPassportModalOpen(false)}
                  className="flex-1 py-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-semibold text-xs rounded-xl transition-colors cursor-pointer"
                >
                  {t('cancel')}
                </button>
                <button
                  type="submit"
                  className={`flex-1 py-2.5 ${applyBtnClasses} text-white font-semibold text-xs rounded-xl shadow-md transition-all cursor-pointer`}
                >
                  {t('submitApplication')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
