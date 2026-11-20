import type { Currency } from '../types';

export interface CurrencyConfig {
  code: Currency;
  label: string;
  symbol: string;
  defaultPrice: number;
  defaultDownPayment: number;
}

export const CURRENCY_CONFIGS: CurrencyConfig[] = [
  {
    code: 'RUB',
    label: 'RUB (₽)',
    symbol: '₽',
    defaultPrice: 10_000_000,
    defaultDownPayment: 2_000_000,
  },
  {
    code: 'USD',
    label: 'USD ($)',
    symbol: '$',
    defaultPrice: 150_000,
    defaultDownPayment: 30_000,
  },
  {
    code: 'EUR',
    label: 'EUR (€)',
    symbol: '€',
    defaultPrice: 140_000,
    defaultDownPayment: 28_000,
  },
  {
    code: 'BYN',
    label: 'BYN (Br)',
    symbol: 'Br',
    defaultPrice: 350_000,
    defaultDownPayment: 70_000,
  },
];

export const formatCurrency = (val?: number | null, currency: Currency = 'RUB'): string => {
  if (val === undefined || val === null) {
    const symbol = getCurrencySymbol(currency);
    return `0 ${symbol}`;
  }

  switch (currency) {
    case 'USD':
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        maximumFractionDigits: 0,
      }).format(val);
    case 'EUR':
      return new Intl.NumberFormat('de-DE', {
        style: 'currency',
        currency: 'EUR',
        maximumFractionDigits: 0,
      }).format(val);
    case 'BYN':
      return `${new Intl.NumberFormat('ru-RU', {
        maximumFractionDigits: 0,
      }).format(val)} Br`;
    case 'RUB':
    default:
      return new Intl.NumberFormat('ru-RU', {
        style: 'currency',
        currency: 'RUB',
        maximumFractionDigits: 0,
      }).format(val);
  }
};

export const getCurrencySymbol = (currency: Currency = 'RUB'): string => {
  switch (currency) {
    case 'USD':
      return '$';
    case 'EUR':
      return '€';
    case 'BYN':
      return 'Br';
    case 'RUB':
    default:
      return '₽';
  }
};
