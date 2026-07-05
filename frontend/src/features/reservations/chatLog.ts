// Lógica del log de chat: merge de mensajes para el polling incremental.
// Aislada de React para poder testearla sin montar componentes.
import { idFromUri } from './api';
import { formatDateLong } from '../../i18n/dateFormat';
import type { MessageDto } from './types';

/** id numérico (como número) de un mensaje a partir de su URN `self`. */
export function messageId(msg: MessageDto): number {
  const raw = idFromUri(msg.links?.self);
  const n = raw != null ? Number(raw) : NaN;
  return Number.isNaN(n) ? -1 : n;
}

/** Mayor id presente en una lista (0 si vacía). Sirve como cursor `afterId`. */
export function latestMessageId(messages: MessageDto[]): number {
  let max = 0;
  for (const m of messages) {
    const id = messageId(m);
    if (id > max) max = id;
  }
  return max;
}

/**
 * Mezcla los mensajes existentes con los recién llegados (polling con afterId).
 * - Deduplica por id (el server podría reenviar el límite).
 * - Mantiene orden ascendente por id (cronológico).
 * El resultado es una lista nueva (inmutable) apta para setState.
 */
export function mergeMessages(existing: MessageDto[], incoming: MessageDto[]): MessageDto[] {
  if (incoming.length === 0) return existing;

  const byId = new Map<number, MessageDto>();
  for (const m of existing) byId.set(messageId(m), m);
  let changed = false;
  for (const m of incoming) {
    const id = messageId(m);
    const prev = byId.get(id);
    // Nuevo id, o el entrante trae estado más fresco (p.ej. `seen` flipea).
    if (!prev || prev.seen !== m.seen || prev.body !== m.body) changed = true;
    byId.set(id, m); // incoming pisa.
  }
  // Si no entró nada nuevo y nada cambió de identidad, devolvemos lo mismo.
  if (!changed && byId.size === existing.length) return existing;

  return [...byId.values()].sort((a, b) => messageId(a) - messageId(b));
}

function dayKeyFromDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

/** Clave de día calendario local (`YYYY-MM-DD`) de un timestamp ISO; '' si inválido. */
export function dayKey(value: string): string {
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? '' : dayKeyFromDate(d);
}

/** Etiqueta legible de un `dayKey`: "Hoy"/"Ayer" (i18n) o fecha larga localizada. */
export function formatDayLabel(
  key: string,
  t: (k: string) => string,
  locale?: string,
): string {
  if (!key) return '';
  const now = new Date();
  if (key === dayKeyFromDate(now)) return t('res.chat.today');
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  if (key === dayKeyFromDate(yesterday)) return t('res.chat.yesterday');
  const [y, m, d] = key.split('-').map(Number);
  return formatDateLong(`${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`, locale);
}

export interface ChatDayGroup {
  dayKey: string;
  messages: MessageDto[];
}

/** Agrupa una lista ORDENADA (ascendente) de mensajes en bloques consecutivos por día. */
export function groupMessagesByDay(messages: MessageDto[]): ChatDayGroup[] {
  const groups: ChatDayGroup[] = [];
  for (const msg of messages) {
    const key = dayKey(msg.createdAt);
    const last = groups[groups.length - 1];
    if (last && last.dayKey === key) {
      last.messages.push(msg);
    } else {
      groups.push({ dayKey: key, messages: [msg] });
    }
  }
  return groups;
}
