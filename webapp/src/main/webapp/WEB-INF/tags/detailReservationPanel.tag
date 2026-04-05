<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="listingId" required="true" type="java.lang.Long" %>
<%@ attribute name="dailyPrice" required="true" type="java.lang.String" %>
<%@ attribute name="deliveryLocation" required="true" type="java.lang.String" %>
<%@ attribute name="fromDateTimeValue" required="false" type="java.lang.String" %>
<%@ attribute name="untilDateTimeValue" required="false" type="java.lang.String" %>
<%@ attribute name="carName" required="true" type="java.lang.String" %>
<%@ attribute name="reservationPeriods" required="false" type="java.util.List" %>

<c:set var="periodCount" value="${empty reservationPeriods ? 0 : fn:length(reservationPeriods)}" />

<c:if test="${periodCount == 1}">
    <c:forEach items="${reservationPeriods}" var="rp" begin="0" end="0">
        <c:set var="singlePeriodMin" value="${rp.minDateTimeLocal}" />
        <c:set var="singlePeriodMax" value="${rp.maxDateTimeLocal}" />
    </c:forEach>
</c:if>

<c:url var="newReservationUrl" value="/reservation/new" />

<div class="detail-reservation-panel border rounded-4 p-4 bg-white shadow-sm">
    <form action="<c:out value='${newReservationUrl}'/>" method="get" id="detailReservationForm">
    <input type="hidden" name="listingId" value="<c:out value='${listingId}'/>" />
    <input type="hidden" name="carName" value="<c:out value='${carName}'/>" />
    <input type="hidden" name="reservationTotal" id="detail_reservation_total_hint" value="" />

    <c:if test="${periodCount > 1}">
        <div class="mb-3">
            <label class="form-label fw-medium" for="detail_reservation_period">Published period for your reservation</label>
            <select class="form-select" name="availabilityId" id="detail_reservation_period"
                    aria-label="Choose which published period you are reserving in">
                <c:forEach items="${reservationPeriods}" var="rp">
                    <option value="<c:out value='${rp.availabilityId}'/>"
                            data-min="<c:out value='${rp.minDateTimeLocal}'/>"
                            data-max="<c:out value='${rp.maxDateTimeLocal}'/>">
                        <c:out value="${rp.label}"/>
                    </option>
                </c:forEach>
            </select>
            <p class="form-text small mb-0 mt-2">Pickup and return must fall inside the range you select.</p>
        </div>
    </c:if>

    <c:if test="${periodCount == 1}">
        <c:forEach items="${reservationPeriods}" var="rp" begin="0" end="0">
            <input type="hidden" name="availabilityId" value="<c:out value='${rp.availabilityId}'/>" />
        </c:forEach>
    </c:if>

    <div class="d-flex align-items-baseline gap-2 mb-3">
        <span class="detail-price-amount fw-bold">$<c:out value="${dailyPrice}"/></span>
        <span class="text-secondary">/ per day</span>
    </div>

    <h2 class="h6 fw-semibold mb-2">Pickup and return</h2>
    <p class="form-text small mb-2">
        <c:choose>
            <c:when test="${periodCount > 1}">Times use the listing&apos;s timezone (Argentina).</c:when>
            <c:when test="${periodCount == 1}">Must fall inside the owner&apos;s published window (Argentina time).</c:when>
            <c:otherwise>Choose when you need the vehicle.</c:otherwise>
        </c:choose>
    </p>
    <div class="row g-3 mb-3">
        <div class="col-md-6">
            <label class="form-label small mb-1" for="detail_from_d">Pickup</label>
            <div class="input-group input-group-sm shadow-none">
                <input type="hidden" name="fromDateTime" id="detail_from_hidden" value="<c:out value='${fromDateTimeValue}'/>"/>
                <span class="input-group-text border-0 bg-light"><i class="bi bi-calendar3" aria-hidden="true"></i></span>
                <input type="date" class="form-control border-0 shadow-none" id="detail_from_d" aria-label="Pickup date"
                       <c:if test="${periodCount == 1}">min="<c:out value='${fn:substring(singlePeriodMin, 0, 10)}'/>" max="<c:out value='${fn:substring(singlePeriodMax, 0, 10)}'/>"</c:if> />
                <span class="input-group-text border-0 bg-light"><i class="bi bi-clock" aria-hidden="true"></i></span>
                <input type="time" class="form-control border-0 shadow-none" id="detail_from_t" step="60" aria-label="Pickup time"/>
            </div>
        </div>
        <div class="col-md-6">
            <label class="form-label small mb-1" for="detail_until_d">Return</label>
            <div class="input-group input-group-sm shadow-none">
                <input type="hidden" name="untilDateTime" id="detail_until_hidden" value="<c:out value='${untilDateTimeValue}'/>"/>
                <span class="input-group-text border-0 bg-light"><i class="bi bi-calendar3" aria-hidden="true"></i></span>
                <input type="date" class="form-control border-0 shadow-none" id="detail_until_d" aria-label="Return date"
                       <c:if test="${periodCount == 1}">min="<c:out value='${fn:substring(singlePeriodMin, 0, 10)}'/>" max="<c:out value='${fn:substring(singlePeriodMax, 0, 10)}'/>"</c:if> />
                <span class="input-group-text border-0 bg-light"><i class="bi bi-clock" aria-hidden="true"></i></span>
                <input type="time" class="form-control border-0 shadow-none" id="detail_until_t" step="60" aria-label="Return time"/>
            </div>
        </div>
    </div>

    <div class="d-flex align-items-start gap-2 mb-3">
        <i class="bi bi-geo-alt text-secondary mt-1" aria-hidden="true"></i>
        <div class="flex-grow-1 w-100">
            <small class="text-muted d-block mb-1">Delivery location</small>
            <p class="mb-0 fw-medium"><c:out value="${deliveryLocation}"/></p>
        </div>
    </div>

    <div class="border-top pt-3 small" id="detail_pricing_summary" hidden>
        <div class="d-flex justify-content-between mb-3">
            <span class="text-secondary" id="detail_days_formula"></span>
            <span id="detail_subtotal_amount"></span>
        </div>
        <div class="d-flex justify-content-between align-items-baseline mb-4">
            <span class="fw-semibold">Total</span>
            <span class="detail-total-amount fw-bold fs-4" id="detail_total_amount"></span>
        </div>
    </div>

    <div class="alert alert-danger mb-3" id="detail_date_alert" role="alert" hidden>
        Please select valid pickup and return dates to continue.
    </div>

    <button type="submit" class="btn btn-lg btn-primary w-100 py-3 rounded-3 mb-2" id="detailReservationSubmitBtn">Start reservation</button>
    </form>

    <p class="text-center text-secondary small mb-0 text-uppercase detail-reservation-disclaimer">
        You won&apos;t be charged yet
    </p>
</div>

<script>
(function () {
    var initialized = false;

    function initPanel() {
    if (initialized) {
        return true;
    }

    var P = window.PawDateTimePair;
    if (!P) {
        return false;
    }

    var fh = document.getElementById('detail_from_hidden');
    var fd = document.getElementById('detail_from_d');
    var ft = document.getElementById('detail_from_t');
    var uh = document.getElementById('detail_until_hidden');
    var ud = document.getElementById('detail_until_d');
    var ut = document.getElementById('detail_until_t');
    var pricingSummary = document.getElementById('detail_pricing_summary');
    var daysFormula = document.getElementById('detail_days_formula');
    var subtotalAmount = document.getElementById('detail_subtotal_amount');
    var totalAmount = document.getElementById('detail_total_amount');

    var pFrom = P.bindPair(fh, fd, ft);
    var pUntil = P.bindPair(uh, ud, ut);
    var form = document.getElementById('detailReservationForm');
    var dateAlert = document.getElementById('detail_date_alert');
    var totalHintField = document.getElementById('detail_reservation_total_hint');

    function parseMoney(value) {
        if (typeof value !== 'string') {
            return NaN;
        }
        return parseFloat(value.replace(',', '.'));
    }

    function parseLocalDateTime(dateValue, timeValue) {
        if (!dateValue || !timeValue) {
            return null;
        }
        var dateParts = dateValue.split('-');
        var timeParts = timeValue.split(':');
        if (dateParts.length !== 3 || timeParts.length < 2) {
            return null;
        }
        var year = parseInt(dateParts[0], 10);
        var month = parseInt(dateParts[1], 10);
        var day = parseInt(dateParts[2], 10);
        var hour = parseInt(timeParts[0], 10);
        var minute = parseInt(timeParts[1], 10);
        var dt = new Date(year, month - 1, day, hour, minute, 0, 0);
        if (isNaN(dt.getTime())) {
            return null;
        }
        return dt;
    }

    function formatMoney(value) {
        var rounded = Math.round(value * 100) / 100;
        var fixed = rounded.toFixed(2);
        return fixed.replace(/\.00$/, '').replace(/(\.\d)0$/, '$1');
    }

    function computeBillableDays(startDate, endDate) {
        var diffMs = endDate.getTime() - startDate.getTime();
        if (diffMs <= 0) {
            return 0;
        }
        return Math.ceil(diffMs / 86400000);
    }

    var dailyPriceValue = parseMoney('<c:out value="${dailyPrice}"/>');

    function hidePricing() {
        if (pricingSummary) {
            pricingSummary.hidden = true;
        }
        if (totalHintField) {
            totalHintField.value = '';
        }
    }

    function setDateAlertVisible(visible) {
        if (!dateAlert) {
            return;
        }
        dateAlert.hidden = !visible;
    }

    function updatePricing() {
        if (!pricingSummary || !daysFormula || !subtotalAmount || !totalAmount || isNaN(dailyPriceValue)) {
            return;
        }

        var startDate = parseLocalDateTime(fd ? fd.value : '', ft ? (ft.value || '00:00') : '');
        var endDate = parseLocalDateTime(ud ? ud.value : '', ut ? (ut.value || '00:00') : '');

        if (!startDate || !endDate) {
            hidePricing();
            return;
        }

        var days = computeBillableDays(startDate, endDate);
        if (days <= 0) {
            hidePricing();
            return;
        }

        var total = dailyPriceValue * days;
        var dailyPriceText = formatMoney(dailyPriceValue);
        var totalText = formatMoney(total);
        var dayLabel = days === 1 ? 'day' : 'days';

        daysFormula.textContent = '$' + dailyPriceText + ' × ' + days + ' ' + dayLabel;
        subtotalAmount.textContent = '$' + totalText;
        totalAmount.textContent = '$' + totalText;
        if (totalHintField) {
            totalHintField.value = totalText;
        }
        pricingSummary.hidden = false;
    }

    if (form) {
        form.addEventListener('submit', function (event) {
            if (pFrom) pFrom.syncToHidden();
            if (pUntil) pUntil.syncToHidden();

            var submitStartDate = parseLocalDateTime(fd ? fd.value : '', ft ? (ft.value || '00:00') : '');
            var submitEndDate = parseLocalDateTime(ud ? ud.value : '', ut ? (ut.value || '00:00') : '');
            var hasValidRange = submitStartDate && submitEndDate && computeBillableDays(submitStartDate, submitEndDate) > 0;

            if (!hasValidRange) {
                event.preventDefault();
                setDateAlertVisible(true);
                return;
            }

            setDateAlertVisible(false);
        });
    }

    if (fd) {
        fd.addEventListener('change', function () { setDateAlertVisible(false); updatePricing(); });
        fd.addEventListener('input', function () { setDateAlertVisible(false); updatePricing(); });
    }
    if (ft) {
        ft.addEventListener('change', function () { setDateAlertVisible(false); updatePricing(); });
        ft.addEventListener('input', function () { setDateAlertVisible(false); updatePricing(); });
    }
    if (ud) {
        ud.addEventListener('change', function () { setDateAlertVisible(false); updatePricing(); });
        ud.addEventListener('input', function () { setDateAlertVisible(false); updatePricing(); });
    }
    if (ut) {
        ut.addEventListener('change', function () { setDateAlertVisible(false); updatePricing(); });
        ut.addEventListener('input', function () { setDateAlertVisible(false); updatePricing(); });
    }

    <c:if test="${periodCount > 1}">
    var sel = document.getElementById('detail_reservation_period');
    if (sel && pFrom && pUntil) {
        function applyWindow() {
            var opt = sel.options[sel.selectedIndex];
            if (!opt) return;
            var min = opt.getAttribute('data-min');
            var max = opt.getAttribute('data-max');
            if (!min || !max) return;
            pFrom.setDateBounds(min, max);
            pUntil.setDateBounds(min, max);
            if (fh) fh.value = '';
            if (fd) fd.value = '';
            if (ft) ft.value = '00:00';
            if (uh) uh.value = '';
            if (ud) ud.value = '';
            if (ut) ut.value = '00:00';
            updatePricing();
        }
        sel.addEventListener('change', applyWindow);
        applyWindow();
    }
    </c:if>

    updatePricing();
    initialized = true;
    return true;
    }

    function boot() {
        if (initPanel()) {
            return;
        }
        var tries = 0;
        var maxTries = 40;
        var retry = window.setInterval(function () {
            tries += 1;
            if (initPanel() || tries >= maxTries) {
                window.clearInterval(retry);
            }
        }, 25);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }
})();
</script>
