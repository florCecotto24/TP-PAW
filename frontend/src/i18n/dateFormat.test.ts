import { describe, expect, it } from 'vitest';
import {
  flatpickrDisplayDateFormat,
  formatDate,
  formatDateTime,
  formatMonthYear,
  resolveIntlLocale,
  shiftMonth,
} from './dateFormat';

describe('dateFormat.resolveIntlLocale', () => {
  it('maps es to es-AR', () => {
    expect(resolveIntlLocale('es')).toBe('es-AR');
  });

  it('maps en to en-US', () => {
    expect(resolveIntlLocale('en')).toBe('en-US');
  });
});

describe('dateFormat.flatpickrDisplayDateFormat', () => {
  it('uses d/m/Y for Spanish', () => {
    expect(flatpickrDisplayDateFormat('es')).toBe('d/m/Y');
  });

  it('uses M j, Y for English', () => {
    expect(flatpickrDisplayDateFormat('en')).toBe('M j, Y');
  });
});

describe('dateFormat.formatDate', () => {
  it('formats ISO dates as dd/mm/yyyy in Spanish', () => {
    expect(formatDate('2026-01-05', 'es')).toBe('05/01/2026');
  });

  it('formats ISO dates with month name in English', () => {
    expect(formatDate('2026-01-05', 'en')).toBe('Jan 5, 2026');
  });
});

describe('dateFormat.formatDateTime', () => {
  it('formats date-time with dd/mm/yyyy date in Spanish', () => {
    const formatted = formatDateTime('2026-01-05T15:30:00-03:00', 'es');
    expect(formatted.startsWith('05/01/2026')).toBe(true);
    expect(formatted).toMatch(/\d{1,2}:\d{2}/);
  });
});

describe('dateFormat.formatMonthYear', () => {
  it('formats YYYY-MM as month name and year in Spanish', () => {
    expect(formatMonthYear('2026-07', 'es')).toBe('Julio 2026');
  });

  it('formats YYYY-MM as month name and year in English', () => {
    expect(formatMonthYear('2026-07', 'en')).toBe('July 2026');
  });
});

describe('dateFormat.shiftMonth', () => {
  it('adds one month', () => {
    expect(shiftMonth('2026-07', 1)).toBe('2026-08');
  });

  it('subtracts one month across year boundary', () => {
    expect(shiftMonth('2026-01', -1)).toBe('2025-12');
  });
});
