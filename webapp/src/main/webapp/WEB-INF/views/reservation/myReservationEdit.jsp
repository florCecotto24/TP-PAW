<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
<head>
    <title><spring:message code="myReservationEdit.pageTitle" arguments="${reservation.id}"/></title>
    <%@include file="../header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="navbar.myReservations" var="myReservationsLabel"/>
    <spring:message code="myReservationEdit.heading" var="editHeadingLabel"/>
    <c:url var="detailHref" value="/my-reservations/${reservation.id}"/>
    <ryden:breadcrumbTrail
            homeLabel="${myReservationsLabel}"
            homeHref="${pageContext.request.contextPath}/my-reservations"
            midLabel="${car.brand} ${car.model}"
            midHref="${detailHref}"
            currentLabel="${editHeadingLabel}"/>

    <c:if test="${not empty reservationEditError}">
        <div class="alert alert-danger" role="alert"><c:out value="${reservationEditError}"/></div>
    </c:if>

    <section class="reservation-management-header mb-4">
        <h1 class="h3 fw-bold mb-2"><spring:message code="myReservationEdit.heading"/></h1>
        <p class="text-secondary mb-0"><spring:message code="myReservationEdit.subheading"/></p>
    </section>

    <div class="row g-4 align-items-start">
        <div class="col-lg-8">
            <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                <div class="card-body p-4">
                    <h2 class="h5 fw-semibold mb-3"><spring:message code="myReservationDetail.carSummary.title"/></h2>
                    <div class="d-flex flex-column flex-md-row gap-3 align-items-start">
                        <div class="reservation-detail-car-media rounded-3 overflow-hidden border">
                            <c:choose>
                                <c:when test="${carImageId > 0}">
                                    <c:url var="carImageUrl" value="/image/${carImageId}"/>
                                    <img src="<c:out value='${carImageUrl}'/>" alt="<c:out value='${car.brand} ${car.model}'/>" class="w-100 h-100" style="object-fit:cover;">
                                </c:when>
                                <c:otherwise>
                                    <div class="w-100 h-100 d-flex align-items-center justify-content-center text-secondary bg-body-tertiary">
                                        <i class="bi bi-car-front fs-1" aria-hidden="true"></i>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div class="min-w-0">
                            <h3 class="h5 mb-1 ryden-text-break"><c:out value="${car.brand} ${car.model}"/></h3>
                            <div class="d-flex flex-wrap gap-2">
                                <spring:message code="enum.car.transmission.${car.transmission.name()}" var="carTransmissionLabel"/>
                                <spring:message code="enum.car.powertrain.${car.powertrain.name()}" var="carPowertrainLabel"/>
                                <span class="badge text-bg-light border"><c:out value="${carTransmissionLabel}"/></span>
                                <span class="badge text-bg-light border"><c:out value="${carPowertrainLabel}"/></span>
                            </div>
                        </div>
                    </div>
                </div>
            </article>

            <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                <div class="card-body p-4">
                    <h2 class="h5 fw-semibold mb-3"><spring:message code="myReservationEdit.currentPeriod.title"/></h2>
                    <div class="row g-3">
                        <div class="col-sm-6">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationEdit.currentPeriod.pickup"/></p>
                            <p class="mb-0 fw-medium"><c:out value="${currentPickupDateTimeDisplay}"/></p>
                        </div>
                        <div class="col-sm-6">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationEdit.currentPeriod.return"/></p>
                            <p class="mb-0 fw-medium"><c:out value="${currentReturnDateTimeDisplay}"/></p>
                        </div>
                        <div class="col-12">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationEdit.currentPeriod.total"/></p>
                            <p class="mb-0 fw-medium"><c:out value="${currentTotalPriceDisplay}"/></p>
                        </div>
                    </div>
                </div>
            </article>
        </div>

        <div class="col-lg-4">
            <article class="card border-0 shadow-sm rounded-4 reservation-detail-sticky bg-white">
                <div class="card-body p-4">
                    <h2 class="h5 fw-semibold mb-3"><spring:message code="myReservationEdit.newPeriod.title"/></h2>

                    <c:url var="editPostUrl" value="/my-reservations/${reservation.id}/edit"/>
                    <spring:message code="validation.reservationForm.maxBillableDays" arguments="${maxReservationBillableDays}" var="maxBillableExceededMsg" htmlEscape="true"/>
                    <c:if test="${not empty minimumRentalDays and minimumRentalDays > 1}">
                        <spring:message code="validation.reservationForm.minRentalDays" arguments="${minimumRentalDays}" var="minRentalDaysMsg" htmlEscape="true"/>
                    </c:if>

                    <%-- Reuses /js/detailReservationForm.js by mirroring its element IDs:
                         detailReservationForm, detail_daterange, detail_from_hidden, detail_until_hidden,
                         detail_pickup_time_label, detail_return_time_label, detail_pickup_location_label,
                         detail_return_location_label, detail_pricing_summary, detail_total_amount,
                         detail_date_alert, detail_max_billable_alert, detail_min_rental_days_alert,
                         detailReservationSubmitBtn.
                         The detail-reservation-panel class is required so the per-day price labels
                         (.fp-day-price) and the inline calendar styling kick in (see components.css). --%>
                    <form action="<c:out value='${editPostUrl}'/>" method="post" id="detailReservationForm"
                          class="detail-reservation-panel"
                          data-bookable-ranges='<c:out value="${bookableWallRangesJson}" escapeXml="false"/>'
                          data-max-billable-days="<c:out value='${maxReservationBillableDays}'/>"
                          data-max-billable-exceeded-msg="<c:out value='${maxBillableExceededMsg}'/>"
                          <c:if test="${not empty minimumRentalDays and minimumRentalDays > 1}">data-min-rental-days="<c:out value='${minimumRentalDays}'/>" data-min-rental-days-msg="<c:out value='${minRentalDaysMsg}'/>"</c:if>
                          data-search-nb-ids='[]'>
                        <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                        <input type="hidden" name="reservationTotal" id="detail_reservation_total_hint" value=""/>

                        <c:if test="${not empty minimumRentalDays and minimumRentalDays > 1}">
                            <spring:message code="carDetail.minRentalDays" arguments="${minimumRentalDays}" var="minRentalDaysBadge"/>
                            <p class="small text-secondary mb-3">
                                <i class="bi bi-calendar-check me-1" aria-hidden="true"></i>
                                <c:out value="${minRentalDaysBadge}"/>
                            </p>
                        </c:if>

                        <div class="mb-3">
                            <label class="form-label small mb-1"><spring:message code="detailReservationPanel.pickupReturnDates"/></label>
                            <spring:message code="detailReservationPanel.maxBillableDays.hint" arguments="${maxReservationBillableDays}" var="maxBillableHintLine"/>
                            <p class="form-text small text-muted mb-2"><c:out value="${maxBillableHintLine}"/></p>
                            <spring:message code="detailReservationPanel.dates.ariaLabel" var="datesAriaLabel"/>
                            <input type="text" id="detail_daterange" class="detail-daterange-anchor"
                                   readonly aria-hidden="true" tabindex="-1"
                                   aria-label="<c:out value='${datesAriaLabel}'/>"/>
                            <input type="hidden" name="fromDateTime" id="detail_from_hidden" value="<c:out value='${currentFromDateTimeWall}'/>"/>
                            <input type="hidden" name="untilDateTime" id="detail_until_hidden" value="<c:out value='${currentUntilDateTimeWall}'/>"/>
                        </div>

                        <h2 class="h6 fw-semibold mb-2"><spring:message code="detailReservationPanel.pickupReturn"/></h2>
                        <p class="form-text small mb-2">
                            <strong><spring:message code="detailReservationPanel.pickupAt"/></strong>
                            <span id="detail_pickup_time_label">&mdash;</span>
                            <span class="text-muted mx-1">&middot;</span>
                            <strong><spring:message code="detailReservationPanel.returnBy"/></strong>
                            <span id="detail_return_time_label">&mdash;</span>
                        </p>

                        <div class="d-flex align-items-start gap-2 mb-2">
                            <i class="bi bi-geo-alt text-secondary mt-1" aria-hidden="true"></i>
                            <div class="flex-grow-1 w-100">
                                <small class="text-muted d-block mb-1"><spring:message code="detailReservationPanel.pickupLocation"/></small>
                                <p class="mb-0 fw-medium" id="detail_pickup_location_label">&mdash;</p>
                            </div>
                        </div>

                        <div class="d-flex align-items-start gap-2 mb-3">
                            <i class="bi bi-geo-alt-fill text-secondary mt-1" aria-hidden="true"></i>
                            <div class="flex-grow-1 w-100">
                                <small class="text-muted d-block mb-1"><spring:message code="detailReservationPanel.returnLocation"/></small>
                                <p class="mb-0 fw-medium" id="detail_return_location_label">&mdash;</p>
                            </div>
                        </div>

                        <div class="border-top pt-3 small" id="detail_pricing_summary" hidden>
                            <div class="d-flex justify-content-between align-items-baseline mb-4">
                                <span class="fw-semibold"><spring:message code="myReservationEdit.newPeriod.total"/></span>
                                <span class="detail-total-amount fw-bold fs-4" id="detail_total_amount"></span>
                            </div>
                        </div>

                        <c:if test="${!hasBookableDays}">
                            <div class="alert alert-warning mb-3" role="alert">
                                <spring:message code="detailReservationPanel.dateAlert"/>
                            </div>
                        </c:if>

                        <div class="alert alert-danger mb-3" id="detail_date_alert" role="alert" hidden>
                            <spring:message code="detailReservationPanel.dateAlert"/>
                        </div>

                        <div class="alert alert-danger mb-3" id="detail_max_billable_alert" role="alert" hidden>
                            <c:out value="${maxBillableExceededMsg}"/>
                        </div>

                        <div class="alert alert-warning mb-3" id="detail_min_rental_days_alert" role="alert" hidden></div>

                        <button type="submit" class="btn btn-primary w-100 py-2 rounded-3 mb-2" id="detailReservationSubmitBtn">
                            <i class="bi bi-check2-circle me-2"></i><spring:message code="myReservationEdit.submit"/>
                        </button>

                        <a href="<c:out value='${detailHref}'/>" class="btn btn-outline-secondary w-100">
                            <spring:message code="myReservationEdit.cancel"/>
                        </a>
                    </form>
                </div>
            </article>
        </div>
    </div>
</main>

<%@include file="../footer.jsp"%>
<script src="<c:url value='/js/detailReservationForm.js'/>"></script>
</body>
</html>
