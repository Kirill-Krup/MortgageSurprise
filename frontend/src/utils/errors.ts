import axios from 'axios';

export type BackendErrorCode =
  | 'EMAIL_ALREADY_EXISTS'
  | 'INVALID_CREDENTIALS'
  | 'INVALID_MAGIC_LINK'
  | 'REFRESH_TOKEN_INVALID'
  | 'ACCOUNT_DISABLED'
  | 'PASSPORT_ALREADY_LINKED'
  | 'SCENARIO_COMPARE_COUNT'
  | 'NOT_FOUND'
  | 'INTERNAL_ERROR'
  | 'RATE_LIMIT_EXCEEDED'
  | 'ACCESS_DENIED'
  | 'INVALID_OAUTH2_CODE'
  | 'VALIDATION_ERROR';

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: BackendErrorCode | string;
  errors?: Record<string, string>;
  message?: string;
  email?: string;
  userId?: string;
  [key: string]: unknown;
}

const ERROR_MESSAGES_RU: Record<string, string> = {
  EMAIL_ALREADY_EXISTS: 'Пользователь с таким email уже зарегистрирован',
  INVALID_CREDENTIALS: 'Неверный email или пароль',
  INVALID_MAGIC_LINK: 'Ссылка для входа недействительна или устарела',
  REFRESH_TOKEN_INVALID: 'Сессия истекла. Пожалуйста, выполните вход заново',
  ACCOUNT_DISABLED: 'Ваш аккаунт заблокирован администратором',
  PASSPORT_ALREADY_LINKED: 'Данный паспорт уже привязан к другому профилю',
  SCENARIO_COMPARE_COUNT: 'Для сравнения необходимо выбрать от 2 до 3 сценариев',
  NOT_FOUND: 'Запрашиваемый ресурс не найден',
  INTERNAL_ERROR: 'Внутренняя ошибка сервера. Попробуйте позже',
  RATE_LIMIT_EXCEEDED: 'Слишком много запросов. Пожалуйста, подождите минуту перед следующей попыткой',
  ACCESS_DENIED: 'Доступ ограничен. Недостаточно прав для выполнения операции',
  INVALID_OAUTH2_CODE: 'Код авторизации недействителен или срок его действия истек',
  VALIDATION_ERROR: 'Проверьте правильность заполнения полей',
};

const ERROR_MESSAGES_EN: Record<string, string> = {
  EMAIL_ALREADY_EXISTS: 'A user with this email already exists',
  INVALID_CREDENTIALS: 'Invalid email or password',
  INVALID_MAGIC_LINK: 'Magic link is invalid or expired',
  REFRESH_TOKEN_INVALID: 'Session has expired. Please sign in again',
  ACCOUNT_DISABLED: 'Your account has been suspended by an administrator',
  PASSPORT_ALREADY_LINKED: 'This passport is already linked to another profile',
  SCENARIO_COMPARE_COUNT: 'You must select 2 or 3 scenarios to compare',
  NOT_FOUND: 'The requested resource was not found',
  INTERNAL_ERROR: 'Internal server error. Please try again later',
  RATE_LIMIT_EXCEEDED: 'Rate limit exceeded. Please wait a moment before trying again',
  ACCESS_DENIED: 'Access denied. You do not have permission for this action',
  INVALID_OAUTH2_CODE: 'OAuth authorization code is invalid or expired',
  VALIDATION_ERROR: 'Please check the entered values',
};

/**
 * Extracts structured ProblemDetail from Axios error or unknown error.
 */
export function extractProblemDetail(err: unknown): ProblemDetail | null {
  if (axios.isAxiosError(err) && err.response?.data) {
    const data = err.response.data as ProblemDetail;
    return typeof data === 'object' ? data : null;
  }
  return null;
}

/**
 * Returns a user-friendly error message based on backend ProblemDetail / ErrorCode.
 */
export function getApiErrorMessage(
  err: unknown,
  fallbackMessage = 'Произошла ошибка при выполнении запроса',
  lang: 'ru' | 'en' = 'ru'
): string {
  const pd = extractProblemDetail(err);
  const messages = lang === 'en' ? ERROR_MESSAGES_EN : ERROR_MESSAGES_RU;

  if (pd) {
    // 1. Check code property from ProblemDetail
    if (pd.code && messages[pd.code]) {
      if (pd.code === 'VALIDATION_ERROR' && pd.errors && Object.keys(pd.errors).length > 0) {
        const details = Object.entries(pd.errors)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join('; ');
        return `${messages[pd.code]} (${details})`;
      }
      return messages[pd.code];
    }

    // 2. Check validation errors
    if (pd.errors && Object.keys(pd.errors).length > 0) {
      return Object.values(pd.errors).join('; ');
    }

    // 3. Check detail
    if (pd.detail && typeof pd.detail === 'string' && pd.detail.trim().length > 0) {
      return pd.detail;
    }

    // 4. Check title or message
    if (pd.message && typeof pd.message === 'string' && pd.message.trim().length > 0) {
      return pd.message;
    }
    if (pd.title && typeof pd.title === 'string' && pd.title.trim().length > 0) {
      return pd.title;
    }
  }

  // 5. Fallback to standard Error message
  if (err instanceof Error && err.message) {
    if (err.message.includes('Network Error')) {
      return lang === 'en'
        ? 'Network error. Please check your connection'
        : 'Сетевая ошибка. Проверьте подключение к интернету';
    }
  }

  return fallbackMessage;
}

/**
 * Extracts field-level validation errors from ProblemDetail.
 */
export function getApiValidationErrors(err: unknown): Record<string, string> | undefined {
  const pd = extractProblemDetail(err);
  return pd?.errors;
}

/**
 * Checks if the error indicates a disabled/blocked account.
 */
export function isAccountDisabledError(err: unknown): boolean {
  if (axios.isAxiosError(err)) {
    if (err.response?.status === 403) {
      const pd = err.response.data as ProblemDetail | undefined;
      return (
        pd?.code === 'ACCOUNT_DISABLED' ||
        pd?.message?.includes('ACCOUNT_DISABLED') ||
        pd?.message?.includes('disabled') ||
        pd?.message?.includes('blocked') ||
        false
      );
    }
  }
  return false;
}
