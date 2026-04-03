<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ attribute name="cars" required="true" type="java.util.List" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="subtitle" required="true" type="java.lang.String" %>
<%@ attribute name="id" required="true" type="java.lang.String" %>

<paw:carouselHeader title="${title}" subtitle="${subtitle}" id="${id}"/>

<c:choose>
    <c:when test="${empty cars}">
        <div class="alert-project" role="alert">
            No vehicles available at the moment.
        </div>
    </c:when>
    <c:otherwise>
        <div id="${id}" class="carousel slide" data-bs-ride="false">
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
                <div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-3">
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

                        <paw:carCard model="${car.model}" brand="${car.brand}" price="${car.price}"
                                     image="${imageUrl}" pricePeriod="day"
                                     href="${pageContext.request.contextPath}/car-detail?listingId=${car.listingId}"/>
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
