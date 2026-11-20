import React, { useState, useEffect } from 'react';
import { scenarioApi, calculatorApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { getApiErrorMessage } from '../utils/errors';
import type { ScenarioResponse, PaymentScheduleItem, Currency } from '../types';
import { formatCurrency, getCurrencySymbol } from '../utils/format';
import {
  FolderBookmark,
  FileDown,
  Trash2,
  Table,
  AlertCircle,
  Sparkles,
  X,
  ShieldCheck,
} from 'lucide-react';

interface MyScenariosPageProps {
  onOpenAuth: () => void;
  onNavigateToCalculator: () => void;
}

export const MyScenariosPage: React.FC<MyScenariosPageProps> = ({
  onOpenAuth,
  onNavigateToCalculator,
}) => {
  const { user, loading: authLoading } = useAuth();
  const { t, language } = useLanguage();
  const isAdmin = user?.role === 'ROLE_ADMIN';

  const [scenarios, setScenarios] = useState<ScenarioResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pdfToast, setPdfToast] = useState<string | null>(null);

  // Schedule viewer modal
  const [selectedSchedule, setSelectedSchedule] = useState<{
    title: string;
    currency?: Currency;
    items: PaymentScheduleItem[];
  } | null>(null);
  const [loadingSchedule, setLoadingSchedule] = useState(false);

  const fetchScenarios = async () => {
    if (authLoading) return;
    if (!user || isAdmin) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const data = await scenarioApi.getAll();
      setScenarios(data);
    } catch {
      setError(t('scenariosLoadError'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchScenarios();
  }, [user, isAdmin, authLoading]);

  const handleDelete = async (id: string) => {
    if (!window.confirm(t('confirmDeleteScenario'))) return;
    try {
      await scenarioApi.delete(id);
      setScenarios(scenarios.filter((s) => s.id !== id));
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, t('scenarioDeleteError')));
    }
  };

  const handleExportPdf = async (id: string) => {
    try {
      const res = await scenarioApi.exportPdf(id);
      setPdfToast(res.message || t('pdfExportStartedToast'));
      setTimeout(() => setPdfToast(null), 5000);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, t('pdfExportErrorToast')));
    }
  };

  const handleViewSchedule = async (scenario: ScenarioResponse) => {
    if (!scenario.mongoScheduleId) {
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

  if (authLoading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center text-slate-400 dark:text-slate-500 animate-in fade-in">
        <div className="inline-block animate-spin rounded-full h-8 w-8 border-3 border-blue-600 border-t-transparent" />
        <p className="mt-2 text-sm">{t('loadingScenarios')}</p>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center animate-in fade-in">
        <div className="w-16 h-16 bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 rounded-3xl flex items-center justify-center mx-auto mb-4 border border-blue-200 dark:border-blue-800">
          <FolderBookmark size={32} />
        </div>
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white mb-2">{t('myScenariosTitle')}</h2>
        <p className="text-slate-600 dark:text-slate-400 max-w-md mx-auto mb-6 text-sm">
          {t('authRequiredForScenarios')}
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
        <p className="text-slate-600 dark:text-slate-400 max-w-lg mx-auto mb-6 text-sm leading-relaxed">
          {t('adminScenariosNotice')}
        </p>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 mb-6 border-b border-slate-200 dark:border-slate-800 gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white flex items-center gap-2.5">
            <FolderBookmark size={26} className="text-blue-600 dark:text-blue-400" />
            {t('myScenariosTitle')}
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            {t('myScenariosDesc')}
          </p>
        </div>

        <button
          onClick={onNavigateToCalculator}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-sm rounded-xl shadow-sm transition-all self-start sm:self-auto cursor-pointer"
        >
          {t('newCalcInCalculator')}
        </button>
      </div>

      {pdfToast && (
        <div className="mb-6 p-4 bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-200 dark:border-emerald-800 rounded-2xl text-emerald-800 dark:text-emerald-300 text-sm font-semibold flex items-center gap-2 animate-in fade-in">
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

      {loading ? (
        <div className="py-20 text-center text-slate-400 dark:text-slate-500">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-3 border-blue-600 border-t-transparent" />
          <p className="mt-2 text-sm">{t('loadingScenarios')}</p>
        </div>
      ) : scenarios.length === 0 ? (
        <div className="bg-white dark:bg-slate-900 rounded-3xl p-12 text-center border border-slate-200 dark:border-slate-800 shadow-sm">
          <FolderBookmark size={36} className="mx-auto text-slate-400 mb-3" />
          <h3 className="text-base font-bold text-slate-800 dark:text-white">{t('emptyScenariosTitle')}</h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mb-5 mt-1">
            {t('emptyScenariosDesc')}
          </p>
          <button
            onClick={onNavigateToCalculator}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-xl shadow-sm cursor-pointer"
          >
            {t('goToCalculator')}
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {scenarios.map((sc) => (
            <div
              key={sc.id}
              className="bg-white dark:bg-slate-900 rounded-3xl p-6 border border-slate-200 dark:border-slate-800 shadow-sm hover:shadow-md transition-all flex flex-col justify-between"
            >
              <div>
                <div className="flex items-start justify-between gap-2 mb-3">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-1.5 mb-1">
                      <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                        {sc.currency || 'RUB'} {getCurrencySymbol(sc.currency)}
                      </span>
                    </div>
                    <h3 className="font-extrabold text-slate-900 dark:text-white text-base line-clamp-1">
                      {sc.title}
                    </h3>
                  </div>
                  <button
                    onClick={() => handleDelete(sc.id)}
                    className="p-1 text-slate-400 hover:text-red-500 rounded-lg hover:bg-red-50 dark:hover:bg-red-950/40 transition-colors shrink-0 cursor-pointer"
                    title={t('deleteScenarioTooltip')}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>

                {/* Primary numbers */}
                <div className="p-4 bg-slate-50 dark:bg-slate-950 rounded-2xl border border-slate-100 dark:border-slate-800 mb-4 space-y-2">
                  <div className="flex justify-between items-baseline">
                    <span className="text-xs text-slate-500 dark:text-slate-400">{t('monthlyPayment')}:</span>
                    <span className="text-lg font-black text-slate-900 dark:text-white">
                      {formatCurrency(sc.monthlyPayment, sc.currency)}
                    </span>
                  </div>
                  <div className="flex justify-between items-baseline">
                    <span className="text-xs text-slate-500 dark:text-slate-400">{t('overpaymentInterest')}:</span>
                    <span className="text-sm font-bold text-amber-600 dark:text-amber-400">
                      {formatCurrency(sc.totalInterest, sc.currency)}
                    </span>
                  </div>
                </div>

                {/* Parameters */}
                <div className="space-y-2 text-xs text-slate-600 dark:text-slate-300 pb-2">
                  <div className="flex justify-between">
                    <span className="text-slate-400 dark:text-slate-500">{t('propertyPrice')}:</span>
                    <span className="font-semibold text-slate-800 dark:text-slate-200">
                      {formatCurrency(sc.propertyPrice, sc.currency)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400 dark:text-slate-500">{t('loanAmountTitle')}:</span>
                    <span className="font-semibold text-slate-800 dark:text-slate-200">
                      {formatCurrency(sc.loanAmount, sc.currency)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400 dark:text-slate-500">{t('rate')} / {t('loanTerm')}:</span>
                    <span className="font-semibold text-slate-800 dark:text-slate-200">
                      {sc.annualInterestRate}% / {Math.floor(sc.termMonths / 12)} {t('years')}
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

              {/* Actions */}
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
          ))}
        </div>
      )}

      {/* Schedule Viewer Modal */}
      {selectedSchedule && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in">
          <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl w-full max-w-4xl max-h-[85vh] flex flex-col overflow-hidden border border-slate-100 dark:border-slate-800">
            <div className="flex items-center justify-between p-6 border-b border-slate-100 dark:border-slate-800">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-lg font-bold text-slate-900 dark:text-white">{t('paymentScheduleModal')}</h3>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-50 dark:bg-blue-950 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                    {selectedSchedule.currency || 'RUB'} {getCurrencySymbol(selectedSchedule.currency)}
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
                        <td className="p-2.5 text-center font-medium text-slate-400">
                          {row.monthNumber}
                        </td>
                        <td className="p-2.5 font-medium text-slate-700 dark:text-slate-300 whitespace-nowrap">
                          {new Date(row.paymentDate).toLocaleDateString(language === 'en' ? 'en-US' : 'ru-RU', {
                            month: 'short',
                            year: 'numeric',
                          })}
                        </td>
                        <td className="p-2.5 text-right font-medium text-slate-600 dark:text-slate-400 whitespace-nowrap">
                          {formatCurrency(row.openingBalance, selectedSchedule.currency)}
                        </td>
                        <td className="p-2.5 text-right font-semibold text-slate-800 dark:text-slate-200 whitespace-nowrap">
                          {formatCurrency(row.principalPayment, selectedSchedule.currency)}
                        </td>
                        <td className="p-2.5 text-right text-amber-600 dark:text-amber-400 font-medium whitespace-nowrap">
                          {formatCurrency(row.interestPayment, selectedSchedule.currency)}
                        </td>
                        <td className="p-2.5 text-right text-emerald-600 dark:text-emerald-400 font-semibold whitespace-nowrap">
                          {row.earlyPayment > 0 ? formatCurrency(row.earlyPayment, selectedSchedule.currency) : '—'}
                        </td>
                        <td className="p-2.5 text-right font-bold text-blue-600 dark:text-blue-400 whitespace-nowrap">
                          {formatCurrency(row.totalMonthlyPayment, selectedSchedule.currency)}
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
    </div>
  );
};
