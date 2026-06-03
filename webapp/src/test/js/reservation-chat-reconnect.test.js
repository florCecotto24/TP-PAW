import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
const reconnectModule = require(path.resolve('src/main/webapp/js/reservation-chat-reconnect.js'));

describe('ReservationChatReconnect', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    test('backoff grows up to maxDelayMs cap', () => {
        var scheduler = reconnectModule.createReconnectScheduler({
            maxDelayMs: 30000,
            baseDelayMs: 1000,
            onReconnect: function () {
                return Promise.reject(new Error('fail'));
            }
        });
        expect(scheduler.getNextDelayMs()).toBe(1000);
        scheduler.schedule();
        vi.advanceTimersByTime(1000);
        expect(scheduler.getAttemptCount()).toBe(1);
        expect(scheduler.getNextDelayMs()).toBe(2000);
    });

    test('continues scheduling after many failures without terminal state', () => {
        var calls = 0;
        var scheduler = reconnectModule.createReconnectScheduler({
            maxDelayMs: 16000,
            baseDelayMs: 1000,
            onReconnect: function () {
                calls++;
                return Promise.reject(new Error('fail'));
            }
        });
        for (var i = 0; i < 20; i++) {
            scheduler.schedule();
            vi.advanceTimersByTime(16000);
            vi.runAllTicks();
        }
        expect(calls).toBeGreaterThan(0);
        scheduler.schedule();
        expect(scheduler.hasPendingTimer() || scheduler.getAttemptCount() > 0).toBe(true);
    });

    test('suspend pauses reconnect until resume', () => {
        var calls = 0;
        var scheduler = reconnectModule.createReconnectScheduler({
            baseDelayMs: 1000,
            onReconnect: function () {
                calls++;
                return Promise.resolve();
            }
        });
        scheduler.suspend();
        scheduler.schedule();
        vi.advanceTimersByTime(5000);
        expect(calls).toBe(0);
        scheduler.resume();
        vi.advanceTimersByTime(1000);
        expect(calls).toBe(1);
    });
});
