// Helpers de presentación compartidos por las páginas del área.
import { formatCurrency } from '../../api/format';
import type { ReservationStatus } from './types';

// Re-export del formateador de moneda centralizado para call sites del área.
export { formatCurrency };

/** Clave i18n para una etiqueta de estado de reserva. */
export function statusLabelKey(status: ReservationStatus): string {
  return `res.status.${status}`;
}

/** Formatea un date-time ISO a string local; tolera valores vacíos/inválidos. */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString();
}

/** Formatea un precio total como moneda de la app (ARS). Delega en formatCurrency. */
export function formatPrice(value: number | null | undefined): string {
  return formatCurrency(value);
}
