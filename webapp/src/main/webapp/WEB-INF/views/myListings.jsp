<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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

                <%-- Tab 1: Mis publicaciones --%>
                <div class="tab-pane fade ${selectedListingsTab eq 'listings' ? 'show active' : ''}" id="listings-pane" role="tabpanel" aria-labelledby="listings-tab">
                    <c:set var="hasActiveFilters" value="${not empty param.q or not empty paramValues.listingStatus or not empty paramValues.category or not empty paramValues.transmission or not empty paramValues.powertrain or not empty param.priceMin or not empty param.priceMax or not empty paramValues.rating}"/>

                    <c:url var="myListingsBaseUrl" value="/my-listings">
                        <c:param name="tab" value="listings"/>
                        <c:if test="${not empty param.q}">
                            <c:param name="q"><c:out value="${param.q}"/></c:param>
                        </c:if>
                        <c:forEach var="ls" items="${paramValues.listingStatus}">
                            <c:param name="listingStatus"><c:out value="${ls}"/></c:param>
                        </c:forEach>
                        <c:forEach var="cat" items="${paramValues.category}">
                            <c:param name="category"><c:out value="${cat}"/></c:param>
                        </c:forEach>
                        <c:forEach var="tr" items="${paramValues.transmission}">
                            <c:param name="transmission"><c:out value="${tr}"/></c:param>
                        </c:forEach>
                        <c:forEach var="pw" items="${paramValues.powertrain}">
                            <c:param name="powertrain"><c:out value="${pw}"/></c:param>
                        </c:forEach>
                        <c:if test="${not empty param.priceMin}">
                            <c:param name="priceMin"><c:out value="${param.priceMin}"/></c:param>
                        </c:if>
                        <c:if test="${not empty param.priceMax}">
                            <c:param name="priceMax"><c:out value="${param.priceMax}"/></c:param>
                        </c:if>
                        <c:forEach var="rt" items="${paramValues.rating}">
                            <c:param name="rating"><c:out value="${rt}"/></c:param>
                        </c:forEach>
                    </c:url>

                    <c:if test="${not empty results or hasActiveFilters}">
                        <form id="myListingsFilterForm" class="row g-2 align-items-end mb-4" method="get" action="${pageContext.request.contextPath}/my-listings">
                            <input type="hidden" name="tab" value="listings"/>
                            <div class="col-md-5 col-lg-4">
                                <label class="form-label small text-secondary mb-1" for="myListings_q"><spring:message code="myListings.filter.query"/></label>
                                <input type="search" class="form-control" id="myListings_q" name="q" value="<c:out value='${param.q}'/>"
                                       placeholder="<spring:message code='myListings.filter.query.placeholder'/>"/>
                            </div>
                            <div class="col-12">
                                <div class="d-flex flex-wrap align-items-center gap-0 pt-1">
                                    <spring:message code="myListings.filter.status" var="lstStatusLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${lstStatusLabel}" paramName="listingStatus" ariaGroup="lst-status" options="${listingStatusOptions}"/>
                                    <spring:message code="search.filter.category" var="lstCategoryLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${lstCategoryLabel}" paramName="category" ariaGroup="lst-category" options="${categoryFilterOptions}"/>
                                    <spring:message code="search.filter.transmission" var="lstTransmissionLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${lstTransmissionLabel}" paramName="transmission" ariaGroup="lst-transmission" options="${transmissionFilterOptions}"/>
                                    <spring:message code="search.filter.powertrain" var="lstPowertrainLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${lstPowertrainLabel}" paramName="powertrain" ariaGroup="lst-powertrain" options="${powertrainFilterOptions}"/>
                                    <spring:message code="search.filter.price" var="lstPriceLabel"/>
                                    <spring:message code="search.filter.price.min" var="lstPriceMinLabel"/>
                                    <spring:message code="search.filter.price.max" var="lstPriceMaxLabel"/>
                                    <c:set var="hasActiveLstPrice" value="${not empty param.priceMin or not empty param.priceMax}"/>
                                    <div class="dropdown explore-filter-dropdown mx-1 my-1">
                                        <button class="btn btn-light border dropdown-toggle rounded-4 d-inline-flex align-items-center gap-1" type="button" data-bs-toggle="dropdown" data-bs-auto-close="outside">
                                            <span class="explore-filter-dropdown__label"><c:out value="${lstPriceLabel}"/></span>
                                            <span class="badge text-bg-primary rounded-pill <c:if test='${not hasActiveLstPrice}'>d-none</c:if>" data-filter-count="true">1</span>
                                        </button>
                                        <div class="dropdown-menu p-3" style="min-width:200px">
                                            <div class="mb-2">
                                                <label class="form-label small mb-1"><c:out value="${lstPriceMinLabel}"/></label>
                                                <input type="number" class="form-control form-control-sm" name="priceMin" min="0" step="1" value="<c:out value='${param.priceMin}'/>"/>
                                            </div>
                                            <div>
                                                <label class="form-label small mb-1"><c:out value="${lstPriceMaxLabel}"/></label>
                                                <input type="number" class="form-control form-control-sm" name="priceMax" min="0" step="1" value="<c:out value='${param.priceMax}'/>"/>
                                            </div>
                                        </div>
                                    </div>
                                    <spring:message code="search.filter.rating" var="lstRatingLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${lstRatingLabel}" paramName="rating" ariaGroup="lst-rating" options="${ratingFilterOptions}"/>
                                </div>
                            </div>
                            <div class="col-auto d-flex flex-wrap gap-2">
                                <button type="submit" class="btn btn-primary"><spring:message code="myListings.filter.search"/></button>
                                <a href="${pageContext.request.contextPath}/my-listings" class="btn btn-outline-secondary"><spring:message code="search.filters.clear"/></a>
                            </div>
                        </form>

                        <ryden:sortBar baseUrl="${myListingsBaseUrl}" currentSort="${listingsCurrentSort}"/>

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
                            <div class="search-empty-state text-center">
                                <c:choose>
                                    <c:when test="${hasActiveFilters}">
                                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myListings.noResults.title"/></h2>
                                        <div class="search-empty-state__actions mt-4">
                                            <a href="${pageContext.request.contextPath}/my-listings?tab=listings" class="btn btn-outline-secondary">
                                                <spring:message code="search.filters.clear"/>
                                            </a>
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myListings.empty.title"/></h2>
                                        <p class="text-secondary mb-0 search-empty-state__text">
                                            <spring:message code="myListings.empty.description"/>
                                        </p>
                                        <div class="search-empty-state__actions mt-4">
                                            <a href="${pageContext.request.contextPath}/publish-car" class="btn btn-primary btn-action btn-action-md">
                                                <spring:message code="home.cta.button"/>
                                            </a>
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </div>
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
                                                    <c:when test="${car.statusKey == 'PAUSED'}">
                                                        <span class="position-absolute top-0 end-0 m-3" style="background-color:#e4960b; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                            <spring:message code="enum.listing.status.PAUSED"/>
                                                        </span>
                                                    </c:when>
                                                    <c:when test="${car.statusKey == 'FINISHED'}">
                                                        <span class="position-absolute top-0 end-0 m-3" style="background-color:#6c757d; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                            <spring:message code="enum.listing.status.FINISHED"/>
                                                        </span>
                                                    </c:when>
                                                    <c:when test="${car.statusKey == 'PAUSED_DUE_TO_LACK_OF_CBU'}">
                                                        <span class="position-absolute top-0 end-0 m-3" style="background-color:#b91c1c; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                            <spring:message code="enum.listing.status.PAUSED_DUE_TO_LACK_OF_CBU"/>
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
                                    baseUrl="${myListingsBaseUrl}"
                                    sortParam="${listingsCurrentSort}"/>
                        </c:otherwise>
                    </c:choose>
                </div>

                <%-- Tab 2: Reservas de mis autos --%>
                <div class="tab-pane fade ${selectedListingsTab eq 'reservations' ? 'show active' : ''}" id="reservations-pane" role="tabpanel" aria-labelledby="reservations-tab">
                    <c:set var="hasActiveOwnerFilters" value="${not empty paramValues.ownerStatus or not empty paramValues.ownerCategory or not empty paramValues.ownerTransmission or not empty paramValues.ownerPowertrain or not empty param.ownerPriceMin or not empty param.ownerPriceMax or not empty paramValues.ownerRating}"/>
                    <c:url var="myListingsOwnerResPaginationBaseUrl" value="/my-listings">
                        <c:param name="tab" value="reservations"/>
                        <c:forEach var="rs" items="${paramValues.ownerStatus}">
                            <c:param name="ownerStatus"><c:out value="${rs}"/></c:param>
                        </c:forEach>
                        <c:forEach var="cat" items="${paramValues.ownerCategory}">
                            <c:param name="ownerCategory"><c:out value="${cat}"/></c:param>
                        </c:forEach>
                        <c:forEach var="tr" items="${paramValues.ownerTransmission}">
                            <c:param name="ownerTransmission"><c:out value="${tr}"/></c:param>
                        </c:forEach>
                        <c:forEach var="pw" items="${paramValues.ownerPowertrain}">
                            <c:param name="ownerPowertrain"><c:out value="${pw}"/></c:param>
                        </c:forEach>
                        <c:if test="${not empty param.ownerPriceMin}">
                            <c:param name="ownerPriceMin"><c:out value="${param.ownerPriceMin}"/></c:param>
                        </c:if>
                        <c:if test="${not empty param.ownerPriceMax}">
                            <c:param name="ownerPriceMax"><c:out value="${param.ownerPriceMax}"/></c:param>
                        </c:if>
                        <c:forEach var="rt" items="${paramValues.ownerRating}">
                            <c:param name="ownerRating"><c:out value="${rt}"/></c:param>
                        </c:forEach>
                    </c:url>

                    <c:if test="${not empty ownerReservations or hasActiveOwnerFilters}">
                        <form id="myListingsOwnerResFilterForm" class="row g-2 align-items-end mb-3" method="get" action="${pageContext.request.contextPath}/my-listings">
                            <input type="hidden" name="tab" value="reservations"/>
                            <div class="col-12">
                                <div class="d-flex flex-wrap align-items-center gap-0 pt-1">
                                    <spring:message code="myReservations.filter.status" var="ownStatusLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${ownStatusLabel}" paramName="ownerStatus" ariaGroup="own-status" options="${reservationStatusOptions}"/>
                                    <spring:message code="search.filter.category" var="ownCategoryLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${ownCategoryLabel}" paramName="ownerCategory" ariaGroup="own-category" options="${categoryFilterOptions}"/>
                                    <spring:message code="search.filter.transmission" var="ownTransmissionLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${ownTransmissionLabel}" paramName="ownerTransmission" ariaGroup="own-transmission" options="${transmissionFilterOptions}"/>
                                    <spring:message code="search.filter.powertrain" var="ownPowertrainLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${ownPowertrainLabel}" paramName="ownerPowertrain" ariaGroup="own-powertrain" options="${powertrainFilterOptions}"/>
                                    <spring:message code="search.filter.price" var="ownPriceLabel"/>
                                    <spring:message code="search.filter.price.min" var="ownPriceMinLabel"/>
                                    <spring:message code="search.filter.price.max" var="ownPriceMaxLabel"/>
                                    <c:set var="hasActiveOwnPrice" value="${not empty param.ownerPriceMin or not empty param.ownerPriceMax}"/>
                                    <div class="dropdown explore-filter-dropdown mx-1 my-1">
                                        <button class="btn btn-light border dropdown-toggle rounded-4 d-inline-flex align-items-center gap-1" type="button" data-bs-toggle="dropdown" data-bs-auto-close="outside">
                                            <span class="explore-filter-dropdown__label"><c:out value="${ownPriceLabel}"/></span>
                                            <span class="badge text-bg-primary rounded-pill <c:if test='${not hasActiveOwnPrice}'>d-none</c:if>" data-filter-count="true">1</span>
                                        </button>
                                        <div class="dropdown-menu p-3" style="min-width:200px">
                                            <div class="mb-2">
                                                <label class="form-label small mb-1"><c:out value="${ownPriceMinLabel}"/></label>
                                                <input type="number" class="form-control form-control-sm" name="ownerPriceMin" min="0" step="1" value="<c:out value='${param.ownerPriceMin}'/>"/>
                                            </div>
                                            <div>
                                                <label class="form-label small mb-1"><c:out value="${ownPriceMaxLabel}"/></label>
                                                <input type="number" class="form-control form-control-sm" name="ownerPriceMax" min="0" step="1" value="<c:out value='${param.ownerPriceMax}'/>"/>
                                            </div>
                                        </div>
                                    </div>
                                    <spring:message code="search.filter.rating" var="ownRatingLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${ownRatingLabel}" paramName="ownerRating" ariaGroup="own-rating" options="${ratingFilterOptions}"/>
                                </div>
                            </div>
                            <div class="col-auto d-flex flex-wrap gap-2">
                                <button type="submit" class="btn btn-primary"><spring:message code="myListings.filter.search"/></button>
                                <a href="${pageContext.request.contextPath}/my-listings?tab=reservations" class="btn btn-outline-secondary"><spring:message code="search.filters.clear"/></a>
                            </div>
                        </form>

                        <ryden:sortBar baseUrl="${myListingsOwnerResPaginationBaseUrl}" currentSort="${ownerCurrentSort}"
                                       sortParamName="ownerSort" pageParamName="ownerPage"/>
                    </c:if>

                    <c:if test="${not empty ownerReservations or hasActiveOwnerFilters}">
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
                            <div class="search-empty-state text-center">
                                <c:choose>
                                    <c:when test="${hasActiveOwnerFilters}">
                                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myReservations.noResults.title"/></h2>
                                        <div class="search-empty-state__actions mt-4">
                                            <a href="${pageContext.request.contextPath}/my-listings?tab=reservations" class="btn btn-outline-secondary">
                                                <spring:message code="search.filters.clear"/>
                                            </a>
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myReservations.ownerEmpty.title"/></h2>
                                        <p class="text-secondary mb-0 search-empty-state__text">
                                            <spring:message code="myReservations.ownerEmpty.description"/>
                                        </p>
                                    </c:otherwise>
                                </c:choose>
                            </div>
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
                                                <span class="badge ${reservation.statusKey eq 'accepted' ? 'bg-success' : reservation.statusKey eq 'cancelled' ? 'bg-danger' : reservation.statusKey eq 'started' ? 'bg-info' : reservation.statusKey eq 'pending' ? 'bg-warning text-dark' : 'bg-secondary'}">
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
                                    pageParam="ownerPage"
                                    sortParam="${ownerCurrentSort}"
                                    sortParamName="ownerSort"/>
                        </c:otherwise>
                    </c:choose>
                </div>

            </div>
        </div>


        <%@include file="footer.jsp"%>
    </body>
</html>
