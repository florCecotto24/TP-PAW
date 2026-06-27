import type { RouteObject } from 'react-router-dom';
import LoginPage from './LoginPage';
import RegisterPage from './RegisterPage';
import VerifyEmailPage from './VerifyEmailPage';
import ForgotResetPage from './ForgotResetPage';

// Rutas del área AUTH (en español, §3.3). Aisladas en un .tsx porque contienen
// JSX; el barrel index.ts (que debe ser .ts) las re-exporta.
export const authRoutes: RouteObject[] = [
  { path: 'ingresar', element: <LoginPage /> },
  { path: 'registrarse', element: <RegisterPage /> },
  { path: 'verificar-email', element: <VerifyEmailPage /> },
  { path: 'recuperar-clave', element: <ForgotResetPage /> },
];
