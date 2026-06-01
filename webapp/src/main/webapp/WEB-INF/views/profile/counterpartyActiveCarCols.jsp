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
        <c:url var="counterpartyListingHref" value="/cars/${car.carId}"/>
        <ryden:consumerCarCard card="${car}"
                               image="${counterpartyListingImageUrl}"
                               href="${counterpartyListingHref}"/>
    </div>
</c:forEach>
<div class="d-none js-counterparty-listings-chunk-meta"
     data-has-more="${fragmentHasMore}"
     data-next-page="${fragmentNextPage}"
     aria-hidden="true"></div>
