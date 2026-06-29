export interface CounterpartyContextTagProps {
  label: string;
}

/** Espejo de {@code ryden:counterpartyContextTag}. */
export default function CounterpartyContextTag({ label }: CounterpartyContextTagProps) {
  return <span className="counterparty-tag">{label}</span>;
}
