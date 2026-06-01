<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="insight" required="false" type="ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight" %>
<%@ attribute name="priceInputId" required="false" type="java.lang.String" %>
<%@ attribute name="initialUserPrice" required="false" type="java.math.BigDecimal" %>
<%@ attribute name="showDefaultPriceHint" required="false" type="java.lang.Boolean" %>

<c:set var="resolvedPriceInputId" value="${empty priceInputId ? 'pricePerDay' : priceInputId}"/>

<div class="ryden-price-insight card border-0 shadow-sm rounded-4 mb-0"
     <c:if test="${not empty insight}">
         data-ryden-price-insight
         data-min="${insight.minPrice}"
         data-max="${insight.maxPrice}"
         data-avg="${insight.averagePrice}"
         <fmt:formatNumber value="${insight.minPrice}" type="currency" currencyCode="ARS" minFractionDigits="0" maxFractionDigits="0" var="minPriceFormatted"/>
         <fmt:formatNumber value="${insight.maxPrice}" type="currency" currencyCode="ARS" minFractionDigits="0" maxFractionDigits="0" var="maxPriceFormatted"/>
         <fmt:formatNumber value="${insight.averagePrice}" type="currency" currencyCode="ARS" minFractionDigits="0" maxFractionDigits="0" var="avgPriceFormatted"/>
     </c:if>
     data-price-input-id="${resolvedPriceInputId}"
     <c:if test="${initialUserPrice != null}">data-initial-user-price="${initialUserPrice}"</c:if>>
    <div class="card-body p-3 p-md-4">
        <label for="${resolvedPriceInputId}" class="form-label required-label mb-2">
            <spring:message code="publishCar.form.pricePerDay"/>
        </label>

        <div class="ryden-price-insight__price-field mb-2">
            <jsp:doBody/>
        </div>

        <c:if test="${not empty insight}">
            <div class="ryden-price-insight__bar-wrap">
                <div class="ryden-price-insight__bar-track" role="presentation">
                    <div class="ryden-price-insight__bar-hit" aria-hidden="true"></div>
                    <div class="ryden-price-insight__bar-gradient"></div>
                    <div class="ryden-price-insight__marker ryden-price-insight__marker--min ryden-price-insight__marker--bound"
                         role="button"
                         tabindex="0"
                         title="${minPriceFormatted}"
                         aria-label="${minPriceFormatted}">
                        <span class="ryden-price-insight__marker-dot"></span>
                    </div>
                    <div class="ryden-price-insight__marker ryden-price-insight__marker--avg" title="${avgPriceFormatted}">
                        <span class="ryden-price-insight__marker-dot"></span>
                    </div>
                    <div class="ryden-price-insight__marker ryden-price-insight__marker--max ryden-price-insight__marker--bound"
                         role="button"
                         tabindex="0"
                         title="${maxPriceFormatted}"
                         aria-label="${maxPriceFormatted}">
                        <span class="ryden-price-insight__marker-dot"></span>
                    </div>
                    <div class="ryden-price-insight__marker ryden-price-insight__marker--user ryden-price-insight__marker--animate d-none"
                         role="slider"
                         tabindex="0"
                         aria-valuemin="0"
                         aria-valuemax="0"
                         aria-valuenow="0"
                         title="">
                        <span class="ryden-price-insight__marker-dot"></span>
                        <span class="ryden-price-insight__marker-label"></span>
                    </div>
                </div>
            </div>

            <div class="d-flex justify-content-between align-items-start gap-2 mt-2 small">
                <div class="text-danger fw-semibold">
                    <spring:message code="car.price.insight.min"/>
                    <div class="text-body fw-normal"><c:out value="${minPriceFormatted}"/></div>
                </div>
                <div class="text-success fw-semibold text-center">
                    <spring:message code="car.price.insight.avg"/>
                    <div class="text-body fw-normal"><c:out value="${avgPriceFormatted}"/></div>
                </div>
                <div class="text-danger fw-semibold text-end">
                    <spring:message code="car.price.insight.max"/>
                    <div class="text-body fw-normal"><c:out value="${maxPriceFormatted}"/></div>
                </div>
            </div>
        </c:if>
    </div>
</div>
