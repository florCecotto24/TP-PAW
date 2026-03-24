<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ attribute name="pricePerDay" required="true" type="java.lang.String" %>
<%@ attribute name="days" required="true" type="java.lang.String" %>
<%@ attribute name="subtotal" required="true" type="java.lang.String" %>
<%@ attribute name="serviceFee" required="true" type="java.lang.String" %>
<%@ attribute name="total" required="true" type="java.lang.String" %>
<%@ attribute name="deliveryLocation" required="true" type="java.lang.String" %>
<%@ attribute name="fromDateValue" required="false" type="java.lang.String" %>
<%@ attribute name="toDateValue" required="false" type="java.lang.String" %>

<c:if test="${empty fromDateValue}">
    <c:set var="fromDateValue" value="2024-05-12" />
</c:if>
<c:if test="${empty toDateValue}">
    <c:set var="toDateValue" value="2024-05-15" />
</c:if>

<div class="detail-booking-panel border rounded-4 p-4 bg-white shadow-sm">
    <div class="d-flex align-items-baseline gap-2 mb-3">
        <span class="detail-price-amount fw-bold">$${pricePerDay}</span>
        <span class="text-secondary">/ per day</span>
    </div>

    <div class="d-flex flex-column gap-2 mb-3">
        <div class="row g-2">
            <div class="col-6">
                <div class="form-floating">
                    <input type="date" class="form-control" id="detail_from_date" name="fromDate" value="${fromDateValue}" aria-label="From">
                    <label for="detail_from_date">From</label>
                </div>
            </div>
            <div class="col-6">
                <div class="form-floating">
                    <input type="date" class="form-control" id="detail_until_date" name="untilDate" value="${toDateValue}" aria-label="Until">
                    <label for="detail_until_date">Until</label>
                </div>
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

    <paw:button text="Reserve now" size="lg" type="primary" cssClass="btn-primary w-100 py-3 rounded-3 mb-2" id="detailReserveBtn" />

    <p class="text-center text-secondary small mb-0 text-uppercase detail-booking-disclaimer">
        You won&apos;t be charged yet
    </p>
</div>
