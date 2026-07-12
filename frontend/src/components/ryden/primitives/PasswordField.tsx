import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Form } from 'react-bootstrap';

export interface PasswordFieldProps {
  id?: string;
  value: string;
  onChange: (value: string) => void;
  autoComplete: 'current-password' | 'new-password';
  required?: boolean;
  isInvalid?: boolean;
}

/**
 * Input de contraseña con botón de mostrar/ocultar (réplica de
 * `ryden-pw-wrap` + `ryden-password-toggle` de `login.jsp`/`register.jsp`).
 */
export default function PasswordField({
  id,
  value,
  onChange,
  autoComplete,
  required,
  isInvalid,
}: PasswordFieldProps) {
  const { t } = useTranslation();
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const [visible, setVisible] = useState(false);
  const label = visible ? t('auth.password.hide') : t('auth.password.show');

  return (
    <div className="position-relative ryden-pw-wrap">
      <Form.Control
        type={visible ? 'text' : 'password'}
        id={inputId}
        className="pe-5"
        value={value}
        autoComplete={autoComplete}
        onChange={(e) => onChange(e.target.value)}
        required={required}
        isInvalid={isInvalid}
      />
      <button
        type="button"
        className="btn btn-sm border-0 bg-transparent text-secondary position-absolute top-50 end-0 translate-middle-y me-1 ryden-password-toggle"
        aria-pressed={visible}
        aria-label={label}
        title={label}
        onClick={() => setVisible((v) => !v)}
      >
        <i className={`bi ${visible ? 'bi-eye-slash' : 'bi-eye'}`} aria-hidden="true" />
      </button>
    </div>
  );
}
