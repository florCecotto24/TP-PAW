import { useEffect, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Alert, Button, Form } from 'react-bootstrap';
import { sessionClient, useSessionStore } from '../../session/sessionStore';
import { MediaTypes } from '../../api/mediaTypes';
import { paths } from '../../routes/paths';
import type { ForgotStep, PasswordResetCodeRequest, PasswordResetPatch } from './types';
import { apiErrorMessage } from './errorMessage';
import PasswordField from './PasswordField';

function basicAuthHeader(email: string, secret: string): string {
  return `Basic ${btoa(`${email}:${secret}`)}`;
}

// /forgot-password — sin verbos en la URL, dos pasos en una sola página:
//   1) POST /credentials { email } (202 uniforme, anti-enumeración).
//   2) La URN /users/{id} a completar no viaja por el mail (revelaría la cuenta);
//      se resuelve con el mismo "auth probe" que login/verify-email: Basic email:otp
//      a GET / trae el Link rel="authenticated-user" (no consume el código). Con esa
//      URN se hace PATCH { password, passwordConfirm } con el mismo Basic, que ahí sí
//      consume el OTP y aplica la nueva contraseña.
export default function ForgotResetPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [params] = useSearchParams();

  const [step, setStep] = useState<ForgotStep>(params.get('email') ? 'reset' : 'request');

  const [email, setEmail] = useState(params.get('email') ?? '');
  const [resetCode, setResetCode] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');

  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    document.body.classList.add('auth-page');
    return () => document.body.classList.remove('auth-page');
  }, []);

  async function onRequest(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setInfo(null);

    const body: PasswordResetCodeRequest = { email: email.trim() };
    setSubmitting(true);
    try {
      await sessionClient.post('/credentials', body, {
        contentType: MediaTypes.credential,
        anonymous: true,
      });
      setInfo(t('auth.forgot.sent'));
    } catch (err) {
      setError(apiErrorMessage(t, err));
    } finally {
      setSubmitting(false);
    }
  }

  async function onReset(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setInfo(null);

    if (password.length < 8) {
      setError(t('auth.register.passwordTooShort', { count: 8 }));
      return;
    }
    if (password.length > 72) {
      setError(t('auth.register.passwordTooLong', { count: 72 }));
      return;
    }
    if (password !== passwordConfirm) {
      setError(t('validation.passwordMismatch'));
      return;
    }
    if (!email.trim() || !resetCode.trim()) {
      setError(t('auth.forgot.missingUser'));
      return;
    }

    const trimmedEmail = email.trim();
    const trimmedCode = resetCode.trim();
    const auth = basicAuthHeader(trimmedEmail, trimmedCode);

    setSubmitting(true);
    try {
      // Probe: no consume el OTP, solo resuelve la URN vía Link rel="authenticated-user".
      await sessionClient.loginBasic(trimmedEmail, trimmedCode);
      const userUri = useSessionStore.getState().currentUserUri;
      if (!userUri) {
        setError(t('auth.forgot.missingUser'));
        return;
      }

      const body: PasswordResetPatch = { password, passwordConfirm };
      await sessionClient.patch(userUri, body, {
        accept: MediaTypes.user,
        contentType: MediaTypes.user,
        anonymous: true,
        authorization: auth,
      });
      navigate(paths.login);
    } catch (err) {
      setError(apiErrorMessage(t, err));
    } finally {
      // El probe Basic deja tokens/URN transitorios en el store; nunca queremos
      // que el usuario quede "logueado" con un OTP que puede haber sido consumido.
      useSessionStore.getState().logout();
      setSubmitting(false);
    }
  }

  const isReset = step === 'reset';

  return (
    <div className="auth-page__shell auth-page__shell--simple auth-page__shell--tall-form">
      <div className="auth-page__main">
        <div className="auth-page__card-wrap auth-page__card-wrap--wide">
          <div className="bg-white rounded-4 shadow-sm p-4 p-md-5">
            <h1 className="h4 mb-3">{t('auth.forgot.title')}</h1>
            <p className="text-muted small mb-4">
              {isReset ? t('auth.forgot.reset.intro') : t('auth.forgot.intro')}
            </p>

            {error && (
              <Alert variant="danger" role="alert">
                {error}
              </Alert>
            )}
            {info && (
              <Alert variant="info" role="status">
                {info}
              </Alert>
            )}

            {step === 'request' && (
              <>
                <Form onSubmit={onRequest} className="needs-validation">
                  <Form.Group className="mb-4" controlId="email">
                    <Form.Label>{t('auth.forgot.email')}</Form.Label>
                    <Form.Control
                      type="email"
                      value={email}
                      autoComplete="email"
                      onChange={(e) => setEmail(e.target.value)}
                      required
                    />
                  </Form.Group>
                  <Button type="submit" variant="primary" className="w-100" disabled={submitting}>
                    {submitting ? t('auth.forgot.submitting') : t('auth.forgot.submit')}
                  </Button>
                </Form>
                {info && (
                  <Button
                    type="button"
                    variant="outline-secondary"
                    className="w-100 mt-2"
                    onClick={() => {
                      setStep('reset');
                      setInfo(null);
                    }}
                  >
                    {t('auth.forgot.haveCode')}
                  </Button>
                )}
              </>
            )}

            {step === 'reset' && (
              <Form onSubmit={onReset} className="needs-validation">
                {!params.get('email') && (
                  <Form.Group className="mb-3" controlId="emailReset">
                    <Form.Label>{t('auth.forgot.email')}</Form.Label>
                    <Form.Control
                      type="email"
                      value={email}
                      autoComplete="email"
                      onChange={(e) => setEmail(e.target.value)}
                      required
                    />
                  </Form.Group>
                )}
                <Form.Group className="mb-3" controlId="code">
                  <Form.Label>{t('auth.forgot.code')}</Form.Label>
                  <Form.Control
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    placeholder="000000"
                    value={resetCode}
                    onChange={(e) => setResetCode(e.target.value)}
                    required
                  />
                </Form.Group>
                <Form.Group className="mb-3" controlId="password">
                  <Form.Label>{t('auth.forgot.newPassword')}</Form.Label>
                  <PasswordField
                    id="password"
                    value={password}
                    onChange={setPassword}
                    autoComplete="new-password"
                    required
                  />
                  <Form.Text className="text-muted">{t('auth.register.passwordHint')}</Form.Text>
                </Form.Group>
                <Form.Group className="mb-4" controlId="passwordConfirm">
                  <Form.Label>{t('auth.forgot.confirmPassword')}</Form.Label>
                  <PasswordField
                    id="passwordConfirm"
                    value={passwordConfirm}
                    onChange={setPasswordConfirm}
                    autoComplete="new-password"
                    required
                  />
                </Form.Group>
                <Button type="submit" variant="primary" className="w-100" disabled={submitting}>
                  {submitting ? t('auth.forgot.resetting') : t('auth.forgot.reset')}
                </Button>
              </Form>
            )}

            <p className="text-center mt-3 small">
              <Link to={paths.login}>{t('auth.forgot.back')}</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
