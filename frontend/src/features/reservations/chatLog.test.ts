import { describe, expect, it } from 'vitest';
import { latestMessageId, mergeMessages, messageId } from './chatLog';
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
