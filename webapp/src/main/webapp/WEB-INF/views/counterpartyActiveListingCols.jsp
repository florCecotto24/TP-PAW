<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<c:forEach var="car" items="${counterpartyActiveListings}">
    <div class="col d-flex justify-content-center">
        <c:choose>
            <c:when test="${car.imageId > 0}">
                <c:url var="counterpartyListingImageUrl" value="/image/${car.imageId}"/>
            </c:when>
            <c:otherwise>
                <c:set var="counterpartyListingImageUrl" value=""/>
            </c:otherwise>
        </c:choose>
        <c:url var="counterpartyListingHref" value="/car-detail">
            <c:param name="carId"><c:out value="${car.carId}"/></c:param>
        </c:url>
        <ryden:carCard
                model="${car.model}"
                brand="${car.brand}"
                price="${car.price}"
                image="${counterpartyListingImageUrl}"
                pricePeriod="day"
                ratingAvg="${car.ratingAvg}"
                reviewCount="${car.reviewCount}"
                href="${counterpartyListingHref}"/>
    </div>
</c:forEach>
<div class="d-none js-counterparty-listings-chunk-meta"
     data-has-more="${fragmentHasMore}"
     data-next-page="${fragmentNextPage}"
     aria-hidden="true"></div>
