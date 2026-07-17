import { useMemo, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Alert, Button, Form } from 'react-bootstrap';
import { canonicalApiUserPath } from '../../api/uri';
import { sessionClient, useSessionStore } from '../../session/sessionStore';
import { apiErrorMessage } from './errorMessage';
import { paths } from '../../routes/paths';

/** Accept only same-origin API user URNs (`/users/{id}`) before POSTing credentials. */
function isTrustedApiUserUri(userUri: string): boolean {
  const trimmed = userUri.trim();
  if (!trimmed) return false;
  if (/^[a-z][a-z0-9+.-]*:/i.test(trimmed) && !/^https?:\/\//i.test(trimmed)) {
    return false;
  }
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    if (typeof window === 'undefined') return false;
    try {
      if (new URL(trimmed).origin !== window.location.origin) return false;
    } catch {
      return false;
    }
  }
  try {
    return /^\/users\/\d+$/.test(canonicalApiUserPath(trimmed));
  } catch {
    return false;
  }
}

// /verificar-email — tras POST /users el servidor envía un OTP por mail.
// Verificar = Basic auth email:otp (el OTP actúa como password); el backend
// consume el código, marca emailVerified y devuelve JWT en headers.
// Reenviar OTP: POST <userUri>/credentials (200 uniforme, sin verbos en la URL).
export default function VerifyEmailPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const login = useSessionStore((s) => s.login);
  const [params] = useSearchParams();

  const rawUserUri = params.get('userUri');
  const userUri = useMemo(
    () => (rawUserUri && isTrustedApiUserUri(rawUserUri) ? canonicalApiUserPath(rawUserUri) : null),
    [rawUserUri],
  );
  const email = params.get('email') ?? '';

  const [code, setCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setInfo(null);

    if (!email.trim()) {
      setError(t('auth.verify.missingUser'));
      return;
    }

    setSubmitting(true);
    try {
      await login(email.trim(), code.trim());
      navigate('/');
    } catch (err) {
      setError(apiErrorMessage(t, err));
    } finally {
      setSubmitting(false);
    }
  }

  async function onResend() {
    setError(null);
    setInfo(null);

    if (!userUri) {
      setError(t('auth.verify.missingUser'));
      return;
    }

    setResending(true);
    try {
      await sessionClient.post(`${userUri}/credentials`, undefined, { anonymous: true });
      setInfo(t('auth.verify.resent'));
    } catch (err) {
      setError(apiErrorMessage(t, err));
    } finally {
      setResending(false);
    }
  }

  return (
    <div className="auth-page__shell auth-page__shell--simple auth-page__shell--tall-form">
      <div className="auth-page__main">
        <div className="auth-page__card-wrap auth-page__card-wrap--wide">
          <div className="bg-white rounded-4 shadow-sm p-4 p-md-5">
            <h1 className="h4 mb-3">{t('auth.verify.title')}</h1>
            <p className="text-muted small mb-4">{t('auth.verify.intro')}</p>

            {email && <p>{t('auth.verify.sentTo', { email })}</p>}

            {error && (
              <Alert variant="danger" role="alert">
                {error}
              </Alert>
            )}
            {info && (
              <Alert variant="success" role="status">
                {info}
              </Alert>
            )}

            <Form onSubmit={onSubmit} className="needs-validation">
              <Form.Group className="mb-3" controlId="code">
                <Form.Label>{t('auth.verify.code')}</Form.Label>
                <Form.Control
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={6}
                  placeholder="000000"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  required
                />
              </Form.Group>
              <Button
                type="submit"
                variant="primary"
                className="w-100 mb-2"
                disabled={submitting || !email.trim()}
              >
                {submitting ? t('auth.verify.submitting') : t('auth.verify.submit')}
              </Button>
            </Form>

            <Button
              type="button"
              variant="outline-secondary"
              className="w-100 mb-3"
              onClick={onResend}
              disabled={resending || !userUri}
            >
              {resending ? t('auth.verify.resending') : t('auth.verify.resend')}
            </Button>

            <p className="text-center mt-3 small mb-1">
              <span className="text-muted">{t('auth.verify.alreadyVerified')}</span>
              <Link className="ms-1" to={paths.login}>
                {t('auth.login.submit')}
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
