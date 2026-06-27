import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Alert, Button, Form } from 'react-bootstrap';
import { sessionClient } from '../../session/sessionStore';
import { MediaTypes } from '../../api/mediaTypes';
import type { ForgotStep, PasswordResetCodeRequest, PasswordResetPatch } from './types';
import { apiErrorMessage } from './errorMessage';

function basicAuthHeader(email: string, secret: string): string {
  return `Basic ${btoa(`${email}:${secret}`)}`;
}

// /recuperar-clave — paso 1: POST /credentials { email } (202 uniforme).
// paso 2: PATCH <userUri> { password, passwordConfirm } con Basic email:otp.
export default function ForgotResetPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [params] = useSearchParams();

  const userUri = params.get('userUri');
  const [step, setStep] = useState<ForgotStep>(userUri ? 'reset' : 'request');

  const [email, setEmail] = useState(params.get('email') ?? '');
  const [resetCode, setResetCode] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');

  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

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
    if (!userUri) {
      setError(t('auth.forgot.missingUser'));
      return;
    }
    if (!email.trim()) {
      setError(t('auth.forgot.missingUser'));
      return;
    }

    const body: PasswordResetPatch = { password, passwordConfirm };
    setSubmitting(true);
    try {
      await sessionClient.patch(userUri, body, {
        accept: MediaTypes.user,
        contentType: MediaTypes.user,
        anonymous: true,
        authorization: basicAuthHeader(email.trim(), resetCode.trim()),
      });
      navigate('/ingresar');
    } catch (err) {
      setError(apiErrorMessage(t, err));
    } finally {
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
                {!userUri && (
                  <Alert variant="danger" role="alert">
                    {t('auth.forgot.missingUser')}
                  </Alert>
                )}
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
                  <Form.Control
                    type="password"
                    value={password}
                    autoComplete="new-password"
                    onChange={(e) => setPassword(e.target.value)}
                    required
                  />
                  <Form.Text className="text-muted">{t('auth.register.passwordHint')}</Form.Text>
                </Form.Group>
                <Form.Group className="mb-4" controlId="passwordConfirm">
                  <Form.Label>{t('auth.forgot.confirmPassword')}</Form.Label>
                  <Form.Control
                    type="password"
                    value={passwordConfirm}
                    autoComplete="new-password"
                    onChange={(e) => setPasswordConfirm(e.target.value)}
                    required
                  />
                </Form.Group>
                <Button
                  type="submit"
                  variant="primary"
                  className="w-100"
                  disabled={submitting || !userUri}
                >
                  {submitting ? t('auth.forgot.resetting') : t('auth.forgot.reset')}
                </Button>
              </Form>
            )}

            <p className="text-center mt-3 small">
              <Link to="/ingresar">{t('auth.forgot.back')}</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
