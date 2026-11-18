import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '../context/ThemeContext';
import {
  ShieldAlert,
  RefreshCw,
  LogOut,
  Send,
  AlertTriangle,
  Lock,
  Sparkles,
  CheckCircle2,
  X,
  Sun,
  Moon,
  Globe,
  Landmark,
} from 'lucide-react';

const FUN_REASONS_RU = [
  'Подозрение в попытке досрочно закрыть 30-летнюю ипотеку за 3 рабочих дня 🏃‍♂️💨',
  'Слишком частое и подозрительно пристальное изучение ставок ЦБ в ночное время 🦉',
  'Попытка взять микрозайм категории «Роскошный максимум» на покупку космолёта 🚀',
  'Решение службы финансового мониторинга: аккаунт помещён в депозитарный карантин 🧊',
];

const FUN_REASONS_EN = [
  'Suspected of attempting to pay off a 30-year mortgage in 3 business days 🏃‍♂️💨',
  'Overly frequent and suspiciously detailed midnight Central Bank rate analyses 🦉',
  'Attempted to take a "Luxury Maximum" microloan to purchase a spacecraft 🚀',
  'Compliance security protocol: account placed under financial quarantine 🧊',
];

export const BannedUserPage: React.FC = () => {
  const { user, logout, refreshUser } = useAuth();
  const { language, toggleLanguage } = useLanguage();
  const { theme, toggleTheme } = useTheme();

  const [checking, setChecking] = useState(false);
  const [appealModalOpen, setAppealModalOpen] = useState(false);
  const [appealReason, setAppealReason] = useState('');
  const [appealSent, setAppealSent] = useState(false);
  const [notification, setNotification] = useState<string | null>(null);

  const isRu = language === 'ru';
  const funReasons = isRu ? FUN_REASONS_RU : FUN_REASONS_EN;
  // Deterministic reason selection based on user id or email length
  const reasonIndex = (user?.email?.length || 0) % funReasons.length;
  const currentReason = funReasons[reasonIndex];

  const handleCheckStatus = async () => {
    setChecking(true);
    setNotification(null);
    try {
      await refreshUser();
      // If still here, user is still banned
      setTimeout(() => {
        setNotification(
          isRu
            ? 'Служба безопасности всё ещё рассматривает ваш статус. Ограничения пока остаются в силе.'
            : 'Security department is still reviewing your profile. Restrictions remain active.'
        );
        setChecking(false);
      }, 1000);
    } catch {
      setChecking(false);
    }
  };

  const handleSendAppeal = (e: React.FormEvent) => {
    e.preventDefault();
    setAppealSent(true);
    setTimeout(() => {
      setAppealModalOpen(false);
      setAppealSent(false);
      setAppealReason('');
      setNotification(
        isRu
          ? 'Апелляция #APL-8841 успешно зарегистрирована и направлена финансовому омбудсмену!'
          : 'Appeal #APL-8841 has been registered and forwarded to the financial compliance committee!'
      );
    }, 1200);
  };

  const quickExcusesRu = [
    'Обещаю больше не кликать так быстро',
    'Это был не я, это кот лёг на клавиатуру 🐱',
    'Разблокируйте пожалуйста, мне очень нужна ипотека!',
    'Я просто хотел купить айфон по акции',
  ];

  const quickExcusesEn = [
    'I promise not to calculate loans this fast',
    'It was not me, my cat stepped on the keyboard 🐱',
    'Please unban, I genuinely need a mortgage!',
    'I just wanted to simulate a gadget loan',
  ];

  const quickExcuses = isRu ? quickExcusesRu : quickExcusesEn;

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col relative overflow-hidden font-sans selection:bg-rose-500 selection:text-white">
      {/* Background glowing gradients */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-rose-600/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute top-1/2 -right-40 w-96 h-96 bg-amber-600/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 left-1/3 w-96 h-96 bg-blue-600/15 rounded-full blur-3xl pointer-events-none" />

      {/* Top Header */}
      <header className="px-6 py-5 border-b border-slate-900/80 flex items-center justify-between relative z-10">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-rose-600/20 border border-rose-500/30 text-rose-400 flex items-center justify-center font-bold">
            <Landmark size={20} />
          </div>
          <div>
            <div className="font-extrabold text-base tracking-tight text-white leading-tight">
              Mortgage<span className="text-rose-500">Surprise</span>
            </div>
            <div className="text-[10px] uppercase font-bold tracking-wider text-rose-400">
              {isRu ? 'Система безопасности' : 'Security Compliance'}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Language Toggle */}
          <button
            type="button"
            onClick={toggleLanguage}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-800 bg-slate-900/60 hover:bg-slate-800 text-xs font-bold text-slate-300 transition-colors"
          >
            <Globe size={13} className="text-rose-400" />
            <span>{language.toUpperCase()}</span>
          </button>

          {/* Theme Toggle */}
          <button
            type="button"
            onClick={toggleTheme}
            className="p-2 rounded-xl border border-slate-800 bg-slate-900/60 hover:bg-slate-800 text-slate-300 transition-colors"
          >
            {theme === 'dark' ? <Sun size={15} className="text-amber-400" /> : <Moon size={15} />}
          </button>

          {/* Logout */}
          <button
            type="button"
            onClick={() => logout()}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900 hover:bg-rose-950/60 border border-slate-800 hover:border-rose-800/60 text-xs font-semibold text-slate-300 hover:text-rose-300 transition-colors cursor-pointer"
          >
            <LogOut size={13} />
            <span>{isRu ? 'Выйти' : 'Logout'}</span>
          </button>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 flex items-center justify-center p-4 sm:p-6 relative z-10">
        <div className="max-w-2xl w-full space-y-6 animate-in fade-in zoom-in-95 duration-300">
          {/* Notification Alert */}
          {notification && (
            <div className="p-4 rounded-2xl bg-amber-500/15 border border-amber-500/30 text-amber-300 text-xs font-semibold flex items-center justify-between gap-3 shadow-lg animate-in fade-in slide-in-from-top-2">
              <div className="flex items-center gap-2.5">
                <AlertTriangle size={16} className="text-amber-400 shrink-0" />
                <span>{notification}</span>
              </div>
              <button
                type="button"
                onClick={() => setNotification(null)}
                className="text-amber-400 hover:text-white"
              >
                <X size={14} />
              </button>
            </div>
          )}

          {/* Core Card */}
          <div className="bg-slate-900/90 backdrop-blur-xl border border-slate-800 rounded-3xl p-6 sm:p-10 shadow-2xl relative overflow-hidden text-center space-y-6">
            {/* Caution Ribbon Glow */}
            <div className="w-24 h-24 mx-auto rounded-3xl bg-gradient-to-tr from-rose-600/30 via-rose-500/20 to-amber-500/20 border border-rose-500/40 flex items-center justify-center text-rose-400 shadow-xl shadow-rose-950/50 relative">
              <ShieldAlert size={48} className="animate-pulse" />
              <div className="absolute -bottom-1 -right-1 w-7 h-7 rounded-full bg-slate-950 border border-rose-500/60 flex items-center justify-center text-rose-400">
                <Lock size={14} />
              </div>
            </div>

            {/* Title & Tag */}
            <div className="space-y-2">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-rose-500/20 border border-rose-500/40 text-rose-300 text-[11px] font-black uppercase tracking-wider">
                <span className="w-1.5 h-1.5 rounded-full bg-rose-400 animate-ping" />
                <span>{isRu ? 'Финансовый карантин' : 'Account Suspended'}</span>
              </div>
              <h1 className="text-2xl sm:text-4xl font-black tracking-tight text-white">
                {isRu ? 'Доступ временно ограничен' : 'Access Temporarily Restricted'}
              </h1>
              <p className="text-xs sm:text-sm text-slate-400 max-w-lg mx-auto">
                {isRu
                  ? 'Служба безопасности MortgageSurprise временно приостановила операции по вашему аккаунту в соответствии с политикой риск-менеджмента.'
                  : 'MortgageSurprise security compliance has temporarily suspended operations for your profile according to banking risk management protocols.'}
              </p>
            </div>

            {/* Playful Reason Banner */}
            <div className="p-4 rounded-2xl bg-gradient-to-r from-rose-950/40 via-slate-950/60 to-amber-950/40 border border-slate-800 text-left space-y-1.5">
              <div className="text-[10px] uppercase font-bold tracking-wider text-rose-400 flex items-center gap-1.5">
                <Sparkles size={12} />
                <span>{isRu ? 'Возможная причина блокировки' : 'Suspected Reason'}</span>
              </div>
              <p className="text-xs font-semibold text-slate-200 leading-relaxed">
                «{currentReason}»
              </p>
            </div>

            {/* Account Info Pill */}
            <div className="p-3.5 rounded-2xl bg-slate-950/70 border border-slate-800/80 flex flex-wrap items-center justify-between gap-3 text-xs">
              <div className="text-left">
                <div className="text-[10px] font-bold uppercase tracking-wider text-slate-500">
                  {isRu ? 'Клиент' : 'Client Profile'}
                </div>
                <div className="font-bold text-slate-200 mt-0.5">{user?.email}</div>
              </div>
              <div className="text-right">
                <div className="text-[10px] font-bold uppercase tracking-wider text-slate-500">
                  {isRu ? 'Статус реестра' : 'Registry Status'}
                </div>
                <div className="font-extrabold text-rose-400 mt-0.5">
                  {isRu ? 'ДЕАКТИВИРОВАН' : 'BLOCKED'}
                </div>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
              <button
                type="button"
                onClick={handleCheckStatus}
                disabled={checking}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white text-xs font-bold shadow-lg shadow-blue-600/30 transition-all cursor-pointer disabled:opacity-50"
              >
                <RefreshCw size={15} className={checking ? 'animate-spin' : ''} />
                <span>
                  {checking
                    ? isRu
                      ? 'Проверяем статус...'
                      : 'Checking status...'
                    : isRu
                      ? 'Проверить снятие бана'
                      : 'Check Ban Status'}
                </span>
              </button>

              <button
                type="button"
                onClick={() => setAppealModalOpen(true)}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 rounded-2xl bg-slate-800/90 hover:bg-slate-800 border border-slate-700/80 text-slate-200 text-xs font-bold transition-all cursor-pointer"
              >
                <Send size={14} className="text-amber-400" />
                <span>{isRu ? 'Подать апелляцию' : 'File an Appeal'}</span>
              </button>
            </div>
          </div>

          {/* Footer note */}
          <div className="text-center text-[11px] text-slate-500">
            {isRu
              ? 'Если вы считаете, что блокировка произошла по ошибке, вы можете отправить апелляцию или обратиться к администратору платформы.'
              : 'If you believe this restriction is an error, please file a quick appeal or contact platform administrators.'}
          </div>
        </div>
      </main>

      {/* Appeal Modal */}
      {appealModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 max-w-lg w-full space-y-5 shadow-2xl animate-in zoom-in-95">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center font-bold">
                  <Send size={15} />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-white">
                    {isRu ? 'Финансовая апелляция' : 'Security Appeal'}
                  </h3>
                  <div className="text-[11px] text-slate-400">
                    {isRu ? 'Запрос на восстановление доступа' : 'Request to restore account access'}
                  </div>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setAppealModalOpen(false)}
                className="text-slate-400 hover:text-white"
              >
                <X size={18} />
              </button>
            </div>

            {appealSent ? (
              <div className="py-8 text-center space-y-3">
                <CheckCircle2 size={40} className="text-emerald-400 mx-auto animate-bounce" />
                <div className="font-bold text-white text-sm">
                  {isRu ? 'Апелляция отправлена!' : 'Appeal Submitted!'}
                </div>
                <p className="text-xs text-slate-400">
                  {isRu
                    ? 'Администраторы рассмотрят ваш запрос в ближайшее время.'
                    : 'Compliance administrators will review your request shortly.'}
                </p>
              </div>
            ) : (
              <form onSubmit={handleSendAppeal} className="space-y-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-slate-300">
                    {isRu ? 'Быстрые аргументы (кликните):' : 'Quick excuses (click to select):'}
                  </label>
                  <div className="flex flex-wrap gap-1.5">
                    {quickExcuses.map((excuse) => (
                      <button
                        key={excuse}
                        type="button"
                        onClick={() => setAppealReason(excuse)}
                        className="text-[11px] px-2.5 py-1 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700/60 transition-colors text-left"
                      >
                        {excuse}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-slate-300">
                    {isRu ? 'Текст обращения:' : 'Appeal Message:'}
                  </label>
                  <textarea
                    rows={4}
                    required
                    value={appealReason}
                    onChange={(e) => setAppealReason(e.target.value)}
                    placeholder={
                      isRu
                        ? 'Опишите ситуацию, почему ваш аккаунт должен быть разблокирован...'
                        : 'Explain why your account should be unblocked...'
                    }
                    className="w-full px-3.5 py-2.5 rounded-2xl bg-slate-950 border border-slate-800 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
                  />
                </div>

                <div className="flex items-center justify-end gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setAppealModalOpen(false)}
                    className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-400 hover:text-white"
                  >
                    {isRu ? 'Отмена' : 'Cancel'}
                  </button>
                  <button
                    type="submit"
                    className="px-5 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold shadow-md shadow-blue-600/30 transition-all cursor-pointer"
                  >
                    {isRu ? 'Отправить запрос' : 'Submit Appeal'}
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
