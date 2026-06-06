/**
 * Owner-facing read-only availability calendar. Renders an inline Flatpickr for each
 * .js-owner-cal-anchor element, highlighting bookable days with compact prices.
 * When the container carries data-navigate-month="true", changing the visible month
 * reloads the page with ?month=YYYY-MM so the server-side list panel stays in sync.
 *
 * Depends on: flatpickr (global), components.js (window.RydenFlatpickrRange)
 */
(function () {
    'use strict';

    function pad2(n) {
        return String(n).padStart(2, '0');
    }

    function ymdFromDate(d) {
        if (!d) { return ''; }
        return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
    }

    function parseSegmentsJson(raw) {
        if (!raw) { return []; }
        try {
            var arr = JSON.parse(raw.trim());
            return Array.isArray(arr) ? arr : [];
        } catch (e) { return []; }
    }

    function findSegmentForYmd(ymd, segs) {
        for (var i = 0; i < segs.length; i++) {
            var s = segs[i];
            if (s.from <= ymd && ymd <= s.to) { return s; }
        }
        return null;
    }

    function compactPrice(price) {
        if (price == null || !isFinite(price)) { return ''; }
        var n = Math.round(price);
        if (n >= 1000000) {
            var m = n / 1000000;
            return '$' + (m % 1 === 0 ? m : m.toFixed(1)) + 'M';
        }
        if (n >= 1000) {
            var k = n / 1000;
            return '$' + (k % 1 === 0 ? k : k.toFixed(1)) + 'k';
        }
        return '$' + n;
    }

    function setUrlMonth(baseUrl, year, month) {
        return baseUrl + '?month=' + year + '-' + pad2(month);
    }

    function initOwnerCalendar(anchor) {
        if (typeof flatpickr === 'undefined') { return; }
        var container = anchor.closest('.owner-cal-container');
        if (!container) { return; }

        var rangesRaw = container.getAttribute('data-bookable-ranges');
        var segments = parseSegmentsJson(rangesRaw);
        var navigateOnMonthChange = container.getAttribute('data-navigate-month') === 'true';
        var baseUrl = container.getAttribute('data-base-url') || window.location.pathname;

        var enable = segments.map(function (s) {
            if (!window.RydenFlatpickrRange) { return null; }
            return {
                from: window.RydenFlatpickrRange.localWallDayStartFromYmd(s.from),
                to: window.RydenFlatpickrRange.localWallDayEndFromYmd(s.to)
            };
        }).filter(function (r) { return r && r.from && r.to; });

        var defaultMonthAttr = anchor.getAttribute('data-default-month');
        var defaultMonthDate = null;
        if (defaultMonthAttr && defaultMonthAttr.length >= 7) {
            var parts = defaultMonthAttr.split('-');
            if (parts.length >= 2) {
                var yr = parseInt(parts[0], 10);
                var mo = parseInt(parts[1], 10) - 1;
                if (!isNaN(yr) && !isNaN(mo)) {
                    defaultMonthDate = new Date(yr, mo, 1);
                }
            }
        }

        var fpConfig = {
            inline: true,
            mode: 'single',
            clickOpens: false,
            disableMobile: true,
            showMonths: 1,
            onDayCreate: function (dObj, dStr, fp, dayElem) {
                var date = dayElem.dateObj;
                if (!date) { return; }
                var ymd = ymdFromDate(date);
                var seg = findSegmentForYmd(ymd, segments);
                if (seg && seg.dayPrice != null) {
                    var priceEl = document.createElement('span');
                    priceEl.className = 'fp-day-price';
                    priceEl.textContent = compactPrice(seg.dayPrice);
                    dayElem.appendChild(priceEl);
                }
                if (seg) {
                    dayElem.addEventListener('click', function () {
                        var formSection = document.getElementById('inlinePeriodFormSection');
                        if (formSection && formSection.classList.contains('show')) { return; }
                        document.querySelectorAll('.manage-period-card.period-highlighted')
                                .forEach(function (el) { el.classList.remove('period-highlighted'); });
                        var card = document.querySelector(
                            '[data-period-from="' + seg.from + '"][data-period-to="' + seg.to + '"]'
                        );
                        if (card) {
                            card.classList.add('period-highlighted');
                            card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                        }
                    });
                }
            }
        };

        if (enable.length > 0) {
            fpConfig.enable = enable;
        } else {
            fpConfig.disable = [function () { return true; }];
        }

        if (navigateOnMonthChange) {
            fpConfig.onMonthChange = function (selectedDates, dateStr, fp) {
                var year = fp.currentYear;
                var month = fp.currentMonth + 1;
                window.location.href = setUrlMonth(baseUrl, year, month);
            };
        }

        var fp = flatpickr(anchor, fpConfig);

        if (fp && defaultMonthDate) {
            fp.jumpToDate(defaultMonthDate, false);
        }
    }

    function init() {
        var anchors = document.querySelectorAll('.js-owner-cal-anchor');
        for (var i = 0; i < anchors.length; i++) {
            initOwnerCalendar(anchors[i]);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
