import { useCallback, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import flatpickr from 'flatpickr';
import type { Instance } from 'flatpickr/dist/types/instance';
import { flatpickrLocalizedInputOptions } from '../../../i18n/dateFormat';

function formatYmd(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

/**
 * Par de pickers from/until como en components.js (search_from_picker_*).
 * Devuelve {@code clearPickers} para vaciar los inputs visibles al resetear la barra.
 */
export function useSearchDatePickers(
  formId: string,
  from: string,
  until: string,
  onFromChange: (v: string) => void,
  onUntilChange: (v: string) => void,
) {
  const { i18n } = useTranslation();
  const fromFp = useRef<Instance | null>(null);
  const untilFp = useRef<Instance | null>(null);

  const clearPickers = useCallback(() => {
    fromFp.current?.clear();
    untilFp.current?.clear();
    if (untilFp.current) {
      untilFp.current.set('minDate', 'today');
    }
    onFromChange('');
    onUntilChange('');
  }, [onFromChange, onUntilChange]);

  useEffect(() => {
    const fpLocale = flatpickrLocalizedInputOptions(i18n.language);
    const fromPicker = document.getElementById(`search_from_picker_${formId}`);
    const untilPicker = document.getElementById(`search_until_picker_${formId}`);
    if (!fromPicker || !untilPicker) return;

    fromFp.current?.destroy();
    untilFp.current?.destroy();

    fromFp.current = flatpickr(fromPicker, {
      ...fpLocale,
      disableMobile: true,
      minDate: 'today',
      defaultDate: from || undefined,
      onChange: (dates) => {
        onFromChange(dates[0] ? formatYmd(dates[0]) : '');
        if (dates[0] && untilFp.current) {
          untilFp.current.set('minDate', dates[0]);
        }
      },
    });

    untilFp.current = flatpickr(untilPicker, {
      ...fpLocale,
      disableMobile: true,
      minDate: from || 'today',
      defaultDate: until || undefined,
      onChange: (dates) => onUntilChange(dates[0] ? formatYmd(dates[0]) : ''),
    });

    return () => {
      fromFp.current?.destroy();
      untilFp.current?.destroy();
      fromFp.current = null;
      untilFp.current = null;
    };
  }, [formId, i18n.language, from, until, onFromChange, onUntilChange]);

  return { clearPickers };
}
