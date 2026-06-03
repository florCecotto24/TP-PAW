import { describe, test, expect } from 'vitest';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
const mergeModule = require(path.resolve('src/main/webapp/js/reservation-chat-message-merge.js'));

describe('ReservationChatMessageMerge', () => {
    test('mergeIncomingMessages dedupes by id', () => {
        var rendered = { '1': true, '2': true };
        var incoming = [{ id: 2, body: 'dup' }, { id: 3, body: 'new' }];
        var merged = mergeModule.mergeIncomingMessages(rendered, incoming);
        expect(merged).toHaveLength(1);
        expect(merged[0].id).toBe(3);
        expect(rendered['3']).toBeUndefined();
    });

    test('mergeIncomingMessages skips DTOs without id', () => {
        var rendered = Object.create(null);
        var incoming = [{ body: 'no id' }, { id: 7, body: 'ok' }];
        var merged = mergeModule.mergeIncomingMessages(rendered, incoming);
        expect(merged).toHaveLength(1);
        expect(merged[0].id).toBe(7);
    });
});
