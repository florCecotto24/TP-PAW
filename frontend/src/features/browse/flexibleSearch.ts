const FLEX_MONTH = /^\d{4}-\d{2}$/;

export interface FlexMonthOption {
  value: string;
  label: string;
}

/** Days in month for a {@code YYYY-MM} string (defaults to 31 when invalid). */
export function daysInFlexMonth(yyyyMm: string): number {
  const parts = yyyyMm.split('-');
  if (parts.length < 2) return 31;
  const year = Number(parts[0]);
  const month = Number(parts[1]);
  if (!Number.isFinite(year) || !Number.isFinite(month) || month < 1 || month > 12) return 31;
  return new Date(year, month, 0).getDate();
}

/** Next 12 calendar months from today (wall), for the flex-month dropdown. */
export function buildFlexMonthOptions(locale: string, selected?: string): FlexMonthOption[] {
  const now = new Date();
  const options: FlexMonthOption[] = [];
  for (let i = 0; i < 12; i += 1) {
    const d = new Date(now.getFullYear(), now.getMonth() + i, 1);
    const y = d.getFullYear();
    const mo = d.getMonth() + 1;
    const value = `${y}-${mo < 10 ? `0${mo}` : mo}`;
    let label = d.toLocaleDateString(locale, { month: 'long', year: 'numeric' });
    label = label.charAt(0).toUpperCase() + label.slice(1);
    options.push({ value, label });
  }
  if (selected && FLEX_MONTH.test(selected) && !options.some((o) => o.value === selected)) {
    const [y, m] = selected.split('-').map(Number);
    const d = new Date(y, m - 1, 1);
    let label = d.toLocaleDateString(locale, { month: 'long', year: 'numeric' });
    label = label.charAt(0).toUpperCase() + label.slice(1);
    options.unshift({ value: selected, label });
  }
  return options;
}

export function parseFlexMonth(value: string | null | undefined): string | undefined {
  return value != null && FLEX_MONTH.test(value) ? value : undefined;
}

export function parseFlexDays(value: string | null | undefined, maxDays = 31): number | undefined {
  if (value == null || value.trim() === '') return undefined;
  const n = Number(value);
  if (!Number.isFinite(n) || !Number.isInteger(n) || n < 1) return undefined;
  return Math.min(n, maxDays);
}

export function clampFlexDays(value: number, maxDays: number): number {
  return Math.min(maxDays, Math.max(1, value));
}
