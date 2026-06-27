// Lógica del log de chat: merge de mensajes para el polling incremental.
// Aislada de React para poder testearla sin montar componentes.
import { idFromUri } from './api';
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
