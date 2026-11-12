export type PaymentType = 'ANNUITY' | 'DIFFERENTIATED';

export type EarlyRepaymentType = 'ONE_TIME' | 'MONTHLY';

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
}

export interface AuthResponse {
  accessToken?: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
  userId: string;
  email: string;
  role: string;
}
