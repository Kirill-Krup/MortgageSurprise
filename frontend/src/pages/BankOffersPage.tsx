import React, { useState, useEffect } from 'react';
import { bankOfferApi, calculatorApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import type { TranslationKey } from '../i18n/translations';
import type { BankOffer, BankOfferRequest, Currency } from '../types';
import { CURRENCY_CONFIGS, formatCurrency, getCurrencySymbol } from '../utils/format';
import {
  Building2,
  Percent,
  Calendar,
  Wallet,
  ArrowRight,
  Plus,
  Pencil,
  Trash2,
  AlertCircle,
  X,
  Coins,
  Sliders,
  CheckCircle2,
  ShieldCheck,
} from 'lucide-react';

interface BankOffersPageProps {
  onSelectOffer: (offer: BankOffer) => void;
}

const getBankDescription = (bankName: string, t: (k: TranslationKey) => string): string => {
  if (bankName.includes('Сбер')) return t('bankDescSber');
  if (bankName.includes('ВТБ') || bankName.includes('VTB')) return t('bankDescVtb');
  if (bankName.includes('Альфа') || bankName.includes('Alfa')) return t('bankDescAlfa');
  if (bankName.includes('Т-Банк') || bankName.includes('T-Bank')) return t('bankDescTbank');
  if (bankName.includes('Газпром') || bankName.includes('Gazprom')) return t('bankDescGpb');
  if (bankName.includes('ДОМ.РФ') || bankName.includes('DOM.RF')) return t('bankDescDomRf');
  return t('officialProgramDesc');
};

const formatLoanLimit = (
  minAmount?: number | null,
  maxAmount?: number | null,
  offerCurrency: Currency = 'RUB',
  targetCurrency: 'ALL' | Currency = 'ALL',
  fromWord: string = 'от',
  upToWord: string = 'до'
): string => {
  const displayCurr: Currency = targetCurrency === 'ALL' ? offerCurrency : targetCurrency;

  const scale = (val?: number | null): number | null => {
    if (!val) return null;
    if (offerCurrency === displayCurr) return val;
    const toRub =
      offerCurrency === 'USD'
        ? val * 100
        : offerCurrency === 'EUR'
        ? val * 105
        : offerCurrency === 'BYN'
        ? val * 30
        : val;

    if (displayCurr === 'USD') return Math.round(toRub / 100 / 1000) * 1000;
    if (displayCurr === 'EUR') return Math.round(toRub / 105 / 1000) * 1000;
    if (displayCurr === 'BYN') return Math.round(toRub / 30 / 1000) * 1000;
    return Math.round(toRub / 10000) * 10000;
  };

  const scaledMin = scale(minAmount);
  const scaledMax = scale(maxAmount);

  const parts = [];
  if (scaledMin) parts.push(`${fromWord} ${formatCurrency(scaledMin, displayCurr)}`);
  if (scaledMax) parts.push(`${upToWord} ${formatCurrency(scaledMax, displayCurr)}`);
  return parts.join(' ');
};

const DEFAULT_PLACEHOLDER_OFFERS: BankOffer[] = [
  {
    id: 'placeholder-sber-1',
    bankName: 'СберБанк',
    offerName: 'Ипотека на готовое жилье (Вторичный рынок)',
    annualInterestRate: 18.5,
    minDownPaymentPercent: 15.0,
    maxTermMonths: 360,
    minLoanAmount: 300000,
    maxLoanAmount: 100000000,
    currency: 'RUB',
    active: true,
  },
  {
    id: 'placeholder-sber-family',
    bankName: 'СберБанк',
    offerName: 'Семейная ипотека с господдержкой',
    annualInterestRate: 6.0,
    minDownPaymentPercent: 20.0,
    maxTermMonths: 360,
    minLoanAmount: 500000,
    maxLoanAmount: 12000000,
    currency: 'RUB',
    active: true,
  },
  {
    id: 'placeholder-vtb-1',
    bankName: 'ВТБ',
    offerName: 'Ипотека на новостройку',
    annualInterestRate: 19.2,
    minDownPaymentPercent: 20.0,
    maxTermMonths: 360,
    minLoanAmount: 500000,
    maxLoanAmount: 60000000,
    currency: 'RUB',
    active: true,
  },
  {
    id: 'placeholder-alfa-1',
    bankName: 'Альфа-Банк',
    offerName: 'Льготная ипотека для IT-специалистов',
    annualInterestRate: 5.0,
    minDownPaymentPercent: 20.1,
    maxTermMonths: 360,
    minLoanAmount: 1000000,
    maxLoanAmount: 18000000,
    currency: 'RUB',
    active: true,
  },
  {
    id: 'placeholder-tbank-1',
    bankName: 'Т-Банк',
    offerName: 'Ипотека под залог недвижимости',
    annualInterestRate: 21.0,
    minDownPaymentPercent: 0.0,
    maxTermMonths: 180,
    minLoanAmount: 500000,
    maxLoanAmount: 30000000,
    currency: 'RUB',
    active: true,
  },
  {
    id: 'placeholder-domrf-1',
    bankName: 'Банк ДОМ.РФ',
    offerName: 'Строительство жилого дома (ИЖС)',
    annualInterestRate: 18.9,
    minDownPaymentPercent: 20.0,
    maxTermMonths: 360,
    minLoanAmount: 500000,
    maxLoanAmount: 50000000,
    currency: 'RUB',
    active: true,
  },
];

export const BankOffersPage: React.FC<BankOffersPageProps> = ({ onSelectOffer }) => {
  const { user } = useAuth();
  const { t } = useLanguage();
  const isAdmin = user?.role === 'ROLE_ADMIN';

  const [offers, setOffers] = useState<BankOffer[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCurrency, setSelectedCurrency] = useState<'ALL' | Currency>('ALL');

  // Modal create/edit offer
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingOfferId, setEditingOfferId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState<BankOfferRequest>({
    bankName: '',
    offerName: '',
    annualInterestRate: 18.0,
    minDownPaymentPercent: 20.0,
    maxTermMonths: 360,
    minLoanAmount: 500_000,
    maxLoanAmount: 30_000_000,
    currency: 'RUB',
  });

  // Admin Microloan Config state
  const [adminBaseRate, setAdminBaseRate] = useState<number>(0.8);
  const [adminSurchargeRate, setAdminSurchargeRate] = useState<number>(0.4);
  const [savingRates, setSavingRates] = useState<boolean>(false);
  const [ratesSuccessMsg, setRatesSuccessMsg] = useState<string | null>(null);

  const fetchOffers = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await bankOfferApi.getAll();
      if (data && data.length > 0) {
        setOffers(data);
      } else {
        setOffers(DEFAULT_PLACEHOLDER_OFFERS);
      }
    } catch {
      setOffers(DEFAULT_PLACEHOLDER_OFFERS);
    } finally {
      setLoading(false);
    }
  };

  const fetchMicroConfig = async () => {
    try {
      const cfg = await calculatorApi.getMicroloanConfig();
      if (cfg) {
        setAdminBaseRate(cfg.baseDailyRate ?? 0.8);
        setAdminSurchargeRate(cfg.bankInterest ?? 2000);
      }
    } catch (err) {
      console.warn('Could not load microloan config:', err);
    }
  };

  useEffect(() => {
    fetchOffers();
    fetchMicroConfig();
  }, []);

  const handleUpdateRates = async (e: React.FormEvent) => {
    e.preventDefault();
    setSavingRates(true);
    setError(null);
    try {
      await calculatorApi.updateMicroloanConfig({
        bankInterest: adminSurchargeRate,
      });
      setRatesSuccessMsg(t('ratesUpdated'));
      setTimeout(() => setRatesSuccessMsg(null), 4000);
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || t('ratesUpdateError'));
    } finally {
      setSavingRates(false);
    }
  };

  const openCreateModal = () => {
    setEditingOfferId(null);
    setFormData({
      bankName: '',
      offerName: '',
      annualInterestRate: 18.0,
      minDownPaymentPercent: 20.0,
      maxTermMonths: 360,
      minLoanAmount: 500_000,
      maxLoanAmount: 30_000_000,
      currency: 'RUB',
    });
    setIsModalOpen(true);
  };

  const openEditModal = (offer: BankOffer) => {
    setEditingOfferId(offer.id);
    setFormData({
      bankName: offer.bankName,
      offerName: offer.offerName,
      annualInterestRate: offer.annualInterestRate,
      minDownPaymentPercent: offer.minDownPaymentPercent,
      maxTermMonths: offer.maxTermMonths,
      minLoanAmount: offer.minLoanAmount ?? undefined,
      maxLoanAmount: offer.maxLoanAmount ?? undefined,
      currency: offer.currency || 'RUB',
    });
    setIsModalOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (editingOfferId) {
        await bankOfferApi.update(editingOfferId, formData);
      } else {
        await bankOfferApi.create(formData);
      }
      setIsModalOpen(false);
      await fetchOffers();
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || t('saveOfferError'));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm(t('deleteOfferConfirm'))) return;
    try {
      await bankOfferApi.delete(id);
      await fetchOffers();
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || t('deleteOfferError'));
    }
  };

  const handleApplyOffer = (offer: BankOffer) => {
    const displayCurr = selectedCurrency === 'ALL' ? (offer.currency || 'RUB') : selectedCurrency;
    onSelectOffer({
      ...offer,
      currency: displayCurr,
    });
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 mb-6 border-b border-slate-200 dark:border-slate-800 gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white flex items-center gap-2.5">
            <Building2 size={26} className="text-blue-600 dark:text-blue-400" />
            {t('offersCatalogTitle')}
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            {t('offersCatalogDesc')}
          </p>
        </div>

        {isAdmin && (
          <div className="flex items-center gap-2.5 self-start sm:self-auto">
            <button
              onClick={openCreateModal}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs sm:text-sm rounded-xl shadow-md transition-all cursor-pointer"
            >
              <Plus size={18} />
              <span>{t('addProgramBtn')}</span>
            </button>
          </div>
        )}
      </div>

      {/* Admin Microloan Config Panel */}
      {isAdmin && (
        <div className="mb-8 p-6 bg-white dark:bg-slate-900 border border-slate-200 dark:border-blue-500/30 rounded-3xl shadow-sm text-slate-900 dark:text-white transition-colors">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800 gap-3 mb-5">
            <div className="flex items-center gap-2.5">
              <div className="p-2 bg-blue-50 dark:bg-blue-600/20 text-blue-600 dark:text-blue-400 rounded-xl border border-blue-100 dark:border-blue-800/40">
                <Sliders size={20} />
              </div>
              <div>
                <h3 className="text-base font-extrabold tracking-tight text-slate-900 dark:text-white">
                  {t('adminConfigTitle')}
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  {t('adminConfigDesc')}
                </p>
              </div>
            </div>
            <span className="text-[11px] font-extrabold uppercase px-2.5 py-1 rounded-lg bg-blue-50 dark:bg-blue-900/60 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-700/50 self-start sm:self-center">
              {t('ratesConfiguratorBadge')}
            </span>
          </div>

          <form onSubmit={handleUpdateRates} className="grid grid-cols-1 sm:grid-cols-12 gap-4 items-end">
            <div className="sm:col-span-5">
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('baseDailyRateField')}
              </label>
              <div className="relative">
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  max="2.00"
                  required
                  value={adminBaseRate}
                  onChange={(e) => setAdminBaseRate(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm font-bold text-slate-900 dark:text-white focus:outline-none focus:border-blue-500 transition-colors"
                />
                <span className="absolute right-3 top-2.5 text-xs text-slate-400 font-bold">% / {t('perDay')}</span>
              </div>
              <span className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 block">{t('cbrfLimitHint')}</span>
            </div>

            <div className="sm:col-span-5">
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                {t('vanitySurchargeField')}
              </label>
              <div className="relative">
                <input
                  type="number"
                  step="0.1"
                  min="0.00"
                  max="50.00"
                  required
                  value={adminSurchargeRate}
                  onChange={(e) => setAdminSurchargeRate(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-sm font-bold text-slate-900 dark:text-white focus:outline-none focus:border-blue-500 transition-colors"
                />
                <span className="absolute right-3 top-2.5 text-xs text-slate-400 font-bold">%</span>
              </div>
              <span className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 block">{t('fixedOneTimePaymentHint')}</span>
            </div>

            <div className="sm:col-span-2">
              <button
                type="submit"
                disabled={savingRates}
                className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white font-semibold text-xs rounded-xl shadow-md transition-all cursor-pointer disabled:opacity-60 h-[42px] flex items-center justify-center gap-1.5"
              >
                {savingRates ? t('saving') : t('saveRates')}
              </button>
            </div>
          </form>

          {ratesSuccessMsg && (
            <div className="mt-4 p-3 bg-emerald-50 dark:bg-emerald-950/60 border border-emerald-200 dark:border-emerald-800 rounded-xl text-emerald-800 dark:text-emerald-300 text-xs font-semibold flex items-center gap-2 animate-in fade-in">
              <CheckCircle2 size={16} className="text-emerald-600 dark:text-emerald-400 shrink-0" />
              <span>{ratesSuccessMsg}</span>
            </div>
          )}
        </div>
      )}

      {/* Currency Switcher & Slogan Bar */}
      <div className="mb-6 flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 p-3.5 rounded-2xl transition-colors">
        <div className="flex items-center gap-2 overflow-x-auto pb-1 md:pb-0">
          <span className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider flex items-center gap-1.5 shrink-0 mr-1">
            <Coins size={14} className="text-blue-600 dark:text-blue-400" />
            {t('currencyLabel')}
          </span>
          {(['ALL', 'RUB', 'USD', 'EUR', 'BYN'] as const).map((curr) => {
            const isSelected = selectedCurrency === curr;
            return (
              <button
                key={curr}
                type="button"
                onClick={() => setSelectedCurrency(curr)}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1 ${
                  isSelected
                    ? 'bg-blue-600 text-white shadow-sm shadow-blue-500/20'
                    : 'bg-white dark:bg-slate-950 text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 border border-slate-200 dark:border-slate-800'
                }`}
              >
                <span>{curr === 'ALL' ? t('allCurrencies') : curr}</span>
                {curr !== 'ALL' && <span className="opacity-75">({getCurrencySymbol(curr)})</span>}
              </button>
            );
          })}
        </div>

        <div className="text-xs text-slate-700 dark:text-slate-300 bg-blue-50 dark:bg-slate-950 border border-blue-200 dark:border-slate-800 px-3.5 py-1.5 rounded-xl flex items-center gap-2 self-start md:self-auto">
          <ShieldCheck size={14} className="text-blue-600 dark:text-blue-400 shrink-0" />
          <span>{t('ratesUpdatedCbrfNote')}</span>
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 rounded-2xl text-red-700 dark:text-red-300 text-sm flex items-start gap-2">
          <AlertCircle size={18} className="shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="py-20 text-center text-slate-400 dark:text-slate-500">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-3 border-blue-600 border-t-transparent" />
          <p className="mt-2 text-sm">{t('loadingOffers')}</p>
        </div>
      ) : offers.length === 0 ? (
        <div className="bg-white dark:bg-slate-900 rounded-3xl p-12 text-center border border-slate-200 dark:border-slate-800 shadow-sm">
          <Building2 size={36} className="mx-auto text-slate-400 mb-3" />
          <h3 className="text-base font-bold text-slate-800 dark:text-white">{t('noOffersTitle')}</h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            {isAdmin ? t('noOffersAdminDesc') : t('noOffersUserDesc')}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {offers.map((offer) => {
            const displayCurr = selectedCurrency === 'ALL' ? (offer.currency || 'RUB') : selectedCurrency;
            return (
              <div
                key={offer.id}
                className="bg-white dark:bg-slate-900 rounded-3xl p-6 border border-slate-200 dark:border-slate-800 shadow-sm hover:shadow-md transition-all flex flex-col justify-between"
              >
                <div>
                  {/* Header card */}
                  <div className="flex items-start justify-between gap-3 mb-4">
                    <div>
                      <div className="flex flex-wrap items-center gap-1.5 mb-1">
                        <span className="text-xs font-bold text-blue-600 dark:text-blue-400 uppercase tracking-wider">
                          {offer.bankName}
                        </span>
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
                          {selectedCurrency === 'ALL' ? t('anyCurrency') : `${displayCurr} ${getCurrencySymbol(displayCurr)}`}
                        </span>
                      </div>
                      <h3 className="text-lg font-bold text-slate-900 dark:text-white mt-0.5">{offer.offerName}</h3>
                    </div>
                    <div className="px-3 py-1.5 bg-blue-50 dark:bg-blue-950/60 border border-blue-100 dark:border-blue-900 rounded-xl text-center shrink-0">
                      <span className="text-xs text-blue-600 dark:text-blue-400 font-semibold block leading-none">
                        {t('rate')}
                      </span>
                      <span className="text-lg font-black text-blue-700 dark:text-blue-300 leading-tight">
                        {offer.annualInterestRate}%
                      </span>
                    </div>
                  </div>

                  {/* Specs */}
                  <div className="space-y-2.5 py-4 border-y border-slate-100 dark:border-slate-800 text-xs text-slate-600 dark:text-slate-300">
                    <div className="flex items-center justify-between">
                      <span className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400">
                        <Percent size={14} className="text-slate-400" />
                        {t('downPaymentLabel')}
                      </span>
                      <span className="font-bold text-slate-800 dark:text-white">
                        {t('from')} {offer.minDownPaymentPercent}%
                      </span>
                    </div>

                    <div className="flex items-center justify-between">
                      <span className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400">
                        <Calendar size={14} className="text-slate-400" />
                        {t('maxTermLabel')}
                      </span>
                      <span className="font-bold text-slate-800 dark:text-white">
                        {t('upTo')} {Math.floor(offer.maxTermMonths / 12)} {t('years')} ({offer.maxTermMonths} {t('months')})
                      </span>
                    </div>

                    {(offer.minLoanAmount || offer.maxLoanAmount) && (
                      <div className="flex items-center justify-between">
                        <span className="flex items-center gap-1.5 text-slate-500 dark:text-slate-400">
                          <Wallet size={14} className="text-slate-400" />
                          {t('loanAmountTitle')}:
                        </span>
                        <span className="font-semibold text-slate-800 dark:text-white">
                          {formatLoanLimit(offer.minLoanAmount, offer.maxLoanAmount, offer.currency, selectedCurrency, t('from'), t('upTo'))}
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Bank Info Card */}
                  <div className="mt-3.5 p-2.5 bg-slate-50 dark:bg-slate-950 rounded-xl text-[11px] text-slate-600 dark:text-slate-400 border border-slate-100 dark:border-slate-800 flex items-center gap-2">
                    <span>{getBankDescription(offer.bankName, t)}</span>
                  </div>
                </div>

                {/* Action buttons */}
                <div className="mt-6 space-y-2">
                  <button
                    onClick={() => handleApplyOffer(offer)}
                    className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white font-semibold text-xs rounded-xl shadow-sm transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <span>
                      {t('applyInCurrency')} {displayCurr} ({getCurrencySymbol(displayCurr)})
                    </span>
                    <ArrowRight size={14} />
                  </button>

                  {isAdmin && (
                    <div className="flex gap-2 pt-1">
                      <button
                        onClick={() => openEditModal(offer)}
                        className="flex-1 py-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-semibold rounded-lg transition-colors flex items-center justify-center gap-1 cursor-pointer"
                      >
                        <Pencil size={12} />
                        {t('editBtn')}
                      </button>
                      <button
                        onClick={() => handleDelete(offer.id)}
                        className="p-1.5 text-red-500 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-lg transition-colors cursor-pointer"
                        title={t('deleteBtn')}
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Admin Create / Edit Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl w-full max-w-lg p-6 sm:p-7 border border-slate-100 dark:border-slate-800">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800 mb-5">
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                {editingOfferId ? t('editBankOfferTitle') : t('newBankOfferTitle')}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSave} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('bankNameLabel')}
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.bankName}
                    onChange={(e) => setFormData({ ...formData, bankName: e.target.value })}
                    placeholder="SberBank / СберБанк"
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('programNameLabel')}
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.offerName}
                    onChange={(e) => setFormData({ ...formData, offerName: e.target.value })}
                    placeholder="Mortgage Primary / Новостройка"
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('rate')}
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={formData.annualInterestRate}
                    onChange={(e) =>
                      setFormData({ ...formData, annualInterestRate: Number(e.target.value) })
                    }
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('minDownPaymentLabel')}
                  </label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    required
                    value={formData.minDownPaymentPercent}
                    onChange={(e) =>
                      setFormData({ ...formData, minDownPaymentPercent: Number(e.target.value) })
                    }
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('termMonthsLabel')}
                  </label>
                  <input
                    type="number"
                    min="1"
                    required
                    value={formData.maxTermMonths}
                    onChange={(e) =>
                      setFormData({ ...formData, maxTermMonths: Number(e.target.value) })
                    }
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('minLoanAmountLabel')} ({getCurrencySymbol(formData.currency)})
                  </label>
                  <input
                    type="number"
                    value={formData.minLoanAmount || ''}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        minLoanAmount: e.target.value ? Number(e.target.value) : null,
                      })
                    }
                    placeholder="500000"
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                    {t('maxLoanAmountLabel')} ({getCurrencySymbol(formData.currency)})
                  </label>
                  <input
                    type="number"
                    value={formData.maxLoanAmount || ''}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        maxLoanAmount: e.target.value ? Number(e.target.value) : null,
                      })
                    }
                    placeholder="30000000"
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-medium text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1">
                  {t('offerCurrencyLabel')}
                </label>
                <select
                  value={formData.currency || 'RUB'}
                  onChange={(e) =>
                    setFormData({ ...formData, currency: e.target.value as Currency })
                  }
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-semibold text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 cursor-pointer"
                >
                  {CURRENCY_CONFIGS.map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="flex-1 py-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-semibold text-xs rounded-xl transition-colors cursor-pointer"
                >
                  {t('cancel')}
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="flex-1 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs rounded-xl shadow-md transition-all cursor-pointer disabled:opacity-70"
                >
                  {saving ? t('saving') : t('save')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};
