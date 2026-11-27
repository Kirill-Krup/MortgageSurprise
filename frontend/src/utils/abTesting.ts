import type { ExperimentConfig, ExperimentVariantConfig } from '../types';

export type ButtonColorVariant = 'blue' | 'emerald' | 'purple' | 'amber' | 'red' | 'rose' | string;

export const BUTTON_COLOR_VARIANTS: readonly ButtonColorVariant[] = ['blue', 'emerald', 'red', 'purple', 'amber'] as const;

export const EXPERIMENT_CONFIGS_KEY = 'mortgage_surprise_ab_configs';
export const STORAGE_PREFIX = 'mortgage_surprise_ab_';
const SALT = 'mortgage_surprise_ab_salt_2026';

export const DEFAULT_EXPERIMENTS: ExperimentConfig[] = [
  {
    id: 'mortgage_calculate_button',
    name: 'Кнопка расчета ипотеки',
    description: 'Тестирование цвета основной кнопки расчета в ипотечном калькуляторе (синий vs зеленый)',
    status: 'ACTIVE',
    isCustom: false,
    variants: [
      { name: 'blue', weight: 50, description: 'Синяя кнопка (дефолт)' },
      { name: 'emerald', weight: 50, description: 'Изумрудная кнопка (акцент)' },
    ],
    createdAt: '2026-01-01T00:00:00.000Z',
  },
  {
    id: 'apply_microloan_button',
    name: 'Кнопка оформления микрозайма',
    description: 'Тестирование цвета кнопки подачи заявки на микрозайм (синий vs зеленый)',
    status: 'ACTIVE',
    isCustom: false,
    variants: [
      { name: 'blue', weight: 50, description: 'Синяя кнопка (дефолт)' },
      { name: 'emerald', weight: 50, description: 'Изумрудная кнопка (акцент)' },
    ],
    createdAt: '2026-01-01T00:00:00.000Z',
  },
];

export function deterministicHash(input: string): number {
  let hash = 0;
  for (let i = 0; i < input.length; i++) {
    hash = (hash << 5) - hash + input.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
}

export function getAnonymousClientId(): string {
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
 * Retrieve all registered experiments from localStorage, falling back to defaults.
 */
export function getExperimentConfigs(): ExperimentConfig[] {
  try {
    const raw = localStorage.getItem(EXPERIMENT_CONFIGS_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed) && parsed.length > 0) {
        const map = new Map<string, ExperimentConfig>();
        DEFAULT_EXPERIMENTS.forEach((exp) => map.set(exp.id, { ...exp }));
        parsed.forEach((exp: ExperimentConfig) => {
          if (exp && exp.id) {
            map.set(exp.id, exp);
          }
        });
        return Array.from(map.values());
      }
    }
  } catch (error) {
    console.warn('[AB Testing] Failed to read experiment configs from storage:', error);
  }
  return DEFAULT_EXPERIMENTS.map((exp) => ({ ...exp }));
}

/**
 * Get single experiment configuration by key/ID.
 */
export function getExperimentConfig(experimentKey: string): ExperimentConfig | null {
  const configs = getExperimentConfigs();
  return configs.find((c) => c.id === experimentKey) || null;
}

/**
 * Save or update experiment configuration.
 */
export function saveExperimentConfig(config: ExperimentConfig): ExperimentConfig {
  const configs = getExperimentConfigs();
  const index = configs.findIndex((c) => c.id === config.id);
  const now = new Date().toISOString();

  const toSave: ExperimentConfig = {
    ...config,
    updatedAt: now,
    createdAt: config.createdAt || now,
    isCustom: config.isCustom !== undefined ? config.isCustom : !DEFAULT_EXPERIMENTS.some((d) => d.id === config.id),
  };

  if (index >= 0) {
    configs[index] = toSave;
  } else {
    configs.push(toSave);
  }

  try {
    localStorage.setItem(EXPERIMENT_CONFIGS_KEY, JSON.stringify(configs));
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('ab_configs_changed', { detail: toSave }));
    }
  } catch (error) {
    console.warn('[AB Testing] Failed to save experiment config to storage:', error);
  }

  return toSave;
}

/**
 * Delete a custom experiment or reset a default experiment to original values.
 */
export function deleteExperimentConfig(experimentKey: string): boolean {
  try {
    const configs = getExperimentConfigs();
    const isDefault = DEFAULT_EXPERIMENTS.some((d) => d.id === experimentKey);
    let updated: ExperimentConfig[];

    if (isDefault) {
      const defaultExp = DEFAULT_EXPERIMENTS.find((d) => d.id === experimentKey)!;
      updated = configs.map((c) => (c.id === experimentKey ? { ...defaultExp } : c));
    } else {
      updated = configs.filter((c) => c.id !== experimentKey);
    }

    localStorage.setItem(EXPERIMENT_CONFIGS_KEY, JSON.stringify(updated));
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('ab_configs_changed', { detail: { id: experimentKey, deleted: true } }));
    }
    return true;
  } catch (error) {
    console.warn('[AB Testing] Failed to delete experiment config:', error);
    return false;
  }
}

/**
 * Weighted deterministic variant selection using hash & salt.
 */
export function selectWeightedVariant<T extends string>(
  seed: string,
  variantsWithWeights: Array<{ name: T; weight: number }>
): T {
  if (variantsWithWeights.length === 0) {
    throw new Error('Variants list cannot be empty');
  }

  const totalWeight = variantsWithWeights.reduce((sum, v) => sum + Math.max(0, v.weight), 0);
  if (totalWeight <= 0) {
    return variantsWithWeights[0].name;
  }

  const hash = deterministicHash(seed);
  const slot = hash % totalWeight;

  let accumulated = 0;
  for (const item of variantsWithWeights) {
    accumulated += Math.max(0, item.weight);
    if (slot < accumulated) {
      return item.name;
    }
  }

  return variantsWithWeights[variantsWithWeights.length - 1].name;
}

/**
 * Assigns or retrieves an experiment variant according to configured weights / percentages.
 */
export function getOrAssignExperimentVariant<T extends string>(
  experimentKey: string,
  variants?: readonly T[],
  userId?: string
): T {
  const fullKey = `${STORAGE_PREFIX}${experimentKey}`;

  // Sticky assignment: if this client already received an assigned variant, keep it permanently
  try {
    const saved = localStorage.getItem(fullKey);
    if (saved) {
      return saved as T;
    }
  } catch {
    // storage unavailable
  }

  const config = getExperimentConfig(experimentKey);

  // If experiment has custom config with variants and status is ACTIVE
  if (config && config.variants && config.variants.length > 0 && config.status !== 'PAUSED') {
    const validVariants = config.variants.map((v: ExperimentVariantConfig) => ({
      name: v.name as T,
      weight: v.weight,
    }));

    const seed = userId
      ? `${userId}:${experimentKey}:${SALT}`
      : `${getAnonymousClientId()}:${experimentKey}:${SALT}`;

    const assigned = selectWeightedVariant(seed, validVariants);

    try {
      localStorage.setItem(fullKey, assigned);
    } catch {
      // storage unavailable
    }

    return assigned;
  }

  // Fallback to provided variants with equal distribution
  const fallbackVariants = (variants && variants.length > 0 ? variants : ['blue', 'emerald']) as readonly T[];
  try {
    if (userId) {
      const hash = deterministicHash(`${userId}:${experimentKey}:${SALT}`);
      return fallbackVariants[hash % fallbackVariants.length];
    }

    const saved = localStorage.getItem(fullKey);
    if (saved && fallbackVariants.includes(saved as T)) {
      return saved as T;
    }

    const clientId = getAnonymousClientId();
    const hash = deterministicHash(`${clientId}:${experimentKey}:${SALT}`);
    const assigned = fallbackVariants[hash % fallbackVariants.length];
    localStorage.setItem(fullKey, assigned);
    return assigned;
  } catch (error) {
    console.warn(`[AB Testing] Failed to access localStorage for key "${fullKey}":`, error);
    return fallbackVariants[0];
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
 * Helper to get inline styling for buttons participating in color A/B test (supports HEX and RGB).
 */
export function getButtonColorStyle(variant: ButtonColorVariant | string | undefined): React.CSSProperties {
  if (!variant) return {};
  const v = variant.trim();
  if (v.startsWith('#') || v.startsWith('rgb')) {
    return {
      backgroundColor: v,
      borderColor: v,
      boxShadow: `0 10px 15px -3px ${v}40, 0 4px 6px -4px ${v}30`,
    };
  }
  return {};
}

/**
 * Helper to get styling classes for buttons participating in color A/B test.
 */
export function getMortgageCalculateButtonClasses(variant: ButtonColorVariant | string): string {
  const v = (variant || '').toLowerCase();
  if (v.includes('emerald') || v.includes('green')) {
    return 'bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 shadow-emerald-600/20';
  }
  if (v.includes('purple') || v.includes('violet')) {
    return 'bg-purple-600 hover:bg-purple-700 active:bg-purple-800 shadow-purple-600/20';
  }
  if (v.includes('amber') || v.includes('orange') || v.includes('yellow')) {
    return 'bg-amber-600 hover:bg-amber-700 active:bg-amber-800 shadow-amber-600/20';
  }
  if (v.includes('red') || v.includes('rose')) {
    return 'bg-rose-600 hover:bg-rose-700 active:bg-rose-800 shadow-rose-600/20';
  }
  if (v.includes('cyan') || v.includes('teal')) {
    return 'bg-cyan-600 hover:bg-cyan-700 active:bg-cyan-800 shadow-cyan-600/20';
  }
  if (v.includes('pink')) {
    return 'bg-pink-600 hover:bg-pink-700 active:bg-pink-800 shadow-pink-600/20';
  }
  return 'bg-blue-700 hover:bg-blue-800 active:bg-blue-900 shadow-blue-700/20';
}

export function getMicroloanApplyButtonClasses(variant: ButtonColorVariant | string): string {
  const v = (variant || '').toLowerCase();
  if (v.includes('emerald') || v.includes('green')) {
    return 'bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 shadow-emerald-600/20';
  }
  if (v.includes('purple') || v.includes('violet')) {
    return 'bg-purple-600 hover:bg-purple-700 active:bg-purple-800 shadow-purple-600/20';
  }
  if (v.includes('amber') || v.includes('orange') || v.includes('yellow')) {
    return 'bg-amber-600 hover:bg-amber-700 active:bg-amber-800 shadow-amber-600/20';
  }
  if (v.includes('red') || v.includes('rose')) {
    return 'bg-rose-600 hover:bg-rose-700 active:bg-rose-800 shadow-rose-600/20';
  }
  if (v.includes('cyan') || v.includes('teal')) {
    return 'bg-cyan-600 hover:bg-cyan-700 active:bg-cyan-800 shadow-cyan-600/20';
  }
  if (v.includes('pink')) {
    return 'bg-pink-600 hover:bg-pink-700 active:bg-pink-800 shadow-pink-600/20';
  }
  return 'bg-blue-600 hover:bg-blue-700 active:bg-blue-800 shadow-blue-600/20';
}
