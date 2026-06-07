<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<head>
    <%@include file="../header.jsp" %>
</head>
<body class="has-fixed-navbar">
<ryden:navbar/>

<main class="container py-5">
    <spring:message code="createCarAvailability.form.title" var="createListingTitle"/>
    <spring:message code="navbar.myCars" var="myListingsLabel"/>
    <ryden:breadcrumbTrail
            homeLabel="${myListingsLabel}"
            homeHref="${pageContext.request.contextPath}/my-cars"
            currentLabel="${createListingTitle}"/>
    <div class="row justify-content-center">
        <div class="col-md-9 col-lg-7">

            <%-- Car summary card --%>
            <div class="card border-0 shadow-sm rounded-4 bg-white mb-4">
                <div class="card-body p-4">
                    <h5 class="fw-semibold mb-3"><spring:message code="publishCar.confirmation.details"/></h5>
                    <div class="d-flex flex-wrap gap-3 align-items-center">
                        <div>
                            <p class="mb-1"><strong><c:out value="${car.brand}"/> <c:out value="${car.model}"/></strong></p>
                            <spring:message code="enum.car.type.${car.type.name()}" var="carTypeLbl"/>
                            <spring:message code="enum.car.transmission.${car.transmission.name()}" var="carTransLbl"/>
                            <p class="mb-0 text-secondary small"><c:out value="${carTypeLbl}"/> &bull; <c:out value="${carTransLbl}"/> &bull; <c:out value="${car.plate}"/></p>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card border-0 shadow-sm rounded-4 bg-white">
                <div class="card-body p-4 p-md-5">
                    <h4 class="fw-semibold mb-4"><c:out value="${createListingTitle}"/></h4>

                    <spring:message code="validation.neighborhood.invalid" var="createNbInvalidMsg" htmlEscape="true"/>
                    <spring:message code="validation.neighborhood.notNull" var="createNbRequiredMsg" htmlEscape="true"/>
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

                        <form:errors element="div" cssClass="alert alert-danger mb-3"/>

                        <%-- Price per day --%>
                        <div class="mb-3">
                            <ryden:priceMarketInsightCard insight="${priceMarketInsight}"
                                                          initialUserPrice="${createCarAvailabilityForm.pricePerDay}"
                                                          showDefaultPriceHint="true">
                                <form:input path="pricePerDay" id="pricePerDay" required="required" type="number"
                                            step="${listingPricePerDayMin}" min="${listingPricePerDayMin}" max="${listingPricePerDayMax}"
                                            data-max-int="${listingPricePerDayIntegerDigits}" data-max-frac="${listingPricePerDayFractionDigits}"
                                            cssClass="form-control js-no-number-wheel-step js-listing-price-decimal"
                                            cssErrorClass="form-control is-invalid js-no-number-wheel-step js-listing-price-decimal"/>
                            </ryden:priceMarketInsightCard>
                            <form:errors path="pricePerDay" cssClass="text-danger d-block"/>
                        </div>

                        <%-- Neighborhood --%>
                        <spring:message code="publishCar.form.neighborhood.placeholder" var="createNbPh"/>
                        <spring:message code="publishCar.form.neighborhood" var="createNbFieldLabel"/>
                        <spring:message code="publishCar.form.neighborhood.search" var="createNbSearchPh"/>
                        <form:hidden path="neighborhoodId" id="nb_hid_publish"/>
                        <div class="mb-3">
                            <ryden:neighborhoodPicker
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

                        <%-- Minimum rental days --%>
                        <div class="mb-3">
                            <label class="form-label required-label" for="minimumRentalDays">
                                <spring:message code="car.minRentalDays.label"/>
                            </label>
                            <form:input path="minimumRentalDays" id="minimumRentalDays" type="number" min="1" max="365"
                                        cssClass="form-control" cssErrorClass="form-control is-invalid"
                                        style="max-width: 10rem;"/>
                            <p class="form-text text-muted small"><spring:message code="car.minRentalDays.help"/></p>
                            <form:errors path="minimumRentalDays" cssClass="text-danger d-block"/>
                        </div>

                        <%-- Availability row --%>
                        <spring:message code="publishCar.form.period" var="periodLabel"/>
                        <spring:message code="publishCar.form.dateRange.placeholder" var="dateRangePlaceholder"/>
                        <spring:message code="availability.dateRange.aria" var="availabilityDateRangeAria" htmlEscape="true"/>
                        <spring:message code="createCarAvailability.availabilityRow.dayPrice" var="dayPriceLabel" htmlEscape="true"/>
                        <spring:message code="createCarAvailability.availabilityRow.dayPrice.placeholder" var="dayPricePh" htmlEscape="true"/>
                        <spring:message code="carAvailability.beyondPublishHorizon" arguments="${maxAvailabilityForwardWallDays}" var="beyondHorizonMsg" htmlEscape="true"/>
                        <spring:message code="carAvailability.required" var="availRequiredClientMsg" htmlEscape="true"/>
                        <div class="mb-4" id="publishAvailabilitySection"
                             data-publish-avail-required="<c:out value='${availRequiredClientMsg}'/>"
                             data-publish-min-avail-ymd="<c:out value='${publishMinAvailabilityFrom}'/>"
                             data-publish-max-avail-wall-ymd="<c:out value='${publishMaxAvailabilityWallInclusive}'/>"
                             data-publish-availability-beyond-msg="<c:out value='${beyondHorizonMsg}'/>">
                            <label class="form-label required-label"><spring:message code="publishCar.form.availability"/></label>
                            <p class="small text-muted mb-2"><spring:message code="publishCar.form.availability.hint" arguments="${pickupLeadHours}"/></p>
                            <p class="small text-muted mb-2"><spring:message code="publishCar.form.availability.forwardHorizonHint" arguments="${maxAvailabilityForwardWallDays}"/></p>
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
                            <%-- Required by components.js guard; hidden because add/remove is not available --%>
                            <button type="button" id="publish_avail_add" hidden style="display:none"></button>
                        </div>

                        <template id="publish_avail_row_template">
                            <div class="publish-avail-row border rounded-3 p-3 mb-2" data-publish-avail-row>
                                <input type="text" class="form-control form-control-sm ryden-avail-range-input" readonly placeholder="<c:out value='${dateRangePlaceholder}'/>" aria-label="Availability date range"/>
                                <input type="hidden" class="ryden-avail-from" name="availabilityRows[__IDX__].from" value=""/>
                                <input type="hidden" class="ryden-avail-until" name="availabilityRows[__IDX__].until" value=""/>
                            </div>
                        </template>

                        <div class="d-flex justify-content-end mt-4">
                            <button type="submit" class="btn btn-primary" id="createListingSubmitBtn">
                                <i class="bi bi-check-lg me-1"></i> <spring:message code="createCarAvailability.form.submit"/>
                            </button>
                        </div>

                    </form:form>
                </div>
            </div>

        </div>
    </div>
</main>

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

<script>window.rydenPublishAvailMinFromUrl = '<c:url value="/my-cars/car/${car.id}/availability-min-from"/>';</script>
<%@include file="../footer.jsp" %>
</body>
</html>
