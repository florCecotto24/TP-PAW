<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
    <head>
        <title><spring:message code="search.pageTitle"/></title>
        <%@include file="header.jsp"%>
    </head>
    <body class="has-fixed-navbar">
        <ryden:navbar/>
        <div>
            <c:url var="searchFiltersClearHref" value="/search"/>
            <ryden:searchWithFilters
                    formId="exploreSearchForm"
                    formClass="search-menu sticky-top w-100"
                    actionPath="/search"
                    showFilters="true"
                    allowFlexibleSearch="true"
                    autoSubmitOnFilterChange="false"
                    clearFiltersHref="${searchFiltersClearHref}"
                    showClearFilters="${hasActiveSearchFilters}"
                    categoryFilterOptions="${categoryFilterOptions}"
                    transmissionFilterOptions="${transmissionFilterOptions}"
                    powertrainFilterOptions="${powertrainFilterOptions}"/>
            <div class="container">

                <%-- Build baseUrl preserving all filter params except page and sort --%>
                <c:url var="searchBaseUrl" value="/search">
                    <c:if test="${not empty param.query}">
                        <c:param name="query"><c:out value="${param.query}"/></c:param>
                    </c:if>
                    <c:choose>
                        <c:when test="${searchFlexible and not empty searchFlexMonth}">
                            <c:param name="flexible" value="true"/>
                            <c:param name="flexMonth"><c:out value="${searchFlexMonth}"/></c:param>
                            <c:if test="${not empty searchFlexDays}">
                                <c:param name="flexDays"><c:out value="${searchFlexDays}"/></c:param>
                            </c:if>
                        </c:when>
                        <c:otherwise>
                            <c:if test="${not empty param.from}">
                                <c:param name="from"><c:out value="${param.from}"/></c:param>
                            </c:if>
                            <c:if test="${not empty param.until}">
                                <c:param name="until"><c:out value="${param.until}"/></c:param>
                            </c:if>
                        </c:otherwise>
                    </c:choose>
                    <c:forEach items="${searchSanitizedNeighborhoodIds}" var="nid">
                        <c:param name="neighborhoodId"><c:out value="${nid}"/></c:param>
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
                                    <p class="text-secondary mb-2 search-empty-state__text">
                                        <spring:message code="search.empty.description"/>
                                    </p>
                                    <div class="search-empty-state__actions">
                                        <a href="${resetSearchUrl}" class="btn btn-primary btn-action btn-action-md">
                                            <spring:message code="search.empty.reset"/>
                                        </a>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <h2 class="h4 fw-semibold mb-2"><spring:message code="search.empty.noCars.title"/></h2>
                                    <p class="text-secondary mb-0 search-empty-state__text">
                                        <spring:message code="search.empty.noCars.description"/>
                                    </p>
                                    <div class="search-empty-state__actions mt-4">
                                        <a href="${resetSearchUrl}" class="btn btn-primary btn-action btn-action-md">
                                            <spring:message code="search.empty.noCars.cta"/>
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

                                        <c:url var="searchCarDetailHref" value="/cars/${car.carId}">
                                            <c:param name="src" value="search"/>
                                            <c:choose>
                                                <c:when test="${searchFlexible and not empty searchFlexMonth}">
                                                    <c:param name="flexMonth"><c:out value="${searchFlexMonth}"/></c:param>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:if test="${not empty param.from}"><c:param name="from"><c:out value="${param.from}"/></c:param></c:if>
                                                    <c:if test="${not empty param.until}"><c:param name="until"><c:out value="${param.until}"/></c:param></c:if>
                                                </c:otherwise>
                                            </c:choose>
                                            <c:forEach var="nid" items="${paramValues.neighborhoodId}">
                                                <c:param name="searchNbId"><c:out value="${nid}"/></c:param>
                                            </c:forEach>
                                        </c:url>
                                        <ryden:consumerCarCard card="${car}" image="${imageUrl}" href="${searchCarDetailHref}"/>
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
    </body>
</html>
