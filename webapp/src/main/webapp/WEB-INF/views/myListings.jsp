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
            <!-- <ryden:breadcrumbTrail currentLabel="${myListingsLabel}" showHome="${false}"/> -->
            <section class="reservation-management-header mt-4 pt-5 mb-4">
                <h1 class="h3 fw-bold mb-2"><spring:message code="myListings.heading"/></h1>
                <p class="text-secondary mb-0"><spring:message code="myListings.subheading"/></p>
            </section>

            <div>
                    <c:set var="hasActiveFilters" value="${not empty param.q or not empty paramValues.category or not empty paramValues.transmission or not empty paramValues.powertrain}"/>

                    <c:url var="myListingsBaseUrl" value="/my-cars">
                        <c:param name="tab" value="listings"/>
                        <c:if test="${not empty param.q}">
                            <c:param name="q"><c:out value="${param.q}"/></c:param>
                        </c:if>
                        <c:forEach var="cat" items="${paramValues.category}">
                            <c:param name="category"><c:out value="${cat}"/></c:param>
                        </c:forEach>
                        <c:forEach var="tr" items="${paramValues.transmission}">
                            <c:param name="transmission"><c:out value="${tr}"/></c:param>
                        </c:forEach>
                        <c:forEach var="pw" items="${paramValues.powertrain}">
                            <c:param name="powertrain"><c:out value="${pw}"/></c:param>
                        </c:forEach>
                    </c:url>

                    <c:if test="${not empty results or hasActiveFilters}">
                        <c:set var="showLstClear" value="${hasActiveFilters or (not empty param.sort and param.sort ne 'date,desc')}"/>
                        <form id="myListingsFilterForm" class="mb-4" method="get" action="${pageContext.request.contextPath}/my-cars">
                            <input type="hidden" name="tab" value="listings"/>
                            <div class="d-flex justify-content-center mb-3">
                                <div class="d-flex align-items-center gap-2 w-100" style="max-width:600px">
                                    <div class="d-flex align-items-center ryden-search-pill rounded-4 px-3 py-1 flex-grow-1 gap-2">
                                        <i class="bi bi-search text-secondary flex-shrink-0" aria-hidden="true"></i>
                                        <input type="search" class="form-control" id="myListings_q" name="q" value="<c:out value='${param.q}'/>"
                                               placeholder="<spring:message code='myListings.filter.query.placeholder'/>"/>
                                    </div>
                                    <button type="submit" class="btn btn-primary rounded-3 flex-shrink-0"><spring:message code="myListings.filter.search"/></button>
                                    <c:if test="${showLstClear}">
                                        <a href="${pageContext.request.contextPath}/my-cars" class="btn btn-outline-secondary flex-shrink-0"><spring:message code="search.filters.clear"/></a>
                                    </c:if>
                                </div>
                            </div>
                            <div class="d-flex justify-content-center">
                                <div class="d-flex flex-wrap align-items-center justify-content-center gap-0 pt-1">
                                    <spring:message code="search.filter.category" var="lstCategoryLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${lstCategoryLabel}" paramName="category" ariaGroup="lst-category" options="${categoryFilterOptions}"/>
                                    <spring:message code="search.filter.transmission" var="lstTransmissionLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${lstTransmissionLabel}" paramName="transmission" ariaGroup="lst-transmission" options="${transmissionFilterOptions}"/>
                                    <spring:message code="search.filter.powertrain" var="lstPowertrainLabel"/>
                                    <ryden:exploreFilterDropdown filterLabel="${lstPowertrainLabel}" paramName="powertrain" ariaGroup="lst-powertrain" options="${powertrainFilterOptions}"/>
                                </div>
                            </div>
                        </form>

                        <div class="mb-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
                            <h3 class="h6 mb-0">
                                <c:choose>
                                    <c:when test="${myListingsPage.totalItems > 0}">
                                        <spring:message code="myListings.resultsRange"
                                                        arguments="${myListingsPage.firstItemNumber},${myListingsPage.lastItemNumber},${myListingsPage.totalItems}"/>
                                    </c:when>
                                    <c:otherwise>
                                        <spring:message code="myListings.resultsCount" arguments="0"/>
                                    </c:otherwise>
                                </c:choose>
                            </h3>
                            <ryden:sortBar baseUrl="${myListingsBaseUrl}" currentSort="${listingsCurrentSort}"
                                           wrapperClass="d-flex align-items-center gap-2 flex-wrap"
                                           dateOnly="${true}"/>
                        </div>
                    </c:if>

                    <c:choose>
                        <c:when test="${empty results}">
                            <div class="search-empty-state text-center">
                                <c:choose>
                                    <c:when test="${hasActiveFilters}">
                                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myListings.noResults.title"/></h2>
                                        <div class="search-empty-state__actions mt-4">
                                            <a href="${pageContext.request.contextPath}/my-cars?tab=listings" class="btn btn-outline-secondary">
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
                            <fmt:setLocale value="es_AR"/>
                            <div class="d-flex flex-column gap-3">
                                <c:forEach var="car" items="${results}">
                                    <c:url var="carDetailUrl" value="/my-cars/car/${car.carId}"/>
                                    <a href="<c:out value='${carDetailUrl}'/>" class="reservation-card text-decoration-none text-reset">
                                        <article class="card border-0 shadow-sm rounded-4 overflow-hidden reservation-card__surface position-relative">
                                <c:choose>
                                    <c:when test="${car.modelPendingValidation}">
                                        <span class="position-absolute top-0 end-0 m-3" style="background-color:#0369a1; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                            <i class="bi bi-clock me-1"></i><spring:message code="myCars.modelPendingValidation.badge"/>
                                        </span>
                                    </c:when>
                                    <c:when test="${car.hasListing}">
                                        <c:choose>
                                            <c:when test="${car.statusKey == 'ACTIVE'}">
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#198754; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                    <spring:message code="enum.car.status.ACTIVE"/>
                                                </span>
                                            </c:when>
                                            <c:when test="${car.statusKey == 'PAUSED'}">
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#e4960b; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                    <spring:message code="enum.car.status.PAUSED"/>
                                                </span>
                                            </c:when>
                                            <c:when test="${car.statusKey == 'DEACTIVATED'}">
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#6c757d; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                    <spring:message code="enum.car.status.DEACTIVATED"/>
                                                </span>
                                            </c:when>
                                            <c:when test="${car.statusKey == 'LACK_DOC'}">
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#b91c1c; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                    <spring:message code="enum.car.status.LACK_DOC"/>
                                                </span>
                                            </c:when>
                                            <c:when test="${car.statusKey == 'ADMIN_PAUSED'}">
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#6d28d9; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                    <spring:message code="enum.car.status.ADMIN_PAUSED"/>
                                                </span>
                                            </c:when>
                                            <c:when test="${car.statusKey == 'UNAVAILABLE'}">
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#6c757d; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                    <spring:message code="enum.car.status.UNAVAILABLE"/>
                                                </span>
                                            </c:when>
                                        </c:choose>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="position-absolute top-0 end-0 m-3" style="background-color:#6c757d; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                            <spring:message code="myCars.noListing.badge"/>
                                        </span>
                                    </c:otherwise>
                                </c:choose>
                                            <div class="row g-0 align-items-stretch">
                                                <div class="col-12 col-md-3 reservation-card__media-wrap">
                                                    <c:choose>
                                                        <c:when test="${car.imageId > 0}">
                                                            <c:url var="carImgUrl" value="/image/${car.imageId}"/>
                                                            <img src="<c:out value='${carImgUrl}'/>" alt="<c:out value='${car.brand} ${car.model}'/>" class="reservation-card__media">
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
                                                        </div>
                                                        <c:if test="${car.hasListing}">
                                                            <div class="pt-1 d-flex align-items-center justify-content-between gap-2 flex-wrap">
                                                                <!-- <div class="reservation-price-compact">
                                                                    <span class="reservation-card__meta-label mb-0"><spring:message code="myListings.card.pricePerDay"/></span>
                                                                    <span class="h5 mb-0 fw-bold text-primary"><fmt:formatNumber value="${car.dayPrice}" type="currency" currencyCode="ARS"/></span>
                                                                </div> -->
                                                            </div>
                                                        </c:if>
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

        </div>


        <%@include file="footer.jsp"%>
    </body>
</html>
