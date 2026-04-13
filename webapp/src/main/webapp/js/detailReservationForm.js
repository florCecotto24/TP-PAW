/**
 * Car detail reservation (GET /reservation/new): Flatpickr range limited to bookable segments,
 * early client-side check that pickup/return dates are complete before submit.
 * Depends on flatpickr and window.RydenFlatpickrRange from components.js (load this file after).
 */
(function () {
    function init() {
        var daterangeInput = document.getElementById('detail_daterange');
        var fromHidden = document.getElementById('detail_from_hidden');
        var untilHidden = document.getElementById('detail_until_hidden');
        var form = document.getElementById('detailReservationForm');
        var dateAlert = document.getElementById('detail_date_alert');
        if (!daterangeInput || !fromHidden || !untilHidden || !form || !window.RydenFlatpickrRange) {
            return;
        }

        function hasCompleteRange() {
            return !!fromHidden.value && !!untilHidden.value;
        }

        function syncDateAlert() {
            if (!dateAlert) {
                return;
            }
            if (hasCompleteRange()) {
                dateAlert.setAttribute('hidden', 'hidden');
            } else {
                dateAlert.removeAttribute('hidden');
            }
        }

        function parseBookableRangesJson(raw) {
            if (!raw || typeof raw !== 'string') {
                return [];
            }
            try {
                var parsed = JSON.parse(raw.trim());
                if (!Array.isArray(parsed)) {
                    return [];
                }
                var out = [];
                for (var i = 0; i < parsed.length; i++) {
                    var seg = parsed[i];
                    if (!seg || typeof seg.from !== 'string' || typeof seg.to !== 'string') {
                        continue;
                    }
                    var fromD = RydenFlatpickrRange.localWallDayStartFromYmd(seg.from);
                    var toD = RydenFlatpickrRange.localWallDayEndFromYmd(seg.to);
                    if (fromD && toD) {
                        out.push({ from: fromD, to: toD });
                    }
                }
                return out;
            } catch (e) {
                return [];
            }
        }

        var pickAttr = form.getAttribute('data-pickup-time') || '10:00';
        var retAttr = form.getAttribute('data-return-time') || '18:00';
        var segments = parseBookableRangesJson(form.getAttribute('data-bookable-ranges'));
        var enable = segments.map(function (s) {
            return { from: s.from, to: s.to };
        });

        var dd = [];
        if (fromHidden.value && fromHidden.value.length >= 10) {
            var d1 = RydenFlatpickrRange.localWallDayStartFromYmd(fromHidden.value);
            if (d1) {
                dd.push(d1);
            }
        }
        if (untilHidden.value && untilHidden.value.length >= 10) {
            var d2 = RydenFlatpickrRange.localWallDayStartFromYmd(untilHidden.value);
            if (d2) {
                dd.push(d2);
            }
        }

        var initOpts = {
            anchor: daterangeInput,
            fromHidden: fromHidden,
            untilHidden: untilHidden,
            combineWallTimes: { pickup: pickAttr, returnDeadline: retAttr },
            dateFormat: 'Y-m-d',
            defaultDate: dd.length ? dd : undefined
        };
        if (enable.length > 0) {
            initOpts.enable = enable;
        } else {
            initOpts.disableAllDates = true;
        }

        var ctrl = RydenFlatpickrRange.init(initOpts);
        if (!ctrl) {
            return;
        }

        daterangeInput.addEventListener('change', syncDateAlert);
        daterangeInput.addEventListener('input', syncDateAlert);

        form.addEventListener('submit', function (e) {
            syncDateAlert();
            if (!hasCompleteRange()) {
                e.preventDefault();
                return false;
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
