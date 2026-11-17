import React, { useState, useEffect } from 'react';
import { scenarioApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
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
  const { t } = useLanguage();
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
      setError(t('scenariosLoadError'));
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
        setError(t('maxCompareLimitError'));
        return;
      }
      setError(null);
      setSelectedIds([...selectedIds, id]);
    }
  };

  const handleCompare = async () => {
    if (selectedIds.length < 2 || selectedIds.length > 3) {
      setError(t('selectScenariosError'));
      return;
    }
    setComparing(true);
    setError(null);
    try {
      const res = await scenarioApi.compare(selectedIds);
      setComparison(res);
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || t('compareErrorDefault'));
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
      setPdfToast(res.message || t('pdfExportStartedToast'));
      setTimeout(() => setPdfToast(null), 5000);
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || t('pdfExportErrorToast'));
    }
  };

  if (!user) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center animate-in fade-in">
        <div className="w-16 h-16 bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 rounded-3xl flex items-center justify-center mx-auto mb-4 border border-blue-200 dark:border-blue-800 shadow-sm">
          <GitCompare size={32} />
        </div>
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white mb-2">{t('compareTitle')}</h2>
        <p className="text-slate-600 dark:text-slate-400 max-w-md mx-auto mb-6 text-sm">
          {t('compareSubtitle')}
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

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-in fade-in duration-300">
      {/* Page Title */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 mb-6 border-b border-slate-200 dark:border-slate-800 gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white flex items-center gap-2.5">
            <GitCompare size={26} className="text-blue-600 dark:text-blue-400" />
            {t('compareTitle')}
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            {t('compareSubtitle')}
          </p>
        </div>

        <button
          onClick={onNavigateToCalculator}
          className="px-4 py-2 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-sm font-semibold rounded-xl transition-colors self-start sm:self-auto cursor-pointer"
        >
          {t('newCalcInCalculator')}
        </button>
      </div>

      {pdfToast && (
        <div className="mb-6 p-4 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 rounded-2xl text-emerald-800 dark:text-emerald-300 text-sm font-semibold flex items-center gap-2 animate-in fade-in">
          <Sparkles size={18} className="text-emerald-600 dark:text-emerald-400" />
          <span>{pdfToast}</span>
        </div>
      )}

      {error && (
        <div className="mb-6 p-4 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 rounded-2xl text-red-700 dark:text-red-300 text-sm flex items-start gap-2">
          <AlertCircle size={18} className="shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {/* Scenarios Picker Grid */}
      {loading ? (
        <div className="py-20 text-center text-slate-400 dark:text-slate-500">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-3 border-blue-600 border-t-transparent" />
          <p className="mt-2 text-sm">{t('loadingScenarios')}</p>
        </div>
      ) : scenarios.length === 0 ? (
        <div className="bg-white dark:bg-slate-900 rounded-3xl p-12 text-center border border-slate-200 dark:border-slate-800 shadow-sm">
          <div className="w-14 h-14 bg-slate-100 dark:bg-slate-800 text-slate-400 dark:text-slate-500 rounded-2xl flex items-center justify-center mx-auto mb-3">
            <FolderOpen size={28} />
          </div>
          <h3 className="text-base font-bold text-slate-800 dark:text-white mb-1">{t('noSavedScenarios')}</h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mb-5">
            {t('noSavedScenariosDesc')}
          </p>
          <button
            onClick={onNavigateToCalculator}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium text-xs rounded-xl shadow-sm transition-all cursor-pointer"
          >
            {t('goToCalculator')}
          </button>
        </div>
      ) : (
        <div className="space-y-6">
          <div>
            <div className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-3">
              {t('selectScenariosPrompt').replace('{count}', String(selectedIds.length))}
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
                        ? 'border-blue-600 dark:border-blue-500 bg-blue-50/50 dark:bg-blue-950/40 shadow-xs'
                        : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:border-slate-300 dark:hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5 mb-1">
                          <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                            {sc.currency || 'RUB'} {getCurrencySymbol(sc.currency)}
                          </span>
                        </div>
                        <h4 className="font-bold text-slate-900 dark:text-white text-sm line-clamp-1">{sc.title}</h4>
                      </div>
                      <div
                        className={`w-5 h-5 rounded-md flex items-center justify-center text-xs shrink-0 ${
                          isSelected
                            ? 'bg-blue-600 text-white'
                            : 'border border-slate-300 dark:border-slate-700 text-transparent'
                        }`}
                      >
                        <Check size={14} />
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-2 text-xs text-slate-600 dark:text-slate-300 mt-2">
                      <div>
                        <span className="text-slate-400 dark:text-slate-500">{t('monthlyPayment')}:</span>{' '}
                        <span className="font-bold text-slate-900 dark:text-white">
                          {formatCurrency(sc.monthlyPayment, sc.currency)}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-400 dark:text-slate-500">{t('rate')}:</span>{' '}
                        <span className="font-bold text-slate-900 dark:text-white">{sc.annualInterestRate}%</span>
                      </div>
                      <div>
                        <span className="text-slate-400 dark:text-slate-500">{t('loanAmountTitle')}:</span>{' '}
                        <span className="font-medium text-slate-800 dark:text-slate-200">
                          {formatCurrency(sc.loanAmount, sc.currency)}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-400 dark:text-slate-500">{t('loanTerm')}:</span>{' '}
                        <span className="font-medium text-slate-800 dark:text-slate-200">
                          {Math.floor(sc.termMonths / 12)} {t('years')}
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
            <div className="py-12 text-center text-slate-400 dark:text-slate-500 bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800">
              <div className="inline-block animate-spin rounded-full h-8 w-8 border-3 border-blue-600 border-t-transparent" />
              <p className="mt-2 text-sm">{t('comparingParameters')}</p>
            </div>
          ) : comparison && comparison.scenarios.length >= 2 ? (
            <div className="bg-white dark:bg-slate-900 rounded-3xl p-6 sm:p-8 border border-slate-200 dark:border-slate-800 shadow-sm">
              <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-6 flex items-center gap-2">
                <Trophy size={20} className="text-amber-500" />
                {t('compareResults')}
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
                          ? 'border-blue-500 bg-gradient-to-b from-blue-50/50 to-white dark:from-blue-950/40 dark:to-slate-900 shadow-md'
                          : isBestPayment || isBestOverpayment
                          ? 'border-amber-300 dark:border-amber-700/60 bg-amber-50/20 dark:bg-amber-950/20 shadow-xs'
                          : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900'
                      }`}
                    >
                      <div>
                        {/* Winner Badges */}
                        <div className="flex flex-wrap items-center gap-1.5 mb-3">
                          <span className="text-[11px] font-bold px-2.5 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                            {sc.currency || 'RUB'} {getCurrencySymbol(sc.currency)}
                          </span>
                          {isBestPayment && (
                            <span className="inline-flex items-center gap-1 text-[11px] font-bold px-2.5 py-1 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
                              <Trophy size={12} />
                              {t('minPaymentBadge')}
                            </span>
                          )}
                          {isBestOverpayment && (
                            <span className="inline-flex items-center gap-1 text-[11px] font-bold px-2.5 py-1 rounded-full bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-800">
                              <Sparkles size={12} />
                              {t('minOverpaymentBadge')}
                            </span>
                          )}
                        </div>

                        <h4 className="font-extrabold text-slate-900 dark:text-white text-base mb-4">{sc.title}</h4>

                        {/* Metric highlights */}
                        <div className="space-y-3 pb-6 border-b border-slate-100 dark:border-slate-800 text-sm">
                          <div className="flex justify-between items-center">
                            <span className="text-slate-500 dark:text-slate-400">{t('monthlyPayment')}</span>
                            <span
                              className={`font-black text-base ${
                                isBestPayment ? 'text-emerald-600 dark:text-emerald-400 font-extrabold' : 'text-slate-900 dark:text-white'
                              }`}
                            >
                              {formatCurrency(sc.monthlyPayment, sc.currency)}
                            </span>
                          </div>

                          <div className="flex justify-between items-center">
                            <span className="text-slate-500 dark:text-slate-400">{t('overpaymentInterest')}</span>
                            <span
                              className={`font-black text-base ${
                                isBestOverpayment
                                  ? 'text-amber-600 dark:text-amber-400 font-extrabold'
                                  : 'text-slate-900 dark:text-white'
                              }`}
                            >
                              {formatCurrency(sc.totalInterest, sc.currency)}
                            </span>
                          </div>

                          <div className="flex justify-between items-center">
                            <span className="text-slate-500 dark:text-slate-400">{t('totalPaymentsSum')}</span>
                            <span className="font-bold text-slate-800 dark:text-slate-200">
                              {formatCurrency(sc.totalPayment, sc.currency)}
                            </span>
                          </div>
                        </div>

                        {/* Detailed Specs */}
                        <div className="space-y-2 py-4 text-xs text-slate-600 dark:text-slate-300">
                          <div className="flex justify-between">
                            <span className="text-slate-400 dark:text-slate-500">{t('propertyPrice')}:</span>
                            <span className="font-medium text-slate-800 dark:text-slate-200">
                              {formatCurrency(sc.propertyPrice, sc.currency)}
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400 dark:text-slate-500">{t('downPayment')}:</span>
                            <span className="font-medium text-slate-800 dark:text-slate-200">
                              {formatCurrency(sc.downPayment, sc.currency)}
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400 dark:text-slate-500">{t('loanAmountTitle')}:</span>
                            <span className="font-medium text-slate-800 dark:text-slate-200">
                              {formatCurrency(sc.loanAmount, sc.currency)}
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400 dark:text-slate-500">{t('loanTerm')}:</span>
                            <span className="font-medium text-slate-800 dark:text-slate-200">
                              {Math.floor(sc.termMonths / 12)} {t('years')} ({sc.termMonths} {t('months')})
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400 dark:text-slate-500">{t('annualRate')}:</span>
                            <span className="font-bold text-slate-900 dark:text-white">
                              {sc.annualInterestRate}%
                            </span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-400 dark:text-slate-500">{t('paymentType')}:</span>
                            <span className="font-semibold text-slate-800 dark:text-slate-200">
                              {sc.paymentType === 'ANNUITY'
                                ? t('annuity')
                                : t('differentiated')}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* PDF Export Button */}
                      <button
                        onClick={() => handleExportPdf(sc.id)}
                        className="w-full mt-4 py-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-blue-50 dark:hover:bg-blue-950/60 hover:text-blue-700 dark:hover:text-blue-300 text-slate-700 dark:text-slate-200 font-semibold text-xs rounded-xl transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                      >
                        <FileDown size={14} />
                        {t('exportPdf')}
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
