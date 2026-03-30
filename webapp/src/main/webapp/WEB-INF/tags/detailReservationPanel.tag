<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="listingId" required="true" type="java.lang.Long" %>
<%@ attribute name="pricePerDay" required="true" type="java.lang.String" %>
<%@ attribute name="days" required="true" type="java.lang.String" %>
<%@ attribute name="subtotal" required="true" type="java.lang.String" %>
<%@ attribute name="serviceFee" required="true" type="java.lang.String" %>
<%@ attribute name="total" required="true" type="java.lang.String" %>
<%@ attribute name="deliveryLocation" required="true" type="java.lang.String" %>
<%@ attribute name="fromDateTimeValue" required="false" type="java.lang.String" %>
<%@ attribute name="untilDateTimeValue" required="false" type="java.lang.String" %>
<%@ attribute name="carName" required="false" type="java.lang.String" %>
<%@ attribute name="reservationPeriods" required="false" type="java.util.List" %>

<c:if test="${empty carName}">
    <c:set var="carName" value="Vehicle" />
</c:if>
<c:set var="periodCount" value="${empty reservationPeriods ? 0 : fn:length(reservationPeriods)}" />

<c:if test="${periodCount == 1}">
    <c:forEach items="${reservationPeriods}" var="rp" begin="0" end="0">
        <c:set var="singlePeriodMin" value="${rp.minDateTimeLocal}" />
        <c:set var="singlePeriodMax" value="${rp.maxDateTimeLocal}" />
    </c:forEach>
</c:if>

<c:url var="newReservationUrl" value="/reservation/new" />

<div class="detail-reservation-panel border rounded-4 p-4 bg-white shadow-sm">
    <form action="${newReservationUrl}" method="get" id="detailReservationForm">
    <input type="hidden" name="listingId" value="${listingId}" />
    <input type="hidden" name="carName" value="${carName}" />

    <c:if test="${periodCount > 1}">
        <div class="mb-3">
            <label class="form-label fw-medium" for="detail_reservation_period">Published period for your reservation</label>
            <select class="form-select" name="availabilityId" id="detail_reservation_period"
                    aria-label="Choose which published period you are reserving in">
                <c:forEach items="${reservationPeriods}" var="rp">
                    <option value="${rp.availabilityId}"
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
            <input type="hidden" name="availabilityId" value="${rp.availabilityId}" />
        </c:forEach>
    </c:if>

    <div class="d-flex align-items-baseline gap-2 mb-3">
        <span class="detail-price-amount fw-bold">$${pricePerDay}</span>
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
                       <c:if test="${periodCount == 1}">min="${fn:substring(singlePeriodMin, 0, 10)}" max="${fn:substring(singlePeriodMax, 0, 10)}"</c:if> />
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
                       <c:if test="${periodCount == 1}">min="${fn:substring(singlePeriodMin, 0, 10)}" max="${fn:substring(singlePeriodMax, 0, 10)}"</c:if> />
                <span class="input-group-text border-0 bg-light"><i class="bi bi-clock" aria-hidden="true"></i></span>
                <input type="time" class="form-control border-0 shadow-none" id="detail_until_t" step="60" aria-label="Return time"/>
            </div>
        </div>
    </div>

    <div class="d-flex align-items-center bg-white rounded-3 px-3 py-2 border mb-3">
        <div class="form-floating flex-grow-1 w-100">
            <input type="text" class="form-control border-0 shadow-none" name="deliveryLocation" id="detail_delivery_location"
                   value="${deliveryLocation}" aria-label="Delivery location">
            <label for="detail_delivery_location">Delivery location</label>
        </div>
    </div>

    <div class="border-top pt-3 small">
        <div class="d-flex justify-content-between mb-2">
            <span class="text-secondary">$${pricePerDay} &times; ${days} days</span>
            <span>$${subtotal}</span>
        </div>
        <div class="d-flex justify-content-between mb-3">
            <span class="text-secondary">Service fee</span>
            <span>$${serviceFee}</span>
        </div>
        <div class="d-flex justify-content-between align-items-baseline mb-4">
            <span class="fw-semibold">Total</span>
            <span class="detail-total-amount fw-bold fs-4">$${total}</span>
        </div>
    </div>

    <button type="submit" class="btn btn-lg btn-primary w-100 py-3 rounded-3 mb-2" id="detailReservationSubmitBtn">Start reservation</button>
    </form>

    <p class="text-center text-secondary small mb-0 text-uppercase detail-reservation-disclaimer">
        You won&apos;t be charged yet
    </p>
</div>

<script>
(function () {
    var P = window.PawDateTimePair;
    if (!P) return;
    var fh = document.getElementById('detail_from_hidden');
    var fd = document.getElementById('detail_from_d');
    var ft = document.getElementById('detail_from_t');
    var uh = document.getElementById('detail_until_hidden');
    var ud = document.getElementById('detail_until_d');
    var ut = document.getElementById('detail_until_t');
    var pFrom = P.bindPair(fh, fd, ft);
    var pUntil = P.bindPair(uh, ud, ut);
    var form = document.getElementById('detailReservationForm');
    if (form) {
        form.addEventListener('submit', function () {
            if (pFrom) pFrom.syncToHidden();
            if (pUntil) pUntil.syncToHidden();
        });
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
        }
        sel.addEventListener('change', applyWindow);
        applyWindow();
    }
    </c:if>
})();
</script>
