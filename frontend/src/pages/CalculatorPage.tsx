import React, { useState, useEffect } from 'react';
import { calculatorApi, scenarioApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
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
} from 'lucide-react';
import { CURRENCY_CONFIGS, formatCurrency, getCurrencySymbol } from '../utils/format';

interface CalculatorPageProps {
  presetOffer?: BankOffer | null;
  onClearPreset?: () => void;
  onOpenAuth: () => void;
}

const MORTGAGE_JOKES = [
  '«Проценты не главное — главное, чтобы в квартире было уютно котику» 🐾',
  '«Ипотека — это когда банк верит в ваше будущее сильнее, чем вы сами» 🤝',
  '«Берёшь чужие и на пару лет, отдаёшь свои и 30 лет» ⏳',
  '«Проценты не главное — зато своя стена, в которую можно забить гвоздь в 8 утра» 🔨',
  '«Вторая квартира в подарок... любимому банку!» 🎁',
  '«Каждая досрочка — маленькая слеза кредитного менеджера» 🥲',
  '«Ипотека дисциплинирует: сразу понимаешь, зачем ходить на работу каждый день» 💼',
  '«Проценты не главное — главное, чтобы доширак не дорожал!» 🍜',
  '«Собственная жилплощадь: теперь ты не бездомный, ты — ипотечник!» 🏡',
];

export const CalculatorPage: React.FC<CalculatorPageProps> = ({
  presetOffer,
  onClearPreset,
  onOpenAuth,
}) => {
  const { user } = useAuth();

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

  // Humor ticker
  const [jokeIndex, setJokeIndex] = useState<number>(0);
  const nextJoke = () => setJokeIndex((prev) => (prev + 1) % MORTGAGE_JOKES.length);

  // Run calculation
  const calculateMortgage = async (overrideParams?: {
    price?: number;
    dp?: number;
    termY?: number;
    rate?: number;
    pType?: PaymentType;
    curr?: Currency;
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

    const req: CalculationRequest = {
      propertyPrice: targetPrice,
      downPayment: targetDp,
      termMonths: targetTermY * 12,
      annualInterestRate: targetRate,
      paymentType: targetPType,
      currency: targetCurr,
      earlyRepayments: earlyRepayments.map((er) => ({
        startMonth: Number(er.startMonth),
        endMonth: er.type === 'MONTHLY' && er.endMonth ? Number(er.endMonth) : null,
        amount: Number(er.amount),
        strategy: er.strategy,
        type: er.type,
      })),
    };

    try {
      const res = await calculatorApi.calculate(req);
      setResult(res);
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(
        axiosError.response?.data?.message ||
          'Ошибка при расчете ипотеки. Проверьте правильность введенных данных.'
      );
    } finally {
      setLoading(false);
    }
  };

  // Currency change handler with sensible defaults adaptation & bank preservation
  const handleCurrencyChange = (newCurrency: Currency) => {
    setCurrency(newCurrency);
    const cfg = CURRENCY_CONFIGS.find((c) => c.code === newCurrency);
    if (cfg) {
      const newPrice = cfg.defaultPrice;
      const effectiveDpPercent = presetOffer?.minDownPaymentPercent ?? downPaymentPercent;
      const newDp = Math.round((newPrice * effectiveDpPercent) / 100);

      // If a bank offer is currently active, preserve its interest rate!
      // Otherwise, use sensible market rates for that currency:
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

      // Immediately run calculation with fresh parameters so calculations apply instantly
      calculateMortgage({
        price: newPrice,
        dp: newDp,
        termY: termYears,
        rate: newRate,
        curr: newCurrency,
        pType: paymentType,
      });
    }
  };

  // Apply bank offer preset if passed
  useEffect(() => {
    if (presetOffer) {
      const targetCurr = presetOffer.currency || currency;
      const cfg = CURRENCY_CONFIGS.find((c) => c.code === targetCurr);
      // Adapt property price to currency defaults if currency changes
      const nextPrice = (targetCurr !== currency && cfg) ? cfg.defaultPrice : propertyPrice;
      const nextRate = presetOffer.annualInterestRate;
      const nextTermYears = presetOffer.maxTermMonths
        ? Math.min(30, Math.floor(presetOffer.maxTermMonths / 12))
        : termYears;
      const nextDpPercent = presetOffer.minDownPaymentPercent || 20;
      const nextDp = Math.round((nextPrice * nextDpPercent) / 100);

      setCurrency(targetCurr);
      setAnnualRate(nextRate);
      setTermYears(nextTermYears);
      setPropertyPrice(nextPrice);
      setDownPayment(nextDp);
      setDownPaymentPercent(nextDpPercent);

      calculateMortgage({
        price: nextPrice,
        dp: nextDp,
        termY: nextTermYears,
        rate: nextRate,
        curr: targetCurr,
        pType: paymentType,
      });
    }
  }, [presetOffer]);

  // Sync down payment and percentage
  const handlePriceChange = (newPrice: number) => {
    setPropertyPrice(newPrice);
    const newDp = Math.round((newPrice * downPaymentPercent) / 100);
    setDownPayment(newDp);
  };

  const handleDpChange = (newDp: number) => {
    setDownPayment(newDp);
    if (propertyPrice > 0) {
      setDownPaymentPercent(Math.round((newDp / propertyPrice) * 100));
    }
  };

  const handleDpPercentChange = (newPercent: number) => {
    setDownPaymentPercent(newPercent);
    const newDp = Math.round((propertyPrice * newPercent) / 100);
    setDownPayment(newDp);
  };

  // Run calculation on initial mount only (subsequent changes use explicit calculation triggers)
  const isInitialMount = React.useRef(true);
  useEffect(() => {
    if (isInitialMount.current) {
      isInitialMount.current = false;
      if (!presetOffer) {
        calculateMortgage();
      }
    }
  }, []);

  const addEarlyRepayment = () => {
    const newEr: EarlyRepayment = {
      id: Math.random().toString(36).substring(7),
      startMonth: erStartMonth,
      endMonth: erType === 'MONTHLY' ? erEndMonth : null,
      amount: erAmount,
      type: erType,
      strategy: erStrategy,
    };
    setEarlyRepayments([...earlyRepayments, newEr]);
    setShowEarlyModal(false);
  };

  const removeEarlyRepayment = (index: number) => {
    setEarlyRepayments(earlyRepayments.filter((_, i) => i !== index));
  };

  const handleSaveScenario = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) {
      onOpenAuth();
      return;
    }
    setSaving(true);
    try {
      await scenarioApi.save({
        title: scenarioTitle || `Сценарий (${currency}) ${new Date().toLocaleDateString('ru-RU')}`,
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
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || 'Не удалось сохранить сценарий.');
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
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Preset Offer Banner if any */}
      {presetOffer && (
        <div className="mb-6 bg-gradient-to-r from-indigo-600 via-indigo-700 to-purple-700 text-white p-5 rounded-3xl shadow-lg shadow-indigo-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4 animate-in fade-in">
          <div>
            <div className="flex flex-wrap items-center gap-2 mb-1.5">
              <span className="text-[11px] uppercase tracking-wider font-extrabold bg-white/20 px-2.5 py-0.5 rounded-lg">
                Выбран банк
              </span>
              <span className="text-[11px] font-extrabold bg-amber-400 text-amber-950 px-2.5 py-0.5 rounded-lg flex items-center gap-1 shadow-xs">
                <span>{currency}</span>
                <span>{getCurrencySymbol(currency)}</span>
              </span>
              <span className="text-xs text-indigo-100 italic">
                «Проценты не главное — главное, чтобы банк одобрил!» ✨
              </span>
            </div>
            <h3 className="text-lg font-black tracking-tight">
              {presetOffer.bankName} — {presetOffer.offerName} ({presetOffer.annualInterestRate}%)
            </h3>
            <p className="text-xs text-indigo-200 mt-0.5">
              Условия банка активны. Вы можете свободно переключать валюту ({currency}), сумму и срок кредита.
            </p>
          </div>
          {onClearPreset && (
            <button
              onClick={onClearPreset}
              className="text-xs bg-white/20 hover:bg-white/30 active:bg-white/40 text-white font-bold px-4 py-2 rounded-xl transition-all cursor-pointer self-start sm:self-center shrink-0"
            >
              Сбросить банк
            </button>
          )}
        </div>
      )}

      {/* Humorous Quote / Easter Egg Banner */}
      <div className="mb-6 bg-amber-50/90 border border-amber-200/80 rounded-2xl px-4 py-3 flex items-center justify-between gap-3 text-xs text-amber-900 shadow-xs">
        <div className="flex items-center gap-2.5 min-w-0">
          <span className="text-base select-none shrink-0">💡</span>
          <span className="font-semibold italic truncate sm:overflow-visible sm:whitespace-normal">
            {MORTGAGE_JOKES[jokeIndex]}
          </span>
        </div>
        <button
          type="button"
          onClick={nextJoke}
          className="text-[11px] font-bold text-amber-800 hover:text-amber-950 bg-amber-200/60 hover:bg-amber-200 px-2.5 py-1 rounded-lg transition-colors cursor-pointer shrink-0"
          title="Следующая народная мудрость"
        >
          Ещё шутка 😄
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column: Calculator Parameters Form */}
        <div className="lg:col-span-5 space-y-6">
          <div className="bg-white rounded-3xl p-6 sm:p-7 shadow-sm border border-slate-200">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
                <span className="w-2.5 h-6 bg-indigo-600 rounded-full inline-block" />
                Параметры ипотеки
              </h2>
            </div>

            {/* Currency Selector Bar */}
            <div className="mb-6 p-1.5 bg-slate-100 rounded-2xl">
              <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider px-2 py-1 flex items-center gap-1.5">
                <Coins size={14} className="text-indigo-600" />
                Валюта расчета
              </div>
              <div className="grid grid-cols-4 gap-1.5 mt-1">
                {CURRENCY_CONFIGS.map((cfg) => {
                  const isSelected = currency === cfg.code;
                  return (
                    <button
                      key={cfg.code}
                      type="button"
                      onClick={() => handleCurrencyChange(cfg.code)}
                      className={`py-2 px-1 rounded-xl text-xs font-bold transition-all cursor-pointer flex flex-col items-center justify-center ${
                        isSelected
                          ? 'bg-indigo-600 text-white shadow-md shadow-indigo-100 scale-102'
                          : 'bg-white text-slate-700 hover:bg-slate-50 border border-slate-200/60'
                      }`}
                    >
                      <span className="text-sm font-black">{cfg.symbol}</span>
                      <span className="text-[10px] tracking-tight">{cfg.code}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="space-y-5">
              {/* Property Price */}
              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <label className="text-sm font-semibold text-slate-700">
                    Стоимость недвижимости
                  </label>
                  <span className="text-sm font-bold text-indigo-600">
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
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white text-sm"
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
                  className="w-full mt-2 accent-indigo-600 cursor-pointer"
                />
              </div>

              {/* Down Payment */}
              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <label className="text-sm font-semibold text-slate-700">Первоначальный взнос</label>
                  <div className="flex items-center gap-2 text-sm font-bold text-indigo-600">
                    <span>{formatCurrency(downPayment, currency)}</span>
                    <span className="text-xs bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-full">
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
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white text-sm"
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
                      className="w-full pl-3 pr-7 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white text-sm text-center"
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
                  className="w-full mt-2 accent-indigo-600 cursor-pointer"
                />
              </div>

              {/* Term & Rate */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">
                    Срок кредита
                  </label>
                  <div className="relative">
                    <input
                      type="number"
                      min="1"
                      max="30"
                      value={termYears}
                      onChange={(e) => setTermYears(Number(e.target.value))}
                      className="w-full pl-3 pr-10 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white text-sm"
                    />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 font-medium">
                      лет
                    </span>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">
                    Ставка (%)
                  </label>
                  <div className="relative">
                    <input
                      type="number"
                      step="0.1"
                      min="0.1"
                      max="40"
                      value={annualRate}
                      onChange={(e) => setAnnualRate(Number(e.target.value))}
                      className="w-full pl-3 pr-8 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-slate-800 font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white text-sm"
                    />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-slate-400 font-medium">
                      %
                    </span>
                  </div>
                </div>
              </div>

              {/* Payment Type */}
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">
                  Тип платежей
                </label>
                <div className="grid grid-cols-2 gap-2 p-1 bg-slate-100 rounded-xl text-xs font-semibold">
                  <button
                    type="button"
                    onClick={() => setPaymentType('ANNUITY')}
                    className={`py-2 rounded-lg transition-all cursor-pointer ${
                      paymentType === 'ANNUITY'
                        ? 'bg-white text-indigo-600 shadow-sm font-bold'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    Аннуитетный
                  </button>
                  <button
                    type="button"
                    onClick={() => setPaymentType('DIFFERENTIATED')}
                    className={`py-2 rounded-lg transition-all cursor-pointer ${
                      paymentType === 'DIFFERENTIATED'
                        ? 'bg-white text-indigo-600 shadow-sm font-bold'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    Дифференцированный
                  </button>
                </div>
              </div>

              {/* Early Repayments Header & List */}
              <div className="pt-2 border-t border-slate-100">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-1.5">
                    <span className="text-sm font-bold text-slate-800">Досрочные погашения</span>
                    <span className="text-xs bg-slate-100 text-slate-600 px-2 py-0.5 rounded-full font-semibold">
                      {earlyRepayments.length}
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setShowEarlyModal(true)}
                    className="flex items-center gap-1 text-xs text-indigo-600 hover:text-indigo-700 font-semibold cursor-pointer"
                  >
                    <Plus size={14} />
                    Добавить
                  </button>
                </div>

                {earlyRepayments.length > 0 ? (
                  <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                    {earlyRepayments.map((er, idx) => (
                      <div
                        key={idx}
                        className="flex items-center justify-between p-2.5 bg-slate-50 rounded-xl border border-slate-100 text-xs"
                      >
                        <div>
                          <div className="font-semibold text-slate-800">
                            {formatCurrency(er.amount, currency)}
                            <span className="ml-1.5 font-normal text-slate-500">
                              {er.type === 'ONE_TIME'
                                ? `в ${er.startMonth} мес.`
                                : `с ${er.startMonth} по ${er.endMonth || 'конец'} мес.`}
                            </span>
                          </div>
                          <div className="text-[11px] text-slate-500 mt-0.5">
                            {er.strategy === 'REDUCE_TERM'
                              ? 'Сокращение срока'
                              : 'Уменьшение платежа'}
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => removeEarlyRepayment(idx)}
                          className="p-1 text-slate-400 hover:text-red-500 rounded-lg hover:bg-red-50 transition-colors cursor-pointer"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-slate-400 italic">
                    Каждая досрочка — это слёзы банкира 🥲 Добавьте платёж, чтобы банк не расслаблялся!
                  </p>
                )}
              </div>

              {/* Submit calculate */}
              <button
                type="button"
                onClick={() => calculateMortgage()}
                disabled={loading}
                className="w-full py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-2xl shadow-lg shadow-indigo-100 transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-70"
              >
                {loading ? (
                  <span className="inline-block animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent" />
                ) : (
                  <span>
                    Рассчитать ипотеку ({getCurrencySymbol(currency)}){' '}
                    <span className="text-xs font-normal opacity-85">— Проценты не главное! 😉</span>
                  </span>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Right Column: Results Summary & Payment Schedule */}
        <div className="lg:col-span-7 space-y-6">
          {error && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-2xl text-red-700 text-sm flex items-start gap-3">
              <AlertCircle size={20} className="shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {/* Results Summary Cards */}
          {result && (
            <div className="bg-white rounded-3xl p-6 sm:p-7 shadow-sm border border-slate-200">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 border-b border-slate-100 gap-4">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                      Ежемесячный платеж
                    </span>
                    <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700">
                      {result.currency || currency}
                    </span>
                  </div>
                  <div className="text-3xl sm:text-4xl font-extrabold text-slate-900 mt-1">
                    {paymentType === 'ANNUITY' ? (
                      formatCurrency(result.monthlyPayment, result.currency || currency)
                    ) : (
                      <span>
                        {formatCurrency(result.minMonthlyPayment, result.currency || currency)} ...{' '}
                        {formatCurrency(result.maxMonthlyPayment, result.currency || currency)}
                      </span>
                    )}
                  </div>
                  <span className="text-xs text-slate-400 font-medium block mt-1">
                    «Зато своя!» — шепчет кошелёк 💳
                  </span>
                </div>

                <div className="flex gap-2">
                  <button
                    onClick={() => setSaveModalOpen(true)}
                    className="flex items-center gap-1.5 px-4 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-sm font-semibold rounded-xl transition-colors cursor-pointer"
                  >
                    <BookmarkPlus size={18} />
                    Сохранить сценарий
                  </button>
                </div>
              </div>

              {/* Grid Metrics */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-6">
                <div className="p-3 bg-slate-50 rounded-2xl border border-slate-100 flex flex-col justify-between">
                  <div>
                    <div className="text-xs text-slate-500 font-medium">Сумма кредита</div>
                    <div className="text-base font-bold text-slate-800 mt-1">
                      {formatCurrency(result.loanAmount, result.currency || currency)}
                    </div>
                  </div>
                  <span className="text-[10px] text-slate-400 font-medium mt-1">
                    🏠 Собственное гнёздышко
                  </span>
                </div>

                <div className="p-3 bg-slate-50 rounded-2xl border border-slate-100 flex flex-col justify-between">
                  <div>
                    <div className="text-xs text-slate-500 font-medium">Переплата по %</div>
                    <div className="text-base font-bold text-amber-600 mt-1">
                      {formatCurrency(result.totalInterest, result.currency || currency)}
                    </div>
                  </div>
                  <span className="text-[10px] text-amber-600 font-medium mt-1">
                    🎁 Вторая квартира банку
                  </span>
                </div>

                <div className="p-3 bg-slate-50 rounded-2xl border border-slate-100 flex flex-col justify-between">
                  <div>
                    <div className="text-xs text-slate-500 font-medium">Всего выплат</div>
                    <div className="text-base font-bold text-slate-800 mt-1">
                      {formatCurrency(result.totalPayment, result.currency || currency)}
                    </div>
                  </div>
                  <span className="text-[10px] text-slate-500 font-medium mt-1">
                    📊 Проценты не главное!
                  </span>
                </div>

                <div className="p-3 bg-slate-50 rounded-2xl border border-slate-100 flex flex-col justify-between">
                  <div>
                    <div className="text-xs text-slate-500 font-medium">Фактический срок</div>
                    <div className="text-base font-bold text-indigo-600 mt-1">
                      {Math.floor(result.actualTermMonths / 12)} г. {result.actualTermMonths % 12} мес.
                    </div>
                  </div>
                  <span className="text-[10px] text-indigo-600 font-medium mt-1">
                    🏃 Длиннее марафона
                  </span>
                </div>
              </div>

              {/* Savings hint if term was reduced */}
              {result.actualTermMonths < result.termMonths && (
                <div className="mt-4 p-3 bg-emerald-50 border border-emerald-100 rounded-xl flex items-center gap-2 text-xs text-emerald-800 font-medium">
                  <TrendingDown size={16} className="text-emerald-600 shrink-0" />
                  <span>
                    Благодаря досрочным погашениям срок сократился на{' '}
                    <span className="font-bold">
                      {Math.floor((result.termMonths - result.actualTermMonths) / 12)} лет{' '}
                      {(result.termMonths - result.actualTermMonths) % 12} мес.
                    </span>
                  </span>
                </div>
              )}
            </div>
          )}

          {/* Payment Schedule Table */}
          {result?.schedule && result.schedule.length > 0 && (
            <div className="bg-white rounded-3xl p-6 sm:p-7 shadow-sm border border-slate-200">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                  <Table size={20} className="text-indigo-600" />
                  График платежей ({result.currency || currency})
                </h3>
                <span className="text-xs font-semibold text-slate-500">
                  Всего платежей: {result.schedule.length}
                </span>
              </div>

              <div className="overflow-x-auto rounded-xl border border-slate-100">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-slate-50 text-slate-600 font-bold border-b border-slate-100">
                      <th className="p-3 text-center">№</th>
                      <th className="p-3">Дата</th>
                      <th className="p-3 text-right">Остаток долга</th>
                      <th className="p-3 text-right">Основной долг</th>
                      <th className="p-3 text-right">Проценты</th>
                      <th className="p-3 text-right">Досрочно</th>
                      <th className="p-3 text-right">Платеж</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {paginatedSchedule.map((row) => (
                      <tr key={row.monthNumber} className="hover:bg-slate-50/70 transition-colors">
                        <td className="p-3 text-center font-medium text-slate-400">
                          {row.monthNumber}
                        </td>
                        <td className="p-3 font-medium text-slate-700 whitespace-nowrap">
                          {new Date(row.paymentDate).toLocaleDateString('ru-RU', {
                            month: 'short',
                            year: 'numeric',
                          })}
                        </td>
                        <td className="p-3 text-right font-medium text-slate-600 whitespace-nowrap">
                          {formatCurrency(row.openingBalance, result.currency || currency)}
                        </td>
                        <td className="p-3 text-right font-semibold text-slate-800 whitespace-nowrap">
                          {formatCurrency(row.principalPayment, result.currency || currency)}
                        </td>
                        <td className="p-3 text-right text-amber-600 font-medium whitespace-nowrap">
                          {formatCurrency(row.interestPayment, result.currency || currency)}
                        </td>
                        <td className="p-3 text-right text-emerald-600 font-semibold whitespace-nowrap">
                          {row.earlyPayment > 0
                            ? formatCurrency(row.earlyPayment, result.currency || currency)
                            : '—'}
                        </td>
                        <td className="p-3 text-right font-bold text-indigo-600 whitespace-nowrap">
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
                    className="px-3 py-1.5 rounded-lg border border-slate-200 text-xs font-semibold text-slate-600 disabled:opacity-40 hover:bg-slate-50 cursor-pointer"
                  >
                    Назад (год {currentPage - 1})
                  </button>
                  <span className="text-xs text-slate-500 font-medium">
                    Год {currentPage} из {totalPages}
                  </span>
                  <button
                    disabled={currentPage === totalPages}
                    onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                    className="px-3 py-1.5 rounded-lg border border-slate-200 text-xs font-semibold text-slate-600 disabled:opacity-40 hover:bg-slate-50 cursor-pointer"
                  >
                    Вперед (год {currentPage + 1})
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Add Early Repayment Modal */}
      {showEarlyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 border border-slate-100">
            <h3 className="text-lg font-bold text-slate-900 mb-4">Добавить досрочное погашение</h3>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                  Тип погашения
                </label>
                <div className="grid grid-cols-2 gap-2 p-1 bg-slate-100 rounded-xl text-xs font-semibold">
                  <button
                    type="button"
                    onClick={() => setErType('ONE_TIME')}
                    className={`py-2 rounded-lg cursor-pointer ${
                      erType === 'ONE_TIME' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-600'
                    }`}
                  >
                    Разовое
                  </button>
                  <button
                    type="button"
                    onClick={() => setErType('MONTHLY')}
                    className={`py-2 rounded-lg cursor-pointer ${
                      erType === 'MONTHLY' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-600'
                    }`}
                  >
                    Ежемесячное
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                  Сумма погашения ({getCurrencySymbol(currency)})
                </label>
                <input
                  type="number"
                  min="100"
                  step={currency === 'USD' || currency === 'EUR' ? '500' : '5000'}
                  value={erAmount}
                  onChange={(e) => setErAmount(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                    {erType === 'ONE_TIME' ? 'Месяц выплаты' : 'С месяца'}
                  </label>
                  <input
                    type="number"
                    min="1"
                    max={termYears * 12}
                    value={erStartMonth}
                    onChange={(e) => setErStartMonth(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                  />
                </div>

                {erType === 'MONTHLY' && (
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                      По месяц (опционально)
                    </label>
                    <input
                      type="number"
                      min={erStartMonth}
                      max={termYears * 12}
                      value={erEndMonth || ''}
                      placeholder="До конца"
                      onChange={(e) =>
                        setErEndMonth(e.target.value ? Number(e.target.value) : undefined)
                      }
                      className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                    />
                  </div>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                  Стратегия досрочки
                </label>
                <select
                  value={erStrategy}
                  onChange={(e) => setErStrategy(e.target.value as RepaymentStrategy)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                >
                  <option value="REDUCE_TERM">Сократить срок кредита (меньше переплата)</option>
                  <option value="REDUCE_PAYMENT">
                    Уменьшить ежемесячный платеж (меньше нагрузка)
                  </option>
                </select>
              </div>

              <div className="flex gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setShowEarlyModal(false)}
                  className="flex-1 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-sm rounded-xl transition-colors cursor-pointer"
                >
                  Отмена
                </button>
                <button
                  type="button"
                  onClick={addEarlyRepayment}
                  className="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-xl shadow-md transition-all cursor-pointer"
                >
                  Добавить
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Save Scenario Modal */}
      {saveModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 border border-slate-100">
            <h3 className="text-lg font-bold text-slate-900 mb-2">Сохранить сценарий</h3>
            <p className="text-xs text-slate-500 mb-4">
              Сценарий будет сохранен с выбранной валютой ({currency}) и доступен для сравнения и
              экспорта в PDF.
            </p>

            {saveSuccess ? (
              <div className="p-4 bg-emerald-50 text-emerald-800 rounded-xl flex items-center gap-2 text-sm font-semibold">
                <CheckCircle size={18} className="text-emerald-600" />
                <span>Сценарий успешно сохранен!</span>
              </div>
            ) : (
              <form onSubmit={handleSaveScenario} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                    Название сценария
                  </label>
                  <input
                    type="text"
                    required
                    value={scenarioTitle}
                    onChange={(e) => setScenarioTitle(e.target.value)}
                    placeholder={`Например: 2-к кв (${currency}) на 20 лет`}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                  />
                </div>

                <div className="flex gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setSaveModalOpen(false)}
                    className="flex-1 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-sm rounded-xl transition-colors cursor-pointer"
                  >
                    Отмена
                  </button>
                  <button
                    type="submit"
                    disabled={saving}
                    className="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-xl shadow-md transition-all cursor-pointer disabled:opacity-70"
                  >
                    {saving ? 'Сохранение...' : 'Сохранить'}
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
