import React, { useState, useEffect } from 'react';
import { scenarioApi, calculatorApi } from '../api/client';
import { useAuth } from '../context/AuthContext';
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
} from 'lucide-react';

interface MyScenariosPageProps {
  onOpenAuth: () => void;
  onNavigateToCalculator: () => void;
}

export const MyScenariosPage: React.FC<MyScenariosPageProps> = ({
  onOpenAuth,
  onNavigateToCalculator,
}) => {
  const { user } = useAuth();
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
    if (!user) return;
    setLoading(true);
    try {
      const data = await scenarioApi.getAll();
      setScenarios(data);
    } catch {
      setError('Не удалось загрузить сохраненные сценарии.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchScenarios();
  }, [user]);

  const handleDelete = async (id: string) => {
    if (!window.confirm('Вы уверены, что хотите удалить этот сценарий?')) return;
    try {
      await scenarioApi.delete(id);
      setScenarios(scenarios.filter((s) => s.id !== id));
    } catch (err: unknown) {
      const axiosError = err as { response?: { data?: { message?: string } } };
      setError(axiosError.response?.data?.message || 'Не удалось удалить сценарий.');
    }
  };

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

  const handleViewSchedule = async (scenario: ScenarioResponse) => {
    if (!scenario.mongoScheduleId) {
      setError('Для данного сценария график платежей не найден.');
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
      setError('Не удалось загрузить подробный график платежей.');
    } finally {
      setLoadingSchedule(false);
    }
  };


  if (!user) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-3xl flex items-center justify-center mx-auto mb-4">
          <FolderBookmark size={32} />
        </div>
        <h2 className="text-2xl font-bold text-slate-900 mb-2">Сохраненные сценарии</h2>
        <p className="text-slate-600 max-w-md mx-auto mb-6 text-sm">
          Авторизуйтесь, чтобы просматривать сохраненные расчеты и экспортировать их в PDF.
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
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-6 mb-6 border-b border-slate-200 gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 flex items-center gap-2.5">
            <FolderBookmark size={26} className="text-indigo-600" />
            Мои сохраненные сценарии
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Все ваши персональные расчеты с возможностью сформировать PDF-отчет. «Проценты не главное — главное своя крыша над головой!» 🏠
          </p>
        </div>

        <button
          onClick={onNavigateToCalculator}
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-xl shadow-sm transition-all self-start sm:self-auto cursor-pointer"
        >
          Новый расчет
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

      {loading ? (
        <div className="py-20 text-center text-slate-400">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-3 border-indigo-600 border-t-transparent" />
          <p className="mt-2 text-sm">Загрузка сценариев...</p>
        </div>
      ) : scenarios.length === 0 ? (
        <div className="bg-white rounded-3xl p-12 text-center border border-slate-200 shadow-sm">
          <FolderBookmark size={36} className="mx-auto text-slate-400 mb-3" />
          <h3 className="text-base font-bold text-slate-800">Список сценариев пуст</h3>
          <p className="text-xs text-slate-500 mb-5 mt-1">
            Рассчитайте условия в калькуляторе и нажмите «Сохранить сценарий».
          </p>
          <button
            onClick={onNavigateToCalculator}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-xl shadow-sm cursor-pointer"
          >
            Перейти в калькулятор
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {scenarios.map((sc) => (
            <div
              key={sc.id}
              className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm hover:shadow-md transition-all flex flex-col justify-between"
            >
              <div>
                <div className="flex items-start justify-between gap-2 mb-3">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-1.5 mb-1">
                      <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-indigo-50 text-indigo-700">
                        {sc.currency || 'RUB'} {getCurrencySymbol(sc.currency)}
                      </span>
                    </div>
                    <h3 className="font-extrabold text-slate-900 text-base line-clamp-1">
                      {sc.title}
                    </h3>
                  </div>
                  <button
                    onClick={() => handleDelete(sc.id)}
                    className="p-1 text-slate-400 hover:text-red-500 rounded-lg hover:bg-red-50 transition-colors shrink-0 cursor-pointer"
                    title="Удалить сценарий"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>

                {/* Primary numbers */}
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100 mb-4 space-y-2">
                  <div className="flex justify-between items-baseline">
                    <span className="text-xs text-slate-500">Платеж в месяц:</span>
                    <span className="text-lg font-black text-slate-900">
                      {formatCurrency(sc.monthlyPayment, sc.currency)}
                    </span>
                  </div>
                  <div className="flex justify-between items-baseline">
                    <span className="text-xs text-slate-500">Переплата (%):</span>
                    <span className="text-sm font-bold text-amber-600">
                      {formatCurrency(sc.totalInterest, sc.currency)}
                    </span>
                  </div>
                </div>

                {/* Parameters */}
                <div className="space-y-2 text-xs text-slate-600 pb-2">
                  <div className="flex justify-between">
                    <span className="text-slate-400">Стоимость жилья:</span>
                    <span className="font-semibold text-slate-800">
                      {formatCurrency(sc.propertyPrice, sc.currency)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Сумма кредита:</span>
                    <span className="font-semibold text-slate-800">
                      {formatCurrency(sc.loanAmount, sc.currency)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Ставка / Срок:</span>
                    <span className="font-semibold text-slate-800">
                      {sc.annualInterestRate}% / {Math.floor(sc.termMonths / 12)} лет
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-400">Тип платежа:</span>
                    <span className="font-semibold text-slate-800">
                      {sc.paymentType === 'ANNUITY' ? 'Аннуитетный' : 'Дифференцированный'}
                    </span>
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div className="mt-5 space-y-2 pt-3 border-t border-slate-100">
                <button
                  onClick={() => handleViewSchedule(sc)}
                  disabled={loadingSchedule}
                  className="w-full py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-xl transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <Table size={14} />
                  <span>График платежей</span>
                </button>

                <button
                  onClick={() => handleExportPdf(sc.id)}
                  className="w-full py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-semibold text-xs rounded-xl transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <FileDown size={14} />
                  <span>Экспорт в PDF</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Schedule Viewer Modal */}
      {selectedSchedule && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-4xl max-h-[85vh] flex flex-col overflow-hidden border border-slate-100">
            <div className="flex items-center justify-between p-6 border-b border-slate-100">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-lg font-bold text-slate-900">График платежей</h3>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700">
                    {selectedSchedule.currency || 'RUB'} {getCurrencySymbol(selectedSchedule.currency)}
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-0.5">{selectedSchedule.title}</p>
              </div>
              <button
                onClick={() => setSelectedSchedule(null)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg hover:bg-slate-100 transition-colors"
              >
                <X size={20} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6">
              <div className="overflow-x-auto rounded-xl border border-slate-100">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-slate-50 text-slate-600 font-bold border-b border-slate-100 sticky top-0">
                      <th className="p-3 text-center">№</th>
                      <th className="p-3">Дата</th>
                      <th className="p-3 text-right">Остаток долга</th>
                      <th className="p-3 text-right">Основной долг</th>
                      <th className="p-3 text-right">Проценты</th>
                      <th className="p-3 text-right">Досрочно</th>
                      <th className="p-3 text-right">Всего платеж</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {selectedSchedule.items.map((row) => (
                      <tr key={row.monthNumber} className="hover:bg-slate-50/70 transition-colors">
                        <td className="p-2.5 text-center font-medium text-slate-400">
                          {row.monthNumber}
                        </td>
                        <td className="p-2.5 font-medium text-slate-700 whitespace-nowrap">
                          {new Date(row.paymentDate).toLocaleDateString('ru-RU', {
                            month: 'short',
                            year: 'numeric',
                          })}
                        </td>
                        <td className="p-2.5 text-right font-medium text-slate-600 whitespace-nowrap">
                          {formatCurrency(row.openingBalance, selectedSchedule.currency)}
                        </td>
                        <td className="p-2.5 text-right font-semibold text-slate-800 whitespace-nowrap">
                          {formatCurrency(row.principalPayment, selectedSchedule.currency)}
                        </td>
                        <td className="p-2.5 text-right text-amber-600 font-medium whitespace-nowrap">
                          {formatCurrency(row.interestPayment, selectedSchedule.currency)}
                        </td>
                        <td className="p-2.5 text-right text-emerald-600 font-semibold whitespace-nowrap">
                          {row.earlyPayment > 0 ? formatCurrency(row.earlyPayment, selectedSchedule.currency) : '—'}
                        </td>
                        <td className="p-2.5 text-right font-bold text-indigo-600 whitespace-nowrap">
                          {formatCurrency(row.totalMonthlyPayment, selectedSchedule.currency)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="p-4 border-t border-slate-100 bg-slate-50 flex justify-end">
              <button
                onClick={() => setSelectedSchedule(null)}
                className="px-5 py-2 bg-slate-200 hover:bg-slate-300 text-slate-800 font-semibold text-xs rounded-xl transition-colors cursor-pointer"
              >
                Закрыть
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
