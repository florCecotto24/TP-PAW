<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="myFavorites.title"/></title>
    <%@include file="../header.jsp"%>
</head>
<body class="has-fixed-navbar">
    <ryden:navbar/>
    <div class="container my-4">
        <div class="mb-3">
            <h1 class="h3 mb-1 fw-semibold"><spring:message code="myFavorites.title"/></h1>
            <p class="text-secondary mb-0"><spring:message code="myFavorites.subtitle"/></p>
        </div>

        <c:if test="${not empty favoriteToggleErrorMessage}">
            <div class="alert alert-warning" role="alert">
                <c:out value="${favoriteToggleErrorMessage}"/>
            </div>
        </c:if>

        <c:choose>
            <c:when test="${empty favoritesPage.content}">
                <div class="search-empty-state text-center">
                    <div class="search-empty-state__icon" aria-hidden="true">
                        <i class="bi bi-heart"></i>
                    </div>
                    <h2 class="h4 fw-semibold mb-2"><spring:message code="myFavorites.empty.title"/></h2>
                    <p class="text-secondary mb-0 search-empty-state__text">
                        <spring:message code="myFavorites.empty.description"/>
                    </p>
                    <div class="search-empty-state__actions mt-4">
                        <a href="${pageContext.request.contextPath}/search" class="btn btn-primary btn-action btn-action-md">
                            <spring:message code="myFavorites.empty.cta"/>
                        </a>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center">
                    <div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 pt-2 g-3">
                        <c:forEach var="car" items="${favoritesPage.content}">
                            <div class="col d-flex justify-content-center">
                                <c:choose>
                                    <c:when test="${car.imageId > 0}">
                                        <c:url var="imageUrl" value="/image/${car.imageId}" />
                                    </c:when>
                                    <c:otherwise>
                                        <c:set var="imageUrl" value="" />
                                    </c:otherwise>
                                </c:choose>
                                <c:url var="favoriteCarDetailHref" value="/cars/${car.carId}">
                                    <c:param name="src" value="my-favorites"/>
                                </c:url>
                                <ryden:consumerCarCard card="${car}" image="${imageUrl}" href="${favoriteCarDetailHref}"/>
                            </div>
                        </c:forEach>
                    </div>
                </div>

                <c:url var="favoritesBaseUrl" value="/my-favorites"/>
                <ryden:pagination
                        currentPage="${favoritesPage.currentPage}"
                        totalPages="${favoritesPage.totalPages}"
                        baseUrl="${favoritesBaseUrl}"/>
            </c:otherwise>
        </c:choose>
    </div>
<%@include file="../footer.jsp"%>
</body>
</html>
