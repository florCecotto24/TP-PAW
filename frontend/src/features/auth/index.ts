// Barrel público del área AUTH. El host compone estas rutas e i18n; la feature
// no registra nada por sí sola (no toca AppRouter ni el i18n global).
export { authRoutes } from './routes';
export { authI18n } from './i18n';
export type {
  LoginForm,
  RegisterForm,
  PasswordResetCodeRequest,
  PasswordResetPatch,
  ForgotStep,
} from './types';
