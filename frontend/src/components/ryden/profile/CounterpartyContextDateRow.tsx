export interface CounterpartyContextDateRowProps {
  label: string;
  value: string;
  iconClass?: string;
}

/** Espejo de {@code ryden:counterpartyContextDateRow}. */
export default function CounterpartyContextDateRow({
  label,
  value,
  iconClass,
}: CounterpartyContextDateRowProps) {
  return (
    <div className="counterparty-context-row">
      {iconClass ? <i className={`bi ${iconClass}`} aria-hidden="true"></i> : null}
      <div>
        <div className="counterparty-context-row__label">{label}</div>
        <div className="counterparty-context-row__value">{value}</div>
      </div>
    </div>
  );
}
