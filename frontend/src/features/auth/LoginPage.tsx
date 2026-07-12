import { useEffect, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, Form } from 'react-bootstrap';
import { useSessionStore } from '../../session/sessionStore';
import { paths } from '../../routes/paths';
import { PasswordField } from '../../components/ryden';

// /ingresar — autentica con email+password vía useSessionStore.login (que hace
// Basic → tokens, sin /login). Al autenticar navega a la ruta de origen (o "/").
export default function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const login = useSessionStore((s) => s.login);
  const status = useSessionStore((s) => s.status);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);

  const submitting = status === 'authenticating';
  const redirectTo = (location.state as { from?: string } | null)?.from ?? '/';

  // Réplica de `body.auth-page` del JSP: encaja el formulario en el viewport
  // (scroll solo adentro de `.auth-page__main` en pantallas muy chicas).
  useEffect(() => {
    document.body.classList.add('auth-page');
    return () => document.body.classList.remove('auth-page');
  }, []);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await login(email, password);
      navigate(redirectTo, { replace: true });
    } catch {
      // El store deja status==='error'; mostramos un mensaje genérico de auth.
      setError(t('auth.login.failed'));
    }
  }

  return (
    <div className="auth-page__shell">
      <div className="auth-page__brand">
        <Link to="/" className="text-decoration-none">
          <span className="auth-page__logo">{t('app.name')}</span>
        </Link>
      </div>
      <div className="auth-page__main">
        <div className="auth-page__card-wrap">
          <div className="bg-white rounded-4 shadow-sm p-4 p-md-5">
            <h1 className="h4 mb-4">{t('auth.login.title')}</h1>

            {(error || status === 'error') && (
              <Alert variant="danger" role="alert">
                {error ?? t('auth.login.failed')}
              </Alert>
            )}

            <Form onSubmit={onSubmit}>
              <Form.Group className="mb-3" controlId="email">
                <Form.Label>{t('auth.login.email')}</Form.Label>
                <Form.Control
                  type="email"
                  value={email}
                  autoComplete="username"
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </Form.Group>
              <Form.Group className="mb-3" controlId="password">
                <div className="d-flex justify-content-between align-items-baseline mb-1">
                  <Form.Label className="mb-0">{t('auth.login.password')}</Form.Label>
                  <Link to={paths.forgotPassword} className="small text-muted">
                    {t('auth.login.forgotPassword')}
                  </Link>
                </div>
                <PasswordField
                  id="password"
                  value={password}
                  onChange={setPassword}
                  autoComplete="current-password"
                  required
                />
              </Form.Group>
              <Button type="submit" variant="primary" className="w-100" disabled={submitting}>
                {submitting ? t('auth.login.submitting') : t('auth.login.submit')}
              </Button>
            </Form>

            <p className="text-center text-muted small mt-4 mb-0">
              {t('auth.login.noAccount')}{' '}
              <Link to={paths.register}>{t('auth.register.submit')}</Link>
            </p>
            <p className="text-center mt-2">
              <Link to={paths.verifyEmail} className="text-muted small">
                {t('auth.login.verifyEmailLink')}
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
