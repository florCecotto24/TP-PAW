import { describe, test, expect } from 'vitest';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
const guardModule = require(path.resolve('src/main/webapp/js/reservation-chat-send-guard.js'));

describe('ReservationChatSendGuard', () => {
    test('tryAcquire blocks second acquire until release', () => {
        const guard = guardModule.createSendGuard();
        expect(guard.tryAcquire()).toBe(true);
        expect(guard.tryAcquire()).toBe(false);
        guard.release();
        expect(guard.tryAcquire()).toBe(true);
    });

    test('simulates rapid Enter on HTTP path with single success', () => {
        const guard = guardModule.createSendGuard();
        var successes = 0;
        for (var i = 0; i < 5; i++) {
            if (guard.tryAcquire()) {
                successes++;
            }
        }
        expect(successes).toBe(1);
        expect(guard.isLocked()).toBe(true);
    });
});
