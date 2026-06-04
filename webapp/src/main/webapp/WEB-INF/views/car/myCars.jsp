<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <title><spring:message code="myCars.pageTitle"/></title>
        <%@include file="../header.jsp"%>
    </head>
    <body class="has-fixed-navbar">
        <ryden:navbar/>
        <div class="container pt-5 pb-4">
            <spring:message code="myCars.heading" var="myListingsLabel"/>
            <!-- <ryden:breadcrumbTrail currentLabel="${myListingsLabel}" showHome="${false}"/> -->
            <section class="reservation-management-header mt-4 pt-5 mb-4">
                <h1 class="h3 fw-bold mb-2"><spring:message code="myCars.heading"/></h1>
                <p class="text-secondary mb-0"><spring:message code="myCars.subheading"/></p>
            </section>

            <div>
                    <c:set var="hasActiveFilters" value="${not empty param.q or not empty paramValues.category or not empty paramValues.transmission or not empty paramValues.powertrain or not empty paramValues.listingStatus or not empty param.priceMin or not empty param.priceMax or not empty paramValues.rating}"/>

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
                        <c:forEach var="ls" items="${paramValues.listingStatus}">
                            <c:param name="listingStatus"><c:out value="${ls}"/></c:param>
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
                        <c:set var="showLstClear" value="${hasActiveFilters or (not empty param.sort and param.sort ne 'date,desc')}"/>
                        <form id="myListingsFilterForm" class="mb-4" method="get" action="${pageContext.request.contextPath}/my-cars">
                            <input type="hidden" name="tab" value="listings"/>
                            <div class="d-flex justify-content-center mb-3">
                                <div class="d-flex align-items-center gap-2 w-100" style="max-width:600px">
                                    <div class="d-flex align-items-center ryden-search-pill rounded-4 px-3 py-1 flex-grow-1 gap-2">
                                        <i class="bi bi-search text-secondary flex-shrink-0" aria-hidden="true"></i>
                                        <input type="search" class="form-control" id="myListings_q" name="q" value="<c:out value='${param.q}'/>"
                                               placeholder="<spring:message code='myCars.filter.query.placeholder'/>"/>
                                    </div>
                                    <button type="submit" class="btn btn-primary rounded-3 flex-shrink-0"><spring:message code="myCars.filter.search"/></button>
                                    <c:if test="${showLstClear}">
                                        <a href="${pageContext.request.contextPath}/my-cars" class="btn btn-outline-secondary flex-shrink-0"><spring:message code="search.filters.clear"/></a>
                                    </c:if>
                                </div>
                            </div>
                            <div class="d-flex justify-content-center">
                                <div class="d-flex flex-wrap align-items-center justify-content-center gap-0 pt-1">
                                    <spring:message code="myCars.filter.status" var="lstStatusLabel"/>
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
                                        <button class="btn btn-light border dropdown-toggle rounded-4 d-inline-flex align-items-center gap-1" type="button"
                                                data-bs-toggle="dropdown" data-bs-auto-close="outside">
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
                        </form>

                        <div class="mb-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
                            <h3 class="h6 mb-0">
                                <c:choose>
                                    <c:when test="${myListingsPage.totalItems > 0}">
                                        <spring:message code="myCars.resultsRange"
                                                        arguments="${myListingsPage.firstItemNumber},${myListingsPage.lastItemNumber},${myListingsPage.totalItems}"/>
                                    </c:when>
                                    <c:otherwise>
                                        <spring:message code="myCars.resultsCount" arguments="0"/>
                                    </c:otherwise>
                                </c:choose>
                            </h3>
                            <ryden:sortBar baseUrl="${myListingsBaseUrl}" currentSort="${listingsCurrentSort}"
                                           wrapperClass="d-flex align-items-center gap-2 flex-wrap"/>
                        </div>
                    </c:if>

                    <c:choose>
                        <c:when test="${empty results}">
                            <div class="search-empty-state text-center">
                                <c:choose>
                                    <c:when test="${hasActiveFilters}">
                                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myCars.noResults.title"/></h2>
                                        <div class="search-empty-state__actions mt-4">
                                            <a href="${pageContext.request.contextPath}/my-cars?tab=listings" class="btn btn-outline-secondary">
                                                <spring:message code="search.filters.clear"/>
                                            </a>
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myCars.empty.title"/></h2>
                                        <p class="text-secondary mb-0 search-empty-state__text">
                                            <spring:message code="myCars.empty.description"/>
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
                                    <%-- Owner-blocked overrides any owner-controlled status (ACTIVE/PAUSED/LACK_DOC/UNAVAILABLE)
                                         to make it obvious why the car is no longer bookable. Terminal/admin states keep their badge. --%>
                                    <c:when test="${currentUserBlocked and car.statusKey ne 'DEACTIVATED' and car.statusKey ne 'ADMIN_PAUSED'}">
                                        <span class="position-absolute top-0 end-0 m-3" style="background-color:#b91c1c; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                            <i class="bi bi-shield-exclamation me-1"></i><spring:message code="myCars.badge.ownerBlocked"/>
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
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#ffc107; color:#212529; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
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
                                        <c:choose>
                                            <c:when test="${car.statusKey == 'LACK_DOC'}">
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#ffc107; color:#212529; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                    <spring:message code="enum.car.status.LACK_DOC"/>
                                                </span>
                                            </c:when>
                                            <c:when test="${car.statusKey == 'ADMIN_PAUSED'}">
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#6d28d9; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                    <spring:message code="enum.car.status.ADMIN_PAUSED"/>
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
                                            <c:otherwise>
                                                <span class="position-absolute top-0 end-0 m-3" style="background-color:#6c757d; color:#ffffff; padding:.25rem .5rem; border-radius:.375rem; font-weight:600; font-size:.75rem;">
                                                    <spring:message code="myCars.noAvailability.badge"/>
                                                </span>
                                            </c:otherwise>
                                        </c:choose>
                                    </c:otherwise>
                                </c:choose>
                                <%-- "Refund proof pending" notice: stacked vertically right under the status badge,
                                     right-aligned with it (same right edge). Uses an explicit top offset (2.75rem)
                                     so it clears the ~1.5rem-tall status badge plus its m-3 (1rem) top margin. --%>
                                <c:if test="${not empty pendingRefundCarIds and pendingRefundCarIds.contains(car.carId)}">
                                    <span class="position-absolute end-0 me-3 d-inline-flex align-items-center gap-2 text-end small fw-semibold"
                                          style="top: 2.75rem; max-width: 55%; background-color:#b91c1c; color:#ffffff; padding:.4rem .75rem; border-radius:.5rem; white-space:normal; word-break:break-word; line-height:1.25;">
                                        <i class="bi bi-cash-coin flex-shrink-0" aria-hidden="true"></i>
                                        <span><spring:message code="myCars.badge.refundProofPending"/></span>
                                    </span>
                                </c:if>
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
                                                <div class="col-12 col-md-9 min-w-0">
                                                    <div class="card-body p-3 p-md-4 h-100 d-flex flex-column justify-content-between gap-3">
                                                        <div class="min-w-0">
                                                            <h3 class="h5 fw-semibold mb-1 ryden-text-break"><c:out value="${car.brand} ${car.model}"/></h3>
                                                        </div>
                                                        <c:if test="${car.hasListing}">
                                                            <div class="pt-1 d-flex align-items-center justify-content-between gap-2 flex-wrap">
                                                                <!-- <div class="reservation-price-compact">
                                                                    <span class="reservation-card__meta-label mb-0"><spring:message code="myCars.card.pricePerDay"/></span>
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


        <%@include file="../footer.jsp"%>
    </body>
</html>
