<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="myListingDetail.pageTitle" arguments="${listing.title}"/></title>
    <%@include file="header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="navbar.myListings" var="myListingsLabel"/>
    <c:url var="editListingUrl" value="/my-listings/${listing.id}/edit"/>
    <c:url var="toggleListingUrl" value="/my-listings/${listing.id}/toggle"/>
    <ryden:breadcrumbTrail
            homeLabel="${myListingsLabel}"
            homeHref="${pageContext.request.contextPath}/my-listings"
            currentLabel="${listing.title}"/>

    <section class="reservation-management-header mb-4">
        <h1 class="h3 fw-bold mb-2"><spring:message code="myListingDetail.heading"/></h1>
        <p class="text-secondary mb-0"><spring:message code="myListingDetail.subheading"/></p>
    </section>

    <div class="row g-4 align-items-start">
        <div class="col-lg-8">
            <article class="card border-0 shadow-sm rounded-4 mb-4">
                <div class="card-body p-4">
                    <div class="d-flex align-items-center justify-content-between mb-3">
                        <h2 class="h5 fw-semibold mb-0"><spring:message code="myListingDetail.carSummary.title"/></h2>
                        <button type="button"
                                class="btn btn-outline-primary btn-sm"
                                id="toggleEditBtn"
                                onclick="toggleEditForm()">
                            <i class="bi bi-pencil me-1" aria-hidden="true"></i>
                            <spring:message code="myListingDetail.actions.editListing"/>
                        </button>
                    </div>
                    <div class="d-flex flex-column flex-md-row gap-3 align-items-start">
                        <div class="reservation-detail-car-media rounded-3 overflow-hidden border flex-shrink-0">
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
                        <div class="flex-grow-1">
                            <h3 class="h5 mb-1"><c:out value="${car.brand} ${car.model}"/></h3>
                            <p class="text-secondary mb-2"><c:out value="${listing.title}"/></p>
                            <div class="d-flex flex-wrap gap-2 mb-2">
                                <spring:message code="enum.car.transmission.${car.transmission.name()}" var="carTransmissionLabel"/>
                                <spring:message code="enum.car.powertrain.${car.powertrain.name()}" var="carPowertrainLabel"/>
                                <span class="badge text-bg-light border"><c:out value="${carTransmissionLabel}"/></span>
                                <span class="badge text-bg-light border"><c:out value="${carPowertrainLabel}"/></span>
                            </div>
                            <p class="mb-2 text-secondary small">
                                <spring:message code="myListingDetail.details.createdAt"/>: <c:out value="${listingCreatedAtDisplay}"/>
                            </p>
                            <div class="d-flex align-items-center gap-2 mt-3 pt-2 border-top">
                                <span class="text-secondary small text-uppercase fw-semibold" style="letter-spacing:.04em;">
                                    <spring:message code="myListingDetail.pricePerDay"/>
                                </span>
                                <span class="h4 fw-bold text-primary mb-0">$<c:out value="${listing.dayPrice}"/></span>
                            </div>
                        </div>
                    </div>

                </div>
            </article>

            <article class="card border-0 shadow-sm rounded-4 mb-4 d-none" id="editListingSection">
                <div class="card-body p-4">
                    <h2 class="h5 fw-semibold mb-3"><spring:message code="myListingDetail.edit.title"/></h2>
                    <spring:message code="validation.neighborhood.invalid" var="editNbInvalidMsg" htmlEscape="true"/>
                    <spring:message code="validation.neighborhood.notNull" var="editNbRequiredMsg" htmlEscape="true"/>
                    <form:form id="editListingFormEl" method="post"
                               action="${editListingUrl}"
                               modelAttribute="editForm"
                               class="row g-3"
                               htmlEscape="true"
                               data-ryden-nb-invalid="<c:out value='${editNbInvalidMsg}'/>"
                               data-ryden-nb-required="<c:out value='${editNbRequiredMsg}'/>">
                        <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                        <form:hidden path="neighborhoodId" id="nb_hid_editListing"/>

                        <div class="col-sm-6">
                            <label for="pricePerDay" class="form-label required-label"><spring:message code="publishCar.form.pricePerDay"/></label>
                            <form:input path="pricePerDay" id="pricePerDay" type="number" step="0.01" cssClass="form-control js-no-number-wheel-step" cssErrorClass="form-control is-invalid js-no-number-wheel-step"/>
                            <form:errors path="pricePerDay" cssClass="text-danger d-block"/>
                        </div>
                        <spring:message code="publishCar.form.neighborhood.placeholder" var="editNbPh"/>
                        <spring:message code="publishCar.form.neighborhood" var="editNbFieldLabel"/>
                        <spring:message code="publishCar.form.neighborhood.search" var="editNbSearchPh"/>
                        <div class="col-md-12">
                            <ryden:neighborhoodPicker
                                    pickerId="editListing"
                                    mode="get"
                                    allowMultiple="false"
                                    springPath="neighborhoodId"
                                    selectedNeighborhoodId="${editForm.neighborhoodId}"
                                    neighborhoodList="${allNeighborhoods}"
                                    anyLabel="${editNbPh}"
                                    searchPlaceholder="${editNbSearchPh}"
                                    selectFieldLabel="${editNbFieldLabel}"
                                    toggleAriaLabel="${editNbFieldLabel}"
                                    outerLabel="${editNbFieldLabel}"
                                    outerLabelRequired="true"
                                    required="true"
                                    nbRequiredMessage="${editNbRequiredMsg}"
                                    formId="editListingFormEl"/>
                        </div>
                        <div class="col-md-8">
                            <label for="start_point_street_edit" class="form-label required-label"><spring:message code="publishCar.form.pickupStreet"/></label>
                            <form:input path="startPointStreet" id="start_point_street_edit" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="startPointStreet" cssClass="text-danger d-block"/>
                        </div>
                        <div class="col-md-4">
                            <label for="start_point_number_edit" class="form-label required-label"><spring:message code="publishCar.form.pickupStreetNumber"/></label>
                            <form:input path="startPointNumber" id="start_point_number_edit" maxlength="10" inputmode="numeric" autocomplete="off"
                                        data-ryden-digits-only="true" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="startPointNumber" cssClass="text-danger d-block"/>
                        </div>
                        <div class="col-12 mb-2">
                            <p class="small text-muted mb-0"><spring:message code="publishCar.form.samePickupReturnHint"/></p>
                        </div>
                        <div class="col-sm-6">
                            <label for="checkInTime" class="form-label required-label"><spring:message code="publishCar.form.checkInTime"/></label>
                            <form:input path="checkInTime" id="checkInTime" type="time" step="60" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="checkInTime" cssClass="text-danger d-block"/>
                        </div>
                        <div class="col-sm-6">
                            <label for="checkOutTime" class="form-label required-label"><spring:message code="publishCar.form.checkOutTime"/></label>
                            <form:input path="checkOutTime" id="checkOutTime" type="time" step="60" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="checkOutTime" cssClass="text-danger d-block"/>
                        </div>
                        <div class="col-12">
                            <label for="description" class="form-label"><spring:message code="publishCar.form.description"/></label>
                            <form:textarea path="description" id="description" rows="3" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="description" cssClass="text-danger d-block"/>
                        </div>

                        <div class="col-12 d-flex justify-content-end gap-2">
                            <button type="button" class="btn btn-outline-secondary" onclick="cancelEdit()">
                                <spring:message code="common.cancel"/>
                            </button>
                            <button type="submit" class="btn btn-primary">
                                <spring:message code="myListingDetail.actions.modify"/>
                            </button>
                        </div>
                    </form:form>
                </div>
            </article>

            <article class="card border-0 shadow-sm rounded-4 mb-4">
                <div class="card-body p-4">
                    <h2 class="h5 fw-semibold mb-3"><spring:message code="myListingDetail.availability.title"/></h2>
                    <c:choose>
                        <c:when test="${empty availabilities}">
                            <p class="text-secondary mb-0"><spring:message code="myListingDetail.availability.empty"/></p>
                        </c:when>
                        <c:otherwise>
                            <div class="d-flex flex-column gap-2">
                                <c:forEach var="availability" items="${availabilities}">
                                    <div class="p-2 border rounded-3 bg-body-tertiary small">
                                        <c:out value="${availability.startInclusive}"/> - <c:out value="${availability.endInclusive}"/>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </article>
        </div>

        <div class="col-lg-4">
            <article class="card border-0 shadow-sm rounded-4 reservation-detail-sticky">
                <div class="card-body p-4">
                    <spring:message code="enum.listing.status.${statusKey}" var="listingStatusLabel"/>
                    <div class="d-flex align-items-center justify-content-between mb-3">
                        <span class="text-secondary small text-uppercase fw-semibold" style="letter-spacing:.04em;">
                            <spring:message code="myListingDetail.status.label"/>
                        </span>
                        <c:choose>
                            <c:when test="${statusKey == 'ACTIVE'}">
                                <span class="badge text-bg-success fs-6 px-3 py-2"><c:out value="${listingStatusLabel}"/></span>
                            </c:when>
                            <c:when test="${statusKey == 'PAUSED' || statusKey == 'PAUSED_DUE_TO_LACK_OF_CBU'}">
                                <span class="badge text-bg-warning text-dark fs-6 px-3 py-2"><c:out value="${listingStatusLabel}"/></span>
                            </c:when>
                            <c:when test="${statusKey == 'FINISHED'}">
                                <span class="badge text-bg-secondary fs-6 px-3 py-2"><c:out value="${listingStatusLabel}"/></span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge text-bg-light border fs-6 px-3 py-2"><c:out value="${listingStatusLabel}"/></span>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <div class="d-flex flex-column gap-3 px-2">
                        <c:url var="listingUrl" value="/car-detail">
                            <c:param name="listingId"><c:out value="${listing.id}"/></c:param>
                        </c:url>
                        <a href="<c:out value='${listingUrl}'/>" class="btn btn-light border w-100">
                            <spring:message code="myListingDetail.actions.viewListing"/>
                        </a>

                        <c:choose>
                            <c:when test="${statusKey == 'ACTIVE'}">
                                <spring:message code="myListingDetail.pauseModal.title" var="pauseModalTitle"/>
                                <spring:message code="myListingDetail.pauseModal.message" var="pauseModalMessage"/>
                                <spring:message code="myListingDetail.pauseModal.confirm" var="pauseModalConfirm"/>
                                <spring:message code="myListingDetail.pauseModal.back" var="pauseModalBack"/>
                                <spring:message code="myListingDetail.actions.pause" var="pauseBtnLabel"/>
                                <button type="button" class="btn w-100" style="background-color:#e4960b; color:#ffffff; border-color:rgb(228 150 11);" data-modal-open="pauseListingModal" aria-label="${pauseBtnLabel}">
                                    <c:out value="${pauseBtnLabel}"/>
                                </button>
                                <ryden:modal
                                    id="pauseListingModal"
                                    title="${pauseModalTitle}"
                                    message="${pauseModalMessage}"
                                    variant="danger">
                                    <form method="post" action="<c:out value='${toggleListingUrl}'/>">
                                        <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                                        <div class="d-flex justify-content-end gap-2 mt-3">
                                            <button type="button" class="btn btn-secondary" data-modal-close="pauseListingModal">
                                                <c:out value="${pauseModalBack}"/>
                                            </button>
                                            <button type="submit" class="btn btn-danger">
                                                <c:out value="${pauseModalConfirm}"/>
                                            </button>
                                        </div>
                                    </form>
                                </ryden:modal>
                            </c:when>
                            <c:when test="${statusKey == 'PAUSED'}">
                                <form method="post" action="<c:out value='${toggleListingUrl}'/>">
                                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                                    <button type="submit" class="btn btn-success w-100" aria-label="<spring:message code='myListingDetail.actions.activate'/>">
                                        <spring:message code="myListingDetail.actions.activate"/>
                                    </button>
                                </form>
                            </c:when>
                            <c:when test="${statusKey == 'FINISHED'}">
                                <p class="text-secondary small mb-0">
                                    <spring:message code="myListingDetail.status.finishedHint"/>
                                </p>
                            </c:when>
                        </c:choose>
                    </div>
                </div>
            </article>
        </div>
    </div>

    <c:url var="listingReservationsUrl" value="/my-listings/${listing.id}/reservations"/>
    <article class="card border-0 shadow-sm rounded-4 mt-4">
        <div class="card-body p-4">
            <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
                <h2 class="h5 fw-semibold mb-0"><spring:message code="myListingReservations.dashboard.title"/></h2>
                <c:if test="${reservationTotal > 0}">
                    <a href="<c:out value='${listingReservationsUrl}'/>" class="btn btn-outline-primary btn-sm">
                        <spring:message code="myListingReservations.dashboard.viewAll"/>
                    </a>
                </c:if>
            </div>

            <%-- Row 1: status stats --%>
            <div class="row g-2 mb-4">
                <div class="col-6 col-sm-4 col-lg-2">
                    <div class="p-3 rounded-3 text-center bg-body-tertiary border">
                        <div class="fw-bold fs-4 mb-0"><c:out value="${reservationTotal}"/></div>
                        <div class="small text-secondary mt-1"><spring:message code="myListingReservations.dashboard.total"/></div>
                    </div>
                </div>
                <div class="col-6 col-sm-4 col-lg-2">
                    <div class="p-3 rounded-3 text-center" style="background:rgba(255,193,7,.12);">
                        <div class="fw-bold fs-4 mb-0 text-warning-emphasis"><c:out value="${reservationStatusCounts['pending'] != null ? reservationStatusCounts['pending'] : 0}"/></div>
                        <div class="small text-secondary mt-1"><spring:message code="myListingReservations.dashboard.status.pending"/></div>
                    </div>
                </div>
                <div class="col-6 col-sm-4 col-lg-2">
                    <div class="p-3 rounded-3 text-center" style="background:rgba(25,135,84,.1);">
                        <div class="fw-bold fs-4 mb-0 text-success"><c:out value="${reservationStatusCounts['accepted'] != null ? reservationStatusCounts['accepted'] : 0}"/></div>
                        <div class="small text-secondary mt-1"><spring:message code="myListingReservations.dashboard.status.accepted"/></div>
                    </div>
                </div>
                <div class="col-6 col-sm-4 col-lg-2">
                    <div class="p-3 rounded-3 text-center" style="background:rgba(13,202,240,.1);">
                        <div class="fw-bold fs-4 mb-0 text-info"><c:out value="${reservationStatusCounts['started'] != null ? reservationStatusCounts['started'] : 0}"/></div>
                        <div class="small text-secondary mt-1"><spring:message code="myListingReservations.dashboard.status.started"/></div>
                    </div>
                </div>
                <div class="col-6 col-sm-4 col-lg-2">
                    <div class="p-3 rounded-3 text-center" style="background:rgba(220,53,69,.1);">
                        <div class="fw-bold fs-4 mb-0 text-danger"><c:out value="${reservationStatusCounts['cancelled'] != null ? reservationStatusCounts['cancelled'] : 0}"/></div>
                        <div class="small text-secondary mt-1"><spring:message code="myListingReservations.dashboard.status.cancelled"/></div>
                    </div>
                </div>
                <div class="col-6 col-sm-4 col-lg-2">
                    <div class="p-3 rounded-3 text-center" style="background:rgba(108,117,125,.1);">
                        <div class="fw-bold fs-4 mb-0 text-secondary"><c:out value="${reservationStatusCounts['finished'] != null ? reservationStatusCounts['finished'] : 0}"/></div>
                        <div class="small text-secondary mt-1"><spring:message code="myListingReservations.dashboard.status.finished"/></div>
                    </div>
                </div>
            </div>

            <%-- Row 2: financial and activity stats --%>
            <div class="row g-3 pt-2 border-top">
                <div class="col-sm-6 col-md-4">
                    <div class="d-flex flex-column gap-1">
                        <span class="small text-secondary text-uppercase fw-semibold" style="letter-spacing:.04em; font-size:.7rem;">
                            <spring:message code="myListingReservations.dashboard.earnings.total"/>
                        </span>
                        <span class="fw-bold fs-5 text-primary">$<c:out value="${listingTotalEarnings}"/></span>
                    </div>
                </div>
                <div class="col-sm-6 col-md-4">
                    <div class="d-flex flex-column gap-1">
                        <span class="small text-secondary text-uppercase fw-semibold" style="letter-spacing:.04em; font-size:.7rem;">
                            <spring:message code="myListingReservations.dashboard.earnings.pending"/>
                        </span>
                        <span class="fw-bold fs-5 text-warning-emphasis">$<c:out value="${listingPendingEarnings}"/></span>
                    </div>
                </div>
                <div class="col-sm-6 col-md-4">
                    <div class="d-flex flex-column gap-1">
                        <span class="small text-secondary text-uppercase fw-semibold" style="letter-spacing:.04em; font-size:.7rem;">
                            <spring:message code="myListingReservations.dashboard.daysRented"/>
                        </span>
                        <span class="fw-bold fs-5"><c:out value="${listingTotalDaysRented}"/></span>
                    </div>
                </div>
                <div class="col-sm-6 col-md-4">
                    <div class="d-flex flex-column gap-1">
                        <span class="small text-secondary text-uppercase fw-semibold" style="letter-spacing:.04em; font-size:.7rem;">
                            <spring:message code="myListingReservations.dashboard.thisMonth"/>
                        </span>
                        <span class="fw-bold fs-5"><c:out value="${listingReservationsThisMonth}"/></span>
                    </div>
                </div>
                <div class="col-sm-6 col-md-4">
                    <div class="d-flex flex-column gap-1">
                        <span class="small text-secondary text-uppercase fw-semibold" style="letter-spacing:.04em; font-size:.7rem;">
                            <spring:message code="myListingReservations.dashboard.cancellationRate"/>
                        </span>
                        <span class="fw-bold fs-5"><c:out value="${listingCancellationRate}"/></span>
                    </div>
                </div>
                <div class="col-sm-6 col-md-4">
                    <div class="d-flex flex-column gap-1">
                        <span class="small text-secondary text-uppercase fw-semibold" style="letter-spacing:.04em; font-size:.7rem;">
                            <spring:message code="myListingReservations.dashboard.nextReservation"/>
                        </span>
                        <c:choose>
                            <c:when test="${not empty listingNextReservationDisplay}">
                                <span class="fw-bold fs-5 text-success"><c:out value="${listingNextReservationDisplay}"/></span>
                            </c:when>
                            <c:otherwise>
                                <span class="text-secondary fst-italic small mt-1"><spring:message code="myListingReservations.dashboard.nextReservation.none"/></span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </div>
    </article>
</main>
<%@include file="footer.jsp"%>

<script>
    function toggleEditForm() {
        var section = document.getElementById('editListingSection');
        var btn = document.getElementById('toggleEditBtn');
        section.classList.remove('d-none');
        section.scrollIntoView({ behavior: 'smooth', block: 'start' });
        btn.disabled = true;
    }

    function cancelEdit() {
        var section = document.getElementById('editListingSection');
        var btn = document.getElementById('toggleEditBtn');
        section.classList.add('d-none');
        btn.disabled = false;
    }

    (function() {
        var section = document.getElementById('editListingSection');
        if (section && section.querySelector('.text-danger')) {
            section.classList.remove('d-none');
            document.getElementById('toggleEditBtn').disabled = true;
        }
    })();
</script>
</body>
</html>
