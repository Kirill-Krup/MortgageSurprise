import { useState, useEffect } from 'react';
import { getOrAssignExperimentVariant, saveExperimentVariant, STORAGE_PREFIX } from '../utils/abTesting';
import { useAuth } from '../context/AuthContext';
import { abTestApi } from '../api/client';

/**
 * Hook to participate in an A/B test.
 * Assigns one of the provided/configured variants upon first run via deterministic hash with salt
 * and percentage weights distribution, persists it permanently in localStorage, and syncs with backend.
 */
export function useExperiment<T extends string>(
    experimentKey: string,
    variants?: readonly T[]
): T {
    const { user } = useAuth();
    const [variant, setVariant] = useState<T>(() =>
        getOrAssignExperimentVariant(experimentKey, variants, user?.id)
    );

    useEffect(() => {
        setVariant(getOrAssignExperimentVariant(experimentKey, variants, user?.id));
    }, [user?.id, experimentKey, variants]);

    // React to live changes in experiment configuration made by admin
    // Sticky rule: If the user has already received a variant, their assigned variant stays permanently.
    useEffect(() => {
        const handleConfigChange = (e: Event) => {
            const customEvent = e as CustomEvent<{ id?: string }>;
            if (!customEvent.detail || customEvent.detail.id === experimentKey) {
                const fullKey = `${STORAGE_PREFIX}${experimentKey}`;
                try {
                    const saved = localStorage.getItem(fullKey);
                    if (saved) {
                        return;
                    }
                } catch {
                    // ignore
                }
                setVariant(getOrAssignExperimentVariant(experimentKey, variants, user?.id));
            }
        };

        if (typeof window !== 'undefined') {
            window.addEventListener('ab_configs_changed', handleConfigChange);
            return () => {
                window.removeEventListener('ab_configs_changed', handleConfigChange);
            };
        }
    }, [experimentKey, user?.id, variants]);

    useEffect(() => {
        // Only sync with backend for authenticated users who have a persistent profile in DB
        if (!user?.id) return;

        let isMounted = true;
        abTestApi.getVariant(experimentKey)
            .then((res) => {
                if (!isMounted || !res?.color) return;
                saveExperimentVariant(experimentKey, res.color);
                setVariant(res.color as T);
            })
            .catch((err) => console.warn('[AB Testing] sync failed:', err));
        return () => { isMounted = false; };
    }, [user?.id, experimentKey, variants]);

    return variant;
}
