(function (root, factory) {
    var api = factory();
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    } else {
        root.ReservationChatWallTime = api;
    }
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
    'use strict';

    var WALL_TIMEZONE = 'America/Argentina/Buenos_Aires';
    var MS_PER_DAY = 86400000;

    function parseTimestamp(value) {
        if (value == null || value === '') {
            return null;
        }
        if (typeof value === 'number' && isFinite(value)) {
            return new Date(value < 1e12 ? value * 1000 : value);
        }
        if (typeof value === 'string') {
            var s = value.trim();
            if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(s) && !/[Zz]|[+-]\d{2}:\d{2}$/.test(s)) {
                s += 'Z';
            }
            var parsed = Date.parse(s);
            return isNaN(parsed) ? null : new Date(parsed);
        }
        return null;
    }

    function wallDayKeyFromDate(date) {
        if (!date || isNaN(date.getTime())) {
            return '';
        }
        return new Intl.DateTimeFormat('en-CA', {
            timeZone: WALL_TIMEZONE,
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        }).format(date);
    }

    function wallDayKey(value) {
        return wallDayKeyFromDate(parseTimestamp(value));
    }

    function wallTodayKey(now) {
        return wallDayKeyFromDate(now instanceof Date ? now : new Date());
    }

    function wallYesterdayKey(now) {
        var todayKey = wallTodayKey(now);
        var parts = todayKey.split('-');
        if (parts.length !== 3) {
            return '';
        }
        var noonUtc = Date.UTC(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]), 15, 0, 0);
        return wallDayKeyFromDate(new Date(noonUtc - MS_PER_DAY));
    }

    function formatDayLabel(dayKey, options) {
        options = options || {};
        var labelToday = options.labelToday != null ? options.labelToday : 'Today';
        var labelYesterday = options.labelYesterday != null ? options.labelYesterday : 'Yesterday';
        var locale = options.intlLocale;
        if (!dayKey) {
            return '';
        }
        var now = options.now instanceof Date ? options.now : new Date();
        if (dayKey === wallTodayKey(now)) {
            return labelToday;
        }
        if (dayKey === wallYesterdayKey(now)) {
            return labelYesterday;
        }
        try {
            return new Intl.DateTimeFormat(locale, {
                timeZone: WALL_TIMEZONE,
                dateStyle: 'medium'
            }).format(new Date(dayKey + 'T15:00:00Z'));
        } catch (e) {
            return dayKey;
        }
    }

    function formatMessageTime(value, options) {
        options = options || {};
        var locale = options.intlLocale;
        var date = parseTimestamp(value);
        if (!date || isNaN(date.getTime())) {
            return '';
        }
        try {
            return new Intl.DateTimeFormat(locale, {
                timeZone: WALL_TIMEZONE,
                timeStyle: 'short'
            }).format(date);
        } catch (e) {
            return '';
        }
    }

    return {
        WALL_TIMEZONE: WALL_TIMEZONE,
        parseTimestamp: parseTimestamp,
        wallDayKeyFromDate: wallDayKeyFromDate,
        wallDayKey: wallDayKey,
        wallTodayKey: wallTodayKey,
        wallYesterdayKey: wallYesterdayKey,
        formatDayLabel: formatDayLabel,
        formatMessageTime: formatMessageTime
    };
}));
