<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
    <head>
        <title><spring:message code="search.pageTitle"/></title>
        <%@include file="header.jsp"%>
    </head>
    <body class="has-fixed-navbar">
        <ryden:navbar/>
        <div>
            <form id="exploreSearchForm" class="search-menu sticky-top w-100" method="get" action="${pageContext.request.contextPath}/search">
                <ryden:searchBar/>
                <nav class="navbar navbar-expand-lg d-flex justify-content-center align-items-center border py-2 flex-wrap gap-1"
                     aria-label="Search filters">
                    <spring:message code="search.filter.category" var="categoryLabel"/>
                    <spring:message code="search.filter.category.helper" var="categoryHelper"/>
                    <ryden:exploreFilterDropdown
                            filterLabel="${categoryLabel}"
                            helperText="${categoryHelper}"
                            paramName="category"
                            ariaGroup="category"
                            options="${categoryFilterOptions}"/>
                    <span class="vr d-none d-md-inline-block align-self-stretch my-2 mx-1"></span>
                    <spring:message code="search.filter.transmission" var="transmissionLabel"/>
                    <spring:message code="search.filter.transmission.helper" var="transmissionHelper"/>
                    <ryden:exploreFilterDropdown
                            filterLabel="${transmissionLabel}"
                            helperText="${transmissionHelper}"
                            paramName="transmission"
                            ariaGroup="transmission"
                            options="${transmissionFilterOptions}"/>
                    <spring:message code="search.filter.powertrain" var="powertrainLabel"/>
                    <spring:message code="search.filter.powertrain.helper" var="powertrainHelper"/>
                    <ryden:exploreFilterDropdown
                            filterLabel="${powertrainLabel}"
                            helperText="${powertrainHelper}"
                            paramName="powertrain"
                            ariaGroup="powertrain"
                            options="${powertrainFilterOptions}"/>
                    <span class="vr d-none d-md-inline-block align-self-stretch my-2 mx-1"></span>
                    <spring:message code="search.filter.price" var="priceLabel"/>
                    <spring:message code="search.filter.price.helper" var="priceHelper"/>
                    <ryden:exploreFilterDropdown
                            filterLabel="${priceLabel}"
                            helperText="${priceHelper}"
                            paramName="price"
                            ariaGroup="price"
                            options="${priceFilterOptions}"/>
                </nav>
            </form>
            <div class="container">

                <%-- Build baseUrl preserving all filter params except page and sort --%>
                <c:url var="searchBaseUrl" value="/search">
                    <c:if test="${not empty param.query}">
                        <c:param name="query" value="${param.query}"/>
                    </c:if>
                    <c:if test="${not empty param.from}">
                        <c:param name="from" value="${param.from}"/>
                    </c:if>
                    <c:if test="${not empty param.until}">
                        <c:param name="until" value="${param.until}"/>
                    </c:if>
                    <c:forEach var="cat" items="${paramValues.category}">
                        <c:param name="category" value="${cat}"/>
                    </c:forEach>
                    <c:forEach var="tr" items="${paramValues.transmission}">
                        <c:param name="transmission" value="${tr}"/>
                    </c:forEach>
                    <c:forEach var="pw" items="${paramValues.powertrain}">
                        <c:param name="powertrain" value="${pw}"/>
                    </c:forEach>
                    <c:forEach var="pr" items="${paramValues.price}">
                        <c:param name="price" value="${pr}"/>
                    </c:forEach>
                </c:url>

                <div class="mb-3 pt-5 d-flex flex-wrap align-items-center justify-content-between gap-2">
                    <h4 class="font-semibold mb-0">
                        <c:choose>
                            <c:when test="${searchPage.totalItems > 0}">
                                <spring:message code="search.resultsRange"
                                    arguments="${searchPage.firstItemNumber},${searchPage.lastItemNumber},${searchPage.totalItems}"/>
                            </c:when>
                            <c:otherwise>
                                <spring:message code="search.resultsCount" arguments="0"/>
                            </c:otherwise>
                        </c:choose>
                    </h4>
                    <c:if test="${not empty results}">
                        <ryden:sortBar baseUrl="${searchBaseUrl}" currentSort="${currentSort}"/>
                    </c:if>
                </div>

                <c:set var="hasActiveSearchFilters"
                       value="${not empty param.query or not empty param.from or not empty param.until or not empty paramValues.category or not empty paramValues.transmission or not empty paramValues.powertrain or not empty paramValues.price}"/>
                <c:url var="resetSearchUrl" value="/search"/>
                <c:url var="publishCarUrl" value="/publish-car"/>

                <c:choose>
                    <c:when test="${empty results}">
                        <div class="search-empty-state text-center">
                            <div class="search-empty-state__icon" aria-hidden="true">
                                <i class="bi bi-search"></i>
                            </div>
                            <c:choose>
                                <c:when test="${hasActiveSearchFilters}">
                                    <h2 class="h4 fw-semibold mb-2"><spring:message code="search.empty.title"/></h2>
                                    <p class="text-secondary mb-0 search-empty-state__text">
                                        <spring:message code="search.empty.description"/>
                                    </p>
                                    <div class="search-empty-state__actions">
                                        <a href="${resetSearchUrl}" class="btn btn-primary btn-action btn-action-md">
                                            <spring:message code="search.empty.reset"/>
                                        </a>
                                        <a href="${publishCarUrl}" class="btn btn-outline-secondary btn-action btn-action-md">
                                            <spring:message code="home.cta.button"/>
                                        </a>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <h2 class="h4 fw-semibold mb-2"><spring:message code="search.empty.noListings.title"/></h2>
                                    <p class="text-secondary mb-0 search-empty-state__text">
                                        <spring:message code="search.empty.noListings.description"/>
                                    </p>
                                    <div class="search-empty-state__actions">
                                        <a href="${publishCarUrl}" class="btn btn-primary btn-action btn-action-md">
                                            <spring:message code="home.cta.button"/>
                                        </a>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="text-center">
                            <div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 pt-4 g-3">
                                <c:forEach var="car" items="${results}">
                                    <div class="col d-flex justify-content-center">
                                        <c:choose>
                                            <c:when test="${car.imageId > 0}">
                                                <c:url var="imageUrl" value="/image/${car.imageId}" />
                                            </c:when>
                                            <c:otherwise>
                                                <c:set var="imageUrl" value="" />
                                            </c:otherwise>
                                        </c:choose>

                                        <ryden:carCard
                                                model="${car.model}"
                                                brand="${car.brand}"
                                                price="${car.price}"
                                                image="${imageUrl}"
                                                pricePeriod="day"
                                                href="${pageContext.request.contextPath}/car-detail?listingId=${car.listingId}"/>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>

                        <ryden:pagination
                                currentPage="${searchPage.currentPage}"
                                totalPages="${searchPage.totalPages}"
                                baseUrl="${searchBaseUrl}"
                                sortParam="${currentSort}"/>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
        <%@include file="footer.jsp"%>
        <script>
            (function () {
                var form = document.getElementById('exploreSearchForm');
                if (!form) return;
                form.querySelectorAll('.js-explore-filter').forEach(function (cb) {
                    cb.addEventListener('change', function () {
                        form.submit();
                    });
                });
            })();
        </script>
    </body>
</html>
