// Tipos locales del área AUTH.

/** Estado del formulario de login. */
export interface LoginForm {
  email: string;
  password: string;
}

/** Estado del formulario de registro (espeja UserCreateDto del contrato). */
export interface RegisterForm {
  email: string;
  forename: string;
  surname: string;
  password: string;
  passwordConfirm: string;
}

/** Cuerpo del POST /credentials (solicitud de OTP de reset). */
export interface PasswordResetCodeRequest {
  email: string;
}

/**
 * Cuerpo del PATCH /users/{id} para completar reset.
 * El OTP viaja en Authorization: Basic email:otp (no en el JSON).
 */
export interface PasswordResetPatch {
  password: string;
  passwordConfirm: string;
}

/** Pasos del flujo de recuperación de clave (dos pasos, una sola página). */
export type ForgotStep = 'request' | 'reset';
