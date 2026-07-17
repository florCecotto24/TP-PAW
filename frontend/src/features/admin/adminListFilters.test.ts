import { describe, expect, it } from 'vitest';
import {
  parseAdminCarStatus,
  parseAdminUserFilters,
  withAdminCarStatus,
  withAdminUserFilters,
} from './adminListFilters';

describe('adminListFilters', () => {
  it('testParseAdminCarStatusDropsUnknownValues', () => {
      // 1.Arrange
      const params = new URLSearchParams('status=hacked&page=2');

      // 2.Act
      const status = parseAdminCarStatus(params);

      // 3.Assert
      expect(status).toBe('');
    });

    it('testParseAdminCarStatusAcceptsWhitelist', () => {
      // 1.Arrange / 2.Act / 3.Assert
      expect(parseAdminCarStatus(new URLSearchParams('status=admin_paused'))).toBe('admin_paused');
      expect(parseAdminCarStatus(new URLSearchParams('status=all'))).toBe('');
    });

    it('testWithAdminCarStatusOmitsEmpty', () => {
      // 1.Arrange
      const params = new URLSearchParams('status=paused&page=1');

      // 2.Act
      const next = withAdminCarStatus(params, '');

      // 3.Assert
      expect(next.get('status')).toBeNull();
      expect(next.get('page')).toBe('1');
    });

    it('testParseAdminUserFiltersSanitizesRoleAndBlocked', () => {
      // 1.Arrange
      const params = new URLSearchParams('q=%20ana%20&role=super&blocked=maybe');

      // 2.Act
      const filters = parseAdminUserFilters(params);

      // 3.Assert
      expect(filters).toEqual({ q: 'ana', role: '', blocked: '' });
    });

    it('testWithAdminUserFiltersRoundTrip', () => {
      // 1.Arrange
      const params = new URLSearchParams('page=3');

      // 2.Act
      const next = withAdminUserFilters(params, { q: 'bob', role: 'admin', blocked: 'true' });

      // 3.Assert
      expect(parseAdminUserFilters(next)).toEqual({ q: 'bob', role: 'admin', blocked: 'true' });
      expect(next.get('page')).toBe('3');
    });
});
