<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
            <section class="reservation-management-header mt-4 pt-5 mb-4">
                <h1 class="h3 fw-bold mb-2"><spring:message code="myListings.heading"/></h1>
                <p class="text-secondary mb-0"><spring:message code="myListings.subheading"/></p>
            </section>

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

            <c:url var="myListingsBaseUrl" value="/my-listings"/>
            <c:url var="publishCarUrl" value="/publish-car"/>

            <c:choose>
                <c:when test="${empty results}">
                    <spring:message var="myListings.emptyTitle" code="myListings.empty.title"/>
                    <spring:message var="myListings.emptyDescription" code="myListings.empty.description"/>
                    <spring:message var="homeCtaButton" code="home.cta.button"/>

                    <div class="search-empty-state text-center">
                        <div class="search-empty-state__icon" aria-hidden="true">
                            <i class="bi bi-car-front"></i>
                        </div>
                        <h2 class="h4 fw-semibold mb-2">
                            <spring:message code="myListings.empty.title"/>
                        </h2>
                        <p class="text-secondary mb-0 search-empty-state__text">
                            <spring:message code="myListings.empty.description"/>
                        </p>
                        <div class="search-empty-state__actions mt-4">
                            <a href="<c:out value='${publishCarUrl}'/>" class="btn btn-primary btn-action btn-action-md">
                                <spring:message code="home.cta.button"/>
                            </a>
                        </div>
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
                            currentPage="${myListingsPage.currentPage}"
                            totalPages="${myListingsPage.totalPages}"
                            baseUrl="${myListingsBaseUrl}"/>
                </c:otherwise>
            </c:choose>
        </div>
        <%@include file="footer.jsp"%>
    </body>
</html>

