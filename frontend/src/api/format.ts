// Formateadores de presentación compartidos por las features (browse, reservations…).
// Centralizar acá evita que cada página invente su propio formato de moneda.

// Locale fijo es-AR: la app opera en Argentina (ARS). El símbolo lo ponemos a mano
// (en vez de style:'currency') para mantener el estilo "$12.000" que ya usaban las
// vistas (CarCard / CarDetail) sin el "ARS" ni el espacio que agrega Intl currency.
const AMOUNT_FORMATTER = new Intl.NumberFormat('es-AR', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
});

/**
 * Formatea un monto en la moneda de la app (ARS): "$12.000".
 * Tolera valores vacíos/NaN devolviendo string vacío.
 */
export function formatCurrency(amount: number | null | undefined): string {
  if (amount == null || Number.isNaN(amount)) return '';
  return `$${AMOUNT_FORMATTER.format(amount)}`;
}
