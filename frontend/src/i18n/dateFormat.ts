import { Spanish } from 'flatpickr/dist/l10n/es.js';
import type { CustomLocale } from 'flatpickr/dist/types/locale';
import type { Options } from 'flatpickr/dist/types/options';

/** Zona horaria de negocio (Argentina). Alineado con AppTimezone.WALL_ZONE del backend. */
export const APP_TIMEZONE = 'America/Argentina/Buenos_Aires';

/** Mapea el código i18next (`es`, `en`, …) a un locale BCP 47 para Intl. */
export function resolveIntlLocale(language: string | undefined): string {
  const lang = (language || 'es').toLowerCase();
  if (lang.startsWith('es')) return 'es-AR';
  return 'en-US';
}

export function isSpanishLanguage(language: string | undefined): boolean {
  return (language || 'es').toLowerCase().startsWith('es');
}

/** Tokens Flatpickr para el input visible (legacy: ES dd/MM/yyyy, EN MMM d, yyyy). */
export function flatpickrDisplayDateFormat(language: string | undefined): string {
  return isSpanishLanguage(language) ? 'd/m/Y' : 'M j, Y';
}

export function flatpickrLocale(language: string | undefined): CustomLocale | 'default' {
  return isSpanishLanguage(language) ? Spanish : 'default';
}

/** Opciones Flatpickr para inputs visibles: valor ISO en el modelo, formato local en pantalla. */
export function flatpickrLocalizedInputOptions(
  language: string | undefined,
): Pick<Options, 'locale' | 'dateFormat' | 'altInput' | 'altFormat'> {
  return {
    locale: flatpickrLocale(language),
    dateFormat: 'Y-m-d',
    altInput: true,
    altFormat: flatpickrDisplayDateFormat(language),
  };
}

function parseDateValue(value: string | null | undefined): Date | null {
  if (value == null || value === '') return null;
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [y, m, d] = value.split('-').map(Number);
    return new Date(y, m - 1, d, 12, 0, 0, 0);
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

/** Fecha corta: ES dd/mm/yyyy; EN MMM d, yyyy. */
export function formatDate(
  value: string | Date | null | undefined,
  language?: string,
): string {
  const d = value instanceof Date ? value : parseDateValue(value);
  if (!d || Number.isNaN(d.getTime())) {
    return typeof value === 'string' ? value : '';
  }
  const locale = resolveIntlLocale(language);
  if (isSpanishLanguage(language)) {
    return new Intl.DateTimeFormat(locale, {
      timeZone: APP_TIMEZONE,
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(d);
  }
  return new Intl.DateTimeFormat(locale, {
    timeZone: APP_TIMEZONE,
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(d);
}

/** Fecha con hora en la zona de negocio. */
export function formatDateTime(value: string | null | undefined, language?: string): string {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  const locale = resolveIntlLocale(language);
  const opts: Intl.DateTimeFormatOptions = {
    timeZone: APP_TIMEZONE,
    hour: '2-digit',
    minute: '2-digit',
  };
  if (isSpanishLanguage(language)) {
    return new Intl.DateTimeFormat(locale, {
      ...opts,
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(d);
  }
  return new Intl.DateTimeFormat(locale, {
    ...opts,
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(d);
}

/** Fecha larga para reseñas y encabezados de chat (p. ej. "5 ene 2026" / "Jan 5, 2026"). */
export function formatDateLong(value: string | null | undefined, language?: string): string {
  if (!value) return '';
  const d = parseDateValue(value) ?? new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  const locale = resolveIntlLocale(language);
  return new Intl.DateTimeFormat(locale, {
    timeZone: APP_TIMEZONE,
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(d);
}

/** Hora corta en la zona de negocio. */
export function formatTime(value: string | null | undefined, language?: string): string {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  const locale = resolveIntlLocale(language);
  return new Intl.DateTimeFormat(locale, {
    timeZone: APP_TIMEZONE,
    hour: '2-digit',
    minute: '2-digit',
  }).format(d);
}

export function formatDateRange(
  start: string | null | undefined,
  end: string | null | undefined,
  language?: string,
): string {
  const left = start ? formatDate(start, language) : '…';
  const right = end ? formatDate(end, language) : '…';
  return `${left} – ${right}`;
}

/** Mes calendario (`YYYY-MM`) legible: "Julio 2026" / "July 2026". */
export function formatMonthYear(yyyyMm: string, language?: string): string {
  const match = /^(\d{4})-(\d{2})$/.exec(yyyyMm);
  if (!match) return yyyyMm;
  const y = Number(match[1]);
  const m = Number(match[2]);
  if (m < 1 || m > 12) return yyyyMm;
  const d = new Date(y, m - 1, 1);
  const locale = resolveIntlLocale(language);
  const parts = new Intl.DateTimeFormat(locale, {
    month: 'long',
    year: 'numeric',
    timeZone: APP_TIMEZONE,
  }).formatToParts(d);
  const monthPart = parts.find((p) => p.type === 'month')?.value ?? '';
  const yearPart = parts.find((p) => p.type === 'year')?.value ?? '';
  const label = `${monthPart} ${yearPart}`.trim();
  return label ? label.charAt(0).toUpperCase() + label.slice(1) : yyyyMm;
}

/** Suma o resta meses a un valor `YYYY-MM`. */
export function shiftMonth(yyyyMm: string, delta: number): string {
  const match = /^(\d{4})-(\d{2})$/.exec(yyyyMm);
  if (!match) return yyyyMm;
  const y = Number(match[1]);
  const m = Number(match[2]);
  const d = new Date(y, m - 1 + delta, 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}
