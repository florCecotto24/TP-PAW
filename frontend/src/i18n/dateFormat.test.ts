import { describe, expect, it } from 'vitest';
import { formatDateTime, formatMonthYear } from './dateFormat';

describe('dateFormat', () => {
  describe('formatDateTime', () => {
    it('testFormatDateTimeDoesNotThrowForEnglish', () => {
      // 1.Arrange
      const iso = '2026-07-10T15:30:00.000Z';

      // 2.Act
      const label = formatDateTime(iso, 'en');

      // 3.Assert
      expect(label.length).toBeGreaterThan(0);
      expect(label).not.toBe(iso);
    });

    it('testFormatDateTimeDoesNotThrowForSpanish', () => {
      // 1.Arrange
      const iso = '2026-07-10T15:30:00.000Z';

      // 2.Act
      const label = formatDateTime(iso, 'es');

      // 3.Assert
      expect(label.length).toBeGreaterThan(0);
      expect(label).not.toBe(iso);
    });

    it('testFormatDateTimeReturnsEmptyForMissingValue', () => {
      // 1.Arrange / 2.Act / 3.Assert
      expect(formatDateTime(null, 'en')).toBe('');
      expect(formatDateTime(undefined, 'es')).toBe('');
    });
  });

  describe('formatMonthYear', () => {
    it('testFormatMonthYearFromIsoDateEnglish', () => {
      // 1.Arrange
      const isoDate = '2026-04-28';

      // 2.Act
      const label = formatMonthYear(isoDate, 'en');

      // 3.Assert
      expect(label).toContain('April');
      expect(label).toContain('2026');
    });

    it('testFormatMonthYearFromIsoDateSpanish', () => {
      // 1.Arrange
      const isoDate = '2026-04-28';

      // 2.Act
      const label = formatMonthYear(isoDate, 'es');

      // 3.Assert
      expect(label.toLowerCase()).toContain('abril');
      expect(label).toContain('2026');
    });

    it('testFormatMonthYearFromYearMonth', () => {
      // 1.Arrange / 2.Act / 3.Assert
      expect(formatMonthYear('2026-04', 'en')).toContain('April');
      expect(formatMonthYear('2026-04', 'en')).toContain('2026');
    });
  });
});
