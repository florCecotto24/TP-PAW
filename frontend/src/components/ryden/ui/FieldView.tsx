export interface FieldViewProps {
  label: string;
  value: string;
  multiline?: boolean;
}

/** Campo de solo lectura (label + valor), mismo markup que el perfil editable. */
export default function FieldView({ label, value, multiline }: FieldViewProps) {
  return (
    <div className="profile-field-view">
      <span className="profile-section-label">{label}</span>
      <span
        className={`profile-field-value ryden-text-break${
          multiline ? ' ryden-multiline-plaintext d-inline-block w-100' : ''
        }`}
      >
        {value}
      </span>
    </div>
  );
}
