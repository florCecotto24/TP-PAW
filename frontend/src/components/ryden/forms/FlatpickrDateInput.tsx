import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import flatpickr from 'flatpickr';
import type { Instance } from 'flatpickr/dist/types/instance';
import { dayStartFromYmd } from '../../FlatpickrCalendar';
import { flatpickrLocalizedInputOptions, wallTodayYmd } from '../../../i18n/dateFormat';

function formatYmd(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export type FlatpickrDateInputProps = {
  id: string;
  value: string;
  onChange: (ymd: string) => void;
  /** When set, dates after this (YYYY-MM-DD) are disabled. Defaults to none. */
  maxDate?: string | 'today';
  /** When set, dates before this (YYYY-MM-DD) are disabled. */
  minDate?: string | 'today';
  className?: string;
  isInvalid?: boolean;
  placeholder?: string;
  autoComplete?: string;
  /** Adds a "clear selection" footer in the calendar (profile birth-date style). */
  allowClear?: boolean;
  clearLabel?: string;
  'aria-invalid'?: boolean | 'true' | 'false';
  'aria-describedby'?: string;
};

/**
 * Single-day Flatpickr input (project theme), same pattern as search from/until pickers.
 * Model value is always ISO {@code YYYY-MM-DD}; the visible field uses locale altFormat.
 */
export default function FlatpickrDateInput({
  id,
  value,
  onChange,
  maxDate,
  minDate,
  className,
  isInvalid,
  placeholder,
  autoComplete,
  allowClear = false,
  clearLabel,
  'aria-invalid': ariaInvalid,
  'aria-describedby': ariaDescribedBy,
}: FlatpickrDateInputProps) {
  const { i18n, t } = useTranslation();
  const inputRef = useRef<HTMLInputElement>(null);
  const fpRef = useRef<Instance | null>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  function resolveBound(bound: string | 'today' | undefined): string | undefined {
    if (bound == null) return undefined;
    return bound === 'today' ? wallTodayYmd() : bound;
  }

  useEffect(() => {
    const node = inputRef.current;
    if (!node) return undefined;

    fpRef.current?.destroy();
    const resolvedClearLabel = clearLabel ?? t('common.clearSelection', { defaultValue: 'Borrar selección' });

    const instance = flatpickr(node, {
      ...flatpickrLocalizedInputOptions(i18n.language),
      disableMobile: true,
      allowInput: false,
      maxDate: resolveBound(maxDate),
      minDate: resolveBound(minDate),
      onChange: (dates) => {
        onChangeRef.current(dates[0] ? formatYmd(dates[0]) : '');
      },
      onReady: (_selected, _dateStr, fp) => {
        const visible = fp.altInput ?? fp.input;
        visible.classList.add('form-control');
        if (className) {
          for (const token of className.split(/\s+/)) {
            if (token) visible.classList.add(token);
          }
        }
        // Keep focus/label targeting on the visible field (altInput is cloned without id).
        if (fp.altInput) {
          fp.altInput.id = id;
          fp.input.removeAttribute('id');
          fp.input.setAttribute('aria-hidden', 'true');
          fp.input.tabIndex = -1;
        }
        if (!allowClear || fp.calendarContainer.querySelector('.flatpickr-profile-clear-footer')) {
          return;
        }
        const footer = document.createElement('div');
        footer.className = 'flatpickr-profile-clear-footer';
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'flatpickr-profile-clear-btn';
        btn.textContent = resolvedClearLabel;
        btn.addEventListener('click', (e) => {
          e.preventDefault();
          e.stopPropagation();
          fp.clear();
          onChangeRef.current('');
          fp.close();
        });
        footer.appendChild(btn);
        fp.calendarContainer.appendChild(footer);
      },
    });
    fpRef.current = instance;

    const initial = value ? dayStartFromYmd(value) : null;
    instance.setDate(initial ?? [], false);

    return () => {
      instance.destroy();
      fpRef.current = null;
    };
    // Re-create when locale / bounds / clear chrome change; value is synced below.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [i18n.language, maxDate, minDate, className, allowClear, clearLabel, id, t]);

  useEffect(() => {
    const fp = fpRef.current;
    if (!fp) return;
    const date = value ? dayStartFromYmd(value) : null;
    fp.setDate(date ?? [], false);
  }, [value]);

  useEffect(() => {
    const fp = fpRef.current;
    const alt = fp?.altInput ?? fp?.input;
    if (!alt) return;
    alt.classList.toggle('is-invalid', Boolean(isInvalid));
    if (ariaInvalid != null) {
      alt.setAttribute('aria-invalid', String(ariaInvalid));
    } else {
      alt.removeAttribute('aria-invalid');
    }
    if (ariaDescribedBy) {
      alt.setAttribute('aria-describedby', ariaDescribedBy);
    } else {
      alt.removeAttribute('aria-describedby');
    }
  }, [isInvalid, ariaInvalid, ariaDescribedBy]);

  return (
    <input
      ref={inputRef}
      id={id}
      type="text"
      className={className}
      readOnly
      placeholder={placeholder}
      autoComplete={autoComplete}
    />
  );
}
