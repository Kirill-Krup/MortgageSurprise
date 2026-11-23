import React, { useState, useEffect } from 'react';
import { useLanguage } from '../context/LanguageContext';
import { calculatorApi, passportApi } from '../api/client';
import { getApiErrorMessage } from '../utils/errors';
import type { Currency, MicroloanType, PassportData, User } from '../types';
import {
  UserCheck,
  X,
  AlertCircle,
  ShieldCheck,
  CheckCircle2,
  ChevronRight,
  ChevronLeft,
  FileText,
  CreditCard,
  Loader2,
  Calendar,
  Building,
  User as UserIcon,
} from 'lucide-react';
import { formatCurrency } from '../utils/format';

interface MicroloanApplicationModalProps {
  isOpen: boolean;
  onClose: () => void;
  amount: number;
  termDays: number;
  currency: Currency;
  detectedType: MicroloanType;
  currentPurpose: string;
  totalRepayment: number;
  baseRate: number;
  dueDate: string;
  scheduleId?: string;
  user: User | null;
  isAdmin: boolean;
  savedPassport: PassportData | null;
  onSavedPassportUpdate: (data: PassportData) => void;
  onSuccess: (applicationId: string) => void;
  applyBtnClasses: string;
  applyBtnStyle?: React.CSSProperties;
}

interface ValidationErrors {
  fullName?: string;
  seriesNumber?: string;
  issueDate?: string;
  departmentCode?: string;
  issuedBy?: string;
}

export const MicroloanApplicationModal: React.FC<MicroloanApplicationModalProps> = ({
  isOpen,
  onClose,
  amount,
  termDays,
  currency,
  detectedType,
  currentPurpose,
  totalRepayment,
  baseRate,
  dueDate,
  scheduleId,
  user,
  isAdmin,
  savedPassport,
  onSavedPassportUpdate,
  onSuccess,
  applyBtnClasses,
  applyBtnStyle,
}) => {
  const { t, language } = useLanguage();

  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [passportData, setPassportData] = useState({
    fullName: '',
    seriesNumber: '',
    issueDate: '',
    departmentCode: '',
    issuedBy: '',
  });

  const [errors, setErrors] = useState<ValidationErrors>({});
  const [stepError, setStepError] = useState<string | null>(null);
  const [saveToProfile, setSaveToProfile] = useState<boolean>(true);
  const [autoFilledSource, setAutoFilledSource] = useState<'profile' | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // Initialize or prefill data when modal opens
  useEffect(() => {
    if (!isOpen) {
      setStep(1);
      setErrors({});
      setStepError(null);
      return;
    }

    // Defensive cleanup: purge any leftover insecure plaintext passport from legacy versions
    try {
      localStorage.removeItem('microloan_saved_passport');
    } catch {
      // Ignore
    }

    if (savedPassport && (savedPassport.fullName || savedPassport.seriesNumber)) {
      setPassportData({
        fullName: savedPassport.fullName || '',
        seriesNumber: savedPassport.seriesNumber || '',
        issueDate: savedPassport.issueDate || '',
        departmentCode: savedPassport.departmentCode || '',
        issuedBy: savedPassport.issuedBy || '',
      });
      setAutoFilledSource('profile');
      setSaveToProfile(false);
      return;
    }

    // Default prefill from email if multi-word name
    if (user?.email) {
      const emailPrefix = user.email.split('@')[0];
      const candidateName = emailPrefix.replace(/[._]/g, ' ').toUpperCase();
      const words = candidateName.split(/\s+/).filter(Boolean);
      if (words.length >= 2) {
        setPassportData((prev) => ({
          ...prev,
          fullName: candidateName,
        }));
      }
    }
    setAutoFilledSource(null);
    setSaveToProfile(true);
  }, [isOpen, savedPassport, user]);

  if (!isOpen) return null;

  // Formatters
  const handleSeriesNumberChange = (val: string) => {
    const raw = val.replace(/\D/g, '').slice(0, 10);
    let formatted = raw;
    if (raw.length > 4) {
      formatted = `${raw.slice(0, 4)} ${raw.slice(4)}`;
    }
    setPassportData((prev) => ({ ...prev, seriesNumber: formatted }));
    if (errors.seriesNumber) {
      setErrors((prev) => ({ ...prev, seriesNumber: undefined }));
    }
  };

  const handleDepartmentCodeChange = (val: string) => {
    const raw = val.replace(/\D/g, '').slice(0, 6);
    let formatted = raw;
    if (raw.length > 3) {
      formatted = `${raw.slice(0, 3)}-${raw.slice(3)}`;
    }
    setPassportData((prev) => ({ ...prev, departmentCode: formatted }));
    if (errors.departmentCode) {
      setErrors((prev) => ({ ...prev, departmentCode: undefined }));
    }
  };

  const handleFullNameChange = (val: string) => {
    setPassportData((prev) => ({ ...prev, fullName: val.toUpperCase() }));
    if (errors.fullName) {
      setErrors((prev) => ({ ...prev, fullName: undefined }));
    }
  };

  // Validation functions
  const validateStep1 = (data: typeof passportData): ValidationErrors => {
    const errs: ValidationErrors = {};
    const trimmed = data.fullName.trim();
    if (!trimmed) {
      errs.fullName = t('validationFioRequired');
    } else {
      const words = trimmed.split(/\s+/).filter(Boolean);
      const validLetters = /^[A-Za-zА-Яа-яЁё\-\s]+$/.test(trimmed);
      if (words.length < 2 || !validLetters) {
        errs.fullName = t('validationFioRequired');
      }
    }

    const seriesDigits = data.seriesNumber.replace(/\D/g, '');
    if (seriesDigits.length !== 10) {
      errs.seriesNumber = t('validationSeriesNumber');
    }
    return errs;
  };

  const validateStep2 = (data: typeof passportData): ValidationErrors => {
    const errs: ValidationErrors = {};
    if (!data.issueDate) {
      errs.issueDate = t('validationIssueDate');
    } else {
      const d = new Date(data.issueDate);
      const today = new Date();
      today.setHours(23, 59, 59, 999);
      if (isNaN(d.getTime()) || d > today || d.getFullYear() < 1920) {
        errs.issueDate = t('validationIssueDate');
      }
    }

    const deptDigits = data.departmentCode.replace(/\D/g, '');
    if (deptDigits.length !== 6) {
      errs.departmentCode = t('validationDepartmentCode');
    }

    if (!data.issuedBy.trim() || data.issuedBy.trim().length < 3) {
      errs.issuedBy = t('validationIssuedBy');
    }
    return errs;
  };

  // Step Navigation
  const handleNextFromStep1 = () => {
    const errs = validateStep1(passportData);
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      setStepError(t('validationFixErrors'));
      return;
    }
    setErrors({});
    setStepError(null);
    setStep(2);
  };

  const handleNextFromStep2 = () => {
    const errs = validateStep2(passportData);
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      setStepError(t('validationFixErrors'));
      return;
    }
    setErrors({});
    setStepError(null);
    setStep(3);
  };

  const handleBack = () => {
    setStepError(null);
    if (step === 3) setStep(2);
    else if (step === 2) setStep(1);
  };

  // Final Submit
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSubmitting) return;

    // Validate all
    const errs1 = validateStep1(passportData);
    const errs2 = validateStep2(passportData);
    const allErrors = { ...errs1, ...errs2 };

    if (Object.keys(allErrors).length > 0) {
      setErrors(allErrors);
      setStepError(t('validationFixErrors'));
      if (Object.keys(errs1).length > 0) {
        setStep(1);
      } else if (Object.keys(errs2).length > 0) {
        setStep(2);
      }
      return;
    }

    setIsSubmitting(true);
    setStepError(null);

    const cleanedData: PassportData = {
      fullName: passportData.fullName.trim(),
      seriesNumber: passportData.seriesNumber.trim(),
      issueDate: passportData.issueDate || undefined,
      departmentCode: passportData.departmentCode.trim() || undefined,
      issuedBy: passportData.issuedBy.trim() || undefined,
    };

    // Save to User Profile if authenticated and checked
    if (user && !isAdmin && saveToProfile) {
      try {
        const saved = await passportApi.save(cleanedData);
        onSavedPassportUpdate(saved);
      } catch (err) {
        console.warn('Could not save passport data to profile:', err);
      }
    }

    // 3. Submit application
    const randomId = `MS-${Math.floor(10000 + Math.random() * 90000)}`;

    if (user && !isAdmin) {
      const safeAmount = Math.max(1000, Math.min(100000, amount || 15000));
      const safeTerm = Math.max(7, Math.min(30, termDays || 14));
      try {
        await calculatorApi.applyMicroloan({
          amount: safeAmount,
          termDays: safeTerm,
          purpose: currentPurpose,
          microloanType: detectedType,
          totalRepayment: totalRepayment || safeAmount,
          scheduleId,
          applicationId: randomId,
          applicantFullName: cleanedData.fullName,
          currency,
        });
      } catch (err: unknown) {
        console.error('Microloan application audit error:', err);
        setStepError(getApiErrorMessage(err, 'Не удалось отправить заявку. Пожалуйста, попробуйте позже.'));
        setIsSubmitting(false);
        return;
      }
    }

    setIsSubmitting(false);
    onClose();
    onSuccess(randomId);
  };

  const stepIndicatorText = t('stepOfTotal')
    .replace('{step}', String(step))
    .replace('{total}', '3');

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in">
      <div className="bg-white dark:bg-slate-900 rounded-3xl shadow-2xl w-full max-w-lg p-6 sm:p-7 border border-slate-100 dark:border-slate-800 max-h-[92vh] overflow-y-auto">
        {/* Modal Top Bar */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800 mb-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-blue-100 dark:bg-blue-950/80 text-blue-700 dark:text-blue-300">
                {stepIndicatorText}
              </span>
              <h3 className="text-base sm:text-lg font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                <UserCheck size={18} className="text-blue-600 dark:text-blue-400" />
                {t('passportModalTitle')}
              </h3>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
              {step === 1 && t('step1Subtitle')}
              {step === 2 && t('step2Subtitle')}
              {step === 3 && t('step3Subtitle')}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1.5 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X size={20} />
          </button>
        </div>

        {/* Loan Summary Header Bar (Requirement 2) */}
        <div className="mb-5 p-3.5 bg-blue-50 dark:bg-slate-950 rounded-2xl border border-blue-200 dark:border-slate-800 flex items-center justify-between text-xs">
          <div>
            <span className="text-slate-500 dark:text-slate-400 block font-medium">{t('loanAmount')}:</span>
            <span className="font-extrabold text-slate-900 dark:text-white text-base">
              {formatCurrency(amount, currency)}
            </span>
          </div>
          <div className="text-right">
            <span className="text-slate-500 dark:text-slate-400 block font-medium">{t('categoryTermLabel')}</span>
            <span className="font-bold text-blue-700 dark:text-blue-300">
              {detectedType === 'VANITY' ? t('vanityType') : t('basicNeedsType')} &bull; {termDays} {t('daysUnit')}
            </span>
          </div>
        </div>

        {/* Step Progress Indicators (1 -> 2 -> 3) */}
        <div className="mb-6 px-1">
          <div className="flex items-center justify-between relative">
            {/* Connecting lines */}
            <div className="absolute top-4 left-6 right-6 h-0.5 bg-slate-200 dark:bg-slate-800 -z-0">
              <div
                className="h-full bg-blue-600 dark:bg-blue-500 transition-all duration-300"
                style={{ width: step === 1 ? '0%' : step === 2 ? '50%' : '100%' }}
              />
            </div>

            {/* Step 1 Indicator */}
            <button
              type="button"
              onClick={() => step > 1 && setStep(1)}
              className={`flex flex-col items-center group relative z-10 cursor-pointer ${
                step > 1 ? 'hover:opacity-80' : ''
              }`}
            >
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-black transition-all ${
                  step > 1
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : step === 1
                    ? 'bg-blue-600 text-white ring-4 ring-blue-100 dark:ring-blue-900/50'
                    : 'bg-slate-200 dark:bg-slate-800 text-slate-500 dark:text-slate-400'
                }`}
              >
                {step > 1 ? <CheckCircle2 size={16} /> : '1'}
              </div>
              <span
                className={`text-[11px] font-bold mt-1.5 transition-colors ${
                  step === 1
                    ? 'text-blue-600 dark:text-blue-400 font-extrabold'
                    : step > 1
                    ? 'text-emerald-600 dark:text-emerald-400'
                    : 'text-slate-400 dark:text-slate-500'
                }`}
              >
                {t('step1Title')}
              </span>
            </button>

            {/* Step 2 Indicator */}
            <button
              type="button"
              onClick={() => {
                if (step === 3) setStep(2);
                else if (step === 1 && Object.keys(validateStep1(passportData)).length === 0) setStep(2);
              }}
              className={`flex flex-col items-center group relative z-10 cursor-pointer`}
            >
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-black transition-all ${
                  step > 2
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : step === 2
                    ? 'bg-blue-600 text-white ring-4 ring-blue-100 dark:ring-blue-900/50'
                    : 'bg-slate-200 dark:bg-slate-800 text-slate-500 dark:text-slate-400'
                }`}
              >
                {step > 2 ? <CheckCircle2 size={16} /> : '2'}
              </div>
              <span
                className={`text-[11px] font-bold mt-1.5 transition-colors ${
                  step === 2
                    ? 'text-blue-600 dark:text-blue-400 font-extrabold'
                    : step > 2
                    ? 'text-emerald-600 dark:text-emerald-400'
                    : 'text-slate-400 dark:text-slate-500'
                }`}
              >
                {t('step2Title')}
              </span>
            </button>

            {/* Step 3 Indicator */}
            <div className="flex flex-col items-center relative z-10">
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-black transition-all ${
                  step === 3
                    ? 'bg-blue-600 text-white ring-4 ring-blue-100 dark:ring-blue-900/50'
                    : 'bg-slate-200 dark:bg-slate-800 text-slate-500 dark:text-slate-400'
                }`}
              >
                3
              </div>
              <span
                className={`text-[11px] font-bold mt-1.5 transition-colors ${
                  step === 3
                    ? 'text-blue-600 dark:text-blue-400 font-extrabold'
                    : 'text-slate-400 dark:text-slate-500'
                }`}
              >
                {t('step3Title')}
              </span>
            </div>
          </div>
        </div>

        {/* Saved Data Pre-fill Notice */}
        {autoFilledSource === 'profile' && (
          <div className="mb-4 p-2.5 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 rounded-xl text-emerald-800 dark:text-emerald-200 text-xs flex items-center justify-between animate-in fade-in">
            <div className="flex items-center gap-2">
              <ShieldCheck size={16} className="text-emerald-600 dark:text-emerald-400 shrink-0" />
              <span className="font-semibold">{t('autoFilledFromSaved')}</span>
            </div>
            <span className="text-[10px] bg-emerald-200/80 dark:bg-emerald-900/60 px-2 py-0.5 rounded-full font-mono font-bold shrink-0">
              AES-256
            </span>
          </div>
        )}

        {/* Validation Error Banner */}
        {stepError && (
          <div className="mb-4 p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 rounded-xl text-red-700 dark:text-red-300 text-xs flex items-center gap-2 animate-in fade-in">
            <AlertCircle size={16} className="shrink-0 text-red-600 dark:text-red-400" />
            <span>{stepError}</span>
          </div>
        )}

        {/* Wizard Steps Form */}
        <form onSubmit={handleSubmit}>
          {/* STEP 1: Данные заемщика и паспорт */}
          {step === 1 && (
            <div className="space-y-4 animate-in fade-in duration-200">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1 flex items-center gap-1.5">
                  <UserIcon size={14} className="text-blue-600 dark:text-blue-400" />
                  <span>{t('fullName')} *</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder={t('placeholderFullName')}
                  value={passportData.fullName}
                  onChange={(e) => handleFullNameChange(e.target.value)}
                  className={`w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border rounded-xl text-xs font-semibold text-slate-900 dark:text-white uppercase transition-colors focus:outline-none ${
                    errors.fullName
                      ? 'border-red-400 focus:ring-2 focus:ring-red-400/50'
                      : 'border-slate-200 dark:border-slate-800 focus:ring-2 focus:ring-blue-500'
                  }`}
                />
                {errors.fullName ? (
                  <p className="text-red-600 dark:text-red-400 text-[11px] mt-1 flex items-center gap-1">
                    <AlertCircle size={12} />
                    {errors.fullName}
                  </p>
                ) : (
                  <p className="text-slate-400 dark:text-slate-500 text-[11px] mt-1">
                    {t('placeholderFullName')}
                  </p>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1 flex items-center gap-1.5">
                  <FileText size={14} className="text-blue-600 dark:text-blue-400" />
                  <span>{t('passportSeriesNumber')} *</span>
                </label>
                <input
                  type="text"
                  required
                  maxLength={11}
                  placeholder="4515 123456"
                  value={passportData.seriesNumber}
                  onChange={(e) => handleSeriesNumberChange(e.target.value)}
                  className={`w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border rounded-xl text-xs font-bold text-slate-900 dark:text-white font-mono tracking-wider transition-colors focus:outline-none ${
                    errors.seriesNumber
                      ? 'border-red-400 focus:ring-2 focus:ring-red-400/50'
                      : 'border-slate-200 dark:border-slate-800 focus:ring-2 focus:ring-blue-500'
                  }`}
                />
                {errors.seriesNumber ? (
                  <p className="text-red-600 dark:text-red-400 text-[11px] mt-1 flex items-center gap-1">
                    <AlertCircle size={12} />
                    {errors.seriesNumber}
                  </p>
                ) : (
                  <p className="text-slate-400 dark:text-slate-500 text-[11px] mt-1">
                    10 цифр (серия 4 цифры и номер 6 цифр)
                  </p>
                )}
              </div>

              <div className="flex items-center gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
                <button
                  type="button"
                  onClick={onClose}
                  className="flex-1 py-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-semibold text-xs rounded-xl transition-colors cursor-pointer"
                >
                  {t('stepCancel')}
                </button>
                <button
                  type="button"
                  onClick={handleNextFromStep1}
                  style={applyBtnStyle}
                  className={`flex-1 py-2.5 ${applyBtnClasses} text-white font-semibold text-xs rounded-xl shadow-md transition-all flex items-center justify-center gap-1.5 cursor-pointer`}
                >
                  <span>{t('stepNext')}</span>
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}

          {/* STEP 2: Кем и когда выдан, Код подразделения */}
          {step === 2 && (
            <div className="space-y-4 animate-in fade-in duration-200">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1 flex items-center gap-1.5">
                    <Calendar size={14} className="text-blue-600 dark:text-blue-400" />
                    <span>{t('issueDate')} *</span>
                  </label>
                  <input
                    type="date"
                    required
                    max={new Date().toISOString().split('T')[0]}
                    value={passportData.issueDate}
                    onChange={(e) => {
                      setPassportData({ ...passportData, issueDate: e.target.value });
                      if (errors.issueDate) setErrors((prev) => ({ ...prev, issueDate: undefined }));
                    }}
                    className={`w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border rounded-xl text-xs font-medium text-slate-900 dark:text-white transition-colors focus:outline-none ${
                      errors.issueDate
                        ? 'border-red-400 focus:ring-2 focus:ring-red-400/50'
                        : 'border-slate-200 dark:border-slate-800 focus:ring-2 focus:ring-blue-500'
                    }`}
                  />
                  {errors.issueDate && (
                    <p className="text-red-600 dark:text-red-400 text-[11px] mt-1 flex items-center gap-1">
                      <AlertCircle size={12} />
                      {errors.issueDate}
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1 flex items-center gap-1.5">
                    <Building size={14} className="text-blue-600 dark:text-blue-400" />
                    <span>{t('departmentCode')} *</span>
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={7}
                    placeholder="770-001"
                    value={passportData.departmentCode}
                    onChange={(e) => handleDepartmentCodeChange(e.target.value)}
                    className={`w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border rounded-xl text-xs font-bold text-slate-900 dark:text-white font-mono tracking-wider transition-colors focus:outline-none ${
                      errors.departmentCode
                        ? 'border-red-400 focus:ring-2 focus:ring-red-400/50'
                        : 'border-slate-200 dark:border-slate-800 focus:ring-2 focus:ring-blue-500'
                    }`}
                  />
                  {errors.departmentCode && (
                    <p className="text-red-600 dark:text-red-400 text-[11px] mt-1 flex items-center gap-1">
                      <AlertCircle size={12} />
                      {errors.departmentCode}
                    </p>
                  )}
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1 flex items-center gap-1.5">
                  <Building size={14} className="text-blue-600 dark:text-blue-400" />
                  <span>{t('issuedBy')} *</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder={t('placeholderIssuedBy')}
                  value={passportData.issuedBy}
                  onChange={(e) => {
                    setPassportData({ ...passportData, issuedBy: e.target.value });
                    if (errors.issuedBy) setErrors((prev) => ({ ...prev, issuedBy: undefined }));
                  }}
                  className={`w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-950 border rounded-xl text-xs font-medium text-slate-900 dark:text-white transition-colors focus:outline-none ${
                    errors.issuedBy
                      ? 'border-red-400 focus:ring-2 focus:ring-red-400/50'
                      : 'border-slate-200 dark:border-slate-800 focus:ring-2 focus:ring-blue-500'
                  }`}
                />
                {errors.issuedBy && (
                  <p className="text-red-600 dark:text-red-400 text-[11px] mt-1 flex items-center gap-1">
                    <AlertCircle size={12} />
                    {errors.issuedBy}
                  </p>
                )}
              </div>

              <div className="flex items-center gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
                <button
                  type="button"
                  onClick={handleBack}
                  className="flex-1 py-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-semibold text-xs rounded-xl transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <ChevronLeft size={16} />
                  <span>{t('stepBack')}</span>
                </button>
                <button
                  type="button"
                  onClick={handleNextFromStep2}
                  style={applyBtnStyle}
                  className={`flex-1 py-2.5 ${applyBtnClasses} text-white font-semibold text-xs rounded-xl shadow-md transition-all flex items-center justify-center gap-1.5 cursor-pointer`}
                >
                  <span>{t('stepNext')}</span>
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}

          {/* STEP 3: Подтвержденная цель займа, чекбокс сохранения и подтверждение */}
          {step === 3 && (
            <div className="space-y-4 animate-in fade-in duration-200">
              {/* Review card */}
              <div className="p-3.5 bg-slate-50 dark:bg-slate-950 rounded-2xl border border-slate-200 dark:border-slate-800 space-y-2 text-xs">
                <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500 pb-1 border-b border-slate-200 dark:border-slate-800">
                  {t('stepSummaryLoan')}
                </div>
                <div className="grid grid-cols-2 gap-2 pt-1">
                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block">{t('fullName')}:</span>
                    <span className="font-bold text-slate-900 dark:text-white uppercase truncate block">
                      {passportData.fullName}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block">{t('passportSeriesNumber')}:</span>
                    <span className="font-mono font-bold text-slate-900 dark:text-white">
                      {passportData.seriesNumber}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block">{t('stepSummaryRate')}:</span>
                    <span className="font-bold text-emerald-600 dark:text-emerald-400">
                      {baseRate}% / {t('perDay')}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block">{t('stepSummaryDue')}:</span>
                    <span className="font-bold text-slate-800 dark:text-slate-200">
                      {new Date(dueDate).toLocaleDateString(language === 'en' ? 'en-US' : 'ru-RU', {
                        day: 'numeric',
                        month: 'short',
                        year: 'numeric',
                      })}
                    </span>
                  </div>
                </div>
                <div className="pt-2 border-t border-slate-200 dark:border-slate-800 flex justify-between items-center">
                  <span className="font-bold text-slate-700 dark:text-slate-300">{t('stepSummaryTotal')}:</span>
                  <span className="font-black text-blue-700 dark:text-blue-400 text-sm">
                    {formatCurrency(totalRepayment, currency)}
                  </span>
                </div>
              </div>

              {/* Confirmed Purpose */}
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-300 mb-1 flex items-center justify-between">
                  <span>
                    {t('confirmedPurposeLabel')} {detectedType === 'VANITY' ? t('vanityType') : t('basicNeedsType')})
                  </span>
                  <span
                    className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                      detectedType === 'VANITY'
                        ? 'bg-amber-100 dark:bg-amber-950 text-amber-800 dark:text-amber-300'
                        : 'bg-blue-100 dark:bg-blue-950 text-blue-800 dark:text-blue-300'
                    }`}
                  >
                    {detectedType === 'VANITY' ? t('vanityType') : t('basicNeedsType')}
                  </span>
                </label>
                <input
                  type="text"
                  readOnly
                  value={currentPurpose}
                  className="w-full px-3.5 py-2.5 bg-slate-100 dark:bg-slate-950/80 border border-slate-200 dark:border-slate-800 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300 cursor-not-allowed"
                />
              </div>

              {/* Save to Profile Checkbox */}
              <div className="pt-1">
                <label className="flex items-start gap-2.5 cursor-pointer select-none text-xs text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white transition-colors">
                  <input
                    type="checkbox"
                    checked={saveToProfile}
                    onChange={(e) => setSaveToProfile(e.target.checked)}
                    className="w-4 h-4 mt-0.5 rounded border-slate-300 dark:border-slate-700 text-blue-600 focus:ring-blue-500 cursor-pointer"
                  />
                  <div>
                    <span className="font-semibold block">{t('savePassportToProfileCheckbox')}</span>
                    <span className="text-[11px] text-slate-400 dark:text-slate-500">
                      Данные будут сохранены и автоматически подставлены для ваших будущих заявок
                    </span>
                  </div>
                </label>
              </div>

              {/* 152-FZ Security Notice */}
              <div className="p-3 bg-slate-50 dark:bg-slate-950 rounded-xl border border-slate-200 dark:border-slate-800 text-[11px] text-slate-500 dark:text-slate-400 flex items-start gap-2">
                <ShieldCheck size={16} className="text-blue-600 dark:text-blue-400 shrink-0 mt-0.5" />
                <span>{t('passportSecurityNote')}</span>
              </div>

              {/* Navigation */}
              <div className="flex items-center gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
                <button
                  type="button"
                  onClick={handleBack}
                  disabled={isSubmitting}
                  className="flex-1 py-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-semibold text-xs rounded-xl transition-colors flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-50"
                >
                  <ChevronLeft size={16} />
                  <span>{t('stepBack')}</span>
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={applyBtnStyle}
                  className={`flex-1 py-2.5 ${applyBtnClasses} text-white font-semibold text-xs rounded-xl shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-60`}
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 size={16} className="animate-spin" />
                      <span>{t('submittingApplication')}</span>
                    </>
                  ) : (
                    <>
                      <CreditCard size={16} />
                      <span>{t('stepFinish')}</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          )}
        </form>
      </div>
    </div>
  );
};
