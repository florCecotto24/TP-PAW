import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { Alert, Button, Form } from 'react-bootstrap';
import { ApiError } from '../../../api/client';
import { paths } from '../../../routes/paths';
import BreadcrumbTrail from '../../../components/ryden/layout/BreadcrumbTrail';
import PasswordField from '../../auth/PasswordField';
import { createAdminUser } from '../api';
import { useAdminErrorMessage } from '../useAdminErrorMessage';

// Réplica de admin/createAdminUser.jsp: alta de un admin pre-verificado con
// contraseña temporal (POST /users, Content-Type admincreateuser).
export default function AdminCreateUserPage() {
  const { t } = useTranslation();
  const errorMessage = useAdminErrorMessage();

  const [forename, setForename] = useState('');
  const [surname, setSurname] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    setFieldErrors({});
    setSubmitting(true);
    try {
      const res = await createAdminUser({ forename, surname, email, password });
      setSuccess(t('admin.createAdmin.success', { email: res.data?.email ?? email }));
      setForename('');
      setSurname('');
      setEmail('');
      setPassword('');
    } catch (err) {
      if (err instanceof ApiError && err.body?.errors?.length) {
        const byField: Record<string, string> = {};
        for (const fe of err.body.errors) byField[fe.field] = fe.message;
        setFieldErrors(byField);
      } else if (err instanceof ApiError && err.status === 409 && !err.code) {
        setError(t('admin.createAdmin.emailTaken'));
      } else {
        setError(errorMessage(err));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="container py-5 mt-5" style={{ maxWidth: 520 }}>
      <BreadcrumbTrail
        homeLabel={t('admin.panel.title')}
        homeHref={paths.admin.panel}
        midLabel={t('admin.users.title')}
        midHref={paths.admin.users}
        currentLabel={t('admin.createAdmin.title')}
      />
      <h1 className="h2 fw-bold mb-4">{t('admin.createAdmin.title')}</h1>

      {success && (
        <Alert variant="success" role="status">
          {success}
        </Alert>
      )}
      {error && (
        <Alert variant="danger" role="alert">
          {error}
        </Alert>
      )}

      <div className="card border-0 shadow-sm rounded-4 bg-white">
        <div className="card-body p-4">
          <Form onSubmit={onSubmit} noValidate>
            <Form.Group className="mb-3" controlId="forename">
              <Form.Label className="fw-semibold">{t('admin.createAdmin.forename')}</Form.Label>
              <Form.Control
                className="rounded-3"
                value={forename}
                autoComplete="given-name"
                onChange={(e) => setForename(e.target.value)}
                isInvalid={!!fieldErrors.forename}
                required
              />
              {fieldErrors.forename ? (
                <div className="text-danger small d-block mt-1">{fieldErrors.forename}</div>
              ) : null}
            </Form.Group>
            <Form.Group className="mb-3" controlId="surname">
              <Form.Label className="fw-semibold">{t('admin.createAdmin.surname')}</Form.Label>
              <Form.Control
                className="rounded-3"
                value={surname}
                autoComplete="family-name"
                onChange={(e) => setSurname(e.target.value)}
                isInvalid={!!fieldErrors.surname}
                required
              />
              {fieldErrors.surname ? (
                <div className="text-danger small d-block mt-1">{fieldErrors.surname}</div>
              ) : null}
            </Form.Group>
            <Form.Group className="mb-3" controlId="email">
              <Form.Label className="fw-semibold">{t('admin.createAdmin.email')}</Form.Label>
              <Form.Control
                type="email"
                className="rounded-3"
                value={email}
                autoComplete="email"
                onChange={(e) => setEmail(e.target.value)}
                isInvalid={!!fieldErrors.email}
                required
              />
              {fieldErrors.email ? (
                <div className="text-danger small d-block mt-1">{fieldErrors.email}</div>
              ) : null}
            </Form.Group>
            <Form.Group className="mb-4" controlId="password">
              <Form.Label className="fw-semibold">{t('admin.createAdmin.password')}</Form.Label>
              <PasswordField
                id="password"
                value={password}
                onChange={setPassword}
                autoComplete="new-password"
                isInvalid={!!fieldErrors.password}
                required
              />
              {fieldErrors.password ? (
                <div className="text-danger small d-block mt-1">{fieldErrors.password}</div>
              ) : null}
            </Form.Group>
            <div className="d-flex gap-2">
              <Button type="submit" variant="primary" className="rounded-3 flex-grow-1" disabled={submitting}>
                {submitting ? t('admin.createAdmin.submitting') : t('admin.createAdmin.submit')}
              </Button>
              <Link to={paths.admin.users} className="btn btn-light border rounded-3">
                {t('admin.createAdmin.cancel')}
              </Link>
            </div>
          </Form>
        </div>
      </div>
    </div>
  );
}
