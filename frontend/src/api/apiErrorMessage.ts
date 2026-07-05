import type { TFunction } from 'i18next';
import { ApiError } from './client';
import type { ErrorDto } from './types';
import { publishValidationI18nParams } from '../features/owner/publishCarValidation';

const VALIDATION_KEY_TO_I18N: Record<string, string> = {
  'validation.year.min': 'owner.publish.errors.yearMin',
  'validation.year.max': 'owner.publish.errors.yearMax',
  'validation.plate.notBlank': 'owner.publish.errors.plateRequired',
  'validation.plate.size': 'owner.publish.errors.plateSize',
  'validation.brand.notBlank': 'owner.publish.errors.brandRequired',
  'validation.brand.size': 'owner.publish.errors.brandSize',
  'validation.model.notBlank': 'owner.publish.errors.modelRequired',
  'validation.model.size': 'owner.publish.errors.modelSize',
  'validation.description.size': 'owner.publish.errors.descriptionSize',
  'validation.noPunctuation': 'owner.publish.errors.noPunctuation',
  'validation.type.notNull': 'owner.publish.errors.typeRequired',
};

function normalizeValidationKey(raw: string | undefined): string {
  if (!raw) return '';
  return raw.replace(/^\{|\}$/g, '');
}

function validationInterpolation(t: TFunction, i18nKey: string): string {
  return t(i18nKey, publishValidationI18nParams());
}

function messageFromValidationErrors(t: TFunction, body: ErrorDto): string | null {
  const first = body.errors?.[0];
  if (!first?.message) return null;
  const key = normalizeValidationKey(first.message);
  const i18nKey = VALIDATION_KEY_TO_I18N[key];
  if (i18nKey) return validationInterpolation(t, i18nKey);
  return null;
}

/** Traduce errores de API (ErrorDto / ValidationErrorDto) a texto para la UI. */
export function resolveApiErrorMessage(
  t: TFunction,
  err: unknown,
  fallbackKey: string,
): string {
  if (err instanceof ApiError) {
    const body = err.body;
    if (body) {
      const validationMsg = messageFromValidationErrors(t, body);
      if (validationMsg) return validationMsg;

      if (body.code) {
        const byCode = t(`error.byCode.${body.code}`, { defaultValue: '' });
        if (byCode) return byCode;
      }
    }
    if (err.status === 403) return t('owner.errors.forbidden');
    if (err.status === 409) return t('owner.errors.conflict');
  }
  return t(fallbackKey);
}
