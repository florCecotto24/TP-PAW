import { describe, expect, it } from 'vitest';
import { dayKey, formatDayLabel, groupMessagesByDay, latestMessageId, mergeMessages, messageId } from './chatLog';
import type { MessageDto } from './types';

function msg(id: number, body = `m${id}`, extra: Partial<MessageDto> = {}): MessageDto {
  return {
    body,
    createdAt: new Date(2024, 0, 1, 0, id).toISOString(),
    seen: false,
    hasAttachment: false,
    links: { self: `/reservations/1/messages/${id}` },
    ...extra,
  };
}

describe('chatLog.messageId', () => {
  it('extracts the numeric id from the self URN', () => {
    // 1.Arrange
    const message = msg(42);

    // 2.Act
    const id = messageId(message);

    // 3.Assert
    expect(id).toBe(42);
  });

  it('returns -1 when the URN has no numeric tail', () => {
    // 1.Arrange
    const message = msg(0, 'x', { links: { self: '/reservations/1/messages' } });

    // 2.Act
    const id = messageId(message);

    // 3.Assert
    expect(id).toBe(-1);
  });
});

describe('chatLog.latestMessageId', () => {
  it('is 0 for an empty list (initial poll cursor)', () => {
    // 2.Act / 3.Assert
    expect(latestMessageId([])).toBe(0);
  });

  it('returns the max id regardless of order', () => {
    // 1.Arrange
    const messages = [msg(3), msg(7), msg(5)];

    // 2.Act
    const latest = latestMessageId(messages);

    // 3.Assert
    expect(latest).toBe(7);
  });
});

describe('chatLog.mergeMessages', () => {
  it('returns the same reference when nothing new arrives', () => {
    // 1.Arrange
    const existing = [msg(1), msg(2)];

    // 2.Act
    const merged = mergeMessages(existing, []);

    // 3.Assert
    expect(merged).toBe(existing);
  });

  it('appends new messages keeping ascending id order', () => {
    // 1.Arrange
    const existing = [msg(1), msg(2)];
    const incoming = [msg(3), msg(4)];

    // 2.Act
    const merged = mergeMessages(existing, incoming);

    // 3.Assert
    expect(merged.map(messageId)).toEqual([1, 2, 3, 4]);
    expect(merged).not.toBe(existing);
  });

  it('deduplicates by id when the server resends the boundary message', () => {
    // 1.Arrange
    const existing = [msg(1), msg(2)];
    const incoming = [msg(2), msg(3)];

    // 2.Act
    const merged = mergeMessages(existing, incoming);

    // 3.Assert
    expect(merged.map(messageId)).toEqual([1, 2, 3]);
  });

  it('lets incoming overwrite stale copies (e.g. seen flag flips)', () => {
    // 1.Arrange
    const existing = [msg(1, 'm1', { seen: false })];
    const incoming = [msg(1, 'm1', { seen: true })];

    // 2.Act
    const merged = mergeMessages(existing, incoming);

    // 3.Assert
    expect(merged).toHaveLength(1);
    expect(merged[0].seen).toBe(true);
  });

  it('sorts out-of-order incoming messages', () => {
    // 1.Arrange
    const incoming = [msg(5), msg(2), msg(9)];

    // 2.Act
    const merged = mergeMessages([], incoming);

    // 3.Assert
    expect(merged.map(messageId)).toEqual([2, 5, 9]);
  });

  it('treats merge as the next poll cursor', () => {
    // 1.Arrange
    const existing = [msg(1)];
    const incoming = [msg(2), msg(3)];

    // 2.Act
    const merged = mergeMessages(existing, incoming);
    const cursor = latestMessageId(merged);

    // 3.Assert
    expect(cursor).toBe(3);
  });
});

describe('chatLog.dayKey', () => {
  it('extracts the local calendar day from an ISO timestamp', () => {
    // 1.Arrange
    const iso = new Date(2026, 5, 15, 23, 30).toISOString();

    // 2.Act / 3.Assert
    expect(dayKey(iso)).toBe('2026-06-15');
  });

  it('returns an empty string for an invalid timestamp', () => {
    expect(dayKey('not-a-date')).toBe('');
  });
});

describe('chatLog.formatDayLabel', () => {
  const t = (key: string) => (key === 'res.chat.today' ? 'Hoy' : 'Ayer');

  it('labels today as "Hoy"', () => {
    // 1.Arrange
    const today = dayKey(new Date().toISOString());

    // 2.Act / 3.Assert
    expect(formatDayLabel(today, t)).toBe('Hoy');
  });

  it('labels yesterday as "Ayer"', () => {
    // 1.Arrange
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);

    // 2.Act / 3.Assert
    expect(formatDayLabel(dayKey(yesterday.toISOString()), t)).toBe('Ayer');
  });

  it('formats older days as a localized long date', () => {
    // 1.Arrange / 2.Act
    const label = formatDayLabel('2026-01-05', t, 'es-AR');

    // 3.Assert
    expect(label).toContain('2026');
    expect(label).toContain('ene');
  });
});

describe('chatLog.groupMessagesByDay', () => {
  it('groups consecutive same-day messages into a single block', () => {
    // 1.Arrange
    const messages = [
      msg(1, 'a', { createdAt: new Date(2026, 0, 1, 10).toISOString() }),
      msg(2, 'b', { createdAt: new Date(2026, 0, 1, 11).toISOString() }),
      msg(3, 'c', { createdAt: new Date(2026, 0, 2, 9).toISOString() }),
    ];

    // 2.Act
    const groups = groupMessagesByDay(messages);

    // 3.Assert
    expect(groups).toHaveLength(2);
    expect(groups[0].dayKey).toBe('2026-01-01');
    expect(groups[0].messages).toHaveLength(2);
    expect(groups[1].dayKey).toBe('2026-01-02');
    expect(groups[1].messages).toHaveLength(1);
  });

  it('returns an empty array for an empty message list', () => {
    expect(groupMessagesByDay([])).toEqual([]);
  });
});
