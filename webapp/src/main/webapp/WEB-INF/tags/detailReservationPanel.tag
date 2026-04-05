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
            <c:when test="${periodCount > 1}">Times use the listing's timezone (Argentina).</c:when>
            <c:when test="${periodCount == 1}">Must fall inside the owner's published window (Argentina time).</c:when>
            <c:otherwise>Choose when you need the vehicle.</c:otherwise>
        </c:choose>
    </p>
    <div class="mb-3">
        <label class="form-label small mb-2" for="detail_daterange">Pickup and Return Dates</label>
        <input
            type="text"
            id="detail_daterange"
            class="form-control"
            placeholder="Select pickup and return dates"
            readonly
            aria-label="Select pickup and return date range">
        <input type="hidden" name="fromDateTime" id="detail_from_hidden"/>
        <input type="hidden" name="untilDateTime" id="detail_until_hidden"/>
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
        You won't be charged yet
    </p>
</div>
