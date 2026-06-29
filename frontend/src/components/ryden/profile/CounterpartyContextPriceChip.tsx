export interface CounterpartyContextPriceChipProps {
  label: string;
}

/** Espejo de {@code ryden:counterpartyContextPriceChip}. */
export default function CounterpartyContextPriceChip({ label }: CounterpartyContextPriceChipProps) {
  return <span className="counterparty-price-chip">{label}</span>;
}
