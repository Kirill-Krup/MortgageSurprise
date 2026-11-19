export type ButtonColorVariant = 'blue' | 'emerald';

export const BUTTON_COLOR_VARIANTS: readonly ButtonColorVariant[] = ['blue', 'emerald'] as const;

const STORAGE_PREFIX = 'mortgage_surprise_ab_';
const SALT = 'mortgage_surprise_ab_salt_2026';

function deterministicHash(input: string): number {
  let hash = 0;
  for (let i = 0; i < input.length; i++) {
    hash = (hash << 5) - hash + input.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
}

function getAnonymousClientId(): string {
  try {
    let clientId = localStorage.getItem('mortgage_surprise_client_id');
    if (!clientId) {
      clientId = 'client_' + Math.random().toString(36).substring(2, 15);
      localStorage.setItem('mortgage_surprise_client_id', clientId);
    }
    return clientId;
  } catch {
    return 'anonymous';
  }
}

/**
 * Retrieves an existing variant from localStorage or deterministically assigns one via hash with salt.
 */
export function getOrAssignExperimentVariant<T extends string>(
  experimentKey: string,
  variants: readonly T[]
): T {
  if (variants.length === 0) {
    throw new Error('Experiment must have at least one variant');
  }

  const fullKey = `${STORAGE_PREFIX}${experimentKey}`;

  try {
    const saved = localStorage.getItem(fullKey);
    if (saved && variants.includes(saved as T)) {
      return saved as T;
    }

    const clientId = getAnonymousClientId();
    const hash = deterministicHash(`${clientId}:${experimentKey}:${SALT}`);
    const assigned = variants[hash % variants.length];
    localStorage.setItem(fullKey, assigned);
    return assigned;
  } catch (error) {
    console.warn(`[AB Testing] Failed to access localStorage for key "${fullKey}":`, error);
    return variants[0];
  }
}

export function saveExperimentVariant(experimentKey: string, variant: string): void {
  try {
    localStorage.setItem(`${STORAGE_PREFIX}${experimentKey}`, variant);
  } catch (error) {
    console.warn(`[AB Testing] Failed to save localStorage key for "${experimentKey}":`, error);
  }
}

/**
 * Helper to get styling classes for buttons participating in color A/B test.
 */
export function getMortgageCalculateButtonClasses(variant: ButtonColorVariant): string {
  if (variant === 'emerald') {
    return 'bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 shadow-emerald-600/20';
  }
  return 'bg-blue-700 hover:bg-blue-800 active:bg-blue-900 shadow-blue-700/20';
}

export function getMicroloanApplyButtonClasses(variant: ButtonColorVariant): string {
  if (variant === 'emerald') {
    return 'bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 shadow-emerald-600/20';
  }
  return 'bg-blue-600 hover:bg-blue-700 active:bg-blue-800 shadow-blue-600/20';
}
