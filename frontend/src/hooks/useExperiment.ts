import { useState, useEffect } from 'react';
import { getOrAssignExperimentVariant, saveExperimentVariant } from '../utils/abTesting';
import { useAuth } from '../context/AuthContext';
import { abTestApi } from '../api/client';

/**
 * Hook to participate in an A/B test.
 * Assigns one of the provided variants upon first run via deterministic hash with salt,
 * persists it permanently in localStorage, and syncs the assigned color with the backend.
 */
export function useExperiment<T extends string>(
  experimentKey: string,
  variants: readonly T[]
): T {
  const { user } = useAuth();
  const [variant, setVariant] = useState<T>(() => getOrAssignExperimentVariant(experimentKey, variants));

  useEffect(() => {
    let isMounted = true;

    // Fetch deterministic variant from backend
    abTestApi.getVariant(experimentKey)
      .then((res) => {
        if (!isMounted || !res || !res.color) return;
        if (variants.includes(res.color as T) && res.color !== variant) {
          setVariant(res.color as T);
          saveExperimentVariant(experimentKey, res.color);
        }
      })
      .catch((err) => {
        console.warn(`[AB Testing] Could not sync variant with backend for experiment ${experimentKey}:`, err);
      });

    return () => {
      isMounted = false;
    };
  }, [user?.id, experimentKey, variant, variants]);

  return variant;
}
