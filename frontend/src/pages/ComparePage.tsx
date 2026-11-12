import React, { useState, useEffect } from 'react';
import { scenarioApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import type { ScenarioComparisonResponse, ScenarioResponse } from '../types';
import { formatCurrency, getCurrencySymbol } from '../utils/format';
import {
  GitCompare,
  Trophy,
  Sparkles,
  FileDown,
  Check,
  AlertCircle,
  FolderOpen,
} from 'lucide-react';

interface ComparePageProps {
  onOpenAuth: () => void;
  onNavigateToCalculator: () => void;
}

export const ComparePage: React.FC<ComparePageProps> = ({
  onOpenAuth,
  onNavigateToCalculator,
}) => {
  const { user } = useAuth();
  const [scenarios, setScenarios] = useState<ScenarioResponse[]>([]);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [comparison, setComparison] = useState<ScenarioComparisonResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [comparing, setComparing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pdfToast, setPdfToast] = useState<string | null>(null);

  const fetchScenarios = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await scenarioApi.getAll();
      setScenarios(data);
      // Preselect first 2 scenarios if available
      if (data.length >= 2 && selectedIds.length === 0) {
        setSelectedIds([data[0].id, data[1].id]);
      }
    } catch {
      setError('Не удалось загрузить сохраненные сценарии.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchScenarios();
  }, [user]);

  const toggleSelect = (id: string) => {
    if (selectedIds.includes(id)) {
      setSelectedIds(selectedIds.filter((item) => item !== id));
    } else {
      if (selectedIds.length >= 3) {
        setError('Можно сравнивать от 2 до 3 сценариев одновременно.');
        return;
      }
      setError(null);
      setSelectedIds([...selectedIds, id]);
    }
  };

  const handleCompare = async () => {
    if (selectedIds.length < 2 || selectedIds.length > 3) {
      setError('Пожалуйста, выберите 2 или 3 сценария для сравнения.');
      return;
    }
    setComparing(true);
    setError(null);
    try {
      const res = await scenarioApi.compare(selectedIds);
      setComparison(res);
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || 'Ошибка при сравнении сценариев.');
    } finally {
      setComparing(false);
    }
  };

  useEffect(() => {
    if (selectedIds.length >= 2 && selectedIds.length <= 3) {
      handleCompare();
    } else {
      setComparison(null);
    }
  }, [selectedIds]);

  const handleExportPdf = async (id: string) => {
    try {
      const res = await scenarioApi.exportPdf(id);
      setPdfToast(res.message || 'Экспорт в PDF запущен! Файл будет отправлен на вашу почту.');
      setTimeout(() => setPdfToast(null), 5000);
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || 'Не удалось запустить экспорт PDF.');
    }
  };


  if (!user) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-3xl flex items-center justify-center mx-auto mb-4 shadow-sm">
          <GitCompare size={32} />
        </div>
        <h2 className="text-2xl font-bold text-slate-900 mb-2">Сравнение сценариев ипотеки</h2>
        <p className="text-slate-600 max-w-md mx-auto mb-6 text-sm">
          Чтобы сравнивать различные варианты кредитования, сохраняйте расчеты в личном кабинете.
        </p>
        <button
          onClick={onOpenAuth}
          className="px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-2xl shadow-lg shadow-indigo-100 transition-all cursor-pointer"
        >
          Войти в аккаунт
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Page Title */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 mb-6 border-b border-slate-200 gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 flex items-center gap-2.5">
            <GitCompare size={26} className="text-indigo-600" />
            Сравнение сценариев
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Выберите от 2 до 3 сохраненных расчетов для наглядного сопоставления. Помните: «Проценты не главное — главное выбрать банк по душе!» 🎯
          </p>
        </div>

        <button
          onClick={onNavigateToCalculator}
          className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-sm font-semibold rounded-xl transition-colors self-start sm:self-auto cursor-pointer"
        >
          Новый расчет в калькуляторе
        </button>
      </div>

      {pdfToast && (
        <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 rounded-2xl text-emerald-800 text-sm font-semibold flex items-center gap-2 animate-in fade-in">
          <Sparkles size={18} className="text-emerald-600" />
          <span>{pdfToast}</span>
        </div>
      )}

      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-2xl text-red-700 text-sm flex items-start gap-2">
          <AlertCircle size={18} className="shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {/* Scenarios Picker Grid */}
      {loading ? (
        <div className="py-20 text-center text-slate-400">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-3 border-indigo-600 border-t-transparent" />
          <p className="mt-2 text-sm">Загрузка сценариев...</p>
        </div>
      ) : scenarios.length === 0 ? (
        <div className="bg-white rounded-3xl p-12 text-center border border-slate-200 shadow-sm">
          <div className="w-14 h-14 bg-slate-100 text-slate-400 rounded-2xl flex items-center justify-center mx-auto mb-3">
            <FolderOpen size={28} />
          </div>
          <h3 className="text-base font-bold text-slate-800 mb-1">Нет сохраненных сценариев</h3>
          <p className="text-xs text-slate-500 mb-5">
            Рассчитайте и сохраните хотя бы 2 сценария в калькуляторе, чтобы сравнить их здесь.
          </p>
          <button
            onClick={onNavigateToCalculator}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium text-xs rounded-xl shadow-sm transition-all cursor-pointer"
          >
            Перейти к калькулятору
          </button>
        </div>
      ) : (
        <div className="space-y-6">
          <div>
            <div className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-3">
              Выберите сценарии для сравнения (выбрано: {selectedIds.length} из 3)
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {scenarios.map((sc) => {
                const isSelected = selectedIds.includes(sc.id);
                return (
                  <div
                    key={sc.id}
                    onClick={() => toggleSelect(sc.id)}
                    className={`p-4 rounded-2xl border-2 transition-all cursor-pointer relative ${
                      isSelected
                        ? 'border-indigo-600 bg-indigo-50/40 shadow-sm'
                        : 'border-slate-200 bg-white hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5 mb-1">
                          <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-indigo-50 text-indigo-700">
                            {sc.currency || 'RUB'} {getCurrencySymbol(sc.currency)}
                          </span>
                        </div>
                        <h4 className="font-bold text-slate-900 text-sm line-clamp-1">{sc.title}</h4>
                      </div>
                      <div
                        className={`w-5 h-5 rounded-md flex items-center justify-center text-xs shrink-0 ${
                          isSelected
                            ? 'bg-indigo-600 text-white'
                            : 'border border-slate-300 text-transparent'
                        }`}
                      >
                        <Check size={14} />
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-2 text-xs text-slate-600 mt-2">
                      <div>
                        <span className="text-slate-400">Платеж:</span>{' '}
                        <span className="font-bold text-slate-900">
                          {formatCurrency(sc.monthlyPayment, sc.currency)}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-400">Ставка:</span>{' '}
                        <span className="font-bold text-slate-900">{sc.annualInterestRate}%</span>
                      </div>
                      <div>
                        <span className="text-slate-400">Сумма:</span>{' '}
                        <span className="font-medium text-slate-800">
                          {formatCurrency(sc.loanAmount, sc.currency)}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-400">Срок:</span>{' '}
                        <span className="font-medium text-slate-800">
                          {Math.floor(sc.termMonths / 12)} лет
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Side-by-side comparison table / cards */}
          {comparing ? (
            <div className="py-12 text-center text-slate-400 bg-white rounded-3xl border border-slate-200">
              <div className="inline-block animate-spin rounded-full h-8 w-8 border-3 border-indigo-600 border-t-transparent" />
              <p className="mt-2 text-sm">Сравнение параметров сценариев...</p>
            </div>
          ) : comparison && comparison.scenarios.length >= 2 ? (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200 shadow-sm">
              <h3 className="text-lg font-bold text-slate-900 mb-6 flex items-center gap-2">
                <Trophy size={20} className="text-amber-500" />
                Результаты сравнения
              </h3>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {comparison.scenarios.map((sc) => {
                  const isBestPayment = sc.id === comparison.lowestMonthlyPaymentScenarioId;
                  const isBestOverpayment = sc.id === comparison.lowestOverpaymentScenarioId;

                  return (
                    <div
                      key={sc.id}
                      className={`p-6 rounded-3xl border flex flex-col justify-between ${
                        isBestPayment && isBestOverpayment
                          ? 'border-indigo-400 bg-gradient-to-b from-indigo-50/50 to-white shadow-md'
                          : isBestPayment || isBestOverpayment
                          ? 'border-amber-300 bg-amber-50/20 shadow-sm'
                          : 'border-slate-200 bg-white'
                      }`}
                    >
                      <div>
                        {/* Winner Badges */}
                        <div className="flex flex-wrap items-center gap-1.5 mb-3">
                          <span className="text-[11px] font-bold px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700">
                            {sc.currency || 'RUB'} {getCurrencySymbol(sc.currency)}
                          </span>
                          {isBestPayment && (
                            <span className="inline-flex items-center gap-1 text-[11px] font-bold px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800">
                              <Trophy size={12} />
                              Мин. платеж
                            </span>
                          )}
                          {isBestOverpayment && (
                            <span className="inline-flex items-center gap-1 text-[11px] font-bold px-2.5 py-1 rounded-full bg-amber-100 text-amber-900">
                              <Sparkles size={12} />
                              Мин. переплата
                            </span>
                          )}
                        </div>

                        <h4 className="font-extrabold text-slate-900 text-base mb-4">{sc.title}</h4>

                        {/* Metric highlights */}
                        <div className="space-y-3 pb-6 border-b border-slate-100 text-sm">
                          <div className="flex justify-between items-center">
                            <span className="text-slate-500">Ежемесячный платеж</span>
                            <span
                              className={`font-black text-base ${
                                isBestPayment ? 'text-emerald-700 font-extrabold' : 'text-slate-900'
                              }`}
                            >
                              {formatCurrency(sc.monthlyPayment, sc.currency)}
                            </span>
                          </div>

                          <div className="flex justify-between items-center">
                            <span className="text-slate-500">Переплата по %</span>
                            <span
                              className={`font-black text-base ${
                                isBestOverpayment
                                  ? 'text-amber-700 font-extrabold'
                                  : 'text-slate-900'
                              }`}
                            >
                              {formatCurrency(sc.totalInterest, sc.currency)}
                            </span>
                          </div>

                          <div className="flex justify-between items-center">
                            <span className="text-slate-500">Всего выплат</span>
                            <span className="font-bold text-slate-800">
                              {formatCurrency(sc.totalPayment, sc.currency)}
                            </span>
                          </div>
                        </div>

                        {/* Detailed Specs */}
                        <div className="space-y-2 py-4 text-xs text-slate-600">
                          <div className="flex justify-between">
                            <span className="text-slate-400">Стоимость жилья:</span>
                            <span className="font-medium text-slate-800">
                              {formatCurrency(sc.propertyPrice, sc.currency)}
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">Первый взнос:</span>
                            <span className="font-medium text-slate-800">
                              {formatCurrency(sc.downPayment, sc.currency)}
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">Сумма кредита:</span>
                            <span className="font-medium text-slate-800">
                              {formatCurrency(sc.loanAmount, sc.currency)}
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">Срок кредита:</span>
                            <span className="font-medium text-slate-800">
                              {Math.floor(sc.termMonths / 12)} лет ({sc.termMonths} мес.)
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">Процентная ставка:</span>
                            <span className="font-bold text-slate-900">
                              {sc.annualInterestRate}%
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400">Тип платежей:</span>
                            <span className="font-semibold text-slate-800">
                              {sc.paymentType === 'ANNUITY'
                                ? 'Аннуитетный'
                                : 'Дифференцированный'}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* PDF Export Button */}
                      <button
                        onClick={() => handleExportPdf(sc.id)}
                        className="w-full mt-4 py-2.5 bg-slate-100 hover:bg-indigo-50 hover:text-indigo-600 text-slate-700 font-semibold text-xs rounded-xl transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                      >
                        <FileDown size={14} />
                        Экспорт в PDF
                      </button>
                    </div>
                  );
                })}
              </div>
            </div>
          ) : null}
        </div>
      )}
    </div>
  );
};
