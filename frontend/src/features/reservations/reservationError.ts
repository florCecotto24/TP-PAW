import type { TFunction } from 'i18next';
import { ApiError } from '../../api/client';
import { apiErrorMessage } from '../auth/errorMessage';

/** Mapea errores de acciones de reserva a copy i18n del área. */
export function reservationActionError(
  t: TFunction,
  err: unknown,
  section: 'new' | 'detail',
): string {
  if (err instanceof ApiError) {
    const fromBody = apiErrorMessage(t, err);
    if (fromBody !== t('error.generic')) return fromBody;
    if (err.status === 409) return t(`res.${section}.actionError.conflict`);
    if (err.status === 403) return t(`res.${section}.actionError.forbidden`);
    if (err.status === 400) return t(`res.${section}.actionError.validation`);
  }
  return t(`res.${section}.actionError.generic`);
}
