<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ attribute name="model" required="true" %>
<%@ attribute name="brand" required="true" %>
<%@ attribute name="stars" required="true" %>
<%@ attribute name="price" required="true" %>
<%@ attribute name="image" required="false" %>
<%@ attribute name="reviews" required="true" %>

<c:set var="classes" value="carcard" />

<div class="${classes}">
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
            <p id="carcard-price-amount">$${price}</p>
            <p>/hora</p>
        </div>
    </div>
</div>
