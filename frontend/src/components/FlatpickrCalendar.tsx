import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import flatpickr from 'flatpickr';
import { Spanish } from 'flatpickr/dist/l10n/es.js';
import type { Instance } from 'flatpickr/dist/types/instance';
import type { Options } from 'flatpickr/dist/types/options';

// Wrapper React liviano sobre Flatpickr (vanilla) — reemplaza los calendarios
// inline que el sitio JSP dibujaba con flatpickr + ownerCalendar.js /
// detailReservationForm.js. Reutiliza los overrides de styles/components.css
// (.fp-day-price, .has-active-reservation, etc.), por eso usamos flatpickr a pelo
// y no un input nativo. El idioma sigue al de i18next (es ⇒ d/m/Y).

export interface FlatpickrCalendarProps {
  /** Opciones de Flatpickr (mode, inline, enable, onDayCreate, minDate, …). */
  options: Options;
  /** onChange unificado: recibe las fechas seleccionadas. */
  onChange?: (dates: Date[]) => void;
  /** clase para el contenedor (p.ej. owner-cal-container / detail-reservation-panel). */
  className?: string;
}

export default function FlatpickrCalendar({ options, onChange, className }: FlatpickrCalendarProps) {
  const { i18n } = useTranslation();
  const nodeRef = useRef<HTMLDivElement>(null);
  const fpRef = useRef<Instance | null>(null);
  // Mantener el onChange más reciente sin recrear el calendario.
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  useEffect(() => {
    if (!nodeRef.current) return;
    const isEs = (i18n.language || 'es').toLowerCase().startsWith('es');
    const instance = flatpickr(nodeRef.current, {
      disableMobile: true,
      ...options,
      locale: isEs ? Spanish : 'default',
      onChange: (dates) => onChangeRef.current?.(dates as Date[]),
    });
    fpRef.current = Array.isArray(instance) ? instance[0] : instance;
    return () => {
      fpRef.current?.destroy();
      fpRef.current = null;
    };
    // Recreamos sólo cuando cambian las opciones serializables relevantes o el idioma.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [JSON.stringify(serializableOptions(options)), i18n.language]);

  return <div className={className}><div ref={nodeRef} /></div>;
}

// Sólo las opciones "de datos" disparan recreación; las funciones (onDayCreate) se
// capturan en el closure inicial y no son comparables por JSON.
// NOTA: enable/disable se asumen *array-valued* en todos nuestros call sites
// (rangos de fechas), por eso JSON.stringify los compara bien. Si alguna vez se
// pasara una función en enable/disable (forma que Flatpickr también acepta),
// quedaría fuera de la comparación y el calendario no se recrearía al cambiarla.
function serializableOptions(options: Options): Record<string, unknown> {
  const { onDayCreate, onMonthChange, ...rest } = options as Record<string, unknown>;
  void onDayCreate;
  void onMonthChange;
  return rest;
}

// Helpers compartidos (portados de components.js RydenFlatpickrRange / ownerCalendar.js).

export function pad2(n: number): string {
  return String(n).padStart(2, '0');
}

export function ymd(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

/** Inicio de día (hora de pared local) desde 'YYYY-MM-DD'. */
export function dayStartFromYmd(s: string): Date | null {
  if (!s || s.length < 10) return null;
  const p = s.substring(0, 10).split('-');
  if (p.length !== 3) return null;
  return new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]), 0, 0, 0, 0);
}

/** Fin de día (hora de pared local) desde 'YYYY-MM-DD'. */
export function dayEndFromYmd(s: string): Date | null {
  if (!s || s.length < 10) return null;
  const p = s.substring(0, 10).split('-');
  if (p.length !== 3) return null;
  return new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]), 23, 59, 59, 999);
}

/** Precio compacto para la etiqueta del día (p.ej. 12000 → "$12k"). */
export function compactPrice(price: number | null | undefined): string {
  if (price == null || !isFinite(price)) return '';
  const n = Math.round(price);
  if (n >= 1_000_000) {
    const m = n / 1_000_000;
    return '$' + (m % 1 === 0 ? m : m.toFixed(1)) + 'M';
  }
  if (n >= 1000) {
    const k = n / 1000;
    return '$' + (k % 1 === 0 ? k : k.toFixed(1)) + 'k';
  }
  return '$' + n;
}
