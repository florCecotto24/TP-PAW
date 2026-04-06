<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="listingId" required="true" type="java.lang.Long" %>
<%@ attribute name="dailyPrice" required="true" type="java.lang.String" %>
<%@ attribute name="deliveryLocation" required="true" type="java.lang.String" %>
<%@ attribute name="fromDateTimeValue" required="false" type="java.lang.String" %>
<%@ attribute name="untilDateTimeValue" required="false" type="java.lang.String" %>
<%@ attribute name="carName" required="true" type="java.lang.String" %>
<%@ attribute name="bookableWallRangesJson" required="true" type="java.lang.String" %>
<%@ attribute name="pickupTime" required="false" type="java.lang.String" %>
<%@ attribute name="returnTime" required="false" type="java.lang.String" %>

<c:url var="newReservationUrl" value="/reservation/new" />

<div class="detail-reservation-panel border rounded-4 p-4 bg-white shadow-sm">
    <form action="<c:out value='${newReservationUrl}'/>" method="get" id="detailReservationForm"
          data-bookable-ranges='<c:out value="${bookableWallRangesJson}" escapeXml="false"/>'
          data-pickup-time="<c:out value='${pickupTime}'/>"
          data-return-time="<c:out value='${returnTime}'/>">
    <input type="hidden" name="listingId" value="<c:out value='${listingId}'/>" />
    <input type="hidden" name="carName" value="<c:out value='${carName}'/>" />
    <input type="hidden" name="reservationTotal" id="detail_reservation_total_hint" value="" />

    <div class="d-flex align-items-baseline gap-2 mb-3">
        <span class="detail-price-amount fw-bold">$<c:out value="${dailyPrice}"/></span>
        <span class="text-secondary">/ per day</span>
    </div>

    <h2 class="h6 fw-semibold mb-2">Pickup and return</h2>
    <p class="form-text small mb-2">
        <span class="text-muted">Scheduled for this listing (Argentina)</span><br/>
        <strong>Pick up at:</strong>
        <c:out value="${not empty pickupTime ? pickupTime : '—'}"/>
        <span class="text-muted mx-1">·</span>
        <strong>Return by:</strong>
        <c:out value="${not empty returnTime ? returnTime : '—'}"/>
    </p>
    <div class="mb-3">
        <label class="form-label small mb-2" for="detail_daterange">Pickup and return dates</label>
        <input
            type="text"
            id="detail_daterange"
            class="form-control"
            placeholder="Select pickup day and return day"
            readonly
            aria-label="Select pickup and return calendar days">
        <input type="hidden" name="fromDateTime" id="detail_from_hidden" value="<c:out value='${fromDateTimeValue}'/>"/>
        <input type="hidden" name="untilDateTime" id="detail_until_hidden" value="<c:out value='${untilDateTimeValue}'/>"/>
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
