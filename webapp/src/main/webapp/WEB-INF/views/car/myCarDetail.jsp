<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
<head>
    <c:choose>
        <c:when test="${hasPublishedAvailability}">
            <title><spring:message code="myCarDetail.pageTitle" arguments="${carTitle}"/></title>
        </c:when>
        <c:otherwise>
            <title><c:out value="${car.brand} ${car.model}"/> - Ryden</title>
        </c:otherwise>
    </c:choose>
    <%@include file="../header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="navbar.myCars" var="myListingsLabel"/>
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
                                <div class="flex-grow-1 min-w-0" style="flex: 1 1 0; min-width: 0; overflow: hidden;">
                                    <div class="d-flex align-items-start gap-3 mb-2 flex-wrap">
                                        <c:set var="carFullTitle"><c:out value="${car.brand} ${car.model}"/><c:if test="${car.year.present}"> (<c:out value="${car.year.get()}"/>)</c:if></c:set>
                                        <h2 class="h5 fw-semibold mb-0 ryden-text-clamp-2" title="<c:out value='${carFullTitle}'/>" style="flex: 1 1 0; min-width: 0;">
                                            <c:out value="${car.brand} ${car.model}"/><c:if test="${car.year.present}"> <span class="text-secondary fw-normal">(<c:out value="${car.year.get()}"/>)</span></c:if>
                                        </h2>
                                        <c:if test="${carModelPendingValidation}">
                                            <span class="badge text-nowrap flex-shrink-0" style="background-color:#0369a1; color:#ffffff; padding:.35rem .6rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                <i class="bi bi-clock me-1"></i><spring:message code="myCars.modelPendingValidation.badge"/>
                                            </span>
                                        </c:if>
                                    </div>
                                    <div class="d-flex flex-wrap gap-2 mb-3">
                                        <spring:message code="enum.car.transmission.${car.transmission.name()}" var="carTransmissionLabel"/>
                                        <spring:message code="enum.car.powertrain.${car.powertrain.name()}" var="carPowertrainLabel"/>
                                        <spring:message code="enum.car.type.${car.type.name()}" var="carTypeLabel"/>
                                        <span class="badge text-bg-light border" style="background-color: var(--color-surface-elevated) !important;"><c:out value="${carTypeLabel}"/></span>
                                        <span class="badge text-bg-light border" style="background-color: var(--color-surface-elevated) !important;"><c:out value="${carTransmissionLabel}"/></span>
                                        <span class="badge text-bg-light border" style="background-color: var(--color-surface-elevated) !important;"><c:out value="${carPowertrainLabel}"/></span>
                                    </div>
                                    <div class="d-flex align-items-center gap-2 flex-wrap">
                                        <span class="small text-secondary">
                                            <spring:message code="myCarDetail.carDetails.plate"/>:
                                        </span>
                                        <span class="small fw-medium ryden-text-break"><c:out value="${car.plate}"/></span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </article>


                    <c:if test="${!carModelPendingValidation}">
                        <%-- Description block --%>
                        <c:if test="${car.description.present}">
                            <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                                <div class="card-body p-4">
                                    <h2 class="h6 fw-semibold text-secondary text-uppercase mb-2" style="letter-spacing:.04em;">
                                        <spring:message code="myCarDetail.description.title"/>
                                    </h2>
                                    <p class="mb-0 text-body ryden-multiline-plaintext"><c:out value="${car.description.get()}"/></p>
                                </div>
                            </article>
                        </c:if>

                        <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white" id="availabilityDisplaySection">
                            <div class="card-body p-4">
                                <div class="d-flex align-items-center justify-content-between mb-3">
                                    <h2 class="h5 fw-semibold mb-0"><spring:message code="myCarDetail.availability.title"/></h2>
                                    <c:if test="${statusKey != 'DEACTIVATED' && statusKey != 'ADMIN_PAUSED' && !owner.blocked}">
                                        <c:url var="managePeriodsUrl" value="/my-cars/car/${car.id}/periods"/>
                                        <a href="<c:out value='${managePeriodsUrl}'/>" class="btn btn-outline-secondary btn-sm">
                                            <i class="bi bi-calendar2-week" aria-hidden="true"></i>
                                            <spring:message code="myCarDetail.availability.managePeriodsBtn"/>
                                        </a>
                                    </c:if>
                                </div>
                                <c:choose>
                                    <c:when test="${empty availabilities}">
                                        <p class="text-secondary mb-0"><spring:message code="myCarDetail.availability.empty"/></p>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="owner-cal-container owner-cal-readonly" data-bookable-ranges='<c:out value="${bookableWallRangesJson}" escapeXml="false"/>'>
                                            <input type="text" class="js-owner-cal-anchor owner-cal-anchor" aria-hidden="true" tabindex="-1" readonly/>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </article>
                    </c:if><%-- end !carModelPendingValidation --%>
                </div>

                <%-- Modal string variables — declared here so they're available to ryden:modal tags below regardless of sidebar branch --%>
                <spring:message code="myCarDetail.actions.finish" var="finishBtnLabel"/>
                <spring:message code="myCarDetail.deregisterModal.title" var="finishModalTitle"/>
                <spring:message code="myCarDetail.deregisterModal.message" var="finishModalMessage"/>
                <spring:message code="myCarDetail.deregisterModal.confirm" var="finishModalConfirm"/>
                <spring:message code="myCarDetail.deregisterModal.back" var="finishModalBack"/>
                <spring:message code="myCarDetail.pauseModal.title" var="pauseModalTitle"/>
                <spring:message code="myCarDetail.pauseModal.message" var="pauseModalMessage"/>
                <spring:message code="myCarDetail.pauseModal.confirm" var="pauseModalConfirm"/>
                <spring:message code="myCarDetail.pauseModal.back" var="pauseModalBack"/>
                <spring:message code="myCarDetail.actions.pause" var="pauseBtnLabel"/>

                <div class="col-lg-4">
                    <c:choose>
                        <c:when test="${carModelPendingValidation}">
                            <%-- Only issue is model pending approval: show a minimal notice card --%>
                            <article class="card border-0 shadow-sm rounded-4 bg-white">
                                <div class="card-body p-4 d-flex flex-column align-items-center text-center gap-3">
                                    <i class="bi bi-clock-history fs-2" style="color:#0369a1;" aria-hidden="true"></i>
                                    <div>
                                        <p class="fw-semibold mb-1"><spring:message code="myCars.modelPendingValidation.badge"/></p>
                                        <p class="small text-secondary mb-0"><spring:message code="myCarDetail.status.modelPendingValidationHint"/></p>
                                    </div>
                                </div>
                            </article>
                        </c:when>
                        <c:otherwise>
                            <%-- Full status / docs / action sidebar --%>
                            <article class="card border-0 shadow-sm rounded-4 reservation-detail-sticky bg-white">
                                <div class="card-body p-4">
                                    <spring:message code="enum.car.status.${statusKey}" var="carStatusLabel"/>
                                    <div class="mb-3">
                                        <span class="text-secondary small text-uppercase fw-semibold d-block mb-2" style="letter-spacing:.04em;">
                                            <spring:message code="myCarDetail.status.label"/>
                                        </span>
                                        <c:choose>
                                            <%-- Owner-blocked (e.g. refund-proof deadline missed) supersedes ACTIVE/PAUSED/LACK_DOC
                                                 in the sidebar so the owner immediately sees why the car is no longer bookable. --%>
                                            <c:when test="${owner.blocked and statusKey ne 'DEACTIVATED' and statusKey ne 'ADMIN_PAUSED'}">
                                                <span class="badge text-bg-danger w-100 py-2 text-wrap" style="font-size:.8rem; white-space: normal;">
                                                    <spring:message code="myCars.badge.ownerBlocked"/>
                                                </span>
                                            </c:when>
                                            <c:when test="${statusKey == 'ACTIVE'}">
                                                <span class="badge text-bg-success w-100 py-2 text-wrap fs-6" style="white-space: normal;"><c:out value="${carStatusLabel}"/></span>
                                            </c:when>
                                            <c:when test="${statusKey == 'PAUSED' || statusKey == 'LACK_DOC'}">
                                                <span class="badge text-bg-warning text-dark w-100 py-2 text-wrap" style="font-size:.8rem; white-space: normal;"><c:out value="${carStatusLabel}"/></span>
                                            </c:when>
                                            <c:when test="${statusKey == 'ADMIN_PAUSED'}">
                                                <span class="badge text-bg-danger w-100 py-2 text-wrap" style="font-size:.8rem; white-space: normal;"><c:out value="${carStatusLabel}"/></span>
                                            </c:when>
                                            <c:when test="${statusKey == 'DEACTIVATED'}">
                                                <span class="badge text-bg-secondary w-100 py-2 text-wrap fs-6" style="white-space: normal;"><c:out value="${carStatusLabel}"/></span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge text-bg-light border w-100 py-2 text-wrap" style="font-size:.8rem; white-space: normal;"><c:out value="${carStatusLabel}"/></span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>

                                    <div class="d-flex flex-column gap-3 px-2">
                                        <c:url var="carDetailUrl" value="/cars/${car.id}"/>
                                        <a href="<c:out value='${carDetailUrl}'/>" class="btn btn-outline-warm w-100">
                                            <i class="bi bi-eye me-2"></i><spring:message code="myCarDetail.actions.viewCar"/>
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

                                        <c:choose>
                                            <%-- Owner-blocked: hide resume/pause toggles (the service would reject them anyway,
                                                 see CarServiceImpl#toggleListing CAR_ACTIVATE_OWNER_BLOCKED). Keep "finish/deactivate"
                                                 since deactivating an unbookable car is always safe. --%>
                                            <c:when test="${owner.blocked and statusKey ne 'DEACTIVATED' and statusKey ne 'ADMIN_PAUSED'}">
                                                <p class="text-secondary small mb-2">
                                                    <spring:message code="myCarDetail.status.ownerBlockedHint"/>
                                                </p>
                                                <button type="button" class="btn btn-outline-danger w-100" data-modal-open="finishListingModal" aria-label="<c:out value='${finishBtnLabel}'/>">
                                                    <i class="bi bi-x-circle me-2"></i><c:out value="${finishBtnLabel}"/>
                                                </button>
                                            </c:when>
                                            <c:when test="${statusKey == 'ACTIVE'}">
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
                                                <c:if test="${!ownerHasValidCbu}">
                                                    <p class="text-secondary small mb-2">
                                                        <spring:message code="myCarDetail.status.pausedMissingCbuHint" arguments="${cbuRequiredDigits}"/>
                                                    </p>
                                                    <a href="${pageContext.request.contextPath}/profile" class="btn btn-primary w-100">
                                                        <i class="bi bi-person-fill me-2"></i><spring:message code="myCarDetail.status.pausedMissingCbuCta"/>
                                                    </a>
                                                </c:if>
                                                <button type="button" class="btn btn-outline-danger w-100" data-modal-open="finishListingModal" aria-label="<c:out value='${finishBtnLabel}'/>">
                                                    <i class="bi bi-x-circle me-2"></i><c:out value="${finishBtnLabel}"/>
                                                </button>
                                            </c:when>
                                            <c:when test="${statusKey == 'ADMIN_PAUSED'}">
                                                <p class="text-secondary small mb-0">
                                                    <spring:message code="myCarDetail.status.adminPausedHint" arguments="${supportEmail}"/>
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
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <c:if test="${!carModelPendingValidation}">
            <c:url var="carReservationsUrl" value="/my-cars/car/${car.id}/reservations"/>
            <article class="card border-0 shadow-sm rounded-4 mt-4 bg-white">
                <div class="card-body p-4">
                    <div class="mb-4">
                        <h2 class="h5 fw-semibold mb-0"><spring:message code="myCarReservations.dashboard.title"/></h2>
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

            <%-- Recent Reservations preview --%>
            <article class="card border-0 shadow-sm rounded-4 mt-4 bg-white">
                <div class="card-body p-4">
                    <div class="d-flex align-items-center justify-content-between mb-3 flex-wrap gap-2">
                        <h2 class="h5 fw-semibold mb-0"><spring:message code="myCarDetail.recentReservations.title"/></h2>
                        <c:if test="${reservationPreviewTotal > 0}">
                            <c:url var="seeAllReservationsUrl" value="/my-cars/reservations/${car.id}"/>
                            <a href="<c:out value='${seeAllReservationsUrl}'/>" class="btn btn-outline-primary btn-sm">
                                <spring:message code="myCarDetail.recentReservations.seeAll"/>
                            </a>
                        </c:if>
                    </div>
                    <c:choose>
                        <c:when test="${not empty previewReservations}">
                            <div class="d-flex flex-column gap-3">
                                <c:forEach var="reservation" items="${previewReservations}">
                                    <c:url var="previewResDetailUrl" value="/my-reservations/${reservation.reservationId}">
                                        <c:param name="role" value="owner"/>
                                        <c:param name="fromCar" value="${car.id}"/>
                                    </c:url>
                                    <a href="<c:out value='${previewResDetailUrl}'/>" class="reservation-card text-decoration-none text-reset">
                                        <article class="card border-0 shadow-sm rounded-4 overflow-hidden reservation-card__surface position-relative">
                                            <span class="position-absolute top-0 end-0 m-2 z-1">
                                                <span class="badge ${reservation.statusKey eq 'accepted' ? 'bg-success' : fn:startsWith(reservation.statusKey, 'cancelled') ? 'bg-danger' : reservation.statusKey eq 'started' ? 'bg-info' : reservation.statusKey eq 'pending' ? 'bg-warning text-dark' : 'bg-secondary'}">
                                                    <spring:message code="enum.reservation.status.${reservation.statusKey}"/>
                                                </span>
                                            </span>
                                            <div class="row g-0 align-items-stretch">
                                                <div class="col-12 col-md-3 reservation-card__media-wrap">
                                                    <c:choose>
                                                        <c:when test="${reservation.imageId > 0}">
                                                            <c:url var="prevResImgUrl" value="/image/${reservation.imageId}"/>
                                                            <img src="<c:out value='${prevResImgUrl}'/>" alt="<c:out value='${reservation.brand} ${reservation.model}'/>" class="reservation-card__media">
                                                        </c:when>
                                                        <c:otherwise>
                                                            <div class="reservation-card__media reservation-card__media--placeholder d-flex align-items-center justify-content-center text-secondary">
                                                                <i class="bi bi-car-front fs-1" aria-hidden="true"></i>
                                                            </div>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                                <div class="col-12 col-md-9">
                                                    <div class="card-body p-3 p-md-4 h-100 d-flex flex-column justify-content-between gap-3">
                                                        <div>
                                                            <h3 class="h5 fw-semibold mb-1"><c:out value="${reservation.brand} ${reservation.model}"/></h3>
                                                        </div>
                                                        <div class="row g-3">
                                                            <div class="col-12 col-sm-6">
                                                                <p class="reservation-card__meta-label mb-1"><spring:message code="myReservations.card.pickup"/></p>
                                                                <p class="mb-0 fw-medium"><c:out value="${reservation.pickupDateTime}"/></p>
                                                            </div>
                                                            <div class="col-12 col-sm-6">
                                                                <p class="reservation-card__meta-label mb-1"><spring:message code="myReservations.card.return"/></p>
                                                                <p class="mb-0 fw-medium"><c:out value="${reservation.returnDateTime}"/></p>
                                                            </div>
                                                        </div>
                                                        <div class="pt-1">
                                                            <div class="reservation-price-compact">
                                                                <span class="reservation-card__meta-label mb-0"><spring:message code="myReservations.card.totalPrice"/></span>
                                                                <span class="h5 mb-0 fw-bold text-primary"><c:out value="${reservation.totalPrice}"/></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </article>
                                    </a>
                                </c:forEach>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <p class="text-secondary small mb-0"><spring:message code="myCarReservations.empty.description"/></p>
                        </c:otherwise>
                    </c:choose>
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

            </c:if><%-- end !carModelPendingValidation --%>

        </c:when>
    </c:choose>
</main>
<%@include file="../footer.jsp"%>
</body>
</html>
