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
    <spring:message code="editAvailability.heading" var="editAvailabilityTitle"/>
    <spring:message code="navbar.myCars" var="myListingsLabel"/>
    <c:set var="carLabel"><c:out value="${car.brand} ${car.model}"/></c:set>
    <c:url var="carDetailUrl" value="/my-cars/car/${car.id}"/>
    <ryden:breadcrumbTrail
            homeLabel="${myListingsLabel}"
            homeHref="${pageContext.request.contextPath}/my-cars"
            midLabel="${carLabel}"
            midHref="${carDetailUrl}"
            currentLabel="${editAvailabilityTitle}"/>
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
                    <h4 class="fw-semibold mb-4"><c:out value="${editAvailabilityTitle}"/></h4>

                    <spring:message code="validation.neighborhood.invalid" var="editAvNbInvalidMsg" htmlEscape="true"/>
                    <spring:message code="validation.neighborhood.notNull" var="editAvNbRequiredMsg" htmlEscape="true"/>
                    <spring:message code="carAvailability.beyondPublishHorizon" arguments="${maxAvailabilityForwardWallDays}" var="beyondHorizonClientErrMsg" htmlEscape="true"/>

                    <form:form id="editAvailabilityFormEl"
                               action="${pageContext.request.contextPath}/my-cars/car/${car.id}/availability/${availabilityId}/edit"
                               method="POST"
                               modelAttribute="createCarAvailabilityForm"
                               htmlEscape="true"
                               novalidate="novalidate"
                               data-ryden-nb-invalid="${fn:escapeXml(editAvNbInvalidMsg)}"
                               data-ryden-nb-required="${fn:escapeXml(editAvNbRequiredMsg)}">

                        <form:errors element="div" cssClass="alert alert-danger mb-3"/>

                        <%-- Price per day --%>
                        <div class="mb-3">
                            <ryden:priceMarketInsightCard insight="${priceMarketInsight}"
                                                          initialUserPrice="${createCarAvailabilityForm.pricePerDay}"
                                                          showDefaultPriceHint="true">
                                <form:input path="pricePerDay" id="pricePerDay" required="required" type="number" step="0.01" max="99999999.99"
                                            data-max-int="8" data-max-frac="2"
                                            cssClass="form-control js-no-number-wheel-step js-listing-price-decimal"
                                            cssErrorClass="form-control is-invalid js-no-number-wheel-step js-listing-price-decimal"/>
                            </ryden:priceMarketInsightCard>
                            <form:errors path="pricePerDay" cssClass="text-danger d-block"/>
                        </div>

                        <%-- Neighborhood --%>
                        <spring:message code="publishCar.form.neighborhood.placeholder" var="editAvNbPh"/>
                        <spring:message code="publishCar.form.neighborhood" var="editAvNbFieldLabel"/>
                        <spring:message code="publishCar.form.neighborhood.search" var="editAvNbSearchPh"/>
                        <form:hidden path="neighborhoodId" id="nb_hid_editAv"/>
                        <div class="mb-3">
                            <ryden:neighborhoodPicker
                                    pickerId="editAv"
                                    mode="get"
                                    allowMultiple="false"
                                    springPath="neighborhoodId"
                                    selectedNeighborhoodId="${createCarAvailabilityForm.neighborhoodId}"
                                    neighborhoodList="${allNeighborhoods}"
                                    anyLabel="${editAvNbPh}"
                                    searchPlaceholder="${editAvNbSearchPh}"
                                    selectFieldLabel="${editAvNbFieldLabel}"
                                    toggleAriaLabel="${editAvNbFieldLabel}"
                                    outerLabel="${editAvNbFieldLabel}"
                                    outerLabelRequired="true"/>
                        </div>

                        <%-- Pickup address --%>
                        <div class="row g-3 mb-1">
                            <div class="col-md-8">
                                <label class="form-label required-label" for="editAv_start_point_street"><spring:message code="publishCar.form.pickupStreet"/></label>
                                <form:input path="startPointStreet" id="editAv_start_point_street" required="required"
                                            cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                                <form:errors path="startPointStreet" cssClass="text-danger d-block"/>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label required-label" for="editAv_start_point_number"><spring:message code="publishCar.form.pickupStreetNumber"/></label>
                                <form:input path="startPointNumber" id="editAv_start_point_number" maxlength="10" inputmode="numeric" autocomplete="off"
                                            required="required" data-ryden-digits-only="true"
                                            cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                                <form:errors path="startPointNumber" cssClass="text-danger d-block"/>
                            </div>
                        </div>
                        <p class="small text-muted mb-3"><spring:message code="publishCar.form.samePickupReturnHint"/></p>

                        <%-- Check-in / check-out times --%>
                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label class="form-label required-label" for="editAvCheckInTime"><spring:message code="publishCar.form.checkInTime"/></label>
                                <form:input path="checkInTime" id="editAvCheckInTime" type="time" required="required" step="60"
                                            cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                                <form:errors path="checkInTime" cssClass="text-danger d-block"/>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label required-label" for="editAvCheckOutTime"><spring:message code="publishCar.form.checkOutTime"/></label>
                                <form:input path="checkOutTime" id="editAvCheckOutTime" type="time" required="required" step="60"
                                            cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                                <form:errors path="checkOutTime" cssClass="text-danger d-block"/>
                            </div>
                        </div>

                        <%-- Single availability row (no add/remove controls) --%>
                        <spring:message code="publishCar.form.dateRange.placeholder" var="dateRangePlaceholder"/>
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
                                        <input type="text" class="form-control form-control-sm ryden-avail-range-input" readonly
                                               placeholder="<c:out value='${dateRangePlaceholder}'/>" aria-label="Availability date range"/>
                                        <form:hidden path="availabilityRows[${st.index}].from" cssClass="ryden-avail-from"/>
                                        <form:hidden path="availabilityRows[${st.index}].until" cssClass="ryden-avail-until"/>
                                        <form:errors path="availabilityRows[${st.index}].from" cssClass="text-danger d-block"/>
                                        <form:errors path="availabilityRows[${st.index}].until" cssClass="text-danger d-block"/>
                                    </div>
                                </c:forEach>
                            </div>
                            <div id="publishClientAvailError" class="text-danger small d-none mb-1" role="alert"></div>
                            <%-- Required by components.js guard; hidden because adding/removing rows is not available in edit mode --%>
                            <button type="button" id="publish_avail_add" hidden style="display:none"></button>
                        </div>
                        <template id="publish_avail_row_template"></template>

                        <div class="d-flex justify-content-end gap-2 mt-4">
                            <a href="<c:out value='${carDetailUrl}'/>" class="btn btn-outline-secondary">
                                <spring:message code="common.cancel"/>
                            </a>
                            <button type="submit" class="btn btn-primary" id="editAvailabilitySubmitBtn">
                                <i class="bi bi-check-lg me-1"></i> <spring:message code="editAvailability.submit"/>
                            </button>
                        </div>

                    </form:form>
                </div>
            </div>

        </div>
    </div>
</main>

<script>window.rydenPublishAvailMinFromUrl = '<c:url value="/my-cars/car/${car.id}/availability-min-from"/>';</script>
<%@include file="../footer.jsp" %>
</body>
</html>
