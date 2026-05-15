<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="baseUrl"       required="true"  type="java.lang.String" %>
<%@ attribute name="currentSort"   required="false" type="java.lang.String" %>
<%@ attribute name="sortParamName" required="false" type="java.lang.String" %>
<%@ attribute name="pageParamName" required="false" type="java.lang.String" %>
<%@ attribute name="wrapperClass"  required="false" type="java.lang.String" %>

<c:set var="sep" value="${fn:contains(baseUrl, '?') ? '&amp;' : '?'}"/>
<c:set var="sp" value="${empty sortParamName ? 'sort' : sortParamName}"/>
<c:set var="pp" value="${empty pageParamName ? 'page' : pageParamName}"/>
<c:set var="wc" value="${empty wrapperClass ? 'd-flex align-items-center gap-2 flex-wrap mb-3' : wrapperClass}"/>

<spring:message code="search.sort.label"      var="sortLabel"/>
<spring:message code="search.sort.dateDesc"   var="lblDateDesc"/>
<spring:message code="search.sort.dateAsc"    var="lblDateAsc"/>
<spring:message code="search.sort.priceAsc"   var="lblPriceAsc"/>
<spring:message code="search.sort.priceDesc"  var="lblPriceDesc"/>
<spring:message code="search.sort.ratingDesc" var="lblRatingDesc"/>
<spring:message code="search.sort.ratingAsc"  var="lblRatingAsc"/>

<c:choose>
    <c:when test="${currentSort == 'date,asc'}">  <c:set var="activeLabel" value="${lblDateAsc}"/></c:when>
    <c:when test="${currentSort == 'price,asc'}"> <c:set var="activeLabel" value="${lblPriceAsc}"/></c:when>
    <c:when test="${currentSort == 'price,desc'}"><c:set var="activeLabel" value="${lblPriceDesc}"/></c:when>
    <c:when test="${currentSort == 'rating,desc'}"><c:set var="activeLabel" value="${lblRatingDesc}"/></c:when>
    <c:when test="${currentSort == 'rating,asc'}"><c:set var="activeLabel" value="${lblRatingAsc}"/></c:when>
    <c:otherwise><c:set var="activeLabel" value="${lblDateDesc}"/></c:otherwise>
</c:choose>

<div class="${wc}">
    <span class="text-secondary small fw-medium flex-shrink-0"><c:out value="${sortLabel}"/>:</span>
    <div class="dropdown">
        <button type="button"
                class="form-select form-select-sm dropdown-toggle ryden-select-btn text-start"
                style="min-width:190px"
                data-bs-toggle="dropdown"
                data-bs-auto-close="true"
                aria-expanded="false"
                aria-label="<c:out value='${sortLabel}'/>">
            <c:out value="${activeLabel}"/>
        </button>
        <ul class="dropdown-menu shadow ryden-select-menu p-1" style="min-width:200px">
            <c:set var="isAct" value="${empty currentSort || currentSort == 'date,desc'}"/>
            <li>
                <a class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                   href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}<c:out value='${sp}' escapeXml='false'/>=date,desc&amp;<c:out value='${pp}' escapeXml='false'/>=0">
                    <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                    <c:out value="${lblDateDesc}"/>
                </a>
            </li>
            <c:set var="isAct" value="${currentSort == 'date,asc'}"/>
            <li>
                <a class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                   href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}<c:out value='${sp}' escapeXml='false'/>=date,asc&amp;<c:out value='${pp}' escapeXml='false'/>=0">
                    <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                    <c:out value="${lblDateAsc}"/>
                </a>
            </li>
            <c:set var="isAct" value="${currentSort == 'price,asc'}"/>
            <li>
                <a class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                   href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}<c:out value='${sp}' escapeXml='false'/>=price,asc&amp;<c:out value='${pp}' escapeXml='false'/>=0">
                    <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                    <c:out value="${lblPriceAsc}"/>
                </a>
            </li>
            <c:set var="isAct" value="${currentSort == 'price,desc'}"/>
            <li>
                <a class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                   href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}<c:out value='${sp}' escapeXml='false'/>=price,desc&amp;<c:out value='${pp}' escapeXml='false'/>=0">
                    <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                    <c:out value="${lblPriceDesc}"/>
                </a>
            </li>
            <c:set var="isAct" value="${currentSort == 'rating,desc'}"/>
            <li>
                <a class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                   href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}<c:out value='${sp}' escapeXml='false'/>=rating,desc&amp;<c:out value='${pp}' escapeXml='false'/>=0">
                    <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                    <c:out value="${lblRatingDesc}"/>
                </a>
            </li>
            <c:set var="isAct" value="${currentSort == 'rating,asc'}"/>
            <li>
                <a class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                   href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}<c:out value='${sp}' escapeXml='false'/>=rating,asc&amp;<c:out value='${pp}' escapeXml='false'/>=0">
                    <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                    <c:out value="${lblRatingAsc}"/>
                </a>
            </li>
        </ul>
    </div>
</div>
