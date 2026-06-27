<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="ryden-reservation" tagdir="/WEB-INF/tags/reservation" %>
<%@ taglib prefix="ryden-search" tagdir="/WEB-INF/tags/search" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
<head>
    <title><spring:message code="myReservations.pageTitle"/></title>
    <%@include file="../header.jsp"%>
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
                <div class="d-flex align-items-center gap-2 w-100" style="max-width:600px">
                    <div class="d-flex align-items-center ryden-search-pill rounded-4 px-3 py-1 flex-grow-1 gap-2">
                        <i class="bi bi-search text-secondary flex-shrink-0" aria-hidden="true"></i>
                        <input type="search" class="form-control" id="myReservations_q" name="q" value="<c:out value='${param.q}'/>"
                               placeholder="<spring:message code='myCars.filter.query.placeholder'/>"/>
                    </div>
                    <button type="submit" class="btn btn-primary rounded-3 flex-shrink-0"><spring:message code="myCars.filter.search"/></button>
                    <c:if test="${showRiderClear}">
                        <a href="${pageContext.request.contextPath}/my-reservations" class="btn btn-outline-secondary flex-shrink-0"><spring:message code="search.filters.clear"/></a>
                    </c:if>
                </div>
            </div>
            <div class="d-flex justify-content-center mb-3">
                <div class="d-flex flex-wrap align-items-center justify-content-center gap-0 pt-1">
                    <spring:message code="myReservations.filter.status" var="riderStatusLabel"/>
                    <ryden-search:exploreFilterDropdown filterLabel="${riderStatusLabel}" paramName="riderStatus" ariaGroup="rider-status" options="${reservationStatusOptions}"/>
                    <spring:message code="search.filter.category" var="riderCategoryLabel"/>
                    <ryden-search:exploreFilterDropdown filterLabel="${riderCategoryLabel}" paramName="category" ariaGroup="rider-category" options="${categoryFilterOptions}"/>
                    <spring:message code="search.filter.transmission" var="riderTransmissionLabel"/>
                    <ryden-search:exploreFilterDropdown filterLabel="${riderTransmissionLabel}" paramName="transmission" ariaGroup="rider-transmission" options="${transmissionFilterOptions}"/>
                    <spring:message code="search.filter.powertrain" var="riderPowertrainLabel"/>
                    <ryden-search:exploreFilterDropdown filterLabel="${riderPowertrainLabel}" paramName="powertrain" ariaGroup="rider-powertrain" options="${powertrainFilterOptions}"/>
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
                    <ryden-search:exploreFilterDropdown filterLabel="${riderRatingLabel}" paramName="rating" ariaGroup="rider-rating" options="${ratingFilterOptions}"/>
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
                    <ryden-reservation:carReservationCard reservation="${reservation}" href="${reservationDetailUrl}"/>
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

<%@include file="../footer.jsp"%>
</body>
</html>
