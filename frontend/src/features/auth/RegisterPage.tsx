import { useEffect, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router-dom';
import { Alert, Button, Form } from 'react-bootstrap';
import { sessionClient } from '../../session/sessionStore';
import { MediaTypes } from '../../api/mediaTypes';
import { paths } from '../../routes/paths';
import { ApiError } from '../../api/client';
import { getCollectionPath } from '../../api/apiDiscovery';
import type { UserDto } from '../../api/types';
import type { RegisterForm } from './types';
import { apiErrorMessage } from './errorMessage';
import {
  registrationPasswordLimits,
  validatePassword,
  validatePasswordConfirm,
} from './passwordValidation';
import { PasswordField } from '../../components/ryden';

// /registrarse — "registrarse" = crear el recurso usuario (POST /users,
// anónimo, contentType vendor user). En 201 el UserDto trae links.credentials
// para reenviar el OTP; lo pasamos a /verificar-email junto con el email.
export default function RegisterPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [form, setForm] = useState<RegisterForm>({
    email: '',
    forename: '',
    surname: '',
    password: '',
    passwordConfirm: '',
  });
  const [error, setError] = useState<string | null>(null);
  // Errores por campo (cliente + server), igual que `form:errors path="..."`.
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    document.body.classList.add('auth-page');
    return () => document.body.classList.remove('auth-page');
  }, []);

  const userLimits = registrationPasswordLimits();

  function passwordErrorMessage(code: ReturnType<typeof validatePassword>): string | null {
    if (code === 'tooShort') {
      return t('auth.register.passwordTooShort', { count: userLimits.registrationPasswordMinLength });
    }
    if (code === 'tooLong') {
      return t('auth.register.passwordTooLong', { count: userLimits.registrationPasswordMaxLength });
    }
    return null;
  }

  function applyPasswordValidation(password: string): boolean {
    const msg = passwordErrorMessage(validatePassword(password));
    setFieldErrors((prev) => {
      const next = { ...prev };
      if (msg) next.password = msg;
      else delete next.password;
      return next;
    });
    return msg == null;
  }

  function applyPasswordConfirmValidation(password: string, confirm: string): boolean {
    const mismatch = validatePasswordConfirm(password, confirm);
    const msg = mismatch ? t('validation.passwordMismatch') : null;
    setFieldErrors((prev) => {
      const next = { ...prev };
      if (msg) next.passwordConfirm = msg;
      else delete next.passwordConfirm;
      return next;
    });
    return msg == null;
  }

  function update<K extends keyof RegisterForm>(key: K, value: string) {
    setForm((f) => {
      const next = { ...f, [key]: value };
      if (key === 'password') {
        applyPasswordValidation(value);
        if (next.passwordConfirm) applyPasswordConfirmValidation(value, next.passwordConfirm);
      } else if (key === 'passwordConfirm') {
        applyPasswordConfirmValidation(next.password, value);
      } else {
        setFieldErrors((prev) => {
          if (!prev[key]) return prev;
          const cleared = { ...prev };
          delete cleared[key];
          return cleared;
        });
      }
      return next;
    });
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    const passwordOk = applyPasswordValidation(form.password);
    const confirmOk = applyPasswordConfirmValidation(form.password, form.passwordConfirm);
    if (!passwordOk || !confirmOk) {
      document.getElementById(passwordOk ? 'passwordConfirm' : 'password')?.focus();
      return;
    }

    setSubmitting(true);
    try {
      const res = await sessionClient.post<UserDto>(getCollectionPath('users'), form, {
        accept: MediaTypes.user,
        contentType: MediaTypes.user,
        anonymous: true,
      });

      // 201 → links.credentials (reenvío OTP) + email. No inventamos el path.
      const credentialsUri = res.data?.links?.credentials;
      const params = new URLSearchParams({ email: form.email });
      if (credentialsUri) params.set('credentialsUri', credentialsUri);
      navigate(`${paths.verifyEmail}?${params.toString()}`);
    } catch (err) {
      if (err instanceof ApiError && err.body?.errors?.length) {
        const byField: Record<string, string> = {};
        for (const fe of err.body.errors) byField[fe.field] = fe.message;
        setFieldErrors(byField);
      } else if (err instanceof ApiError && err.status === 409 && !err.code) {
        // 409 EmailAlreadyExists → code user.email.alreadyExists ("ya registrado").
        setError(t('auth.register.emailTaken'));
      } else {
        setError(apiErrorMessage(t, err));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page__shell auth-page__shell--tall-form">
      <div className="auth-page__brand">
        <Link to="/" className="text-decoration-none">
          <span className="auth-page__logo">{t('app.name')}</span>
        </Link>
      </div>
      <div className="auth-page__main">
        <div className="auth-page__card-wrap auth-page__card-wrap--wide">
          <div className="bg-white rounded-4 shadow-sm p-4 p-md-5">
            <h1 className="h4 mb-3">{t('auth.register.title')}</h1>

            {error && (
              <Alert variant="danger" role="alert">
                {error}
              </Alert>
            )}

            <Form onSubmit={onSubmit} noValidate>
              <Form.Group className="mb-3" controlId="forename">
                <Form.Label>{t('auth.register.forename')}</Form.Label>
                <Form.Control
                  value={form.forename}
                  autoComplete="given-name"
                  maxLength={userLimits.displayNamePartMaxLength}
                  onChange={(e) => update('forename', e.target.value)}
                  isInvalid={!!fieldErrors.forename}
                  required
                />
                {fieldErrors.forename ? (
                  <div className="text-danger small d-block mt-1">{fieldErrors.forename}</div>
                ) : null}
              </Form.Group>
              <Form.Group className="mb-3" controlId="surname">
                <Form.Label>{t('auth.register.surname')}</Form.Label>
                <Form.Control
                  value={form.surname}
                  autoComplete="family-name"
                  maxLength={userLimits.displayNamePartMaxLength}
                  onChange={(e) => update('surname', e.target.value)}
                  isInvalid={!!fieldErrors.surname}
                  required
                />
                {fieldErrors.surname ? (
                  <div className="text-danger small d-block mt-1">{fieldErrors.surname}</div>
                ) : null}
              </Form.Group>
              <Form.Group className="mb-3" controlId="email">
                <Form.Label>{t('auth.register.email')}</Form.Label>
                <Form.Control
                  type="email"
                  value={form.email}
                  autoComplete="email"
                  maxLength={userLimits.registrationEmailMaxLength}
                  onChange={(e) => update('email', e.target.value)}
                  isInvalid={!!fieldErrors.email}
                  required
                />
                {fieldErrors.email ? (
                  <div className="text-danger small d-block mt-1">{fieldErrors.email}</div>
                ) : null}
              </Form.Group>
              <Form.Group className="mb-3" controlId="password">
                <Form.Label>{t('auth.register.password')}</Form.Label>
                <PasswordField
                  id="password"
                  value={form.password}
                  onChange={(v) => update('password', v)}
                  autoComplete="new-password"
                  isInvalid={!!fieldErrors.password}
                  required
                />
                {fieldErrors.password ? (
                  <div className="text-danger small d-block mt-1">{fieldErrors.password}</div>
                ) : (
                  <Form.Text className="text-muted">
                    {t('auth.register.passwordHint', {
                      min: userLimits.registrationPasswordMinLength,
                      max: userLimits.registrationPasswordMaxLength,
                    })}
                  </Form.Text>
                )}
              </Form.Group>
              <Form.Group className="mb-4" controlId="passwordConfirm">
                <Form.Label>{t('auth.register.passwordConfirm')}</Form.Label>
                <PasswordField
                  id="passwordConfirm"
                  value={form.passwordConfirm}
                  onChange={(v) => update('passwordConfirm', v)}
                  autoComplete="new-password"
                  isInvalid={!!fieldErrors.passwordConfirm}
                  required
                />
                {fieldErrors.passwordConfirm ? (
                  <div className="text-danger small d-block mt-1">{fieldErrors.passwordConfirm}</div>
                ) : null}
              </Form.Group>
              <Button type="submit" variant="primary" className="w-100" disabled={submitting}>
                {submitting ? t('auth.register.submitting') : t('auth.register.submit')}
              </Button>
            </Form>

            <p className="text-center mt-3 small">
              {t('auth.register.haveAccount')} <Link to={paths.login}>{t('auth.login.submit')}</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
