import type { InputHTMLAttributes } from 'react';

export interface RydenInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'name'> {
  name: string;
  label?: string;
  required?: boolean;
  cssClass?: string;
}

/**
 * Espejo de {@code ryden:input}: wrapper {@code mb-3}, label opcional con asterisco,
 * input {@code form-control custom-input}.
 */
export default function Input({
  name,
  type = 'text',
  label,
  placeholder,
  value,
  required,
  disabled,
  readOnly,
  id,
  cssClass = '',
  ...rest
}: RydenInputProps) {
  const inputId = id ?? name;

  return (
    <div className="mb-3">
      {label ? (
        <label htmlFor={inputId} className="form-label custom-label">
          {label}
          {required ? <span className="text-danger">*</span> : null}
        </label>
      ) : null}
      <input
        type={type}
        name={name}
        id={inputId}
        className={`form-control custom-input ${cssClass}`.trim()}
        placeholder={placeholder}
        value={value}
        required={required}
        disabled={disabled}
        readOnly={readOnly}
        {...rest}
      />
    </div>
  );
}
