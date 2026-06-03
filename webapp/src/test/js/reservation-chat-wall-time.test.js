import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest';
import path from 'path';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
const wallTimePath = path.resolve('src/main/webapp/js/reservation-chat-wall-time.js');
const wallTime = require(wallTimePath);

describe('ReservationChatWallTime', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-06-02T18:00:00Z'));
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    test('parseTimestamp parses ISO with Z', () => {
        const date = wallTime.parseTimestamp('2026-06-02T15:00:00Z');
        expect(date.toISOString()).toBe('2026-06-02T15:00:00.000Z');
    });

    test('parseTimestamp treats ISO without offset as UTC', () => {
        const dayKey = wallTime.wallDayKey('2026-06-02T02:00:00');
        expect(dayKey).toBe('2026-06-01');
    });

    test('parseTimestamp converts epoch seconds', () => {
        const seconds = Math.floor(Date.parse('2026-06-02T15:00:00Z') / 1000);
        const date = wallTime.parseTimestamp(seconds);
        expect(date.toISOString()).toBe('2026-06-02T15:00:00.000Z');
    });

    test('wallYesterdayKey is one calendar day before today in ART', () => {
        expect(wallTime.wallTodayKey()).toBe('2026-06-02');
        expect(wallTime.wallYesterdayKey()).toBe('2026-06-01');
    });

    test('formatDayLabel returns today label for current day', () => {
        const label = wallTime.formatDayLabel('2026-06-02', {
            labelToday: 'Today',
            labelYesterday: 'Yesterday',
            now: new Date('2026-06-02T18:00:00Z')
        });
        expect(label).toBe('Today');
    });

    test('formatMessageTime converts UTC to ART', () => {
        const time = wallTime.formatMessageTime('2026-06-02T15:00:00Z', { intlLocale: 'en' });
        expect(time).toBe('12:00 PM');
    });

    test('formatDayLabel uses UTC anchor for older dates', () => {
        const label = wallTime.formatDayLabel('2026-05-30', {
            labelToday: 'Today',
            labelYesterday: 'Yesterday',
            intlLocale: 'en',
            now: new Date('2026-06-02T18:00:00Z')
        });
        expect(label).toMatch(/May 30, 2026/);
    });
});
