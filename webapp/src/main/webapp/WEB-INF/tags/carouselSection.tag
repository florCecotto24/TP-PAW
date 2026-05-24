<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="cars"         required="true"  type="java.util.List" %>
<%@ attribute name="title"        required="true"  type="java.lang.String" %>
<%@ attribute name="subtitle"     required="true"  type="java.lang.String" %>
<%@ attribute name="id"           required="true"  type="java.lang.String" %>
<%@ attribute name="prevPageHref" required="false" type="java.lang.String" %>
<%@ attribute name="nextPageHref" required="false" type="java.lang.String" %>

<ryden:carouselHeader title="${title}" subtitle="${subtitle}" id="${id}"
                      prevPageHref="${prevPageHref}" nextPageHref="${nextPageHref}"/>

<c:choose>
    <c:when test="${empty cars}">
        <div class="alert-project" role="alert">
            <spring:message code="carouselSection.noVehicles"/>
        </div>
    </c:when>
    <c:otherwise>
        <div id="<c:out value='${id}'/>" class="carousel slide" data-bs-ride="false">
            <div class="carousel-inner">
                <c:forEach items="${cars}" var="car" varStatus="status">
                <c:if test="${status.index % 4 == 0}">
                <c:choose>
                <c:when test="${status.first}">
                <div class="carousel-item active">
                    </c:when>
                    <c:otherwise>
                </div><div class="carousel-item">
                </c:otherwise>
                </c:choose>
                <div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-3 pb-3">
                    </c:if>

                    <div class="col d-flex justify-content-center">
                        <c:choose>
                            <c:when test="${car.imageId > 0}">
                                <c:url var="imageUrl" value="/image/${car.imageId}" />
                            </c:when>
                            <c:otherwise>
                                <c:set var="imageUrl" value="" />
                            </c:otherwise>
                        </c:choose>

                        <ryden:carCard model="${car.model}" brand="${car.brand}" price="${car.price}"
                                     image="${imageUrl}" pricePeriod="day" ratingAvg="${car.ratingAvg}"
                                     reviewCount="${car.reviewCount}"
                                     href="${pageContext.request.contextPath}/car-detail?carId=${car.carId}"/>
                    </div>

                    <c:if test="${status.index % 4 == 3 || status.last}">
                </div>
                </c:if>
                </c:forEach>
            </div>
            </div>
        </div>
    </c:otherwise>
</c:choose>
