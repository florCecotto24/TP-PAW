<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="ownerReservations.pageTitle"/></title>
    <%@include file="../header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="navbar.myListings" var="myCarsLabel"/>
    <spring:message code="ownerReservations.heading" var="ownerReservationsHeading"/>
    <c:choose>
        <c:when test="${not empty selectedCar}">
            <%-- 3-level breadcrumb: My Cars > Reservations for my cars > Brand Model --%>
            <ryden:breadcrumbTrail
                    homeLabel="${myCarsLabel}"
                    homeHref="${pageContext.request.contextPath}/my-cars"
                    midLabel="${ownerReservationsHeading}"
                    midHref="${pageContext.request.contextPath}/my-cars/reservations"
                    currentLabel="${selectedCar.brand} ${selectedCar.model}"/>
        </c:when>
        <c:otherwise>
            <%-- 2-level breadcrumb: My Cars > Reservations for my cars --%>
            <ryden:breadcrumbTrail
                    homeLabel="${myCarsLabel}"
                    homeHref="${pageContext.request.contextPath}/my-cars"
                    currentLabel="${ownerReservationsHeading}"/>
        </c:otherwise>
    </c:choose>

    <section class="reservation-management-header mt-4 pt-5 mb-4">
        <h1 class="h3 fw-bold mb-2"><spring:message code="ownerReservations.heading"/></h1>
        <c:choose>
            <c:when test="${not empty selectedCar}">
                <p class="text-secondary mb-0"><c:out value="${selectedCar.brand} ${selectedCar.model}"/></p>
            </c:when>
            <c:otherwise>
                <p class="text-secondary mb-0"><spring:message code="ownerReservations.subheading"/></p>
            </c:otherwise>
        </c:choose>
    </section>

    <%-- ====== UNIFIED FILTER FORM (identical for both /reservations and /reservations/{carId}) ====== --%>

    <c:choose>
        <c:when test="${not empty selectedCar}">
            <%-- category / transmission / powertrain are not shown on the car-specific view --%>
            <c:set var="hasActiveOwnerFilters" value="${not empty paramValues.ownerStatus or not empty param.ownerPriceMin or not empty param.ownerPriceMax or not empty paramValues.ownerRating}"/>
        </c:when>
        <c:otherwise>
            <c:set var="hasActiveOwnerFilters" value="${not empty param.ownerQ or not empty paramValues.ownerStatus or not empty paramValues.ownerCategory or not empty paramValues.ownerTransmission or not empty paramValues.ownerPowertrain or not empty param.ownerPriceMin or not empty param.ownerPriceMax or not empty paramValues.ownerRating}"/>
        </c:otherwise>
    </c:choose>

    <%-- Base URL for pagination (preserves all active filters, no page param) --%>
    <c:choose>
        <c:when test="${not empty selectedCar}">
            <c:url var="ownerResPaginationBaseUrl" value="/my-cars/reservations/${selectedCar.id}">
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
            <c:url var="ownerResClearUrl" value="/my-cars/reservations/${selectedCar.id}"/>
            <c:url var="ownerResFormAction" value="/my-cars/reservations/${selectedCar.id}"/>
        </c:when>
        <c:otherwise>
            <c:url var="ownerResPaginationBaseUrl" value="/my-cars/reservations">
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
            <c:url var="ownerResClearUrl" value="/my-cars/reservations"/>
            <c:url var="ownerResFormAction" value="/my-cars/reservations"/>
        </c:otherwise>
    </c:choose>

    <c:if test="${not empty ownerReservations or hasActiveOwnerFilters}">
        <c:set var="showOwnClear" value="${hasActiveOwnerFilters or (not empty param.ownerSort and param.ownerSort ne 'date,desc')}"/>
        <form id="ownerResAllFilterForm" class="mb-3" method="get" action="<c:out value='${ownerResFormAction}' escapeXml='false'/>">
            <c:if test="${empty selectedCar}">
                <%-- Text search only shown on all-cars view --%>
                <div class="d-flex justify-content-center mb-3">
                    <div class="d-flex align-items-center gap-2 w-100" style="max-width:600px">
                        <div class="d-flex align-items-center ryden-search-pill rounded-4 px-3 py-1 flex-grow-1 gap-2">
                            <i class="bi bi-search text-secondary flex-shrink-0" aria-hidden="true"></i>
                            <input type="search" class="form-control" id="ownerRes_q" name="ownerQ" value="<c:out value='${param.ownerQ}'/>"
                                   placeholder="<spring:message code='myListings.filter.query.placeholder'/>"/>
                        </div>
                        <button type="submit" class="btn btn-primary rounded-3 flex-shrink-0"><spring:message code="myListings.filter.search"/></button>
                        <c:if test="${showOwnClear}">
                            <a href="<c:out value='${ownerResClearUrl}' escapeXml='false'/>" class="btn btn-outline-secondary flex-shrink-0">
                                <spring:message code="search.filters.clear"/>
                            </a>
                        </c:if>
                    </div>
                </div>
            </c:if>
            <div class="d-flex justify-content-center mb-3">
                <div class="d-flex flex-wrap align-items-center justify-content-center gap-0 pt-1">
                    <spring:message code="myReservations.filter.status" var="ownStatusLabel"/>
                    <ryden:exploreFilterDropdown filterLabel="${ownStatusLabel}" paramName="ownerStatus" ariaGroup="own-status" options="${reservationStatusOptions}"/>
                    <c:if test="${empty selectedCar}">
                        <%-- Vehicle-level filters: irrelevant when already scoped to a single car --%>
                        <spring:message code="search.filter.category" var="ownCategoryLabel"/>
                        <ryden:exploreFilterDropdown filterLabel="${ownCategoryLabel}" paramName="ownerCategory" ariaGroup="own-category" options="${categoryFilterOptions}"/>
                        <spring:message code="search.filter.transmission" var="ownTransmissionLabel"/>
                        <ryden:exploreFilterDropdown filterLabel="${ownTransmissionLabel}" paramName="ownerTransmission" ariaGroup="own-transmission" options="${transmissionFilterOptions}"/>
                        <spring:message code="search.filter.powertrain" var="ownPowertrainLabel"/>
                        <ryden:exploreFilterDropdown filterLabel="${ownPowertrainLabel}" paramName="ownerPowertrain" ariaGroup="own-powertrain" options="${powertrainFilterOptions}"/>
                    </c:if>
                    <spring:message code="search.filter.price" var="ownPriceLabel"/>
                    <spring:message code="search.filter.price.min" var="ownPriceMinLabel"/>
                    <spring:message code="search.filter.price.max" var="ownPriceMaxLabel"/>
                    <c:set var="hasActiveOwnPrice" value="${not empty param.ownerPriceMin or not empty param.ownerPriceMax}"/>
                    <div class="dropdown explore-filter-dropdown mx-1 my-1">
                        <button class="btn btn-light border dropdown-toggle rounded-4 d-inline-flex align-items-center gap-1" type="button"
                                data-bs-toggle="dropdown" data-bs-auto-close="outside">
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
                    <%-- On the car-specific view the search row is hidden, so put Search/Clear here --%>
                    <c:if test="${not empty selectedCar}">
                        <button type="submit" class="btn btn-primary rounded-4 mx-1 my-1 flex-shrink-0">
                            <spring:message code="myListings.filter.search"/>
                        </button>
                        <c:if test="${showOwnClear}">
                            <a href="<c:out value='${ownerResClearUrl}' escapeXml='false'/>" class="btn btn-outline-secondary rounded-4 mx-1 my-1 flex-shrink-0">
                                <spring:message code="search.filters.clear"/>
                            </a>
                        </c:if>
                    </c:if>
                </div>
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
            <ryden:sortBar baseUrl="${ownerResPaginationBaseUrl}" currentSort="${ownerCurrentSort}"
                           sortParamName="ownerSort" pageParamName="page"
                           wrapperClass="d-flex align-items-center gap-2 flex-wrap"/>
        </div>
    </c:if>

    <c:choose>
        <c:when test="${empty ownerReservations}">
            <div class="search-empty-state text-center">
                <c:choose>
                    <c:when test="${hasActiveOwnerFilters}">
                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myReservations.noResults.title"/></h2>
                        <div class="search-empty-state__actions mt-4">
                            <a href="<c:out value='${ownerResClearUrl}' escapeXml='false'/>" class="btn btn-outline-secondary">
                                <spring:message code="search.filters.clear"/>
                            </a>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:choose>
                            <c:when test="${not empty selectedCar}">
                                <img src="${pageContext.request.contextPath}/assets/images/filmore-cars.png"
                                     alt="" class="mb-4 img-fluid" style="max-width:260px"/>
                                <h2 class="h4 fw-semibold mb-2"><spring:message code="myCarReservations.empty.title"/></h2>
                                <p class="text-secondary mb-0 search-empty-state__text">
                                    <spring:message code="myCarReservations.empty.description"/>
                                </p>
                            </c:when>
                            <c:otherwise>
                                <h2 class="h4 fw-semibold mb-2"><spring:message code="myReservations.ownerEmpty.title"/></h2>
                                <p class="text-secondary mb-0 search-empty-state__text">
                                    <spring:message code="myReservations.ownerEmpty.description"/>
                                </p>
                            </c:otherwise>
                        </c:choose>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:when>
        <c:otherwise>
            <div class="d-flex flex-column gap-3 mb-4">
                <c:forEach var="reservation" items="${ownerReservations}">
                    <c:url var="ownerResDetailUrl" value="/my-reservations/${reservation.reservationId}">
                        <c:param name="role" value="owner"/>
                        <c:param name="fromCar" value="${reservation.carId}"/>
                    </c:url>
                    <a href="<c:out value='${ownerResDetailUrl}'/>" class="reservation-card text-decoration-none text-reset">
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
            <ryden:pagination
                    currentPage="${ownerReservationsPage.currentPage}"
                    totalPages="${ownerReservationsPage.totalPages}"
                    baseUrl="${ownerResPaginationBaseUrl}"
                    pageParam="page"
                    sortParam="${ownerCurrentSort}"
                    sortParamName="ownerSort"/>
        </c:otherwise>
    </c:choose>
</main>

<%@include file="../footer.jsp"%>
</body>
</html>
