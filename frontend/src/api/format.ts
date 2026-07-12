// Formateadores de presentación compartidos por las features (browse, reservations…).
// Locale y moneda salen de GET /config (fallback documentado en clientConfig.ts).

import { getClientConfig } from './clientConfig';

const AMOUNT_FORMATTERS = new Map<string, Intl.NumberFormat>();

function amountFormatter(locale: string): Intl.NumberFormat {
  let formatter = AMOUNT_FORMATTERS.get(locale);
  if (!formatter) {
    formatter = new Intl.NumberFormat(locale, {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    });
    AMOUNT_FORMATTERS.set(locale, formatter);
  }
  return formatter;
}

function currencyPrefix(currency: string): string {
  if (currency === 'ARS') return '$';
  return `${currency} `;
}

/**
 * Formatea un monto en la moneda de la app: "$12.000" (ARS) u otro prefijo desde config.
 * Tolera valores vacíos/NaN devolviendo string vacío.
 */
export function formatCurrency(amount: number | null | undefined): string {
  if (amount == null || Number.isNaN(amount)) return '';
  const { currency, formatLocale } = getClientConfig().money;
  return `${currencyPrefix(currency)}${amountFormatter(formatLocale).format(amount)}`;
}
