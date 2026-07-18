import { getClientConfig } from '../../api/clientConfig';

export type PasswordPairValidationError = 'tooShort' | 'tooLong' | 'mismatch';

export function registrationPasswordLimits() {
  return getClientConfig().user;
}

/** Length rules only ({@code RegistrationPasswordRulesValidator} on password). */
export function validatePassword(password: string): Exclude<PasswordPairValidationError, 'mismatch'> | null {
  const { registrationPasswordMinLength: min, registrationPasswordMaxLength: max } =
    registrationPasswordLimits();
  if (password.length < min) return 'tooShort';
  if (password.length > max) return 'tooLong';
  return null;
}

export function validatePasswordConfirm(
  password: string,
  passwordConfirm: string,
): 'mismatch' | null {
  if (password !== passwordConfirm) return 'mismatch';
  return null;
}

/** Client-side mirror of {@code RegistrationPasswordRulesValidator}. */
export function validatePasswordPair(
  password: string,
  passwordConfirm: string,
): PasswordPairValidationError | null {
  return validatePassword(password) ?? validatePasswordConfirm(password, passwordConfirm);
}
