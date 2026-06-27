import { describe, expect, it } from 'vitest';
import { apiErrorMessage } from './errorMessage';
import { ApiError } from '../../api/client';
import i18n from '../../i18n';

const t = i18n.t.bind(i18n) as never;

describe('apiErrorMessage', () => {
  it('mapea un error conocido a su mensaje específico vía la clave de mensaje (no el code corto)', () => {
    // 1.Arrange
    const err = new ApiError(409, {
      status: 409,
      code: 'email_already_exists',
      message: 'user.email.alreadyExists',
    });

    // 2.Act
    const msg = apiErrorMessage(t, err);

    // 3.Assert
    expect(msg).toBe(i18n.t('error.byCode.user.email.alreadyExists'));
    expect(msg).not.toBe(i18n.t('error.generic'));
  });

  it('resuelve la validación de CBU por su clave de mensaje', () => {
    // 1.Arrange
    const err = new ApiError(400, {
      status: 400,
      code: 'cbu_format_invalid',
      message: 'user.profile.cbuInvalid',
    });

    // 2.Act
    const resolved = apiErrorMessage(t, err);

    // 3.Assert
    expect(resolved).toBe(i18n.t('error.byCode.user.profile.cbuInvalid'));
  });

  it('cae al mensaje genérico cuando la clave no existe en el catálogo', () => {
    // 1.Arrange
    const err = new ApiError(400, { status: 400, code: 'nope', message: 'no.such.key' });

    // 2.Act
    const resolved = apiErrorMessage(t, err);

    // 3.Assert
    expect(resolved).toBe(i18n.t('error.generic'));
  });

  it('devuelve el genérico para un error que no es ApiError', () => {
    // 2.Act
    const resolved = apiErrorMessage(t, new Error('boom'));

    // 3.Assert
    expect(resolved).toBe(i18n.t('error.generic'));
  });
});
