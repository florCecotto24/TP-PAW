<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="model" required="true" %>
<%@ attribute name="brand" required="true" %>
<%@ attribute name="price" required="true" %>
<%@ attribute name="image" required="false" %>
<%@ attribute name="pricePeriod" required="false" %>
<%@ attribute name="href" required="false" %>
<%@ attribute name="ratingAvg" required="false" type="java.lang.Number" %>

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
    </div>
    <div class="carcard-info">
        <div class="carcard-info-text text">
            <h4 class="carcard-brand"><c:out value="${brand}"/></h4>
            <p class="carcard-model"><c:out value="${model}"/></p>
            <c:if test="${not empty ratingAvg}">
                <p class="carcard-rating small text-secondary mb-0 mt-1">
                    <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
                    <span class="fw-semibold text-dark"><fmt:formatNumber value="${ratingAvg}" maxFractionDigits="1" minFractionDigits="1"/></span>
                </p>
            </c:if>
        </div>
        <div class="carcard-price text">
            <p class="carcard-price-amount">$<c:out value="${price}"/></p>
            <p>/<c:out value="${pricePeriodLabel}"/></p>
        </div>
    </div>
    <c:if test="${not empty href}">
        <spring:message code="carCard.viewAriaLabel" arguments="${brand}, ${model}" var="viewAriaLabel"/>
        <a href="<c:out value='${href}' escapeXml='false'/>" class="stretched-link carcard-stretched-link" aria-label="<c:out value='${viewAriaLabel}'/>"></a>
    </c:if>
</div>
