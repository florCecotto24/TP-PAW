import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  buildFlexMonthOptions,
  clampFlexDays,
  daysInFlexMonth,
  parseFlexDays,
} from '../flexibleSearch';

export interface FlexibleDateControlsProps {
  formId: string;
  flexMonth: string;
  flexDays: string;
  onFlexMonthChange: (value: string) => void;
  onFlexDaysChange: (value: string) => void;
}

/** Month + days inputs for flexible search (JSP {@code searchWithFilters} flex block). */
export default function FlexibleDateControls({
  formId,
  flexMonth,
  flexDays,
  onFlexMonthChange,
  onFlexDaysChange,
}: FlexibleDateControlsProps) {
  const { t, i18n } = useTranslation();
  const monthOptions = useMemo(
    () => buildFlexMonthOptions(i18n.language, flexMonth || undefined),
    [i18n.language, flexMonth],
  );
  const [monthOpen, setMonthOpen] = useState(false);

  const resolvedMonth =
    flexMonth && monthOptions.some((o) => o.value === flexMonth)
      ? flexMonth
      : monthOptions[0]?.value ?? '';

  useEffect(() => {
    if (!flexMonth && monthOptions[0]) {
      onFlexMonthChange(monthOptions[0].value);
    }
  }, [flexMonth, monthOptions, onFlexMonthChange]);

  const maxDays = daysInFlexMonth(resolvedMonth);
  const daysValue = flexDays;
  const hasDaysValue = daysValue.trim() !== '';
  const selectedMonthLabel =
    monthOptions.find((o) => o.value === resolvedMonth)?.label ?? '\u2014';

  const syncDaysMax = (month: string, days: string) => {
    const max = daysInFlexMonth(month);
    const parsed = parseFlexDays(days, max);
    if (parsed != null && parsed !== Number(days)) {
      onFlexDaysChange(String(parsed));
    }
  };

  const onSelectMonth = (value: string) => {
    onFlexMonthChange(value);
    syncDaysMax(value, flexDays);
    setMonthOpen(false);
  };

  const onDaysInput = (raw: string) => {
    if (raw.trim() === '') {
      onFlexDaysChange('');
      return;
    }
    const parsed = parseFlexDays(raw, maxDays);
    onFlexDaysChange(parsed != null ? String(parsed) : raw);
  };

  const decDays = () => {
    const cur = parseFlexDays(daysValue, maxDays);
    onFlexDaysChange(cur == null || cur <= 1 ? '' : String(clampFlexDays(cur - 1, maxDays)));
  };

  const incDays = () => {
    const cur = parseFlexDays(daysValue, maxDays);
    onFlexDaysChange(String(clampFlexDays(cur == null ? 1 : cur + 1, maxDays)));
  };

  return (
    <div
      className="js-flexible-controls"
      style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: '.5rem' }}
    >
      <div>
        <label className="form-label small text-secondary mb-1" htmlFor={`search_flexmonth_${formId}`}>
          {t('search.flexible.month.label')}
        </label>
        <div className={`dropdown js-flex-month-wrapper${monthOpen ? ' show' : ''}`}>
          <button
            type="button"
            id={`search_flexmonth_${formId}`}
            className="form-control form-control-sm border-0 shadow-none dropdown-toggle d-flex align-items-center w-100 text-start"
            aria-expanded={monthOpen}
            onClick={() => setMonthOpen((open) => !open)}
          >
            <span className="text-truncate min-w-0 js-flex-month-display">{selectedMonthLabel}</span>
          </button>
          <div
            className={`dropdown-menu shadow js-flex-month-menu${monthOpen ? ' show' : ''}`}
            style={{ maxHeight: '14rem', overflowY: 'auto', minWidth: '100%' }}
          >
            {monthOptions.map((option) => (
              <button
                key={option.value}
                type="button"
                className={`dropdown-item small px-3 py-1${option.value === resolvedMonth ? ' active' : ''}`}
                data-val={option.value}
                onClick={() => onSelectMonth(option.value)}
              >
                {option.label}
              </button>
            ))}
          </div>
          <input type="hidden" name="flexMonth" className="js-flex-month-hidden" value={resolvedMonth} readOnly />
        </div>
      </div>
      <div>
        <label className="form-label small text-secondary mb-1" htmlFor={`search_flexdays_${formId}`}>
          {t('search.flexible.days.label')}
        </label>
        <div className="d-flex align-items-center gap-0">
          <button
            type="button"
            className={`btn rounded-circle border border-primary text-primary bg-transparent flex-shrink-0 d-flex align-items-center justify-content-center js-flexdays-dec${hasDaysValue ? '' : ' d-none'}`}
            style={{ width: '1.75rem', height: '1.75rem', padding: 0 }}
            aria-label={t('search.flexible.decrementAria')}
            onClick={decDays}
          >
            <i className="bi bi-dash" style={{ fontSize: '1rem', lineHeight: 1 }} aria-hidden="true"></i>
          </button>
          <input
            type="number"
            name="flexDays"
            id={`search_flexdays_${formId}`}
            min={1}
            max={maxDays}
            step={1}
            className={`form-control form-control-sm border-0 shadow-none js-flexdays-input ryden-no-spinner${hasDaysValue ? ' text-center' : ''}`}
            style={hasDaysValue ? { width: '3.5rem' } : undefined}
            placeholder={t('search.flexible.anyDays')}
            value={daysValue}
            onChange={(e) => onDaysInput(e.target.value)}
          />
          <button
            type="button"
            className={`btn rounded-circle border border-primary text-primary bg-transparent flex-shrink-0 d-flex align-items-center justify-content-center js-flexdays-inc${hasDaysValue ? '' : ' d-none'}`}
            style={{ width: '1.75rem', height: '1.75rem', padding: 0 }}
            aria-label={t('search.flexible.incrementAria')}
            onClick={incDays}
          >
            <i className="bi bi-plus" style={{ fontSize: '1rem', lineHeight: 1 }} aria-hidden="true"></i>
          </button>
        </div>
      </div>
    </div>
  );
}
