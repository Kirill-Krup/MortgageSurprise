export type PaymentType = 'ANNUITY' | 'DIFFERENTIATED';

export type EarlyRepaymentType = 'ONE_TIME' | 'MONTHLY' | 'RECURRING';

export type RepaymentStrategy = 'REDUCE_TERM' | 'REDUCE_PAYMENT';

export type Currency = 'RUB' | 'USD' | 'EUR' | 'BYN';

export interface EarlyRepayment {
  id?: string;
  startMonth: number;
  endMonth?: number | null;
  amount: number;
  strategy: RepaymentStrategy;
  type: EarlyRepaymentType;
}

export interface CalculationRequest {
  propertyPrice: number;
  downPayment: number;
  termMonths: number;
  annualInterestRate: number;
  paymentType: PaymentType;
  currency?: Currency;
  startDate?: string;
  earlyRepayments?: EarlyRepayment[];
  bankOfferId?: number;
  bankName?: string;
  offerName?: string;
  audit?: boolean;
}

export interface PaymentScheduleItem {
  monthNumber: number;
  paymentDate: string;
  openingBalance: number;
  principalPayment: number;
  interestPayment: number;
  earlyPayment: number;
  totalMonthlyPayment: number;
  closingBalance: number;
}

export interface CalculationResponse {
  scheduleId: string;
  propertyPrice: number;
  downPayment: number;
  loanAmount: number;
  termMonths: number;
  actualTermMonths: number;
  annualInterestRate: number;
  paymentType: PaymentType | string;
  currency: Currency;
  monthlyPayment: number;
  minMonthlyPayment: number;
  maxMonthlyPayment: number;
  totalInterest: number;
  totalPayment: number;
  schedule: PaymentScheduleItem[];
}

export type MicroloanType = 'BASIC_NEEDS' | 'VANITY';

export interface MicroloanCalculationRequest {
  amount: number;
  termDays: number;
  rateName?: string;
  type?: MicroloanType;
  purpose?: string;
  dailyInterestRate?: number;
  startDate?: string;
  currency?: Currency;
}

export interface DailyScheduleItem {
  dayNumber: number;
  date: string;
  principalBalance: number;
  dailyInterest: number;
  accumulatedInterest: number;
  totalDue: number;
}

export interface MicroloanCalculationResponse {
  scheduleId: string;
  loanAmount: number;
  termDays: number;
  microloanType?: MicroloanType;
  purpose?: string;
  baseDailyRate?: number;
  vanitySurchargeRate?: number;
  vanitySurchargeAmount?: number;
  effectiveDailyRate?: number;
  dailyInterestRate?: number;
  annualEquivalentRate: number;
  totalInterest: number;
  totalRepayment: number;
  startDate: string;
  dueDate: string;
  currency: Currency;
  dailySchedule: DailyScheduleItem[];
}

export interface MicroloanRateDTO {
  id?: number;
  name: string;
  baseDailyRate: number;
  minDayRequired?: number;
  maxDayRequired?: number;
}

export interface MicroloanConfigDTO {
  bankInterest: number;
}

export interface MicroloanConfig {
  id?: string;
  bankInterest: number;
  baseDailyRate?: number;
  vanitySurchargeRate?: number;
  updatedAt?: string;
}

export interface BankOffer {
  id: string;
  bankName: string;
  offerName: string;
  annualInterestRate: number;
  minDownPaymentPercent: number;
  maxTermMonths: number;
  minLoanAmount?: number | null;
  maxLoanAmount?: number | null;
  currency: Currency;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface BankOfferRequest {
  bankName: string;
  offerName: string;
  annualInterestRate: number;
  minDownPaymentPercent: number;
  maxTermMonths: number;
  minLoanAmount?: number | null;
  maxLoanAmount?: number | null;
  currency?: Currency;
}

export interface ScenarioResponse {
  id: string;
  userId: string;
  title: string;
  propertyPrice: number;
  downPayment: number;
  loanAmount: number;
  termMonths: number;
  annualInterestRate: number;
  paymentType: PaymentType;
  currency: Currency;
  monthlyPayment: number;
  totalInterest: number;
  totalPayment: number;
  mongoScheduleId: string;
  createdAt: string;
  updatedAt: string;
}

export interface SaveScenarioRequest {
  title: string;
  propertyPrice: number;
  downPayment: number;
  termMonths: number;
  annualInterestRate: number;
  paymentType: PaymentType;
  currency?: Currency;
  earlyRepayments?: EarlyRepayment[];
}

export interface ScenarioComparisonResponse {
  scenarios: ScenarioResponse[];
  lowestMonthlyPaymentScenarioId: string;
  lowestOverpaymentScenarioId: string;
}

export interface User {
  id: string;
  email: string;
  role: string;
  enabled?: boolean;
  firstName?: string | null;
  lastName?: string | null;
}

export interface AuthResponse {
  accessToken?: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
  userId: string;
  email: string;
  role: string;
  enabled?: boolean;
}

export interface AdminStats {
  totalUsers: number;
  activeLoansCount: number;
  totalPortfolioAmount: number;
  mortgageLoansCount: number;
  microloansCount: number;
  mortgagePortfolioAmount: number;
  microloanPortfolioAmount: number;
  totalRepaidAmount: number;
  totalRemainingDebt: number;
  repaymentProgressPercent: number;
  averageMortgageRate: number;
}

export interface AdminUser {
  id: string;
  email: string;
  firstName?: string | null;
  lastName?: string | null;
  role: string;
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string;
  activeLoansCount: number;
  totalDebt: number;
  remainingDebt: number;
}

export interface AdminLoanMonitoring {
  id: string;
  userId?: string | null;
  borrowerEmail: string;
  borrowerName: string;
  loanType: 'MORTGAGE' | 'MICROLOAN';
  productTitle: string;
  purpose?: string;
  loanAmount: number;
  totalRepayment: number;
  interestRate: number;
  term: string;
  termUnits: number;
  currency: Currency | string;
  startDate: string;
  elapsedPeriods: number;
  remainingPeriods: number;
  paidAmount: number;
  principalPaid: number;
  remainingBalance: number;
  progressPercent: number;
  regularPayment: number;
  status: 'ACTIVE' | 'PAID_OFF' | 'OVERDUE';
  nextPaymentDate: string;
  nextPaymentAmount: number;
  scheduleId?: string | null;
}

export interface ExperimentVariantConfig {
  name: string;
  weight: number;
  description?: string;
}

export type ExperimentStatus = 'ACTIVE' | 'PAUSED' | 'ARCHIVED';

export interface ExperimentConfig {
  id: string;
  name: string;
  description?: string;
  variants: ExperimentVariantConfig[];
  status: ExperimentStatus;
  isCustom?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AbTestVariantResponse {
  userId?: string | null;
  experimentName: string;
  color: string;
}

export interface UserTestStatDetail {
  userId: string;
  userEmail: string;
  color: string;
  clicksCount: number;
  assignedAt: string;
}

export interface VariantStat {
  color: string;
  totalUsers: number;
  totalClicks: number;
  averageClicksPerUser: number;
  targetWeight?: number;
}

export interface AbTestStatsResponse {
  experimentName: string;
  totalAssignedUsers: number;
  totalClicks: number;
  variants: VariantStat[];
  userDetails: UserTestStatDetail[];
}


export type UserAuditActionType =
  | 'MORTGAGE_CALCULATION'
  | 'MICROLOAN_CALCULATION'
  | 'MICROLOAN_APPLICATION'
  | 'BANK_OFFER_SELECTED'
  | 'MICROLOAN_APPLIED'
  | 'SCENARIO_CALCULATED'
  | 'SCENARIO_SAVED';

export interface UserActivityRecord {
  id: string;
  userId: string;
  userEmail: string;
  actionType: UserAuditActionType;
  actionDescription: string;
  bankOfferId?: number | null;
  bankName?: string | null;
  offerName?: string | null;
  loanAmount?: number | null;
  propertyPrice?: number | null;
  downPayment?: number | null;
  termMonths?: number | null;
  termDays?: number | null;
  interestRate?: number | null;
  monthlyPayment?: number | null;
  totalPayment?: number | null;
  totalInterest?: number | null;
  microloanType?: string | null;
  loanPurpose?: string | null;
  applicationId?: string | null;
  currency?: Currency | null;
  metadata?: Record<string, unknown> | null;
  timestamp: string;
}

export interface PassportData {
  fullName: string;
  seriesNumber: string;
  issueDate?: string;
  departmentCode?: string;
  issuedBy?: string;
}

