import React, { useState, useEffect } from 'react';
import { bankOfferApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
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
} from 'lucide-react';

interface BankOffersPageProps {
  onSelectOffer: (offer: BankOffer) => void;
}

const BANK_JOKES: Record<string, string> = {
  'СберБанк': '«Зелёный свет вашей ипотеке (и 20 годам дисциплины)» 🟢',
  'ВТБ': '«Синий цвет успокаивает... даже когда смотришь на переплату» 🔵',
  'Альфа-Банк': '«Красный цвет — цвет страсти к досрочным погашениям» 🔴',
  'Т-Банк': '«Ипотека без очередей: переплачивайте прямо с дивана» 🛋️',
  'Газпромбанк': '«Мечты сбываются! Но строго по графику платежей» ⛽',
  'Банк ДОМ.РФ': '«Построй дом, посади дерево, отдай процент» 🏡',
};

const formatLoanLimit = (
  minAmount?: number | null,
  maxAmount?: number | null,
  offerCurrency: Currency = 'RUB',
  targetCurrency: 'ALL' | Currency = 'ALL'
): string => {
  const displayCurr: Currency = targetCurrency === 'ALL' ? offerCurrency : targetCurrency;

  const scale = (val?: number | null): number | null => {
    if (!val) return null;
    if (offerCurrency === displayCurr) return val;
    // Scale for realistic limits across currencies (1 USD ~ 100 RUB, 1 EUR ~ 105 RUB, 1 BYN ~ 30 RUB)
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
  if (scaledMin) parts.push(`от ${formatCurrency(scaledMin, displayCurr)}`);
  if (scaledMax) parts.push(`до ${formatCurrency(scaledMax, displayCurr)}`);
  return parts.join(' ');
};

const DEFAULT_PLACEHOLDER_OFFERS: BankOffer[] = [
  {
    id: 'placeholder-sber-1',
    bankName: 'СберБанк',
    offerName: 'Ипотека на готовое жилье (Вторичка)',
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
    minLoanAmount: 600000,
    maxLoanAmount: 60000000,
    currency: 'RUB',
    active: true,
  },
  {
    id: 'placeholder-alfa-1',
    bankName: 'Альфа-Банк',
    offerName: 'Готовое жилье',
    annualInterestRate: 18.9,
    minDownPaymentPercent: 20.0,
    maxTermMonths: 360,
    minLoanAmount: 600000,
    maxLoanAmount: 50000000,
    currency: 'RUB',
    active: true,
  },
  {
    id: 'placeholder-tbank-1',
    bankName: 'Т-Банк',
    offerName: 'Ипотека на вторичное жилье',
    annualInterestRate: 18.7,
    minDownPaymentPercent: 15.0,
    maxTermMonths: 360,
    minLoanAmount: 500000,
    maxLoanAmount: 30000000,
    currency: 'RUB',
    active: true,
  },
  {
    id: 'placeholder-gazprom-1',
    bankName: 'Газпромбанк',
    offerName: 'Премиальная ипотека',
    annualInterestRate: 19.5,
    minDownPaymentPercent: 15.0,
    maxTermMonths: 360,
    minLoanAmount: 700000,
    maxLoanAmount: 45000000,
    currency: 'RUB',
    active: true,
  },
  {
    id: 'placeholder-domrf-1',
    bankName: 'Банк ДОМ.РФ',
    offerName: 'Индивидуальное жилищное строительство (ИЖС)',
    annualInterestRate: 20.1,
    minDownPaymentPercent: 25.0,
    maxTermMonths: 360,
    minLoanAmount: 500000,
    maxLoanAmount: 30000000,
    currency: 'RUB',
    active: true,
  },
];

export const BankOffersPage: React.FC<BankOffersPageProps> = ({ onSelectOffer }) => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ROLE_ADMIN';

  const [offers, setOffers] = useState<BankOffer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCurrency, setSelectedCurrency] = useState<'ALL' | Currency>('ALL');

  const handleApplyOffer = (offer: BankOffer) => {
    const effectiveCurr = selectedCurrency === 'ALL' ? (offer.currency || 'RUB') : selectedCurrency;
    onSelectOffer({
      ...offer,
      currency: effectiveCurr,
    });
  };

  // Admin Modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingOfferId, setEditingOfferId] = useState<string | null>(null);
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
  const [saving, setSaving] = useState(false);

  const fetchOffers = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await bankOfferApi.getAll();
      if (Array.isArray(data) && data.length > 0) {
        setOffers(data);
      } else {
        setOffers(DEFAULT_PLACEHOLDER_OFFERS);
      }
    } catch {
      // Gracefully fallback to default placeholder offers so user never faces empty screen
      setOffers(DEFAULT_PLACEHOLDER_OFFERS);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOffers();
  }, []);

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
      setError(axiosError.response?.data?.message || 'Не удалось сохранить предложение.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Вы уверены, что хотите удалить это банковское предложение?')) return;
    try {
      await bankOfferApi.delete(id);
      await fetchOffers();
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || 'Не удалось удалить предложение.');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 mb-6 border-b border-slate-200 gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 flex items-center gap-2.5">
            <Building2 size={26} className="text-indigo-600" />
            Каталог предложений банков
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Актуальные ипотечные программы в разных валютах. Помните: «Проценты не главное — главное тёплый плед и своя кухня!» ☕
          </p>
        </div>

        {isAdmin && (
          <button
            onClick={openCreateModal}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-xl shadow-md transition-all self-start sm:self-auto cursor-pointer"
          >
            <Plus size={18} />
            Добавить программу
          </button>
        )}
      </div>

      {/* Currency Switcher & Slogan Bar */}
      <div className="mb-6 flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-50 border border-slate-200/80 p-3.5 rounded-2xl">
        <div className="flex items-center gap-2 overflow-x-auto pb-1 md:pb-0">
          <span className="text-xs font-bold text-slate-500 uppercase tracking-wider flex items-center gap-1.5 shrink-0 mr-1">
            <Coins size={14} className="text-indigo-600" />
            Валюта:
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
                    ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-100 scale-102'
                    : 'bg-white text-slate-700 hover:bg-slate-100 border border-slate-200/70'
                }`}
              >
                <span>{curr === 'ALL' ? 'Все валюты' : curr}</span>
                {curr !== 'ALL' && <span className="opacity-75">({getCurrencySymbol(curr)})</span>}
              </button>
            );
          })}
        </div>

        <div className="text-xs text-amber-900 bg-amber-50 border border-amber-200/60 px-3 py-1.5 rounded-xl italic flex items-center gap-1.5 self-start md:self-auto">
          <span>💡 «Банки одобряют, котики одобряют, кошелёк думает...» 🐾</span>
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-2xl text-red-700 text-sm flex items-start gap-2">
          <AlertCircle size={18} className="shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="py-20 text-center text-slate-400">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-3 border-indigo-600 border-t-transparent" />
          <p className="mt-2 text-sm">Загрузка предложений...</p>
        </div>
      ) : offers.length === 0 ? (
        <div className="bg-white rounded-3xl p-12 text-center border border-slate-200 shadow-sm">
          <Building2 size={36} className="mx-auto text-slate-400 mb-3" />
          <h3 className="text-base font-bold text-slate-800">Предложения пока отсутствуют</h3>
          <p className="text-xs text-slate-500 mt-1">
            {isAdmin
              ? 'Создайте первую банковскую программу через кнопку «Добавить программу».'
              : 'В настоящий момент банковские программы обновляются.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {offers.map((offer) => {
            const displayCurr = selectedCurrency === 'ALL' ? (offer.currency || 'RUB') : selectedCurrency;
            return (
              <div
                key={offer.id}
                className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm hover:shadow-md transition-all flex flex-col justify-between"
              >
                <div>
                  {/* Header card */}
                  <div className="flex items-start justify-between gap-3 mb-4">
                    <div>
                      <div className="flex flex-wrap items-center gap-1.5 mb-1">
                        <span className="text-xs font-bold text-indigo-600 uppercase tracking-wider">
                          {offer.bankName}
                        </span>
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-100/60">
                          {selectedCurrency === 'ALL' ? 'Любая валюта' : `${displayCurr} ${getCurrencySymbol(displayCurr)}`}
                        </span>
                      </div>
                      <h3 className="text-lg font-bold text-slate-900 mt-0.5">{offer.offerName}</h3>
                    </div>
                    <div className="px-3 py-1.5 bg-indigo-50 border border-indigo-100 rounded-xl text-center shrink-0">
                      <span className="text-xs text-indigo-500 font-semibold block leading-none">
                        Ставка
                      </span>
                      <span className="text-lg font-black text-indigo-700 leading-tight">
                        {offer.annualInterestRate}%
                      </span>
                    </div>
                  </div>

                  {/* Specs */}
                  <div className="space-y-2.5 py-4 border-y border-slate-100 text-xs text-slate-600">
                    <div className="flex items-center justify-between">
                      <span className="flex items-center gap-1.5 text-slate-500">
                        <Percent size={14} className="text-slate-400" />
                        Первый взнос:
                      </span>
                      <span className="font-bold text-slate-800">
                        от {offer.minDownPaymentPercent}%
                      </span>
                    </div>

                    <div className="flex items-center justify-between">
                      <span className="flex items-center gap-1.5 text-slate-500">
                        <Calendar size={14} className="text-slate-400" />
                        Макс. срок:
                      </span>
                      <span className="font-bold text-slate-800">
                        до {Math.floor(offer.maxTermMonths / 12)} лет ({offer.maxTermMonths} мес.)
                      </span>
                    </div>

                    {(offer.minLoanAmount || offer.maxLoanAmount) && (
                      <div className="flex items-center justify-between">
                        <span className="flex items-center gap-1.5 text-slate-500">
                          <Wallet size={14} className="text-slate-400" />
                          Сумма кредита:
                        </span>
                        <span className="font-semibold text-slate-800">
                          {formatLoanLimit(offer.minLoanAmount, offer.maxLoanAmount, offer.currency, selectedCurrency)}
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Bank Humorous Quote / Motto */}
                  <div className="mt-3.5 p-2.5 bg-slate-50 rounded-xl text-[11px] text-slate-600 italic border border-slate-100 flex items-center gap-2">
                    <span>{BANK_JOKES[offer.bankName] || '«Проценты не главное — берите, пока дают!» 😄'}</span>
                  </div>
                </div>

                {/* Action buttons */}
                <div className="mt-6 space-y-2">
                  <button
                    onClick={() => handleApplyOffer(offer)}
                    className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white font-semibold text-xs rounded-xl shadow-sm transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <span>
                      Применить в {displayCurr} ({getCurrencySymbol(displayCurr)})
                    </span>
                    <ArrowRight size={14} />
                  </button>

                  {isAdmin && (
                    <div className="flex gap-2 pt-1">
                      <button
                        onClick={() => openEditModal(offer)}
                        className="flex-1 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-lg transition-colors flex items-center justify-center gap-1 cursor-pointer"
                      >
                        <Pencil size={12} />
                        Редактировать
                      </button>
                      <button
                        onClick={() => handleDelete(offer.id)}
                        className="p-1.5 text-red-500 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
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
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-lg p-6 sm:p-7 border border-slate-100">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-5">
              <h3 className="text-lg font-bold text-slate-900">
                {editingOfferId ? 'Редактировать предложение' : 'Новое предложение банка'}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-100 transition-colors"
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSave} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">
                    Название банка
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.bankName}
                    onChange={(e) => setFormData({ ...formData, bankName: e.target.value })}
                    placeholder="СберБанк"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">
                    Название программы
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.offerName}
                    onChange={(e) => setFormData({ ...formData, offerName: e.target.value })}
                    placeholder="Новостройка"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">
                    Ставка (%)
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
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">
                    Мин. взнос (%)
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
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">
                    Срок (мес.)
                  </label>
                  <input
                    type="number"
                    min="1"
                    required
                    value={formData.maxTermMonths}
                    onChange={(e) =>
                      setFormData({ ...formData, maxTermMonths: Number(e.target.value) })
                    }
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">
                    Мин. сумма кредита ({getCurrencySymbol(formData.currency)})
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
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">
                    Макс. сумма кредита ({getCurrencySymbol(formData.currency)})
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
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:bg-white"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Валюта предложения
                </label>
                <select
                  value={formData.currency || 'RUB'}
                  onChange={(e) =>
                    setFormData({ ...formData, currency: e.target.value as Currency })
                  }
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-indigo-500 focus:bg-white cursor-pointer"
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
                  className="flex-1 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-xl transition-colors cursor-pointer"
                >
                  Отмена
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs rounded-xl shadow-md transition-all cursor-pointer disabled:opacity-70"
                >
                  {saving ? 'Сохранение...' : 'Сохранить'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
