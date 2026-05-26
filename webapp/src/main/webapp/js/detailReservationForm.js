/**
 * Car detail reservation (GET /reservation/new): Flatpickr range limited to bookable wall-day segments,
 * derives the actual pickup/return wall times and pickup/return locations from the EFFECTIVE segment that
 * covers each end of the rider-selected range (a car has many ListingAvailability rows with potentially
 * different prices, hours and locations; for a given day the row with the most recent createdAt wins).
 *
 * Each bookable segment in data-bookable-ranges carries: { from, to, dayPrice, checkInTime, checkOutTime,
 * location }. Pickup attributes come from the segment containing the first selected day; return attributes
 * from the segment containing the last selected day. The subtotal/total are summed day-by-day using each
 * day's effective dayPrice.
 *
 * Depends on flatpickr and window.RydenFlatpickrRange from components.js (load this file after).
 */
(function () {
    function init() {
        var daterangeInput = document.getElementById('detail_daterange');
        var fromHidden = document.getElementById('detail_from_hidden');
        var untilHidden = document.getElementById('detail_until_hidden');
        var form = document.getElementById('detailReservationForm');
        var dateAlert = document.getElementById('detail_date_alert');
        var maxBillableAlert = document.getElementById('detail_max_billable_alert');
        var submitBtn = document.getElementById('detailReservationSubmitBtn');
        var pickupTimeLabel = document.getElementById('detail_pickup_time_label');
        var returnTimeLabel = document.getElementById('detail_return_time_label');
        var pickupLocationLabel = document.getElementById('detail_pickup_location_label');
        var returnLocationLabel = document.getElementById('detail_return_location_label');
        var headerLocationLabel = document.getElementById('detail_header_location');
        var pricingSummary = document.getElementById('detail_pricing_summary');
        var daysFormula = document.getElementById('detail_days_formula');
        var subtotalAmount = document.getElementById('detail_subtotal_amount');
        var totalAmount = document.getElementById('detail_total_amount');
        var totalHidden = document.getElementById('detail_reservation_total_hint');
        if (!daterangeInput || !fromHidden || !untilHidden || !form || !window.RydenFlatpickrRange) {
            return;
        }

        var DASH = '\u2014';
        var currencyFmt = (typeof Intl !== 'undefined' && Intl.NumberFormat)
            ? new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 })
            : { format: function (n) { return '$' + Math.round(n); } };

        function ymdFromDate(d) {
            var y = d.getFullYear();
            var m = d.getMonth() + 1;
            var dd = d.getDate();
            return y + '-' + (m < 10 ? '0' + m : m) + '-' + (dd < 10 ? '0' + dd : dd);
        }

        function ymdCompare(a, b) {
            return a < b ? -1 : a > b ? 1 : 0;
        }

        function ymdPlusOne(ymd) {
            var p = ymd.split('-');
            var d = new Date(parseInt(p[0], 10), parseInt(p[1], 10) - 1, parseInt(p[2], 10));
            d.setDate(d.getDate() + 1);
            return ymdFromDate(d);
        }

        function normalizeHm(value) {
            if (!value || typeof value !== 'string') {
                return null;
            }
            var m = value.match(/^(\d{1,2}):(\d{2})/);
            if (!m) {
                return null;
            }
            var h = parseInt(m[1], 10);
            var mm = parseInt(m[2], 10);
            if (!isFinite(h) || !isFinite(mm) || h < 0 || h > 23 || mm < 0 || mm > 59) {
                return null;
            }
            return (h < 10 ? '0' + h : h) + ':' + (mm < 10 ? '0' + mm : mm);
        }

        function parseSegmentsJson(raw) {
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
                    var dayPriceN = (seg.dayPrice == null) ? null : Number(seg.dayPrice);
                    out.push({
                        from: seg.from,
                        to: seg.to,
                        dayPrice: (dayPriceN != null && isFinite(dayPriceN)) ? dayPriceN : null,
                        checkInTime: normalizeHm(seg.checkInTime),
                        checkOutTime: normalizeHm(seg.checkOutTime),
                        location: typeof seg.location === 'string' ? seg.location : ''
                    });
                }
                return out;
            } catch (e) {
                return [];
            }
        }

        function findSegmentForYmd(ymd, segs) {
            for (var i = 0; i < segs.length; i++) {
                var s = segs[i];
                if (ymdCompare(s.from, ymd) <= 0 && ymdCompare(ymd, s.to) <= 0) {
                    return s;
                }
            }
            return null;
        }

        function wallYmdFromHidden(iso) {
            if (!iso || typeof iso !== 'string' || iso.length < 10) {
                return null;
            }
            return iso.substring(0, 10);
        }

        function billableDaysInclusive(fromIso, untilIso) {
            var a = wallYmdFromHidden(fromIso);
            var b = wallYmdFromHidden(untilIso);
            if (!a || !b) {
                return 0;
            }
            var p = a.split('-');
            var q = b.split('-');
            if (p.length !== 3 || q.length !== 3) {
                return 0;
            }
            var d0 = new Date(parseInt(p[0], 10), parseInt(p[1], 10) - 1, parseInt(p[2], 10));
            var d1 = new Date(parseInt(q[0], 10), parseInt(q[1], 10) - 1, parseInt(q[2], 10));
            var diff = Math.round((d1 - d0) / 86400000);
            return Math.max(1, diff + 1);
        }

        function computeSubtotalForRange(fromYmd, untilYmd, segs) {
            var total = 0;
            var days = 0;
            var anyMissing = false;
            for (var d = fromYmd; ymdCompare(d, untilYmd) <= 0; d = ymdPlusOne(d)) {
                var s = findSegmentForYmd(d, segs);
                if (!s || s.dayPrice == null) {
                    anyMissing = true;
                    days++;
                    continue;
                }
                total += s.dayPrice;
                days++;
            }
            return { total: total, days: days, complete: !anyMissing };
        }

        function clearLabels() {
            if (pickupTimeLabel) { pickupTimeLabel.textContent = DASH; }
            if (returnTimeLabel) { returnTimeLabel.textContent = DASH; }
            if (pickupLocationLabel) { pickupLocationLabel.textContent = DASH; }
            if (returnLocationLabel) { returnLocationLabel.textContent = DASH; }
            if (pricingSummary) { pricingSummary.setAttribute('hidden', 'hidden'); }
            if (daysFormula) { daysFormula.textContent = ''; }
            if (subtotalAmount) { subtotalAmount.textContent = ''; }
            if (totalAmount) { totalAmount.textContent = ''; }
            if (totalHidden) { totalHidden.value = ''; }
        }

        function applyRangeProjection(selectedDates, segs) {
            if (!selectedDates || selectedDates.length === 0) {
                clearLabels();
                fromHidden.value = '';
                untilHidden.value = '';
                return;
            }
            var fromYmd = ymdFromDate(selectedDates[0]);
            var pickupSeg = findSegmentForYmd(fromYmd, segs);
            var pickupHm = pickupSeg && pickupSeg.checkInTime ? pickupSeg.checkInTime : null;
            if (pickupTimeLabel) { pickupTimeLabel.textContent = pickupHm || DASH; }
            if (pickupLocationLabel) { pickupLocationLabel.textContent = (pickupSeg && pickupSeg.location) || DASH; }
            fromHidden.value = pickupHm ? (fromYmd + 'T' + pickupHm) : fromYmd;

            if (selectedDates.length < 2) {
                if (returnTimeLabel) { returnTimeLabel.textContent = DASH; }
                if (returnLocationLabel) { returnLocationLabel.textContent = DASH; }
                if (pricingSummary) { pricingSummary.setAttribute('hidden', 'hidden'); }
                if (totalHidden) { totalHidden.value = ''; }
                untilHidden.value = '';
                return;
            }

            var untilYmd = ymdFromDate(selectedDates[1]);
            var returnSeg = findSegmentForYmd(untilYmd, segs);
            var returnHm = returnSeg && returnSeg.checkOutTime ? returnSeg.checkOutTime : null;
            if (returnTimeLabel) { returnTimeLabel.textContent = returnHm || DASH; }
            if (returnLocationLabel) { returnLocationLabel.textContent = (returnSeg && returnSeg.location) || DASH; }
            untilHidden.value = returnHm ? (untilYmd + 'T' + returnHm) : untilYmd;

            var subtotalInfo = computeSubtotalForRange(fromYmd, untilYmd, segs);
            if (pricingSummary) {
                if (subtotalInfo.complete && subtotalInfo.days > 0) {
                    pricingSummary.removeAttribute('hidden');
                    if (daysFormula) {
                        daysFormula.textContent = subtotalInfo.days + ' \u00d7 ' + currencyFmt.format(subtotalInfo.total / subtotalInfo.days);
                    }
                    if (subtotalAmount) { subtotalAmount.textContent = currencyFmt.format(subtotalInfo.total); }
                    if (totalAmount) { totalAmount.textContent = currencyFmt.format(subtotalInfo.total); }
                    if (totalHidden) { totalHidden.value = String(subtotalInfo.total); }
                } else {
                    pricingSummary.setAttribute('hidden', 'hidden');
                    if (totalHidden) { totalHidden.value = ''; }
                }
            }
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

        function syncMaxBillableAlert() {
            if (!maxBillableAlert) {
                return;
            }
            var maxStr = form.getAttribute('data-max-billable-days');
            var maxD = parseInt(maxStr, 10);
            if (!isFinite(maxD) || maxD < 1) {
                maxBillableAlert.setAttribute('hidden', 'hidden');
                if (submitBtn) { submitBtn.disabled = false; }
                return;
            }
            if (!hasCompleteRange()) {
                maxBillableAlert.setAttribute('hidden', 'hidden');
                if (submitBtn) { submitBtn.disabled = false; }
                return;
            }
            var n = billableDaysInclusive(fromHidden.value, untilHidden.value);
            if (n > maxD) {
                maxBillableAlert.removeAttribute('hidden');
                if (submitBtn) { submitBtn.disabled = true; }
            } else {
                maxBillableAlert.setAttribute('hidden', 'hidden');
                if (submitBtn) { submitBtn.disabled = false; }
            }
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

        function parseSearchNbIds(raw) {
            if (!raw || typeof raw !== 'string') { return []; }
            try {
                var arr = JSON.parse(raw.trim());
                return Array.isArray(arr) ? arr.map(Number).filter(isFinite) : [];
            } catch (e) { return []; }
        }

        var segments = parseSegmentsJson(form.getAttribute('data-bookable-ranges'));
        var enable = segments.map(function (s) {
            return {
                from: RydenFlatpickrRange.localWallDayStartFromYmd(s.from),
                to: RydenFlatpickrRange.localWallDayEndFromYmd(s.to)
            };
        }).filter(function (r) { return r.from && r.to; });

        var dd = [];
        if (fromHidden.value && fromHidden.value.length >= 10) {
            var d1 = RydenFlatpickrRange.localWallDayStartFromYmd(fromHidden.value);
            if (d1) { dd.push(d1); }
        }
        if (untilHidden.value && untilHidden.value.length >= 10) {
            var d2 = RydenFlatpickrRange.localWallDayStartFromYmd(untilHidden.value);
            if (d2) { dd.push(d2); }
        }

        // If no date params from URL, pre-select dates of the first segment matching
        // a searched neighbourhood (passed from the explore/search results page).
        if (dd.length === 0) {
            var searchNbIds = parseSearchNbIds(form.getAttribute('data-search-nb-ids'));
            if (searchNbIds.length > 0) {
                var matchSeg = null;
                for (var si = 0; si < segments.length; si++) {
                    var seg = segments[si];
                    if (seg.neighborhoodId != null && searchNbIds.indexOf(seg.neighborhoodId) !== -1) {
                        matchSeg = seg;
                        break;
                    }
                }
                if (matchSeg) {
                    var nbD1 = RydenFlatpickrRange.localWallDayStartFromYmd(matchSeg.from);
                    var nbD2 = RydenFlatpickrRange.localWallDayStartFromYmd(matchSeg.to);
                    if (nbD1) { dd.push(nbD1); fromHidden.value = matchSeg.from; }
                    if (nbD2) { dd.push(nbD2); untilHidden.value = matchSeg.to; }
                }
            }
        }

        var initOpts = {
            anchor: daterangeInput,
            fromHidden: fromHidden,
            untilHidden: untilHidden,
            dateFormat: 'Y-m-d',
            defaultDate: dd.length ? dd : undefined,
            inline: true,
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
            }
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

        if (ctrl.fp && ctrl.fp.config && Array.isArray(ctrl.fp.config.onChange)) {
            ctrl.fp.config.onChange.push(function (selectedDates) {
                applyRangeProjection(selectedDates, segments);
                syncMaxBillableAlert();
            });
        }

        if (headerLocationLabel && segments.length > 0) {
            var firstLoc = segments[0].location || '';
            var allSame = segments.every(function (s) { return (s.location || '') === firstLoc; });
            headerLocationLabel.textContent = (allSame ? firstLoc : firstLoc) || DASH;
        }

        if (dd.length) {
            applyRangeProjection(dd, segments);
        } else {
            clearLabels();
        }

        syncMaxBillableAlert();

        daterangeInput.addEventListener('change', function () {
            syncMaxBillableAlert();
        });
        daterangeInput.addEventListener('input', function () {
            syncMaxBillableAlert();
        });

        form.addEventListener('submit', function (e) {
            syncDateAlert();
            syncMaxBillableAlert();
            if (!hasCompleteRange()) {
                e.preventDefault();
                return false;
            }
            var maxStr = form.getAttribute('data-max-billable-days');
            var maxD = parseInt(maxStr, 10);
            if (isFinite(maxD) && maxD >= 1) {
                var n = billableDaysInclusive(fromHidden.value, untilHidden.value);
                if (n > maxD) {
                    e.preventDefault();
                    return false;
                }
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
