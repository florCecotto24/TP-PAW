<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ attribute name="model" required="true" %>
<%@ attribute name="brand" required="true" %>
<%@ attribute name="stars" required="true" %>
<%@ attribute name="price" required="true" %>
<%@ attribute name="image" required="false" %>
<%@ attribute name="reviews" required="true" %>
<%@ attribute name="layout" required="false" %>
<%@ attribute name="pricePeriod" required="false" %>
<%@ attribute name="location" required="false" %>
<%@ attribute name="seats" required="false" %>
<%@ attribute name="transmission" required="false" %>
<%@ attribute name="detailsHref" required="false" %>
<%@ attribute name="showFavorite" required="false" type="java.lang.Boolean" %>

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

<c:choose>
    <c:when test="${layout eq 'similar'}">
        <c:if test="${showFavorite eq null}">
            <c:set var="showFavorite" value="${true}" />
        </c:if>
        <div class="carcard carcard--similar">
            <div class="carcard-image carcard-image--similar position-relative">
                <c:if test="${showFavorite}">
                    <button type="button" class="carcard-favorite-btn btn btn-light rounded-circle border-0 shadow-sm" aria-label="Save to favorites">
                        <i class="bi bi-heart" aria-hidden="true"></i>
                    </button>
                </c:if>
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
            <div class="carcard-similar-body">
                <div class="d-flex justify-content-between align-items-start gap-2 mb-2">
                    <h3 class="carcard-similar-title mb-0">${brand} ${model}</h3>
                    <span class="carcard-rating-pill d-inline-flex align-items-center gap-1 flex-shrink-0">
                        <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
                        <span class="fw-semibold">${stars}</span>
                    </span>
                </div>
                <c:if test="${not empty location}">
                    <p class="carcard-similar-location text-secondary small mb-2 d-flex align-items-center gap-1">
                        <i class="bi bi-geo-alt" aria-hidden="true"></i>
                        ${location}
                    </p>
                </c:if>
                <c:if test="${not empty seats or not empty transmission}">
                    <div class="d-flex flex-wrap gap-3 text-secondary small mb-3">
                        <c:if test="${not empty seats}">
                            <span class="d-inline-flex align-items-center gap-1">
                                <i class="bi bi-person" aria-hidden="true"></i>
                                ${seats}
                            </span>
                        </c:if>
                        <c:if test="${not empty transmission}">
                            <span class="d-inline-flex align-items-center gap-1">
                                <i class="bi bi-gear-wide-connected" aria-hidden="true"></i>
                                ${transmission}
                            </span>
                        </c:if>
                    </div>
                </c:if>
                <div class="d-flex justify-content-between align-items-center gap-2 carcard-similar-footer">
                    <div class="carcard-similar-price">
                        <span class="carcard-similar-price-amount">$${price}</span>
                        <span class="text-secondary small">/${pricePeriodLabel}</span>
                    </div>
                    <c:if test="${not empty detailsHref}">
                        <a href="${detailsHref}" class="btn btn-primary btn-sm rounded-3 px-3 carcard-details-btn">Details</a>
                    </c:if>
                </div>
            </div>
        </div>
    </c:when>
    <c:otherwise>
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
                    <p>/${pricePeriodLabel}</p>
                </div>
            </div>
        </div>
    </c:otherwise>
</c:choose>
