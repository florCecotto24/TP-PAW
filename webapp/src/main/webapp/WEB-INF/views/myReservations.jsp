<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="myReservations.pageTitle"/></title>
    <%@include file="header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="myReservations.heading" var="myReservationsLabel"/>
    <ryden:breadcrumbTrail currentLabel="${myReservationsLabel}"/>
    <section class="reservation-management-header mt-4 pt-5 mb-4">
        <h1 class="h3 fw-bold mb-2"><spring:message code="myReservations.heading"/></h1>
        <p class="text-secondary mb-0"><spring:message code="myReservations.subheading"/></p>
    </section>

    <c:url var="reserveCarUrl" value="/search"/>
    <c:set var="hasActiveRiderFilters" value="${not empty param.q or not empty paramValues.riderStatus or not empty paramValues.category or not empty paramValues.transmission or not empty paramValues.powertrain or not empty param.priceMin or not empty param.priceMax or not empty paramValues.rating}"/>
    <c:url var="myReservationsRiderPaginationBaseUrl" value="/my-reservations">
        <c:forEach var="rs" items="${paramValues.riderStatus}">
            <c:param name="riderStatus"><c:out value="${rs}"/></c:param>
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

    <c:if test="${not empty riderReservations or hasActiveRiderFilters}">
        <c:set var="showRiderClear" value="${hasActiveRiderFilters or (not empty param.sort and param.sort ne 'date,desc')}"/>
        <form id="myReservationsRiderFilterForm" class="mb-3" method="get" action="${pageContext.request.contextPath}/my-reservations">
            <div class="d-flex justify-content-center mb-3">
                <div class="d-flex align-items-end gap-2 w-100" style="max-width:600px">
                    <div class="flex-grow-1">
                        <div class="input-group">
                            <span class="input-group-text bg-white border-end-0 text-secondary"><i class="bi bi-search"></i></span>
                            <input type="search" class="form-control border-start-0" id="myReservations_q" name="q" value="<c:out value='${param.q}'/>"
                                   placeholder="<spring:message code='myListings.filter.query.placeholder'/>"/>
                        </div>
                    </div>
                    <button type="submit" class="btn btn-primary"><spring:message code="myListings.filter.search"/></button>
                    <c:if test="${showRiderClear}">
                        <a href="${pageContext.request.contextPath}/my-reservations" class="btn btn-outline-secondary"><spring:message code="search.filters.clear"/></a>
                    </c:if>
                </div>
            </div>
            <div class="d-flex justify-content-center mb-3">
                <div class="d-flex flex-wrap align-items-center justify-content-center gap-0 pt-1">
                    <spring:message code="myReservations.filter.status" var="riderStatusLabel"/>
                    <ryden:exploreFilterDropdown filterLabel="${riderStatusLabel}" paramName="riderStatus" ariaGroup="rider-status" options="${reservationStatusOptions}"/>
                    <spring:message code="search.filter.category" var="riderCategoryLabel"/>
                    <ryden:exploreFilterDropdown filterLabel="${riderCategoryLabel}" paramName="category" ariaGroup="rider-category" options="${categoryFilterOptions}"/>
                    <spring:message code="search.filter.transmission" var="riderTransmissionLabel"/>
                    <ryden:exploreFilterDropdown filterLabel="${riderTransmissionLabel}" paramName="transmission" ariaGroup="rider-transmission" options="${transmissionFilterOptions}"/>
                    <spring:message code="search.filter.powertrain" var="riderPowertrainLabel"/>
                    <ryden:exploreFilterDropdown filterLabel="${riderPowertrainLabel}" paramName="powertrain" ariaGroup="rider-powertrain" options="${powertrainFilterOptions}"/>
                    <spring:message code="search.filter.price" var="riderPriceLabel"/>
                    <spring:message code="search.filter.price.min" var="riderPriceMinLabel"/>
                    <spring:message code="search.filter.price.max" var="riderPriceMaxLabel"/>
                    <c:set var="hasActiveRiderPrice" value="${not empty param.priceMin or not empty param.priceMax}"/>
                    <div class="dropdown explore-filter-dropdown mx-1 my-1">
                        <button class="btn btn-light border dropdown-toggle rounded-4 d-inline-flex align-items-center gap-1" type="button" data-bs-toggle="dropdown" data-bs-auto-close="outside">
                            <span class="explore-filter-dropdown__label"><c:out value="${riderPriceLabel}"/></span>
                            <span class="badge text-bg-primary rounded-pill <c:if test='${not hasActiveRiderPrice}'>d-none</c:if>" data-filter-count="true">1</span>
                        </button>
                        <div class="dropdown-menu p-3" style="min-width:200px">
                            <div class="mb-2">
                                <label class="form-label small mb-1"><c:out value="${riderPriceMinLabel}"/></label>
                                <input type="number" class="form-control form-control-sm" name="priceMin" min="0" step="1" value="<c:out value='${param.priceMin}'/>"/>
                            </div>
                            <div>
                                <label class="form-label small mb-1"><c:out value="${riderPriceMaxLabel}"/></label>
                                <input type="number" class="form-control form-control-sm" name="priceMax" min="0" step="1" value="<c:out value='${param.priceMax}'/>"/>
                            </div>
                        </div>
                    </div>
                    <spring:message code="search.filter.rating" var="riderRatingLabel"/>
                    <ryden:exploreFilterDropdown filterLabel="${riderRatingLabel}" paramName="rating" ariaGroup="rider-rating" options="${ratingFilterOptions}"/>
                </div>
            </div>
        </form>

        <div class="mb-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
            <h3 class="h6 mb-0">
                <c:choose>
                    <c:when test="${riderReservationsPage.totalItems > 0}">
                        <spring:message code="myReservations.resultsRange"
                                        arguments="${riderReservationsPage.firstItemNumber},${riderReservationsPage.lastItemNumber},${riderReservationsPage.totalItems}"/>
                    </c:when>
                    <c:otherwise>
                        <spring:message code="myReservations.resultsCount" arguments="0"/>
                    </c:otherwise>
                </c:choose>
            </h3>
            <ryden:sortBar baseUrl="${myReservationsRiderPaginationBaseUrl}" currentSort="${currentSort}"
                           wrapperClass="d-flex align-items-center gap-2 flex-wrap"/>
        </div>
    </c:if>

    <c:choose>
        <c:when test="${empty riderReservations}">
            <div class="search-empty-state text-center">
                <c:choose>
                    <c:when test="${hasActiveRiderFilters}">
                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myReservations.noResults.title"/></h2>
                        <div class="search-empty-state__actions mt-4">
                            <a href="${pageContext.request.contextPath}/my-reservations" class="btn btn-outline-secondary">
                                <spring:message code="search.filters.clear"/>
                            </a>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myReservations.empty.title"/></h2>
                        <p class="text-secondary mb-0 search-empty-state__text">
                            <spring:message code="myReservations.empty.description"/>
                        </p>
                        <div class="search-empty-state__actions mt-4">
                            <a href="<c:out value='${reserveCarUrl}'/>" class="btn btn-primary btn-action btn-action-md">
                                <spring:message code="myReservations.empty.reserve"/>
                            </a>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:when>
        <c:otherwise>
            <div class="d-flex flex-column gap-3 mb-4">
                <c:forEach var="reservation" items="${riderReservations}">
                    <c:url var="reservationDetailUrl" value="/my-reservations/${reservation.reservationId}"/>
                    <a href="<c:out value='${reservationDetailUrl}'/>" class="reservation-card text-decoration-none text-reset">
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
                                            <c:url var="reservationImgUrl" value="/image/${reservation.imageId}"/>
                                            <img src="<c:out value='${reservationImgUrl}'/>" alt="<c:out value='${reservation.brand} ${reservation.model}'/>" class="reservation-card__media">
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

            <ryden:pagination
                    currentPage="${riderReservationsPage.currentPage}"
                    totalPages="${riderReservationsPage.totalPages}"
                    baseUrl="${myReservationsRiderPaginationBaseUrl}"
                    pageParam="riderPage"
                    sortParam="${currentSort}"/>
        </c:otherwise>
    </c:choose>
</main>

<%@include file="footer.jsp"%>
</body>
</html>
