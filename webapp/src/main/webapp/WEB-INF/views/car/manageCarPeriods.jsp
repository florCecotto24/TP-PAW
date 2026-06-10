<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="ryden-car" tagdir="/WEB-INF/tags/car" %>
<%@ taglib prefix="ryden-search" tagdir="/WEB-INF/tags/search" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
<head>
    <title><spring:message code="manageCarPeriods.pageTitle"/></title>
    <%@include file="../header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="navbar.myCars" var="myListingsLabel"/>
    <c:set var="carLabel"><c:out value="${car.brand} ${car.model}"/></c:set>
    <c:url var="carDetailUrl" value="/my-cars/car/${car.id}"/>
    <spring:message code="myCarDetail.availability.managePeriodsBtn" var="managePeriodsLabel"/>
    <ryden:breadcrumbTrail
            homeLabel="${myListingsLabel}"
            homeHref="${pageContext.request.contextPath}/my-cars"
            midLabel="${carLabel}"
            midHref="${carDetailUrl}"
            currentLabel="${managePeriodsLabel}"/>

    <section class="reservation-management-header mb-4">
        <h1 class="h3 fw-bold mb-2"><spring:message code="manageCarPeriods.heading"/></h1>
        <p class="text-secondary mb-0"><spring:message code="manageCarPeriods.subheading"/></p>
    </section>

    <div class="row g-4 align-items-start">

        <%-- Left column: inline calendar showing all availability --%>
        <div class="col-lg-7">
            <div class="card border-0 shadow-sm rounded-4 bg-white">
                <div class="card-body p-4">
                    <h2 class="h6 fw-semibold text-secondary text-uppercase mb-3" style="letter-spacing:.04em;">
                        <spring:message code="manageCarPeriods.calendar.title"/>
                    </h2>
                    <div class="owner-cal-container"
                         data-bookable-ranges='<c:out value="${allSegmentsJson}" escapeXml="false"/>'
                         data-navigate-month="true"
                         data-base-url="<c:url value='/my-cars/car/${car.id}/periods'/>">
                        <input type="text" class="js-owner-cal-anchor owner-cal-anchor" aria-hidden="true" tabindex="-1" readonly
                               data-default-month="<c:out value='${activeYearMonth}'/>"/>
                    </div>
                </div>
            </div>
        </div>

        <%-- Right column: min rental days badge + period list + inline form --%>
        <div class="col-lg-5">

            <%-- Minimum rental days: display + edit (shown when car already has periods) --%>
            <c:if test="${canManage and not isFirstPeriod}">
                <div class="card border-0 shadow-sm rounded-4 bg-white mb-3">
                    <div class="card-body p-3 d-flex align-items-center justify-content-between gap-3 flex-wrap">
                        <div>
                            <span class="text-secondary small"><spring:message code="manageCarPeriods.minRentalDays.label"/>:</span>
                            <span class="fw-semibold ms-1">
                                <spring:message code="manageCarPeriods.minRentalDays.value" arguments="${car.minimumRentalDays}"/>
                            </span>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <c:if test="${not empty minRentalDaysError}">
                                <span class="text-danger small"><c:out value="${minRentalDaysError}"/></span>
                            </c:if>
                            <button type="button" class="btn btn-sm btn-outline-secondary"
                                    data-bs-toggle="modal" data-bs-target="#editMinRentalDaysModal"
                                    aria-label="<spring:message code='manageCarPeriods.minRentalDays.edit.aria'/>">
                                <i class="bi bi-pencil" aria-hidden="true"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </c:if>

            <%-- Period list card --%>
            <div class="card border-0 shadow-sm rounded-4 bg-white">
                <div class="card-body p-4">
                    <div class="d-flex align-items-center justify-content-between mb-3">
                        <h2 class="h6 fw-semibold text-secondary text-uppercase mb-0" style="letter-spacing:.04em;">
                            <spring:message code="manageCarPeriods.periods.title" arguments="${activeMonthName}"/>
                        </h2>
                        <c:if test="${canManage}">
                            <button type="button" id="inlinePeriodAddBtn" class="btn btn-outline-primary btn-sm">
                                <i class="bi bi-plus-lg" aria-hidden="true"></i>
                                <spring:message code="manageCarPeriods.periods.addPeriod"/>
                            </button>
                        </c:if>
                    </div>

                    <c:choose>
                        <c:when test="${empty monthAvailabilities}">
                            <p class="text-secondary mb-0" id="inlinePeriodEmptyMsg"><spring:message code="manageCarPeriods.periods.empty"/></p>
                        </c:when>
                        <c:otherwise>
                            <spring:eval var="uiLocale"
                                         expression="T(org.springframework.context.i18n.LocaleContextHolder).getLocale()"/>
                            <div class="d-flex flex-column gap-2" id="inlinePeriodList">
                                <c:forEach var="availability" items="${monthAvailabilities}">
                                    <spring:eval var="periodFromDisplay"
                                                 expression="T(ar.edu.itba.paw.models.util.time.WallDateTimeDisplayFormat).formatWallDate(availability.startInclusive, uiLocale)"/>
                                    <spring:eval var="periodToDisplay"
                                                 expression="T(ar.edu.itba.paw.models.util.time.WallDateTimeDisplayFormat).formatWallDate(availability.endInclusive, uiLocale)"/>
                                    <c:set var="reservedRangesJson" value="${empty reservedRangesByAvailabilityIdJson[availability.id] ? '[]' : reservedRangesByAvailabilityIdJson[availability.id]}"/>
                                    <div class="manage-period-card p-3 border rounded-3 bg-white d-flex align-items-center justify-content-between gap-2"
                                         data-period-id="<c:out value='${availability.id}'/>"
                                         data-period-from="<c:out value='${availability.startInclusive}'/>"
                                         data-period-to="<c:out value='${availability.endInclusive}'/>"
                                         data-period-price="<c:out value='${availability.dayPriceValue}'/>"
                                         data-period-neighborhood-id="<c:if test='${availability.neighborhoodId.present}'><c:out value='${availability.neighborhoodId.get()}'/></c:if>"
                                         data-period-street="<c:out value='${availability.startPointStreet}'/>"
                                         data-period-street-number="<c:if test='${availability.startPointNumber.present}'><c:out value='${availability.startPointNumber.get()}'/></c:if>"
                                         data-period-check-in="<c:out value='${availability.checkInTime}'/>"
                                         data-period-check-out="<c:out value='${availability.checkOutTime}'/>"
                                         data-period-reserved-ranges='${reservedRangesJson}'>
                                        <div class="d-flex align-items-center gap-2 min-w-0">
                                            <i class="bi bi-calendar-range text-primary flex-shrink-0" aria-hidden="true"></i>
                                            <span class="fw-medium text-truncate"><c:out value="${periodFromDisplay}"/> &ndash; <c:out value="${periodToDisplay}"/></span>
                                        </div>
                                        <div class="d-flex align-items-center gap-2 flex-shrink-0">
                                            <c:if test="${availability.dayPriceValue != null}">
                                                <fmt:setLocale value="es_AR"/>
                                                <span class="text-secondary small text-nowrap"><fmt:formatNumber value="${availability.dayPriceValue}" type="currency" currencyCode="ARS"/></span>
                                            </c:if>
                                            <c:if test="${canManage}">
                                                <button type="button" class="btn btn-sm btn-outline-secondary js-period-edit"
                                                        aria-label="<spring:message code='manageCarPeriods.periods.edit.aria'/>">
                                                    <i class="bi bi-pencil" aria-hidden="true"></i>
                                                </button>
                                            </c:if>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <%-- Inline form (Bootstrap collapse) --%>
            <c:if test="${canManage}">
                <spring:message code="manageCarPeriods.form.titleEdit" var="inlineTitleEditMsg" javaScriptEscape="true"/>
                <spring:message code="editAvailability.submit" var="inlineSubmitEditMsg" javaScriptEscape="true"/>
                <spring:message code="createCarAvailability.form.submit" var="inlineSubmitCreateMsg" javaScriptEscape="true"/>
                <div id="inlinePeriodFormSection"
                     class="collapse mt-3"
                     data-create-url="${pageContext.request.contextPath}/my-cars/car/${car.id}/create"
                     data-edit-url-prefix="${pageContext.request.contextPath}/my-cars/car/${car.id}/availability/"
                     data-edit-url-suffix="/edit"
                     data-title-edit="${inlineTitleEditMsg}"
                     data-submit-edit="${inlineSubmitEditMsg}"
                     data-first-period="${isFirstPeriod ? 'true' : 'false'}">
                    <div class="card border-0 shadow-sm rounded-4 bg-white">
                        <div class="card-body p-4">
                            <h6 id="inlinePeriodFormTitle" class="fw-semibold mb-4">
                                <spring:message code="manageCarPeriods.form.titleCreate"/>
                            </h6>

                            <spring:message code="validation.neighborhood.invalid" var="createNbInvalidMsg"/>
                            <spring:message code="validation.neighborhood.notNull" var="createNbRequiredMsg"/>
                            <spring:message code="publishCar.missingCbu.invalid" var="createMissingCbuInvalidMsg" htmlEscape="false" arguments="${cbuRequiredDigits}"/>
                            <spring:message code="publishCar.missingCbu.saveFailed" var="createMissingCbuSaveFailedMsg" htmlEscape="false"/>
                            <spring:message code="carAvailability.beyondPublishHorizon" arguments="${maxAvailabilityForwardWallDays}" var="beyondHorizonClientErrMsg" htmlEscape="true"/>

                            <form:form id="publishCarFormEl"
                                       action="${pageContext.request.contextPath}/my-cars/car/${car.id}/create"
                                       method="POST"
                                       modelAttribute="createCarAvailabilityForm"
                                       htmlEscape="true"
                                       novalidate="novalidate"
                                       data-ryden-user-has-cbu="${userHasCbu ? 'true' : 'false'}"
                                       data-ryden-context-path="${pageContext.request.contextPath}"
                                       data-ryden-quick-cbu-url="${pageContext.request.contextPath}/profile/cbu"
                                       data-ryden-cbu-invalid="${fn:escapeXml(createMissingCbuInvalidMsg)}"
                                       data-ryden-cbu-save-failed="${fn:escapeXml(createMissingCbuSaveFailedMsg)}"
                                       data-ryden-nb-invalid="${fn:escapeXml(createNbInvalidMsg)}"
                                       data-ryden-nb-required="${fn:escapeXml(createNbRequiredMsg)}">

                                <%-- Hidden month field so redirect lands on the correct month --%>
                                <input type="hidden" id="inlineFormMonth" name="month" value="<c:out value='${activeYearMonth}'/>"/>

                                <form:errors element="div" cssClass="alert alert-danger mb-3"/>

                                <%-- Price per day --%>
                                <div class="mb-3">
                                    <ryden-car:priceMarketInsightCard insight="${priceMarketInsight}"
                                                                  initialUserPrice="${createCarAvailabilityForm.pricePerDay}"
                                                                  showDefaultPriceHint="true">
                                        <form:input path="pricePerDay" id="pricePerDay" required="required" type="number"
                                                    step="${listingPricePerDayMin}" min="${listingPricePerDayMin}" max="${listingPricePerDayMax}"
                                                    data-max-int="${listingPricePerDayIntegerDigits}" data-max-frac="${listingPricePerDayFractionDigits}"
                                                    cssClass="form-control js-no-number-wheel-step js-listing-price-decimal"
                                                    cssErrorClass="form-control is-invalid js-no-number-wheel-step js-listing-price-decimal"/>
                                    </ryden-car:priceMarketInsightCard>
                                    <form:errors path="pricePerDay" cssClass="text-danger d-block"/>
                                </div>

                                <%-- Neighborhood --%>
                                <spring:message code="publishCar.form.neighborhood.placeholder" var="createNbPh"/>
                                <spring:message code="publishCar.form.neighborhood" var="createNbFieldLabel"/>
                                <spring:message code="publishCar.form.neighborhood.search" var="createNbSearchPh"/>
                                <form:hidden path="neighborhoodId" id="nb_hid_publish"/>
                                <div class="mb-3">
                                    <ryden-search:neighborhoodPicker
                                            pickerId="publish"
                                            mode="get"
                                            allowMultiple="false"
                                            springPath="neighborhoodId"
                                            selectedNeighborhoodId="${createCarAvailabilityForm.neighborhoodId}"
                                            neighborhoodList="${allNeighborhoods}"
                                            anyLabel="${createNbPh}"
                                            searchPlaceholder="${createNbSearchPh}"
                                            selectFieldLabel="${createNbFieldLabel}"
                                            toggleAriaLabel="${createNbFieldLabel}"
                                            outerLabel="${createNbFieldLabel}"
                                            outerLabelRequired="true"/>
                                </div>

                                <%-- Pickup address --%>
                                <div class="row g-3 mb-1">
                                    <div class="col-md-8">
                                        <label class="form-label required-label" for="create_start_point_street"><spring:message code="publishCar.form.pickupStreet"/></label>
                                        <form:input path="startPointStreet" id="create_start_point_street" required="required"
                                                    maxlength="${listingAddressStreetMaxLength}"
                                                    cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                                        <form:errors path="startPointStreet" cssClass="text-danger d-block"/>
                                    </div>
                                    <div class="col-md-4">
                                        <label class="form-label required-label" for="create_start_point_number"><spring:message code="publishCar.form.pickupStreetNumber"/></label>
                                        <form:input path="startPointNumber" id="create_start_point_number"
                                                    maxlength="${listingAddressNumberMaxLength}"
                                                    inputmode="numeric" autocomplete="off"
                                                    required="required" data-ryden-digits-only="true"
                                                    cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                                        <form:errors path="startPointNumber" cssClass="text-danger d-block"/>
                                    </div>
                                </div>
                                <p class="small text-muted mb-3"><spring:message code="publishCar.form.samePickupReturnHint"/></p>

                                <%-- Check-in / check-out times --%>
                                <div class="row g-3 mb-3">
                                    <div class="col-md-6">
                                        <label class="form-label required-label" for="checkInTime"><spring:message code="publishCar.form.checkInTime"/></label>
                                        <form:input path="checkInTime" id="checkInTime" type="time" required="required" step="60"
                                                    cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                                        <form:errors path="checkInTime" cssClass="text-danger d-block"/>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label required-label" for="checkOutTime"><spring:message code="publishCar.form.checkOutTime"/></label>
                                        <form:input path="checkOutTime" id="checkOutTime" type="time" required="required" step="60"
                                                    cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                                        <form:errors path="checkOutTime" cssClass="text-danger d-block"/>
                                    </div>
                                </div>

                                <%-- minimumRentalDays: always a hidden field.
                                     First period: value set by JS from the pre-period modal.
                                     Subsequent periods: current car value passed directly. --%>
                                <c:choose>
                                    <c:when test="${isFirstPeriod}">
                                        <input type="hidden" id="minimumRentalDays" name="minimumRentalDays" value="1"/>
                                    </c:when>
                                    <c:otherwise>
                                        <input type="hidden" id="minimumRentalDays" name="minimumRentalDays" value="${car.minimumRentalDays}"/>
                                    </c:otherwise>
                                </c:choose>

                                <%-- Availability rows --%>
                                <spring:message code="publishCar.form.period" var="periodLabel"/>
                                <spring:message code="publishCar.form.dateRange.placeholder" var="dateRangePlaceholder"/>
                                <spring:message code="carAvailability.beyondPublishHorizon" arguments="${maxAvailabilityForwardWallDays}" var="beyondHorizonMsg"/>
                                <spring:message code="carAvailability.required" var="availRequiredClientMsg"/>
                                <spring:message code="manageCarPeriods.editPicker.reservedShrink" var="reservedShrinkMsg"/>
                                <spring:message code="manageCarPeriods.editPicker.reservedDayTitle" var="reservedDayTitleMsg"/>
                                <div class="mb-4" id="publishAvailabilitySection"
                                     data-publish-avail-required="<c:out value='${availRequiredClientMsg}'/>"
                                     data-publish-min-avail-ymd="<c:out value='${publishMinAvailabilityFrom}'/>"
                                     data-publish-max-avail-wall-ymd="<c:out value='${publishMaxAvailabilityWallInclusive}'/>"
                                     data-publish-availability-beyond-msg="<c:out value='${beyondHorizonMsg}'/>"
                                     data-publish-reserved-shrink-msg="<c:out value='${reservedShrinkMsg}'/>"
                                     data-publish-reserved-day-title="<c:out value='${reservedDayTitleMsg}'/>"
                                     data-publish-blocked-ranges='<c:out value="${reservationBlockedRangesJson}"/>'>
                                    <label class="form-label required-label"><spring:message code="publishCar.form.availability"/></label>
                                    <p class="small text-muted mb-2"><spring:message code="publishCar.form.availability.hint" arguments="${pickupLeadHours}"/></p>
                                    <p class="small text-muted mb-2"><spring:message code="publishCar.form.availability.forwardHorizonHint" arguments="${maxAvailabilityForwardWallDays}"/></p>
                                    <p class="small text-muted mb-2"><spring:message code="publishCar.form.availability.reservedDaysHint"/></p>
                                    <p class="small text-muted mb-2"><spring:message code="publishCar.form.availability.editDoesNotAffectReservations"/></p>
                                    <form:errors path="availabilityRows" cssClass="text-danger d-block mb-2"/>
                                    <div id="publish_availability_rows">
                                        <c:forEach items="${createCarAvailabilityForm.availabilityRows}" var="row" varStatus="st">
                                            <div class="publish-avail-row border rounded-3 p-3 mb-2" data-publish-avail-row>
                                                <span class="small text-secondary d-block mb-2"><c:out value="${periodLabel}"/></span>
                                                <input type="text" class="form-control form-control-sm ryden-avail-range-input" readonly placeholder="<c:out value='${dateRangePlaceholder}'/>" aria-label="Availability date range"/>
                                                <form:hidden path="availabilityRows[${st.index}].from" cssClass="ryden-avail-from"/>
                                                <form:hidden path="availabilityRows[${st.index}].until" cssClass="ryden-avail-until"/>
                                                <form:errors path="availabilityRows[${st.index}].from" cssClass="text-danger d-block"/>
                                                <form:errors path="availabilityRows[${st.index}].until" cssClass="text-danger d-block"/>
                                            </div>
                                        </c:forEach>
                                    </div>
                                    <div id="publishClientAvailError" class="text-danger small d-none mb-1" role="alert"></div>
                                    <button type="button" id="publish_avail_add" hidden style="display:none"></button>
                                </div>

                                <template id="publish_avail_row_template">
                                    <div class="publish-avail-row border rounded-3 p-3 mb-2" data-publish-avail-row>
                                        <input type="text" class="form-control form-control-sm ryden-avail-range-input" readonly placeholder="<c:out value='${dateRangePlaceholder}'/>" aria-label="Availability date range"/>
                                        <input type="hidden" class="ryden-avail-from" name="availabilityRows[__IDX__].from" value=""/>
                                        <input type="hidden" class="ryden-avail-until" name="availabilityRows[__IDX__].until" value=""/>
                                    </div>
                                </template>

                                <%-- Submit + cancel buttons --%>
                                <div class="d-flex justify-content-end gap-2 mt-4">
                                    <button type="button" id="inlinePeriodFormCancelBtn" class="btn btn-outline-secondary">
                                        <spring:message code="manageCarPeriods.form.cancel"/>
                                    </button>
                                    <button type="submit" class="btn btn-primary" id="createListingSubmitBtn">
                                        <i class="bi bi-check-lg me-1" aria-hidden="true"></i>
                                        <span id="inlineFormSubmitLabel"><spring:message code="createCarAvailability.form.submit"/></span>
                                    </button>
                                </div>

                            </form:form>
                        </div>
                    </div>
                </div>

                <%-- CBU modal (used when user doesn't have a valid CBU and tries to create) --%>
                <c:if test="${not userHasCbu}">
                    <spring:message code="publishCar.missingCbu.modalTitle" var="cbuMissingTitle"/>
                    <spring:message code="publishCar.missingCbu.fieldLabel" var="cbuMissingFieldLabel" arguments="${cbuRequiredDigits}"/>
                    <spring:message code="publishCar.missingCbu.save" var="cbuMissingSave"/>
                    <spring:message code="publishCar.missingCbu.cancel" var="cbuMissingCancel"/>
                    <ryden:dataPromptModal
                            id="publishMissingCbuModal"
                            title="${cbuMissingTitle}"
                            fieldId="publishMissingCbuInput"
                            fieldLabel="${cbuMissingFieldLabel}"
                            errorId="publishMissingCbuError"
                            confirmId="publishMissingCbuSaveBtn"
                            openButtonId="rydenPublishMissingCbuModalOpen"
                            includeOpenTrigger="true"
                            inputType="text"
                            maxlength="${cbuRequiredDigits}"
                            inputmode="numeric"
                            inputPattern="[0-9]*"
                            digitsOnly="true"
                            cancelLabel="${cbuMissingCancel}"
                            confirmLabel="${cbuMissingSave}">
                        <p class="mb-3 text-secondary"><spring:message code="publishCar.missingCbu.modalBody" arguments="${cbuRequiredDigits}"/></p>
                    </ryden:dataPromptModal>
                </c:if>

                <%-- Modal: ask minimum rental days before the FIRST period --%>
                <c:if test="${isFirstPeriod}">
                <div class="modal fade" id="inlineMinRentalDaysModal" tabindex="-1" aria-modal="true" role="dialog">
                    <div class="modal-dialog modal-dialog-centered modal-sm">
                        <div class="modal-content rounded-4">
                            <div class="modal-header border-0 pb-0">
                                <h5 class="modal-title fw-semibold fs-6">
                                    <spring:message code="manageCarPeriods.minRentalDays.modal.title"/>
                                </h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                            <div class="modal-body">
                                <p class="text-secondary small mb-3">
                                    <spring:message code="manageCarPeriods.minRentalDays.modal.body"/>
                                </p>
                                <label class="form-label fw-medium" for="minRentalDaysModalInput">
                                    <spring:message code="car.minRentalDays.label"/>
                                </label>
                                <input type="number" id="minRentalDaysModalInput"
                                       class="form-control" style="max-width:10rem;"
                                       min="1" max="365" value="1"/>
                                <p class="form-text text-muted small mt-1">
                                    <spring:message code="car.minRentalDays.help"/>
                                </p>
                            </div>
                            <div class="modal-footer border-0 pt-0">
                                <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">
                                    <spring:message code="manageCarPeriods.minRentalDays.modal.cancel"/>
                                </button>
                                <button type="button" class="btn btn-primary btn-sm"
                                        onclick="window.RydenManagePeriods && window.RydenManagePeriods.confirmMinRentalDays()">
                                    <spring:message code="manageCarPeriods.minRentalDays.modal.confirm"/>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
                </c:if>

            </c:if><%-- end canManage --%>

            <%-- Modal: edit minimum rental days (shown when car already has periods) --%>
            <c:if test="${canManage and not isFirstPeriod}">
                <c:url var="minRentalDaysUpdateUrl" value="/my-cars/car/${car.id}/min-rental-days"/>
                <div class="modal fade" id="editMinRentalDaysModal" tabindex="-1" aria-modal="true" role="dialog">
                    <div class="modal-dialog modal-dialog-centered modal-sm">
                        <div class="modal-content rounded-4">
                            <form method="POST" action="<c:out value='${minRentalDaysUpdateUrl}'/>">
                                <div class="modal-header border-0 pb-0">
                                    <h5 class="modal-title fw-semibold fs-6">
                                        <spring:message code="manageCarPeriods.minRentalDays.modal.editTitle"/>
                                    </h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                </div>
                                <div class="modal-body">
                                    <label class="form-label fw-medium" for="editMinRentalDaysInput">
                                        <spring:message code="car.minRentalDays.label"/>
                                    </label>
                                    <input type="number" id="editMinRentalDaysInput" name="minimumRentalDays"
                                           class="form-control" style="max-width:10rem;"
                                           min="1" max="365" value="${car.minimumRentalDays}" required/>
                                    <p class="form-text text-muted small mt-1">
                                        <spring:message code="car.minRentalDays.help"/>
                                    </p>
                                    <input type="hidden" name="month" value="<c:out value='${activeYearMonth}'/>"/>
                                    <%@include file="../includes/csrfHidden.jspf"%>
                                </div>
                                <div class="modal-footer border-0 pt-0">
                                    <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">
                                        <spring:message code="manageCarPeriods.form.cancel"/>
                                    </button>
                                    <button type="submit" class="btn btn-primary btn-sm">
                                        <spring:message code="manageCarPeriods.minRentalDays.modal.save"/>
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </c:if>

        </div><%-- end right column --%>

    </div>
</main>

<script>window.rydenPublishAvailMinFromUrl = '<c:url value="/my-cars/car/${car.id}/availability-min-from"/>';</script>

<script src="${pageContext.request.contextPath}/js/managePeriods.js"></script>

<%-- Auto-open inline form on error re-renders --%>
<c:if test="${not empty inlineFormOpen}">
<script>
document.addEventListener('DOMContentLoaded', function() {
    if (window.RydenManagePeriods) {
        window.RydenManagePeriods.openOnLoad('<c:out value="${inlineFormOpen}"/>');
    }
});
</script>
</c:if>

<%@include file="../includes/footerScripts.jspf"%>
</body>
</html>
