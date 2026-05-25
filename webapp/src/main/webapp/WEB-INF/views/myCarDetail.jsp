<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <c:choose>
        <c:when test="${hasPublishedAvailability}">
            <title><spring:message code="myCarDetail.pageTitle" arguments="${carTitle}"/></title>
        </c:when>
        <c:otherwise>
            <title><c:out value="${car.brand} ${car.model}"/> - Ryden</title>
        </c:otherwise>
    </c:choose>
    <%@include file="header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="navbar.myListings" var="myListingsLabel"/>
    <c:set var="carLabel"><c:out value="${car.brand} ${car.model}"/></c:set>
    <ryden:breadcrumbTrail
            homeLabel="${myListingsLabel}"
            homeHref="${pageContext.request.contextPath}/my-cars"
            currentLabel="${carLabel}"/>

    <c:choose>
        <c:when test="${hasPublishedAvailability}">
            <%-- ====== WITH PUBLISHED AVAILABILITY: identical to myCarDetail.jsp ====== --%>
            <c:url var="toggleListingUrl" value="/my-cars/car/${car.id}/toggle"/>
            <c:url var="finishListingUrl" value="/my-cars/car/${car.id}/deactivate"/>

            <section class="reservation-management-header mb-4">
                <h1 class="h3 fw-bold mb-2"><spring:message code="myCarDetail.heading"/></h1>
                <p class="text-secondary mb-0"><spring:message code="myCarDetail.subheading"/></p>
            </section>

            <c:if test="${not empty carToggleErrorMessage}">
                <div class="alert alert-warning alert-dismissible fade show" role="alert">
                    <c:out value="${carToggleErrorMessage}"/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                </div>
            </c:if>

            <div class="row g-4 align-items-start">
                <div class="col-lg-8">
                    <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                        <div class="card-body p-4">
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
                                    <div class="d-flex align-items-start justify-content-between gap-2 mb-2">
                                        <h2 class="h5 fw-semibold mb-0"><c:out value="${car.brand} ${car.model}"/></h2>
                                    </div>
                                    <div class="d-flex flex-wrap gap-2">
                                        <spring:message code="enum.car.transmission.${car.transmission.name()}" var="carTransmissionLabel"/>
                                        <spring:message code="enum.car.powertrain.${car.powertrain.name()}" var="carPowertrainLabel"/>
                                        <span class="badge text-bg-light border" style="background-color: var(--color-surface-elevated) !important;"><c:out value="${carTransmissionLabel}"/></span>
                                        <span class="badge text-bg-light border" style="background-color: var(--color-surface-elevated) !important;"><c:out value="${carPowertrainLabel}"/></span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </article>

                    <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white" id="availabilityDisplaySection">
                        <div class="card-body p-4">
                            <div class="d-flex align-items-center justify-content-between mb-3">
                                <h2 class="h5 fw-semibold mb-0"><spring:message code="myCarDetail.availability.title"/></h2>
                                <c:if test="${statusKey != 'DEACTIVATED' && statusKey != 'ADMIN_PAUSED'}">
                                    <c:url var="createPeriodUrl" value="/my-cars/car/${car.id}/create"/>
                                    <a href="<c:out value='${createPeriodUrl}'/>" class="btn btn-outline-primary btn-sm">
                                        <i class="bi bi-plus-lg" aria-hidden="true"></i>
                                        <spring:message code="myCarDetail.availability.addPeriod"/>
                                    </a>
                                </c:if>
                            </div>
                            <c:choose>
                                <c:when test="${empty availabilities}">
                                    <p class="text-secondary mb-0"><spring:message code="myCarDetail.availability.empty"/></p>
                                </c:when>
                                <c:otherwise>
                                    <div class="d-flex flex-column gap-2">
                                        <c:forEach var="availability" items="${availabilities}">
                                            <div class="p-3 border rounded-3 bg-white d-flex align-items-center justify-content-between gap-2">
                                                <div class="d-flex align-items-center gap-2">
                                                    <i class="bi bi-calendar-range text-primary flex-shrink-0" aria-hidden="true"></i>
                                                    <span class="fw-medium"><c:out value="${availability.startInclusive}"/> &ndash; <c:out value="${availability.endInclusive}"/></span>
                                                </div>
                                                <div class="d-flex align-items-center gap-2 flex-shrink-0">
                                                    <c:if test="${availability.dayPriceValue != null}">
                                                        <fmt:setLocale value="es_AR"/>
                                                        <span class="text-secondary small text-nowrap"><fmt:formatNumber value="${availability.dayPriceValue}" type="currency" currencyCode="ARS"/></span>
                                                    </c:if>
                                                    <c:if test="${statusKey != 'DEACTIVATED' && statusKey != 'ADMIN_PAUSED'}">
                                                        <c:url var="editAvailabilityUrl" value="/my-cars/car/${car.id}/availability/${availability.id}/edit"/>
                                                        <a href="<c:out value='${editAvailabilityUrl}'/>" class="btn btn-sm btn-outline-secondary"
                                                           aria-label="<spring:message code='myCarDetail.availability.edit.aria'/>">
                                                            <i class="bi bi-pencil" aria-hidden="true"></i>
                                                        </a>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </article>
                </div>

                <div class="col-lg-4">
                    <article class="card border-0 shadow-sm rounded-4 reservation-detail-sticky bg-white">
                        <div class="card-body p-4">
                            <spring:message code="enum.car.status.${statusKey}" var="carStatusLabel"/>
                            <div class="d-flex align-items-center justify-content-between mb-3">
                                <span class="text-secondary small text-uppercase fw-semibold" style="letter-spacing:.04em;">
                                    <spring:message code="myCarDetail.status.label"/>
                                </span>
                                <c:choose>
                                    <c:when test="${statusKey == 'ACTIVE'}">
                                        <span class="badge text-bg-success fs-6 px-3 py-2"><c:out value="${carStatusLabel}"/></span>
                                    </c:when>
                                    <c:when test="${statusKey == 'PAUSED' || statusKey == 'LACK_DOC'}">
                                        <span class="badge text-bg-warning text-dark fs-6 px-3 py-2"><c:out value="${carStatusLabel}"/></span>
                                    </c:when>
                                    <c:when test="${statusKey == 'ADMIN_PAUSED'}">
                                        <span class="badge text-bg-danger fs-6 px-3 py-2"><c:out value="${carStatusLabel}"/></span>
                                    </c:when>
                                    <c:when test="${statusKey == 'DEACTIVATED'}">
                                        <span class="badge text-bg-secondary fs-6 px-3 py-2"><c:out value="${carStatusLabel}"/></span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge text-bg-light border fs-6 px-3 py-2"><c:out value="${carStatusLabel}"/></span>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <div class="d-flex flex-column gap-3 px-2">
                                <c:url var="carDetailUrl" value="/car-detail">
                                    <c:param name="carId"><c:out value="${car.id}"/></c:param>
                                </c:url>
                                <a href="<c:out value='${carDetailUrl}'/>" class="btn btn-outline-warm w-100">
                                    <i class="bi bi-eye me-2"></i><spring:message code="myCarDetail.actions.viewListing"/>
                                </a>

                                <%-- Insurance document section: shows current state and lets the owner (re)upload. --%>
                                <c:set var="carHasInsurance" value="${car.insuranceFileId.present}"/>
                                <c:url var="carInsuranceUrl" value="/my-cars/car/${car.id}/insurance"/>
                                <div class="border rounded-3 p-3 mt-1 bg-light">
                                    <div class="d-flex align-items-center gap-2 mb-2">
                                        <c:choose>
                                            <c:when test="${carHasInsurance}">
                                                <i class="bi bi-shield-check text-success fs-5" aria-hidden="true"></i>
                                                <span class="fw-semibold"><spring:message code="myCarDetail.insurance.uploaded"/></span>
                                            </c:when>
                                            <c:otherwise>
                                                <i class="bi bi-shield-exclamation text-warning fs-5" aria-hidden="true"></i>
                                                <span class="fw-semibold"><spring:message code="myCarDetail.insurance.missing"/></span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                    <p class="small text-secondary mb-2"><spring:message code="myCarDetail.insurance.hint" arguments="${uploadMaxProfileDocumentMegabytes}"/></p>
                                    <c:if test="${not empty carInsuranceErrorMessage}">
                                        <div class="alert alert-danger py-2 mb-2 small" role="alert">
                                            <c:out value="${carInsuranceErrorMessage}"/>
                                        </div>
                                    </c:if>
                                    <form method="post" action="<c:out value='${carInsuranceUrl}'/>" enctype="multipart/form-data" class="d-flex flex-column gap-2">
                                        <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                                        <input type="file" name="insuranceFile" accept="application/pdf,image/*" class="form-control form-control-sm" required/>
                                        <button type="submit" class="btn btn-sm btn-primary">
                                            <i class="bi bi-upload me-1" aria-hidden="true"></i>
                                            <c:choose>
                                                <c:when test="${carHasInsurance}"><spring:message code="myCarDetail.insurance.replace"/></c:when>
                                                <c:otherwise><spring:message code="myCarDetail.insurance.upload"/></c:otherwise>
                                            </c:choose>
                                        </button>
                                    </form>
                                </div>

                                <spring:message code="myCarDetail.actions.finish" var="finishBtnLabel"/>
                                <spring:message code="myCarDetail.finishModal.title" var="finishModalTitle"/>
                                <spring:message code="myCarDetail.finishModal.message" var="finishModalMessage"/>
                                <spring:message code="myCarDetail.finishModal.confirm" var="finishModalConfirm"/>
                                <spring:message code="myCarDetail.finishModal.back" var="finishModalBack"/>

                                <c:choose>
                                    <c:when test="${statusKey == 'ACTIVE'}">
                                        <spring:message code="myCarDetail.pauseModal.title" var="pauseModalTitle"/>
                                        <spring:message code="myCarDetail.pauseModal.message" var="pauseModalMessage"/>
                                        <spring:message code="myCarDetail.pauseModal.confirm" var="pauseModalConfirm"/>
                                        <spring:message code="myCarDetail.pauseModal.back" var="pauseModalBack"/>
                                        <spring:message code="myCarDetail.actions.pause" var="pauseBtnLabel"/>
                                        <button type="button" class="btn btn-pause w-100" data-modal-open="pauseListingModal" aria-label="<c:out value='${pauseBtnLabel}'/>">
                                            <i class="bi bi-pause-fill me-2"></i><c:out value="${pauseBtnLabel}"/>
                                        </button>
                                        <button type="button" class="btn btn-outline-danger w-100" data-modal-open="finishListingModal" aria-label="<c:out value='${finishBtnLabel}'/>">
                                            <i class="bi bi-x-circle me-2"></i><c:out value="${finishBtnLabel}"/>
                                        </button>
                                    </c:when>
                                    <c:when test="${statusKey == 'PAUSED'}">
                                        <form method="post" action="<c:out value='${toggleListingUrl}'/>">
                                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                                            <button type="submit" class="btn btn-success w-100" aria-label="<spring:message code='myCarDetail.actions.activate'/>">
                                                <i class="bi bi-play-fill me-2"></i><spring:message code="myCarDetail.actions.activate"/>
                                            </button>
                                        </form>
                                        <button type="button" class="btn btn-outline-danger w-100" data-modal-open="finishListingModal" aria-label="<c:out value='${finishBtnLabel}'/>">
                                            <i class="bi bi-x-circle me-2"></i><c:out value="${finishBtnLabel}"/>
                                        </button>
                                    </c:when>
                                    <c:when test="${statusKey == 'LACK_DOC'}">
                                        <p class="text-secondary small mb-2">
                                            <spring:message code="myCarDetail.status.pausedMissingCbuHint" arguments="${cbuRequiredDigits}"/>
                                        </p>
                                        <a href="${pageContext.request.contextPath}/profile" class="btn btn-primary w-100">
                                            <i class="bi bi-person-fill me-2"></i><spring:message code="myCarDetail.status.pausedMissingCbuCta"/>
                                        </a>
                                        <button type="button" class="btn btn-outline-danger w-100" data-modal-open="finishListingModal" aria-label="<c:out value='${finishBtnLabel}'/>">
                                            <i class="bi bi-x-circle me-2"></i><c:out value="${finishBtnLabel}"/>
                                        </button>
                                    </c:when>
                                    <c:when test="${statusKey == 'ADMIN_PAUSED'}">
                                        <p class="text-secondary small mb-0">
                                            <spring:message code="myCarDetail.status.adminPausedHint"/>
                                        </p>
                                    </c:when>
                                    <c:when test="${statusKey == 'DEACTIVATED'}">
                                        <p class="text-secondary small mb-0">
                                            <spring:message code="myCarDetail.status.finishedHint"/>
                                        </p>
                                    </c:when>
                                </c:choose>
                            </div>
                        </div>
                    </article>
                </div>
            </div>

            <c:url var="carReservationsUrl" value="/my-cars/car/${car.id}/reservations"/>
            <article class="card border-0 shadow-sm rounded-4 mt-4 bg-white">
                <div class="card-body p-4">
                    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
                        <h2 class="h5 fw-semibold mb-0"><spring:message code="myCarReservations.dashboard.title"/></h2>
                        <c:if test="${reservationTotal > 0}">
                            <a href="<c:out value='${carReservationsUrl}'/>" class="btn btn-outline-primary btn-sm">
                                <spring:message code="myCarReservations.dashboard.viewAll"/>
                            </a>
                        </c:if>
                    </div>

                    <div class="row g-2 mb-4">
                        <div class="col-6 col-sm-4 col-lg-2">
                            <div class="p-3 rounded-3 text-center bg-white border">
                                <div class="fw-bold fs-4 mb-0"><c:out value="${reservationTotal}"/></div>
                                <div class="small text-secondary mt-1"><spring:message code="myCarReservations.dashboard.total"/></div>
                            </div>
                        </div>
                        <div class="col-6 col-sm-4 col-lg-2">
                            <div class="p-3 rounded-3 text-center" style="background:rgba(255,193,7,.12);">
                                <div class="fw-bold fs-4 mb-0 text-warning-emphasis"><c:out value="${reservationStatusCounts['pending'] != null ? reservationStatusCounts['pending'] : 0}"/></div>
                                <div class="small text-secondary mt-1"><spring:message code="myCarReservations.dashboard.status.pending"/></div>
                            </div>
                        </div>
                        <div class="col-6 col-sm-4 col-lg-2">
                            <div class="p-3 rounded-3 text-center" style="background:rgba(25,135,84,.1);">
                                <div class="fw-bold fs-4 mb-0 text-success"><c:out value="${reservationStatusCounts['accepted'] != null ? reservationStatusCounts['accepted'] : 0}"/></div>
                                <div class="small text-secondary mt-1"><spring:message code="myCarReservations.dashboard.status.accepted"/></div>
                            </div>
                        </div>
                        <div class="col-6 col-sm-4 col-lg-2">
                            <div class="p-3 rounded-3 text-center" style="background:rgba(13,202,240,.1);">
                                <div class="fw-bold fs-4 mb-0 text-info"><c:out value="${reservationStatusCounts['started'] != null ? reservationStatusCounts['started'] : 0}"/></div>
                                <div class="small text-secondary mt-1"><spring:message code="myCarReservations.dashboard.status.started"/></div>
                            </div>
                        </div>
                        <div class="col-6 col-sm-4 col-lg-2">
                            <div class="p-3 rounded-3 text-center" style="background:rgba(220,53,69,.1);">
                                <div class="fw-bold fs-4 mb-0 text-danger"><c:out value="${reservationStatusCounts['cancelled'] != null ? reservationStatusCounts['cancelled'] : 0}"/></div>
                                <div class="small text-secondary mt-1"><spring:message code="myCarReservations.dashboard.status.cancelled"/></div>
                            </div>
                        </div>
                        <div class="col-6 col-sm-4 col-lg-2">
                            <div class="p-3 rounded-3 text-center" style="background:rgba(108,117,125,.1);">
                                <div class="fw-bold fs-4 mb-0 text-secondary"><c:out value="${reservationStatusCounts['finished'] != null ? reservationStatusCounts['finished'] : 0}"/></div>
                                <div class="small text-secondary mt-1"><spring:message code="myCarReservations.dashboard.status.finished"/></div>
                            </div>
                        </div>
                    </div>

                    <div class="row g-3 pt-2 border-top">
                        <div class="col-sm-6 col-md-4">
                            <div class="d-flex flex-column gap-1">
                                <span class="small text-secondary text-uppercase fw-semibold" style="letter-spacing:.04em;">
                                    <spring:message code="myCarReservations.dashboard.earnings.total"/>
                                </span>
                                <span class="fw-bold fs-5 text-primary"><c:out value="${carTotalEarnings}"/></span>
                            </div>
                        </div>
                        <div class="col-sm-6 col-md-4">
                            <div class="d-flex flex-column gap-1">
                                <span class="small text-secondary text-uppercase fw-semibold" style="letter-spacing:.04em;">
                                    <spring:message code="myCarReservations.dashboard.earnings.pending"/>
                                </span>
                                <span class="fw-bold fs-5 text-warning-emphasis"><c:out value="${carPendingEarnings}"/></span>
                            </div>
                        </div>
                        <div class="col-sm-6 col-md-4">
                            <div class="d-flex flex-column gap-1">
                                <span class="small text-secondary text-uppercase fw-semibold" style="letter-spacing:.04em;">
                                    <spring:message code="myCarReservations.dashboard.daysRented"/>
                                </span>
                                <span class="fw-bold fs-5"><c:out value="${carTotalDaysRented}"/></span>
                            </div>
                        </div>
                        <div class="col-sm-6 col-md-4">
                            <div class="d-flex flex-column gap-1">
                                <span class="small text-secondary text-uppercase fw-semibold" style="letter-spacing:.04em;">
                                    <spring:message code="myCarReservations.dashboard.thisMonth"/>
                                </span>
                                <span class="fw-bold fs-5"><c:out value="${carReservationsThisMonth}"/></span>
                            </div>
                        </div>
                        <div class="col-sm-6 col-md-4">
                            <div class="d-flex flex-column gap-1">
                                <span class="small text-secondary text-uppercase fw-semibold" style="letter-spacing:.04em;">
                                    <spring:message code="myCarReservations.dashboard.cancellationRate"/>
                                </span>
                                <span class="fw-bold fs-5"><c:out value="${carCancellationRate}"/></span>
                            </div>
                        </div>
                        <div class="col-sm-6 col-md-4">
                            <div class="d-flex flex-column gap-1">
                                <span class="small text-secondary text-uppercase fw-semibold" style="letter-spacing:.04em;">
                                    <spring:message code="myCarReservations.dashboard.nextReservation"/>
                                </span>
                                <c:choose>
                                    <c:when test="${not empty carNextReservationDisplay}">
                                        <span class="fw-bold fs-5 text-success"><c:out value="${carNextReservationDisplay}"/></span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="text-secondary fst-italic small mt-1"><spring:message code="myCarReservations.dashboard.nextReservation.none"/></span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </div>
                </div>
            </article>

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
            <c:if test="${statusKey == 'ACTIVE' || statusKey == 'PAUSED' || statusKey == 'LACK_DOC'}">
                <ryden:modal
                        id="finishListingModal"
                        title="${finishModalTitle}"
                        message="${finishModalMessage}"
                        variant="danger">
                    <form method="post" action="<c:out value='${finishListingUrl}'/>">
                        <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                        <div class="d-flex justify-content-end gap-2 mt-3">
                            <button type="button" class="btn btn-secondary" data-modal-close="finishListingModal">
                                <c:out value="${finishModalBack}"/>
                            </button>
                            <button type="submit" class="btn btn-danger">
                                <c:out value="${finishModalConfirm}"/>
                            </button>
                        </div>
                    </form>
                </ryden:modal>
            </c:if>

        </c:when>

        <c:otherwise>
            <%-- ====== NO LISTING: show car info + CTA ====== --%>
            <section class="reservation-management-header mb-4">
                <h1 class="h3 fw-bold mb-2"><c:out value="${car.brand} ${car.model}"/></h1>
            </section>

            <div class="row g-4 align-items-start">
                <div class="col-lg-8">
                    <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                        <div class="card-body p-4">
                            <h2 class="h5 fw-semibold mb-3"><spring:message code="myCarDetail.carSummary.title"/></h2>
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
                                    <div class="d-flex flex-wrap gap-2 mb-2">
                                        <spring:message code="enum.car.transmission.${car.transmission.name()}" var="carTransmissionLabel"/>
                                        <spring:message code="enum.car.powertrain.${car.powertrain.name()}" var="carPowertrainLabel"/>
                                        <span class="badge text-bg-light border" style="background-color: var(--color-surface-elevated) !important;"><c:out value="${carTransmissionLabel}"/></span>
                                        <span class="badge text-bg-light border" style="background-color: var(--color-surface-elevated) !important;"><c:out value="${carPowertrainLabel}"/></span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </article>
                </div>

                <div class="col-lg-4">
                    <article class="card border-0 shadow-sm rounded-4 reservation-detail-sticky bg-white">
                        <div class="card-body p-4 text-center">
                            <p class="text-secondary mb-3">
                                <spring:message code="myCarDetail.noListing.message"/>
                            </p>
                            <c:url var="createListingUrl" value="/my-cars/car/${car.id}/create"/>
                            <a href="<c:out value='${createListingUrl}'/>" class="btn btn-primary w-100">
                                <i class="bi bi-plus-lg me-2"></i><spring:message code="myCarDetail.createListing.button"/>
                            </a>
                        </div>
                    </article>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</main>
<%@include file="footer.jsp"%>
</body>
</html>
