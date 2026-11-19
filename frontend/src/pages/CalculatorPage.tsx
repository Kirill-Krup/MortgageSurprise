import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { calculatorApi, scenarioApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import type {
  BankOffer,
  CalculationRequest,
  CalculationResponse,
  EarlyRepayment,
  PaymentType,
  RepaymentStrategy,
  EarlyRepaymentType,
  Currency,
} from '../types';
import {
  TrendingDown,
  Percent,
  Plus,
  Trash2,
  BookmarkPlus,
  Table,
  CheckCircle,
  AlertCircle,
  Coins,
  ShieldCheck,
} from 'lucide-react';
import { CURRENCY_CONFIGS, formatCurrency, getCurrencySymbol } from '../utils/format';
import { useExperiment } from '../hooks/useExperiment';
import {
  BUTTON_COLOR_VARIANTS,
  getMortgageCalculateButtonClasses,
} from '../utils/abTesting';

interface CalculatorPageProps {
  presetOffer?: BankOffer | null;
  onClearPreset?: () => void;
  onOpenAuth: () => void;
}

export const CalculatorPage: React.FC<CalculatorPageProps> = ({
  presetOffer,
  onClearPreset,
  onOpenAuth,
}) => {
  const { user } = useAuth();
  const { t } = useLanguage();

  // AB test button color for Mortgage Calculation (persisted permanently in DB and localStorage)
  const mortgageCalcBtnVariant = useExperiment('mortgage_calculate_button', BUTTON_COLOR_VARIANTS);
  const mortgageCalcBtnClasses = getMortgageCalculateButtonClasses(mortgageCalcBtnVariant);

  const bankingStandards = useMemo(() => [
    t('standardLicense'),
    t('standardSecurity'),
    t('standardBasel'),
    t('standardAsv'),
    t('standardPrepay'),
  ], [t]);

  // Currency
  const [currency, setCurrency] = useState<Currency>('RUB');

  // Inputs
  const [propertyPrice, setPropertyPrice] = useState<number>(10_000_000);
  const [downPayment, setDownPayment] = useState<number>(2_000_000);
  const [downPaymentPercent, setDownPaymentPercent] = useState<number>(20);
  const [termYears, setTermYears] = useState<number>(20);
  const [annualRate, setAnnualRate] = useState<number>(18.5);
  const [paymentType, setPaymentType] = useState<PaymentType>('ANNUITY');

  // Early repayments
  const [earlyRepayments, setEarlyRepayments] = useState<EarlyRepayment[]>([]);
  const [showEarlyModal, setShowEarlyModal] = useState<boolean>(false);
  const [erStartMonth, setErStartMonth] = useState<number>(12);
  const [erEndMonth, setErEndMonth] = useState<number | undefined>(undefined);
  const [erAmount, setErAmount] = useState<number>(50_000);
  const [erType, setErType] = useState<EarlyRepaymentType>('ONE_TIME');
  const [erStrategy, setErStrategy] = useState<RepaymentStrategy>('REDUCE_TERM');

  // Calculation Results
  const [loading, setLoading] = useState<boolean>(false);
  const [result, setResult] = useState<CalculationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Pagination for schedule
  const [currentPage, setCurrentPage] = useState<number>(1);
  const pageSize = 12; // 1 year per page

  // Save Scenario State
  const [saveModalOpen, setSaveModalOpen] = useState<boolean>(false);
  const [scenarioTitle, setScenarioTitle] = useState<string>('');
  const [saving, setSaving] = useState<boolean>(false);
  const [saveSuccess, setSaveSuccess] = useState<boolean>(false);

  // Exchange rates from API
  const [exchangeRates, setExchangeRates] = useState<Record<string, number>>({});

  // Banking standard ticker
  const [jokeIndex, setJokeIndex] = useState<number>(0);
  const nextJoke = () => setJokeIndex((prev) => (prev + 1) % 5);

  // Run calculation
  const calculateMortgage = useCallback(async (overrideParams?: {
    price?: number;
    dp?: number;
    termY?: number;
    rate?: number;
    pType?: PaymentType;
    curr?: Currency;
    audit?: boolean;
  }) => {
    setLoading(true);
    setError(null);
    setCurrentPage(1);

    const targetPrice = overrideParams?.price ?? propertyPrice;
    const targetDp = overrideParams?.dp ?? downPayment;
    const targetTermY = overrideParams?.termY ?? termYears;
    const targetRate = overrideParams?.rate ?? annualRate;
    const targetPType = overrideParams?.pType ?? paymentType;
    const targetCurr = overrideParams?.curr ?? currency;
    const shouldAudit = overrideParams?.audit ?? false;

    const req: CalculationRequest = {
      propertyPrice: targetPrice,
      downPayment: targetDp,
      termMonths: targetTermY * 12,
      annualInterestRate: targetRate,
      paymentType: targetPType,
      currency: targetCurr,
      earlyRepayments: earlyRepayments.map((er) => ({
        startMonth: Number(er.startMonth),
        endMonth: (er.type === 'MONTHLY' || er.type === 'RECURRING') && er.endMonth ? Number(er.endMonth) : null,
        amount: Number(er.amount),
        strategy: er.strategy,
        type: er.type,
      })),
      bankOfferId: presetOffer ? Number(presetOffer.id) : undefined,
      bankName: presetOffer?.bankName,
      offerName: presetOffer?.offerName,
      audit: shouldAudit,
    };

    try {
      const res = await calculatorApi.calculate(req);
      setResult(res);
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(
        axiosError.response?.data?.message || t('calcErrorMsg')
      );
    } finally {
      setLoading(false);
    }
  }, [propertyPrice, downPayment, termYears, annualRate, paymentType, currency, earlyRepayments, presetOffer, t]);

  // Currency change handler
  const handleCurrencyChange = (newCurrency: Currency) => {
    setCurrency(newCurrency);
    const cfg = CURRENCY_CONFIGS.find((c) => c.code === newCurrency);
    if (cfg) {
      const newPrice = cfg.defaultPrice;
      const effectiveDpPercent = presetOffer?.minDownPaymentPercent ?? downPaymentPercent;
      const newDp = Math.round((newPrice * effectiveDpPercent) / 100);

      const newRate = presetOffer
        ? presetOffer.annualInterestRate
        : newCurrency === 'USD'
        ? 6.5
        : newCurrency === 'EUR'
        ? 4.5
        : newCurrency === 'BYN'
        ? 14.5
        : 18.5;

      const newErAmount = Math.round(newDp / 20);

      setPropertyPrice(newPrice);
      setDownPayment(newDp);
      setDownPaymentPercent(effectiveDpPercent);
      setAnnualRate(newRate);
      setErAmount(newErAmount);

      calculateMortgage({
        price: newPrice,
        dp: newDp,
        termY: termYears,
        rate: newRate,
        curr: newCurrency,
        pType: paymentType,
        audit: false,
      });
    }
  };

  useEffect(() => {
    if (presetOffer) {
      const offerCurr = presetOffer.currency || 'RUB';
      setCurrency(offerCurr);
      const cfg = CURRENCY_CONFIGS.find((c) => c.code === offerCurr);
      const basePrice = cfg ? cfg.defaultPrice : 10_000_000;
      const dpPercent = presetOffer.minDownPaymentPercent || 20;
      const dpAmount = Math.round((basePrice * dpPercent) / 100);
      const termY = Math.min(Math.round(presetOffer.maxTermMonths / 12) || 20, 30);
      const rate = presetOffer.annualInterestRate;

      setPropertyPrice(basePrice);
      setDownPayment(dpAmount);
      setDownPaymentPercent(dpPercent);
      setTermYears(termY);
      setAnnualRate(rate);

      calculateMortgage({
        price: basePrice,
        dp: dpAmount,
        termY,
        rate,
        curr: offerCurr,
        audit: false,
      });
    } else {
      calculateMortgage({ audit: false });
    }
  }, [presetOffer]);

  // Fetch live exchange rates on mount
  useEffect(() => {
    calculatorApi.getExchangeRates()
      .then(setExchangeRates)
      .catch(() => {});
  }, []);

  // Price change
  const handlePriceChange = (val: number) => {
    setPropertyPrice(val);
    const newDp = Math.round((val * downPaymentPercent) / 100);
    setDownPayment(newDp);
  };

  // DP change
  const handleDpChange = (val: number) => {
    setDownPayment(val);
    if (propertyPrice > 0) {
      const pct = Math.round((val / propertyPrice) * 100);
      setDownPaymentPercent(Math.min(pct, 99));
    }
  };

  // DP % change
  const handleDpPercentChange = (pct: number) => {
    setDownPaymentPercent(pct);
    const newDp = Math.round((propertyPrice * pct) / 100);
    setDownPayment(newDp);
  };

  // Add Early Repayment
  const addEarlyRepayment = () => {
    const newItem: EarlyRepayment = {
      id: Math.random().toString(36).substring(2, 9),
      startMonth: erStartMonth,
      endMonth: erType === 'MONTHLY' ? erEndMonth || null : null,
      amount: erAmount,
      strategy: erStrategy,
      type: erType,
    };
    setEarlyRepayments([...earlyRepayments, newItem]);
    setShowEarlyModal(false);
  };

  // Remove Early Repayment
  const removeEarlyRepayment = (index: number) => {
    setEarlyRepayments(earlyRepayments.filter((_, i) => i !== index));
  };

  // Save Scenario
  const handleSaveScenario = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) {
      onOpenAuth();
      return;
    }
    if (!scenarioTitle.trim()) return;

    setSaving(true);
    try {
      await scenarioApi.save({
        title: scenarioTitle.trim(),
        propertyPrice,
        downPayment,
        termMonths: termYears * 12,
        annualInterestRate: annualRate,
        paymentType,
        currency,
        earlyRepayments: earlyRepayments.map((er) => ({
          startMonth: Number(er.startMonth),
          endMonth: er.type === 'MONTHLY' && er.endMonth ? Number(er.endMonth) : null,
          amount: Number(er.amount),
          strategy: er.strategy,
          type: er.type,
        })),
      });
      setSaveSuccess(true);
      setTimeout(() => {
        setSaveSuccess(false);
        setSaveModalOpen(false);
        setScenarioTitle('');
      }, 1500);
    } catch {
      alert(t('saveScenarioError'));
    } finally {
      setSaving(false);
    }
  };

  // Pagination slice
  const paginatedSchedule = result?.schedule
    ? result.schedule.slice((currentPage - 1) * pageSize, currentPage * pageSize)
    : [];
  const totalPages = result?.schedule ? Math.ceil(result.schedule.length / pageSize) : 1;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-in fade-in duration-300">
      {/* Preset Offer Banner if any */}
      {presetOffer && (
        <div className="mb-6 bg-gradient-to-r from-blue-700 via-indigo-800 to-slate-900 text-white p-5 rounded-2xl shadow-md border border-blue-600/30 flex flex-col sm:flex-row sm:items-center justify-between gap-4 animate-in fade-in">
          <div>
            <div className="flex flex-wrap items-center gap-2 mb-1.5">
              <span className="text-[11px] uppercase tracking-wider font-extrabold bg-white/20 px-2.5 py-0.5 rounded-lg">
                {t('selectedBank')}
              </span>
              <span className="text-[11px] font-extrabold bg-amber-400 text-amber-950 px-2.5 py-0.5 rounded-lg flex items-center gap-1 shadow-xs">
                <span>{currency}</span>
                <span>{getCurrencySymbol(currency)}</span>
              </span>
              <span className="text-xs text-blue-200 font-medium">
                {t('bankTariffs')}
              </span>
            </div>
            <h3 className="text-lg font-black tracking-tight">
              {presetOffer.bankName} — {presetOffer.offerName} ({presetOffer.annualInterestRate}%)
            </h3>
            <p className="text-xs text-blue-100 mt-0.5">
              {t('bankConditionsActive').replace('{currency}', currency)}
            </p>
          </div>
          {onClearPreset && (
            <button
              onClick={onClearPreset}
              className="text-xs bg-white/20 hover:bg-white/30 active:bg-white/40 text-white font-bold px-4 py-2 rounded-xl transition-all cursor-pointer self-start sm:self-center shrink-0"
            >
              {t('resetBank')}
            </button>
          )}
        </div>
      )}

      {/* Official Banking Standards Banner */}
      <div className="mb-6 bg-blue-50/80 dark:bg-slate-900/90 border border-blue-200 dark:border-slate-800 rounded-2xl px-4 py-3 flex items-center justify-between gap-3 text-xs text-blue-950 dark:text-blue-200 shadow-xs transition-colors">
        <div className="flex items-center gap-2.5 min-w-0">
          <ShieldCheck size={18} className="text-blue-600 dark:text-blue-400 shrink-0" />
          <span className="font-semibold truncate sm:overflow-visible sm:whitespace-normal">
            {bankingStandards[jokeIndex % bankingStandards.length]}
          </span>
        </div>
        <button
          type="button"
          onClick={nextJoke}
          className="text-[11px] font-bold text-blue-800 dark:text-blue-300 hover:text-blue-950 dark:hover:text-white bg-blue-200/60 dark:bg-blue-950 px-2.5 py-1 rounded-lg border border-transparent dark:border-slate-800 transition-colors cursor-pointer shrink-0"
          title={t('nextStandardTooltip')}
        >
          {t('nextYear')} &rarr;
        </button>
      </div>

      {user?.role === 'ROLE_ADMIN' && (
        <div className="mb-6 p-4 bg-amber-50 dark:bg-amber-950/40 border border-amber-300 dark:border-amber-800 rounded-2xl flex items-start gap-3 text-amber-900 dark:text-amber-200 text-sm animate-in fade-in">
          <ShieldCheck size={20} className="text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
          <div>
            <div className="font-bold">{t('adminConfigModeAlert')}</div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column: Calculator Parameters Form */}
        <div className="lg:col-span-5 space-y-6">
          <div className="bg-white dark:bg-slate-900 rounded-2xl p-6 sm:p-7 shadow-xs border border-slate-200 dark:border-slate-800 transition-colors">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <span className="w-2.5 h-6 bg-blue-600 rounded-full inline-block" />
                {t('mortgageParams')}
              </h2>
            </div>

            {/* Currency Selector Bar */}
            <div className="mb-6 p-1.5 bg-slate-100 dark:bg-slate-950 rounded-2xl border border-transparent dark:border-slate-800">
              <div className="text-[11px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider px-2 py-1 flex items-center gap-1.5">
                <Coins size={14} className="text-blue-600 dark:text-blue-400" />
                {t('calcCurrency')}
              </div>
              <div className="grid grid-cols-4 gap-1.5 mt-1">
                {CURRENCY_CONFIGS.map((cfg) => {
                  const isSelected = currency === cfg.code;
                  // Show cross-rate: how much 1 unit of this currency costs in the currently selected currency
                  const showRate = !isSelected && exchangeRates[cfg.code] && exchangeRates[currency];
                  const crossRate = showRate
                    ? (exchangeRates[currency] / exchangeRates[cfg.code])
                    : null;
                  return (
                    <button
                      key={cfg.code}
                      type="button"
                      onClick={() => handleCurrencyChange(cfg.code)}
                      className={`py-2 px-1 rounded-xl text-xs font-bold transition-all cursor-pointer flex flex-col items-center justify-center ${
                        isSelected
                          ? 'bg-blue-700 text-white shadow-xs scale-102'
                          : 'bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 border border-slate-200/60 dark:border-slate-800'
                      }`}
                    >
                      <span className="text-sm font-black">{cfg.symbol}</span>
                      <span className="text-[10px] tracking-tight">{cfg.code}</span>
                      {crossRate !== null && (
                        <span className="text-[9px] mt-0.5 opacity-70 font-medium">
                          1{cfg.symbol} = {crossRate < 10 ? crossRate.toFixed(2) : crossRate.toFixed(0)}{getCurrencySymbol(currency)}
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>

            {Object.keys(exchangeRates).length > 0 && (
              <div className="mb-4 px-3 py-2 bg-blue-50/60 dark:bg-blue-950/30 rounded-xl border border-blue-100 dark:border-blue-900/50 flex items-center gap-2 text-[11px] text-blue-700 dark:text-blue-300 font-medium">
                <TrendingDown size={13} className="shrink-0 opacity-70" />
                <span>
                  {t('liveRates') || 'Курсы'}:{' '}
                  {CURRENCY_CONFIGS.filter(c => c.code !== 'RUB' && exchangeRates[c.code])
                    .map(c => `1 ${c.symbol} = ${exchangeRates[c.code]?.toFixed(2) || '—'} ₽`)
                    .join(' · ')}
                </span>
              </div>
            )}

            <div className="space-y-5">
              {/* Property Price */}
              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    {t('propertyPrice')}
                  </label>
                  <span className="text-sm font-bold text-blue-600 dark:text-blue-400">
                    {formatCurrency(propertyPrice, currency)}
                  </span>
                </div>
                <div className="relative">
                  <input
                    type="number"
                    min="1000"
                    step={currency === 'USD' || currency === 'EUR' ? '5000' : '50000'}
                    value={propertyPrice}
                    onChange={(e) => handlePriceChange(Number(e.target.value))}
                    className="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-slate-800 dark:text-slate-100 font-medium focus:ring-2 focus:ring-blue-500 focus:bg-white dark:focus:bg-slate-900 text-sm"
                  />
                  <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">
                    {getCurrencySymbol(currency)}
                  </span>
                </div>
                <input
                  type="range"
                  min={currency === 'USD' || currency === 'EUR' ? 10000 : 300000}
                  max={currency === 'USD' || currency === 'EUR' ? 2000000 : 50000000}
                  step={currency === 'USD' || currency === 'EUR' ? 5000 : 100000}
                  value={propertyPrice}
                  onChange={(e) => handlePriceChange(Number(e.target.value))}
                  className="w-full mt-2 accent-blue-600 cursor-pointer"
                />
              </div>

              {/* Down Payment */}
              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    {t('downPayment')}
                  </label>
                  <div className="flex items-center gap-2 text-sm font-bold text-blue-600 dark:text-blue-400">
                    <span>{formatCurrency(downPayment, currency)}</span>
                    <span className="text-xs bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 px-2 py-0.5 rounded-full border border-blue-200 dark:border-blue-800">
                      {downPaymentPercent}%
                    </span>
                  </div>
                </div>
                <div className="grid grid-cols-3 gap-2">
                  <div className="col-span-2 relative">
                    <input
                      type="number"
                      min="0"
                      step={currency === 'USD' || currency === 'EUR' ? '2000' : '20000'}
                      value={downPayment}
                      onChange={(e) => handleDpChange(Number(e.target.value))}
                      className="w-full px-4 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-slate-800 dark:text-slate-100 font-medium focus:ring-2 focus:ring-blue-500 focus:bg-white dark:focus:bg-slate-900 text-sm"
                    />
                    <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">
                      {getCurrencySymbol(currency)}
                    </span>
                  </div>
                  <div className="relative">
                    <input
                      type="number"
                      min="0"
                      max="90"
                      value={downPaymentPercent}
                      onChange={(e) => handleDpPercentChange(Number(e.target.value))}
                      className="w-full pl-3 pr-7 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-slate-800 dark:text-slate-100 font-medium focus:ring-2 focus:ring-blue-500 focus:bg-white dark:focus:bg-slate-900 text-sm text-center"
                    />
                    <Percent
                      size={14}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400"
                    />
                  </div>
                </div>
                <input
                  type="range"
                  min="0"
                  max="80"
                  step="1"
                  value={downPaymentPercent}
                  onChange={(e) => handleDpPercentChange(Number(e.target.value))}
                  className="w-full mt-2 accent-blue-600 cursor-pointer"
                />
              </div>

              {/* Term & Rate */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                    {t('loanTerm')}
                  </label>
                  <div className="relative">
                    <input
                      type="number"
                      min="1"
                      max="30"
                      value={termYears}
                      onChange={(e) => setTermYears(Number(e.target.value))}
                      className="w-full pl-3 pr-10 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-slate-800 dark:text-slate-100 font-medium focus:ring-2 focus:ring-blue-500 focus:bg-white dark:focus:bg-slate-900 text-sm"
                    />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 font-medium">
                      {t('years')}
                    </span>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                    {t('rate')}
                  </label>
                  <div className="relative">
                    <input
                      type="number"
                      step="0.1"
                      min="0.1"
                      max="40"
                      value={annualRate}
                      onChange={(e) => setAnnualRate(Number(e.target.value))}
                      className="w-full pl-3 pr-8 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-slate-800 dark:text-slate-100 font-medium focus:ring-2 focus:ring-blue-500 focus:bg-white dark:focus:bg-slate-900 text-sm"
                    />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 font-medium">
                      %
                    </span>
                  </div>
                </div>
              </div>

              {/* Payment Type */}
              <div>
                <label className="block text-sm font-semibold text-slate-700 dark:text-slate-300 mb-2">
                  {t('paymentType')}
                </label>
                <div className="grid grid-cols-2 gap-2 p-1 bg-slate-100 dark:bg-slate-950 rounded-xl border border-transparent dark:border-slate-800 text-xs font-semibold">
                  <button
                    type="button"
                    onClick={() => setPaymentType('ANNUITY')}
                    className={`py-2 rounded-lg transition-all cursor-pointer ${
                      paymentType === 'ANNUITY'
                        ? 'bg-white dark:bg-slate-800 text-blue-700 dark:text-blue-300 shadow-xs font-bold'
                        : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                    }`}
                  >
                    {t('annuity')}
                  </button>
                  <button
                    type="button"
                    onClick={() => setPaymentType('DIFFERENTIATED')}
                    className={`py-2 rounded-lg transition-all cursor-pointer ${
                      paymentType === 'DIFFERENTIATED'
                        ? 'bg-white dark:bg-slate-800 text-blue-700 dark:text-blue-300 shadow-xs font-bold'
                        : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                    }`}
                  >
                    {t('differentiated')}
                  </button>
                </div>
              </div>

              {/* Early Repayments */}
              <div className="pt-2 border-t border-slate-100 dark:border-slate-800">
                <div className="flex items-center justify-between mb-3">
                  <span className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    {t('earlyRepayments')}
                  </span>
                  <button
                    type="button"
                    onClick={() => setShowEarlyModal(true)}
                    className="flex items-center gap-1 text-xs font-bold text-blue-600 dark:text-blue-400 hover:text-blue-700 cursor-pointer"
                  >
                    <Plus size={14} />
                    {t('add')}
                  </button>
                </div>

                {earlyRepayments.length > 0 ? (
                  <div className="space-y-2 mb-4">
                    {earlyRepayments.map((er, idx) => (
                      <div
                        key={er.id || idx}
                        className="flex items-center justify-between p-2.5 bg-slate-50 dark:bg-slate-950 rounded-xl text-xs border border-slate-100 dark:border-slate-800"
                      >
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-slate-800 dark:text-slate-200">
                            {formatCurrency(er.amount, currency)}
                          </span>
                          <span className="text-slate-500 dark:text-slate-400">
                            {er.type === 'ONE_TIME'
                              ? `${t('inMonthPrefix')} ${er.startMonth}`
                              : t('fromMonthTo').replace('{start}', String(er.startMonth)).replace('{end}', String(er.endMonth || t('untilEnd')))}
                          </span>
                        </div>
                        <button
                          type="button"
                          onClick={() => removeEarlyRepayment(idx)}
                          className="text-slate-400 hover:text-red-600 transition-colors cursor-pointer"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-slate-400 dark:text-slate-500">
                    {t('emptyEarlyRepayments')}
                  </p>
                )}
              </div>

              {/* Submit calculate */}
              <button
                type="button"
                onClick={() => calculateMortgage({ audit: true })}
                disabled={loading || user?.role === 'ROLE_ADMIN'}
                className={`w-full py-3.5 ${mortgageCalcBtnClasses} text-white font-bold rounded-xl shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed`}
              >
                {loading ? (
                  <span className="inline-block animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent" />
                ) : (
                  <span>
                    {t('calculateMortgageBtn')} ({getCurrencySymbol(currency)})
                  </span>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Right Column: Results Summary & Payment Schedule */}
        <div className="lg:col-span-7 space-y-6">
          {error && (
            <div className="p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 rounded-2xl text-red-700 dark:text-red-300 text-sm flex items-start gap-3">
              <AlertCircle size={20} className="shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {/* Results Summary Cards */}
          {result && (
            <div className="bg-white dark:bg-slate-900 rounded-2xl p-6 sm:p-7 shadow-xs border border-slate-200 dark:border-slate-800 transition-colors">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 border-b border-slate-100 dark:border-slate-800 gap-4">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-slate-400 dark:text-slate-400 uppercase tracking-wider">
                      {t('financialResult')} &bull; {t('monthlyPayment')}
                    </span>
                    <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                      {result.currency || currency}
                    </span>
                  </div>
                  <div className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white mt-1">
                    {paymentType === 'ANNUITY' ? (
                      formatCurrency(result.monthlyPayment, result.currency || currency)
                    ) : (
                      <span>
                        {formatCurrency(result.minMonthlyPayment, result.currency || currency)} ...{' '}
                        {formatCurrency(result.maxMonthlyPayment, result.currency || currency)}
                      </span>
                    )}
                  </div>
                </div>

                {user?.role !== 'ROLE_ADMIN' && (
                  <div className="flex gap-2">
                    <button
                      onClick={() => setSaveModalOpen(true)}
                      className="flex items-center gap-1.5 px-4 py-2 bg-blue-50 dark:bg-blue-950/60 hover:bg-blue-100 dark:hover:bg-blue-900/60 text-blue-700 dark:text-blue-300 text-sm font-semibold rounded-xl border border-blue-200 dark:border-blue-800 transition-colors cursor-pointer"
                    >
                      <BookmarkPlus size={18} />
                      {t('saveScenarioBtn')}
                    </button>
                  </div>
                )}
              </div>

              {/* Grid Metrics */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-6">
                <div className="p-3 bg-slate-50 dark:bg-slate-950 rounded-xl border border-slate-100 dark:border-slate-800/80 flex flex-col justify-between">
                  <div>
                    <div className="text-xs text-slate-500 dark:text-slate-400 font-medium">{t('loanAmountTitle')}</div>
                    <div className="text-base font-bold text-slate-900 dark:text-white mt-1">
                      {formatCurrency(result.loanAmount, result.currency || currency)}
                    </div>
                  </div>
                </div>

                <div className="p-3 bg-slate-50 dark:bg-slate-950 rounded-xl border border-slate-100 dark:border-slate-800/80 flex flex-col justify-between">
                  <div>
                    <div className="text-xs text-slate-500 dark:text-slate-400 font-medium">{t('overpaymentInterest')}</div>
                    <div className="text-base font-bold text-red-600 dark:text-red-400 mt-1">
                      {formatCurrency(result.totalInterest, result.currency || currency)}
                    </div>
                  </div>
                </div>

                <div className="p-3 bg-slate-50 dark:bg-slate-950 rounded-xl border border-slate-100 dark:border-slate-800/80 flex flex-col justify-between">
                  <div>
                    <div className="text-xs text-slate-500 dark:text-slate-400 font-medium">{t('totalPaymentsSum')}</div>
                    <div className="text-base font-bold text-slate-900 dark:text-white mt-1">
                      {formatCurrency(result.totalPayment, result.currency || currency)}
                    </div>
                  </div>
                </div>

                <div className="p-3 bg-slate-50 dark:bg-slate-950 rounded-xl border border-slate-100 dark:border-slate-800/80 flex flex-col justify-between">
                  <div>
                    <div className="text-xs text-slate-500 dark:text-slate-400 font-medium">{t('actualTerm')}</div>
                    <div className="text-base font-bold text-blue-600 dark:text-blue-400 mt-1">
                      {Math.floor(result.actualTermMonths / 12)} {t('yearUnit')} {result.actualTermMonths % 12} {t('monthUnit')}
                    </div>
                  </div>
                </div>
              </div>

              {/* Savings hint if term was reduced */}
              {result.actualTermMonths < result.termMonths && (
                <div className="mt-4 p-3 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 rounded-xl flex items-center gap-2 text-xs text-emerald-800 dark:text-emerald-200 font-medium">
                  <TrendingDown size={16} className="text-emerald-600 dark:text-emerald-400 shrink-0" />
                  <span>
                    {t('earlyRepaymentsSavings')}{' '}
                    <span className="font-bold">
                      {Math.floor((result.termMonths - result.actualTermMonths) / 12)} {t('years')}{' '}
                      {(result.termMonths - result.actualTermMonths) % 12} {t('months')}
                    </span>
                  </span>
                </div>
              )}
            </div>
          )}

          {/* Payment Schedule Table */}
          {result?.schedule && result.schedule.length > 0 && (
            <div className="bg-white dark:bg-slate-900 rounded-2xl p-6 sm:p-7 shadow-xs border border-slate-200 dark:border-slate-800 transition-colors">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <Table size={20} className="text-blue-600 dark:text-blue-400" />
                  {t('scheduleTitle')} ({result.currency || currency})
                </h3>
                <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">
                  {t('totalPayments')}: {result.schedule.length}
                </span>
              </div>

              <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-slate-800">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-slate-50 dark:bg-slate-950 text-slate-600 dark:text-slate-400 font-bold border-b border-slate-200 dark:border-slate-800">
                      <th className="p-3 text-center">№</th>
                      <th className="p-3">{t('tableDate')}</th>
                      <th className="p-3 text-right">{t('tableRemainingDebt')}</th>
                      <th className="p-3 text-right">{t('tablePrincipalPart')}</th>
                      <th className="p-3 text-right">{t('tableInterestPart')}</th>
                      <th className="p-3 text-right">{t('tableEarlyRepayment')}</th>
                      <th className="p-3 text-right">{t('tableMonthlyPayment')}</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {paginatedSchedule.map((row) => (
                      <tr key={row.monthNumber} className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition-colors">
                        <td className="p-3 text-center font-medium text-slate-400 dark:text-slate-500">
                          {row.monthNumber}
                        </td>
                        <td className="p-3 font-medium text-slate-700 dark:text-slate-300 whitespace-nowrap">
                          {new Date(row.paymentDate).toLocaleDateString(currency === 'RUB' ? 'ru-RU' : 'en-US', {
                            month: 'short',
                            year: 'numeric',
                          })}
                        </td>
                        <td className="p-3 text-right font-medium text-slate-600 dark:text-slate-300 whitespace-nowrap">
                          {formatCurrency(row.openingBalance, result.currency || currency)}
                        </td>
                        <td className="p-3 text-right font-semibold text-slate-800 dark:text-slate-200 whitespace-nowrap">
                          {formatCurrency(row.principalPayment, result.currency || currency)}
                        </td>
                        <td className="p-3 text-right text-red-600 dark:text-red-400 font-medium whitespace-nowrap">
                          {formatCurrency(row.interestPayment, result.currency || currency)}
                        </td>
                        <td className="p-3 text-right text-emerald-600 dark:text-emerald-400 font-semibold whitespace-nowrap">
                          {row.earlyPayment > 0
                            ? formatCurrency(row.earlyPayment, result.currency || currency)
                            : '—'}
                        </td>
                        <td className="p-3 text-right font-bold text-blue-700 dark:text-blue-400 whitespace-nowrap">
                          {formatCurrency(row.totalMonthlyPayment, result.currency || currency)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Schedule Pagination Controls */}
              {totalPages > 1 && (
                <div className="flex items-center justify-between pt-4 mt-2">
                  <button
                    disabled={currentPage === 1}
                    onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                    className="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-800 text-xs font-semibold text-slate-600 dark:text-slate-400 disabled:opacity-40 hover:bg-slate-50 dark:hover:bg-slate-800 cursor-pointer"
                  >
                    {t('prevYear')} ({t('year')} {currentPage - 1})
                  </button>
                  <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">
                    {t('year')} {currentPage} {t('of')} {totalPages}
                  </span>
                  <button
                    disabled={currentPage === totalPages}
                    onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                    className="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-800 text-xs font-semibold text-slate-600 dark:text-slate-400 disabled:opacity-40 hover:bg-slate-50 dark:hover:bg-slate-800 cursor-pointer"
                  >
                    {t('nextYear')} ({t('year')} {currentPage + 1})
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Add Early Repayment Modal */}
      {showEarlyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in">
          <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-xl w-full max-w-md p-6 border border-slate-200 dark:border-slate-800 text-slate-900 dark:text-white transition-colors">
            <h3 className="text-lg font-bold mb-4">{t('addEarlyModalTitle')}</h3>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase mb-1">
                  {t('repaymentType')}
                </label>
                <div className="grid grid-cols-2 gap-2 p-1 bg-slate-100 dark:bg-slate-950 rounded-xl border border-transparent dark:border-slate-800 text-xs font-semibold">
                  <button
                    type="button"
                    onClick={() => setErType('ONE_TIME')}
                    className={`py-2 rounded-lg cursor-pointer ${
                      erType === 'ONE_TIME'
                        ? 'bg-white dark:bg-slate-800 text-blue-700 dark:text-blue-300 shadow-xs'
                        : 'text-slate-600 dark:text-slate-400'
                    }`}
                  >
                    {t('oneTime')}
                  </button>
                  <button
                    type="button"
                    onClick={() => setErType('MONTHLY')}
                    className={`py-2 rounded-lg cursor-pointer ${
                      erType === 'MONTHLY'
                        ? 'bg-white dark:bg-slate-800 text-blue-700 dark:text-blue-300 shadow-xs'
                        : 'text-slate-600 dark:text-slate-400'
                    }`}
                  >
                    {t('monthly')}
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase mb-1">
                  {t('repaymentAmount')} ({getCurrencySymbol(currency)})
                </label>
                <input
                  type="number"
                  min="100"
                  step={currency === 'USD' || currency === 'EUR' ? '500' : '5000'}
                  value={erAmount}
                  onChange={(e) => setErAmount(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm font-medium focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase mb-1">
                    {erType === 'ONE_TIME' ? t('repaymentMonth') : t('fromMonth')}
                  </label>
                  <input
                    type="number"
                    min="1"
                    max={termYears * 12}
                    value={erStartMonth}
                    onChange={(e) => setErStartMonth(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm font-medium focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
                  />
                </div>

                {erType === 'MONTHLY' && (
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase mb-1">
                      {t('toMonth')}
                    </label>
                    <input
                      type="number"
                      min={erStartMonth}
                      max={termYears * 12}
                      value={erEndMonth || ''}
                      placeholder={t('untilEnd')}
                      onChange={(e) =>
                        setErEndMonth(e.target.value ? Number(e.target.value) : undefined)
                      }
                      className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm font-medium focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
                    />
                  </div>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase mb-1">
                  {t('earlyStrategy')}
                </label>
                <select
                  value={erStrategy}
                  onChange={(e) => setErStrategy(e.target.value as RepaymentStrategy)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm font-medium focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
                >
                  <option value="REDUCE_TERM">{t('reduceTermDesc')}</option>
                  <option value="REDUCE_PAYMENT">{t('reducePaymentDesc')}</option>
                </select>
              </div>

              <div className="flex gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setShowEarlyModal(false)}
                  className="flex-1 py-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-semibold text-sm rounded-xl transition-colors cursor-pointer"
                >
                  {t('cancel')}
                </button>
                <button
                  type="button"
                  onClick={addEarlyRepayment}
                  className="flex-1 py-2.5 bg-blue-700 hover:bg-blue-800 text-white font-semibold text-sm rounded-xl shadow-md transition-all cursor-pointer"
                >
                  {t('add')}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Save Scenario Modal */}
      {saveModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in">
          <div className="bg-white dark:bg-slate-900 rounded-2xl shadow-xl w-full max-w-md p-6 border border-slate-200 dark:border-slate-800 text-slate-900 dark:text-white transition-colors">
            <h3 className="text-lg font-bold mb-2">{t('saveScenarioModalTitle')}</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mb-4">
              {t('saveScenarioModalDesc')}
            </p>

            {saveSuccess ? (
              <div className="p-4 bg-emerald-50 dark:bg-emerald-950/40 text-emerald-800 dark:text-emerald-200 rounded-xl flex items-center gap-2 text-sm font-semibold border border-emerald-200 dark:border-emerald-800">
                <CheckCircle size={18} className="text-emerald-600 dark:text-emerald-400" />
                <span>{t('scenarioSavedSuccess')}</span>
              </div>
            ) : (
              <form onSubmit={handleSaveScenario} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 uppercase mb-1">
                    {t('scenarioName')}
                  </label>
                  <input
                    type="text"
                    required
                    value={scenarioTitle}
                    onChange={(e) => setScenarioTitle(e.target.value)}
                    placeholder={t('scenarioPlaceholder').replace('{currency}', currency)}
                    className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm font-medium focus:ring-2 focus:ring-blue-500 text-slate-900 dark:text-white"
                  />
                </div>

                <div className="flex gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setSaveModalOpen(false)}
                    className="flex-1 py-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-semibold text-sm rounded-xl transition-colors cursor-pointer"
                  >
                    {t('cancel')}
                  </button>
                  <button
                    type="submit"
                    disabled={saving}
                    className="flex-1 py-2.5 bg-blue-700 hover:bg-blue-800 text-white font-semibold text-sm rounded-xl shadow-md transition-all cursor-pointer disabled:opacity-70"
                  >
                    {saving ? t('saving') : t('save')}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
