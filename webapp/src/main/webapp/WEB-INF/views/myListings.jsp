<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <title><spring:message code="myListings.pageTitle"/></title>
        <%@include file="header.jsp"%>
    </head>
    <body class="has-fixed-navbar">
        <ryden:navbar/>
        <div class="container pt-5 pb-4">
            <spring:message code="myListings.heading" var="myListingsLabel"/>
            <ryden:breadcrumbTrail currentLabel="${myListingsLabel}"/>
            <section class="reservation-management-header mt-4 pt-5 mb-4">
                <h1 class="h3 fw-bold mb-2"><spring:message code="myListings.heading"/></h1>
                <p class="text-secondary mb-0"><spring:message code="myListings.subheading"/></p>
            </section>

            <ul class="nav nav-tabs mb-4" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link ${selectedListingsTab eq 'listings' ? 'active' : ''}" id="listings-tab" data-bs-toggle="tab" data-bs-target="#listings-pane" type="button" role="tab" aria-controls="listings-pane" aria-selected="${selectedListingsTab eq 'listings' ? 'true' : 'false'}">
                        <spring:message code="myListings.tab.listings"/>
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link ${selectedListingsTab eq 'reservations' ? 'active' : ''}" id="reservations-tab" data-bs-toggle="tab" data-bs-target="#reservations-pane" type="button" role="tab" aria-controls="reservations-pane" aria-selected="${selectedListingsTab eq 'reservations' ? 'true' : 'false'}">
                        <spring:message code="myListings.tab.reservations"/>
                    </button>
                </li>
            </ul>

            <div class="tab-content">

                <%-- Tab 1: my listings --%>
                <div class="tab-pane fade ${selectedListingsTab eq 'listings' ? 'show active' : ''}" id="listings-pane" role="tabpanel" aria-labelledby="listings-tab">
                    <c:set var="hasActiveFilters" value="${not empty param.q or not empty param.listingStatus}"/>

                    <c:url var="myListingsBaseUrl" value="/my-listings">
                        <c:if test="${not empty param.q}">
                            <c:param name="q"><c:out value="${param.q}"/></c:param>
                        </c:if>
                        <c:if test="${not empty ownerListingStatusFilter}">
                            <c:param name="listingStatus"><c:out value="${ownerListingStatusFilter}"/></c:param>
                        </c:if>
                    </c:url>

                    <c:if test="${not empty results or hasActiveFilters}">
                        <spring:message code="validation.dropdown.invalid" var="myListingsDropdownInvalid" htmlEscape="true"/>
                        <form id="myListingsFilterForm" class="row g-2 align-items-end mb-4" method="get" action="${pageContext.request.contextPath}/my-listings"
                              data-ryden-dropdown-invalid="<c:out value='${myListingsDropdownInvalid}'/>">
                            <input type="hidden" name="tab" value="listings"/>
                            <div class="col-md-5 col-lg-4">
                                <label class="form-label small text-secondary mb-1" for="myListings_q"><spring:message code="myListings.filter.query"/></label>
                                <input type="search" class="form-control" id="myListings_q" name="q" value="<c:out value='${param.q}'/>"
                                       placeholder="<spring:message code='myListings.filter.query.placeholder'/>"/>
                            </div>
                            <div class="col-md-4 col-lg-3">
                                <label class="form-label small text-secondary mb-1" for="myListings_status"><spring:message code="myListings.filter.status"/></label>
                                <select class="form-select" id="myListings_status" name="listingStatus">
                                    <option value="" ${empty ownerListingStatusFilter ? 'selected="selected"' : ''}><spring:message code="myListings.filter.status.any"/></option>
                                    <option value="active" ${ownerListingStatusFilter eq 'active' ? 'selected="selected"' : ''}><spring:message code="enum.listing.status.ACTIVE"/></option>
                                    <option value="paused" ${ownerListingStatusFilter eq 'paused' ? 'selected="selected"' : ''}><spring:message code="enum.listing.status.PAUSED"/></option>
                                    <option value="finished" ${ownerListingStatusFilter eq 'finished' ? 'selected="selected"' : ''}><spring:message code="enum.listing.status.FINISHED"/></option>
                                    <option value="paused_due_to_lack_of_cbu" ${ownerListingStatusFilter eq 'paused_due_to_lack_of_cbu' ? 'selected="selected"' : ''}><spring:message code="enum.listing.status.PAUSED_DUE_TO_LACK_OF_CBU"/></option>
                                </select>
                            </div>
                            <div class="col-auto d-flex flex-wrap gap-2">
                                <button type="submit" class="btn btn-primary"><spring:message code="myListings.filter.search"/></button>
                                <a href="${pageContext.request.contextPath}/my-listings" class="btn btn-outline-secondary"><spring:message code="search.filters.clear"/></a>
                            </div>
                        </form>

                        <div class="mb-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
                            <h2 class="h5 mb-0">
                                <c:choose>
                                    <c:when test="${myListingsPage.totalItems > 0}">
                                        <spring:message code="myListings.resultsRange"
                                                        arguments="${myListingsPage.firstItemNumber},${myListingsPage.lastItemNumber},${myListingsPage.totalItems}"/>
                                    </c:when>
                                    <c:otherwise>
                                        <spring:message code="myListings.resultsCount" arguments="0"/>
                                    </c:otherwise>
                                </c:choose>
                            </h2>
                        </div>
                    </c:if>

                    <c:choose>
                        <c:when test="${empty results}">
                            <c:choose>
                                <c:when test="${hasActiveFilters}">
                                    <div class="search-empty-state text-center">
                                        <h2 class="h4 fw-semibold mb-2">
                                            <spring:message code="myListings.noResults.title"/>
                                        </h2>
                                        <div class="search-empty-state__actions mt-4">
                                            <a href="${pageContext.request.contextPath}/my-listings" class="btn btn-outline-secondary">
                                                <spring:message code="search.filters.clear"/>
                                            </a>
                                        </div>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="search-empty-state text-center">
                                        <img src="${pageContext.request.contextPath}/assets/images/filmore-cars.png"
                                             alt="" class="mb-4 img-fluid" style="max-width:260px"/>
                                        <h2 class="h4 fw-semibold mb-2">
                                            <spring:message code="myListings.empty.title"/>
                                        </h2>
                                        <div class="search-empty-state__actions mt-4">
                                            <a href="${pageContext.request.contextPath}/publish" class="btn btn-primary">
                                                <spring:message code="myListings.empty.publishButton"/>
                                            </a>
                                        </div>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </c:when>
                        <c:otherwise>
                            <div class="d-flex flex-column gap-3">
                                <c:forEach var="car" items="${results}">
                                    <c:url var="listingDetailUrl" value="/my-listings/${car.listingId}"/>
                                    <a href="<c:out value='${listingDetailUrl}'/>" class="reservation-card text-decoration-none text-reset">
                                        <article class="card border-0 shadow-sm rounded-4 overflow-hidden reservation-card__surface position-relative">
                                            <c:if test="${not empty car.statusKey}">
                                                <c:choose>
                                                    <c:when test="${car.statusKey == 'ACTIVE'}">
                                                        <span class="position-absolute top-0 end-0 m-3" style="background-color:#198754; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                            <spring:message code="enum.listing.status.ACTIVE"/>
                                                        </span>
                                                    </c:when>
                                                    <c:when test="${car.statusKey == 'PAUSED' || car.statusKey == 'PAUSED_DUE_TO_LACK_OF_CBU'}">
                                                        <span class="position-absolute top-0 end-0 m-3" style="background-color:#e4960b; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                            <spring:message code="enum.listing.status.${car.statusKey}"/>
                                                        </span>
                                                    </c:when>
                                                    <c:when test="${car.statusKey == 'FINISHED'}">
                                                        <span class="position-absolute top-0 end-0 m-3" style="background-color:#6c757d; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                            <spring:message code="enum.listing.status.FINISHED"/>
                                                        </span>
                                                    </c:when>
                                                </c:choose>
                                            </c:if>
                                            <div class="row g-0 align-items-stretch">
                                                <div class="col-12 col-md-3 reservation-card__media-wrap">
                                                    <c:choose>
                                                        <c:when test="${car.imageId > 0}">
                                                            <c:url var="listingImgUrl" value="/image/${car.imageId}"/>
                                                            <img src="<c:out value='${listingImgUrl}'/>" alt="<c:out value='${car.brand} ${car.model}'/>" class="reservation-card__media">
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
                                                            <h3 class="h5 fw-semibold mb-1"><c:out value="${car.brand} ${car.model}"/></h3>
                                                            <c:if test="${not empty car.ratingAvg}">
                                                                <p class="small text-secondary mb-0">
                                                                    <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
                                                                    <span class="fw-semibold text-dark"><fmt:formatNumber value="${car.ratingAvg}" maxFractionDigits="1" minFractionDigits="1"/></span>
                                                                </p>
                                                            </c:if>
                                                        </div>
                                                        <div class="pt-1 d-flex align-items-center justify-content-between gap-2 flex-wrap">
                                                            <div class="reservation-price-compact">
                                                                <span class="reservation-card__meta-label mb-0"><spring:message code="myListings.card.pricePerDay"/></span>
                                                                <span class="h5 mb-0 fw-bold text-primary">$<c:out value="${car.price}"/></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </article>
                                    </a>
                                </c:forEach>
                            </div>

                            <ryden:pagination
                                    currentPage="${myListingsPage.currentPage}"
                                    totalPages="${myListingsPage.totalPages}"
                                    baseUrl="${myListingsBaseUrl}"/>
                        </c:otherwise>
                    </c:choose>
                </div>

                <%-- Tab 2: reservations for my cars --%>
                <div class="tab-pane fade ${selectedListingsTab eq 'reservations' ? 'show active' : ''}" id="reservations-pane" role="tabpanel" aria-labelledby="reservations-tab">
                    <c:url var="myListingsOwnerResPaginationBaseUrl" value="/my-listings">
                        <c:param name="tab" value="reservations"/>
                        <c:if test="${not empty ownerStatusFilter}">
                            <c:param name="ownerStatus" value="${ownerStatusFilter}"/>
                        </c:if>
                    </c:url>

                    <c:if test="${not empty ownerReservations or not empty ownerStatusFilter}">
                        <spring:message code="validation.dropdown.invalid" var="myListingsOwnerResDropdownInvalid" htmlEscape="true"/>
                        <form id="myListingsOwnerResFilterForm" class="row g-2 align-items-end mb-3" method="get" action="${pageContext.request.contextPath}/my-listings"
                              data-ryden-dropdown-invalid="<c:out value='${myListingsOwnerResDropdownInvalid}'/>">
                            <input type="hidden" name="tab" value="reservations"/>
                            <div class="col-md-5 col-lg-4">
                                <label class="form-label small text-secondary mb-1" for="owner_res_status"><spring:message code="myReservations.filter.status"/></label>
                                <select class="form-select" id="owner_res_status" name="ownerStatus">
                                    <option value="" ${empty ownerStatusFilter ? 'selected="selected"' : ''}><spring:message code="myReservations.filter.status.any"/></option>
                                    <option value="pending" ${ownerStatusFilter eq 'pending' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.pending"/></option>
                                    <option value="accepted" ${ownerStatusFilter eq 'accepted' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.accepted"/></option>
                                    <option value="started" ${ownerStatusFilter eq 'started' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.started"/></option>
                                    <option value="cancelled" ${ownerStatusFilter eq 'cancelled' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.cancelled"/></option>
                                    <option value="finished" ${ownerStatusFilter eq 'finished' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.finished"/></option>
                                </select>
                            </div>
                            <div class="col-auto d-flex flex-wrap gap-2">
                                <button type="submit" class="btn btn-primary"><spring:message code="myListings.filter.search"/></button>
                                <a href="${pageContext.request.contextPath}/my-listings?tab=reservations" class="btn btn-outline-secondary"><spring:message code="search.filters.clear"/></a>
                            </div>
                        </form>

                        <div class="mb-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
                            <h3 class="h6 mb-0">
                                <c:choose>
                                    <c:when test="${ownerReservationsPage.totalItems > 0}">
                                        <spring:message code="myReservations.ownerResultsRange"
                                                        arguments="${ownerReservationsPage.firstItemNumber},${ownerReservationsPage.lastItemNumber},${ownerReservationsPage.totalItems}"/>
                                    </c:when>
                                    <c:otherwise>
                                        <spring:message code="myReservations.ownerResultsCount" arguments="0"/>
                                    </c:otherwise>
                                </c:choose>
                            </h3>
                        </div>
                    </c:if>

                    <c:choose>
                        <c:when test="${empty ownerReservations}">
                            <c:choose>
                                <c:when test="${not empty ownerStatusFilter}">
                                    <div class="search-empty-state text-center">
                                        <h2 class="h4 fw-semibold mb-2">
                                            <spring:message code="myReservations.ownerNoResults.title"/>
                                        </h2>
                                        <div class="search-empty-state__actions mt-4">
                                            <a href="${pageContext.request.contextPath}/my-listings?tab=reservations" class="btn btn-outline-secondary">
                                                <spring:message code="search.filters.clear"/>
                                            </a>
                                        </div>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="search-empty-state text-center">
                                        <img src="${pageContext.request.contextPath}/assets/images/filmore-cars.png"
                                             alt="" class="mb-4 img-fluid" style="max-width:260px"/>
                                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myReservations.ownerEmpty.title"/></h2>
                                        <p class="text-secondary mb-0 search-empty-state__text">
                                            <spring:message code="myReservations.ownerEmpty.description"/>
                                        </p>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </c:when>
                        <c:otherwise>
                            <div class="d-flex flex-column gap-3 mb-4">
                                <c:forEach var="reservation" items="${ownerReservations}">
                                    <c:url var="ownerReservationDetailUrl" value="/my-reservations/${reservation.reservationId}">
                                        <c:param name="role" value="owner"/>
                                    </c:url>
                                    <a href="<c:out value='${ownerReservationDetailUrl}'/>" class="reservation-card text-decoration-none text-reset">
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
                                                            <c:url var="ownerResImgUrl" value="/image/${reservation.imageId}"/>
                                                            <img src="<c:out value='${ownerResImgUrl}'/>" alt="<c:out value='${reservation.brand} ${reservation.model}'/>" class="reservation-card__media">
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
                                                        <div class="d-flex flex-wrap align-items-start gap-2">
                                                            <div>
                                                                <h3 class="h5 fw-semibold mb-1"><c:out value="${reservation.brand} ${reservation.model}"/></h3>
                                                            </div>
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
                                                                <span class="h5 mb-0 fw-bold text-primary">$<c:out value="${reservation.totalPrice}"/></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </article>
                                    </a>
                                </c:forEach>
                            </div>

                            <ryden:pagination
                                    currentPage="${ownerReservationsPage.currentPage}"
                                    totalPages="${ownerReservationsPage.totalPages}"
                                    baseUrl="${myListingsOwnerResPaginationBaseUrl}"
                                    pageParam="ownerPage"/>
                        </c:otherwise>
                    </c:choose>
                </div>

            </div>
        </div>

        <script>
            (function () {
                var allowed = ['', 'active', 'paused', 'finished'];
                var form = document.getElementById('myListingsFilterForm');
                var sel = document.getElementById('myListings_status');
                if (form && sel) {
                    var msg = form.getAttribute('data-ryden-dropdown-invalid') || '';
                    form.addEventListener('submit', function (ev) {
                        var v = (sel.value || '').trim().toLowerCase();
                        if (allowed.indexOf(v) < 0) {
                            ev.preventDefault();
                            sel.setCustomValidity(msg);
                            sel.reportValidity();
                            return;
                        }
                        sel.setCustomValidity('');
                    });
                }

                var allowedRes = ['', 'pending', 'accepted', 'started', 'cancelled', 'finished'];
                var formRes = document.getElementById('myListingsOwnerResFilterForm');
                var selRes = document.getElementById('owner_res_status');
                if (formRes && selRes) {
                    var msgRes = formRes.getAttribute('data-ryden-dropdown-invalid') || '';
                    formRes.addEventListener('submit', function (ev) {
                        var v = (selRes.value || '').trim().toLowerCase();
                        if (allowedRes.indexOf(v) < 0) {
                            ev.preventDefault();
                            selRes.setCustomValidity(msgRes);
                            selRes.reportValidity();
                            return;
                        }
                        selRes.setCustomValidity('');
                    });
                }
            })();
        </script>

        <%@include file="footer.jsp"%>
    </body>
</html>
