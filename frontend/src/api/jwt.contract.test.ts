import { afterEach, describe, expect, it, vi } from 'vitest';
import { decodeJwtPayload, isAccessTokenExpired } from './jwt';

function makeUnsignedJwt(payload: Record<string, unknown>): string {
  const encode = (value: unknown): string =>
    btoa(JSON.stringify(value))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  return `${encode({ alg: 'none', typ: 'JWT' })}.${encode(payload)}.sig`;
}

describe('jwt contract (frontend)', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  describe('decodeJwtPayload', () => {
    it('testDecodeJwtPayloadReadsExpClaim', () => {
      // 1.Arrange
      const token = makeUnsignedJwt({ exp: 1_700_000_000, tokenType: 'access' });

      // 2.Act
      const payload = decodeJwtPayload(token);

      // 3.Assert
      expect(payload?.exp).toBe(1_700_000_000);
    });

    it('testDecodeJwtPayloadReturnsNullWhenMalformed', () => {
      // 1.Arrange
      const token = 'not-a-jwt';

      // 2.Act
      const payload = decodeJwtPayload(token);

      // 3.Assert
      expect(payload).toBeNull();
    });
  });

  describe('isAccessTokenExpired', () => {
    it('testIsAccessTokenExpiredTrueWhenPastExp', () => {
      // 1.Arrange
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-07-10T18:40:00Z'));
      const token = makeUnsignedJwt({ exp: Math.floor(Date.now() / 1000) - 60 });

      // 2.Act
      const expired = isAccessTokenExpired(token);

      // 3.Assert
      expect(expired).toBe(true);
    });

    it('testIsAccessTokenExpiredFalseWhenStillValid', () => {
      // 1.Arrange
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-07-10T18:40:00Z'));
      const token = makeUnsignedJwt({ exp: Math.floor(Date.now() / 1000) + 600 });

      // 2.Act
      const expired = isAccessTokenExpired(token);

      // 3.Assert
      expect(expired).toBe(false);
    });

    it('testIsAccessTokenExpiredTrueWithinSkewWindow', () => {
      // 1.Arrange
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-07-10T18:40:00Z'));
      const token = makeUnsignedJwt({ exp: Math.floor(Date.now() / 1000) + 20 });

      // 2.Act
      const expired = isAccessTokenExpired(token, 30);

      // 3.Assert
      expect(expired).toBe(true);
    });

    it('testIsAccessTokenExpiredFalseWhenPayloadHasNoExp', () => {
      // 1.Arrange
      const token = 'not-a-jwt';

      // 2.Act
      const expired = isAccessTokenExpired(token);

      // 3.Assert
      expect(expired).toBe(false);
    });
  });
});
