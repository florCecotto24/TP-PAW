<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="model" required="true" %>
<%@ attribute name="brand" required="true" %>
<%@ attribute name="price" required="true" type="java.lang.Number" %>
<%@ attribute name="image" required="false" %>
<%@ attribute name="pricePeriod" required="false" %>
<%@ attribute name="href" required="false" %>
<%@ attribute name="ratingAvg" required="false" type="java.lang.Number" %>
<%@ attribute name="reviewCount" required="false" type="java.lang.Number" %>
<%@ attribute name="priceMarketPositionModifier" required="false" %>
<%@ attribute name="marketAveragePrice" required="false" type="java.lang.Number" %>
<%@ attribute name="marketSampleCount" required="false" type="java.lang.Number" %>
<%@ attribute name="minimumRentalDays" required="false" type="java.lang.Integer" %>

<fmt:setLocale value="es_AR"/>
<c:if test="${empty pricePeriod}">
    <c:set var="pricePeriod" value="hour" />
</c:if>
<c:choose>
    <c:when test="${pricePeriod eq 'day'}">
        <spring:message code="common.day" var="dayLabel"/>
        <c:set var="pricePeriodLabel" value="${dayLabel}" />
    </c:when>
    <c:otherwise>
        <spring:message code="common.hour" var="hourLabel"/>
        <c:set var="pricePeriodLabel" value="${hourLabel}" />
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
                <img src="<c:out value='${image}'/>" alt="<c:out value='${brand} ${model}'/>">
            </c:otherwise>
        </c:choose>
        <c:if test="${not empty href}">
            <spring:message code="carCard.viewChip" var="viewChipText"/>
            <span class="carcard-view-chip" aria-hidden="true"><c:out value="${viewChipText}"/></span>
        </c:if>
    </div>
    <div class="carcard-info">
        <div class="carcard-info-text text">
            <h4 class="carcard-brand"><c:out value="${brand}"/></h4>
            <p class="carcard-model"><c:out value="${model}"/></p>
            <c:choose>
                <c:when test="${not empty ratingAvg and reviewCount > 0}">
                    <p class="carcard-rating small text-secondary mb-0 mt-1">
                        <span class="fw-semibold text-dark"><fmt:formatNumber value="${ratingAvg}" maxFractionDigits="1" minFractionDigits="1"/></span>
                        <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
                        <c:choose>
                            <c:when test="${reviewCount == 1}">
                                <span class="text-secondary">(<c:out value="${reviewCount}"/> <spring:message code="carCard.review"/>)</span>
                            </c:when>
                            <c:otherwise>
                             <span class="text-secondary">(<c:out value="${reviewCount}"/> <spring:message code="carCard.reviews"/>)</span>
                            </c:otherwise>
                        </c:choose>
                    </p>
                </c:when>
                <c:when test="${not empty ratingAvg and (reviewCount == null or reviewCount == 0)}">
                    <p class="carcard-rating small text-secondary mb-0 mt-1">
                        <span class="fw-semibold text-dark"><fmt:formatNumber value="${ratingAvg}" maxFractionDigits="1" minFractionDigits="1"/></span>
                        <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
                    </p>
                </c:when>
                <c:when test="${(reviewCount == null or reviewCount == 0) and empty ratingAvg}">
                    <p class="carcard-rating small text-secondary mb-0 mt-1">
                        <spring:message code="carCard.noReviews"/>
                    </p>
                </c:when>
            </c:choose>
            <c:if test="${not empty minimumRentalDays and minimumRentalDays > 1}">
                <spring:message code="search.card.minRentalDays" arguments="${minimumRentalDays}" var="minDaysLabel"/>
                <p class="small text-secondary mb-0 mt-1">
                    <i class="bi bi-calendar-check" aria-hidden="true"></i>
                    <c:out value="${minDaysLabel}"/>
                </p>
            </c:if>
        </div>
        <div class="carcard-price text">
            <div class="carcard-price-row">
                <div class="carcard-price-quote">
                    <p class="carcard-price-amount"><fmt:formatNumber value="${price}" type="currency" currencyCode="ARS"/></p>
                    <p>/<c:out value="${pricePeriodLabel}"/></p>
                </div>
                <c:if test="${not empty priceMarketPositionModifier}">
                    <c:choose>
                        <c:when test="${priceMarketPositionModifier eq 'below_market'}">
                            <spring:message code="carCard.priceMarket.below" var="priceMarketBadgeLabel"/>
                        </c:when>
                        <c:when test="${priceMarketPositionModifier eq 'at_market'}">
                            <spring:message code="carCard.priceMarket.at" var="priceMarketBadgeLabel"/>
                        </c:when>
                        <c:otherwise>
                            <spring:message code="carCard.priceMarket.above" var="priceMarketBadgeLabel"/>
                        </c:otherwise>
                    </c:choose>
                    <fmt:formatNumber value="${marketAveragePrice}" type="currency" currencyCode="ARS" var="marketAvgFormatted"/>
                    <spring:message code="carCard.priceMarket.tooltip" arguments="${marketAvgFormatted}, ${marketSampleCount}" var="priceMarketTooltip"/>
                    <span class="carcard-price-market-badge carcard-price-market-badge--${priceMarketPositionModifier}"
                          title="<c:out value='${priceMarketTooltip}'/>"><c:out value="${priceMarketBadgeLabel}"/></span>
                </c:if>
            </div>
        </div>
    </div>
    <c:if test="${not empty href}">
        <spring:message code="carCard.viewAriaLabel" arguments="${brand}, ${model}" var="viewAriaLabel"/>
        <a href="<c:out value='${href}' escapeXml='false'/>" class="stretched-link carcard-stretched-link" aria-label="<c:out value='${viewAriaLabel}'/>"></a>
    </c:if>
</div>
