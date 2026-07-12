import { getClientConfig } from '../../api/clientConfig';

export type PasswordPairValidationError = 'tooShort' | 'tooLong' | 'mismatch';

export function registrationPasswordLimits() {
  return getClientConfig().user;
}

/** Client-side mirror of {@code RegistrationPasswordRulesValidator}. */
export function validatePasswordPair(
  password: string,
  passwordConfirm: string,
): PasswordPairValidationError | null {
  const { registrationPasswordMinLength: min, registrationPasswordMaxLength: max } =
    registrationPasswordLimits();
  if (password.length < min) return 'tooShort';
  if (password.length > max) return 'tooLong';
  if (password !== passwordConfirm) return 'mismatch';
  return null;
}
