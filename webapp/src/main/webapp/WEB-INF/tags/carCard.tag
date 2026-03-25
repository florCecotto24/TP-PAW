<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ attribute name="model" required="true" %>
<%@ attribute name="brand" required="true" %>
<%@ attribute name="stars" required="true" %>
<%@ attribute name="price" required="true" %>
<%@ attribute name="image" required="false" %>
<%@ attribute name="reviews" required="true" %>
<%@ attribute name="pricePeriod" required="false" %>
<%@ attribute name="href" required="false" %>

<c:if test="${empty pricePeriod}">
    <c:set var="pricePeriod" value="hora" />
</c:if>
<c:choose>
    <c:when test="${pricePeriod eq 'day'}">
        <c:set var="pricePeriodLabel" value="day" />
    </c:when>
    <c:otherwise>
        <c:set var="pricePeriodLabel" value="hora" />
    </c:otherwise>
</c:choose>

<div class="carcard<c:if test="${not empty href}"> carcard--clickable position-relative</c:if>">
    <div class="carcard-image">
        <c:choose>
            <c:when test="${empty image}">
                <div class="no-image-badge">
                    <i class="bi bi-car-front"></i>
                </div>
            </c:when>
            <c:otherwise>
                <img src="${image}" alt="${brand} ${model}">
            </c:otherwise>
        </c:choose>
    </div>
    <div class="carcard-info">
        <div class="carcard-info-text text">
            <h3 class="carcard-brand">${brand}</h3>
            <p class="carcard-model">${model}</p>
            <p class="carcard-stars">${stars}<span class="star">&#9733;</span>(${reviews})</p>
        </div>
        <div class="carcard-price text">
            <p class="carcard-price-amount">$${price}</p>
            <p>/${pricePeriodLabel}</p>
        </div>
    </div>
    <c:if test="${not empty href}">
        <a href="${href}" class="stretched-link carcard-stretched-link" aria-label="View ${brand} ${model}"></a>
    </c:if>
</div>
