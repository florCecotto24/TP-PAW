import { describe, expect, it } from 'vitest';
import { ApiError } from '../../api/client';
import { apiErrorMessage } from './errorMessage';

describe('apiErrorMessage', () => {
  const t = ((key: string, options?: { defaultValue?: string } | string) => {
    if (key === 'error.byCode.user.verification.codeInvalid') {
      return 'El código de verificación es inválido o expiró.';
    }
    if (key === 'error.generic') {
      return 'Ocurrió un error. Intentá de nuevo.';
    }
    if (typeof options === 'object' && options?.defaultValue !== undefined) {
      return options.defaultValue;
    }
    if (typeof options === 'string') {
      return options;
    }
    return key;
  }) as unknown as import('i18next').TFunction;

  it('testMapsVerificationCodeInvalidFromErrorDtoCode', () => {
    // 1.Arrange
    const err = new ApiError(401, {
      status: 401,
      code: 'user.verification.codeInvalid',
      message: 'Invalid or expired verification code.',
    });

    // 2.Act
    const message = apiErrorMessage(t, err);

    // 3.Assert
    expect(message).toBe('El código de verificación es inválido o expiró.');
  });

  it('testFallsBackToGenericWhenCodeUnknown', () => {
    // 1.Arrange
    const err = new ApiError(401, {
      status: 401,
      code: 'invalid_credentials',
      message: 'Invalid email or password.',
    });

    // 2.Act / 3.Assert
    expect(apiErrorMessage(t, err)).toBe('Ocurrió un error. Intentá de nuevo.');
  });
});
