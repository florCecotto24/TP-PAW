<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="baseUrl"      required="true"  type="java.lang.String" %>
<%@ attribute name="currentSort"  required="false" type="java.lang.String" %>
<%@ attribute name="sortParamName" required="false" type="java.lang.String" %>
<%@ attribute name="pageParamName" required="false" type="java.lang.String" %>
<%@ attribute name="wrapperClass"  required="false" type="java.lang.String" %>

<c:set var="sep" value="${fn:contains(baseUrl, '?') ? '&amp;' : '?'}"/>
<c:set var="resolvedSortParam" value="${empty sortParamName ? 'sort' : sortParamName}"/>
<c:set var="resolvedPageParam" value="${empty pageParamName ? 'page' : pageParamName}"/>
<c:set var="resolvedWrapperClass" value="${empty wrapperClass ? 'd-flex align-items-center gap-2 flex-wrap mb-3' : wrapperClass}"/>

<spring:message code="search.sort.label"      var="sortLabel"/>
<spring:message code="search.sort.dateDesc"   var="lblDateDesc"/>
<spring:message code="search.sort.dateAsc"    var="lblDateAsc"/>
<spring:message code="search.sort.priceAsc"   var="lblPriceAsc"/>
<spring:message code="search.sort.priceDesc"  var="lblPriceDesc"/>
<spring:message code="search.sort.ratingDesc" var="lblRatingDesc"/>
<spring:message code="search.sort.ratingAsc"  var="lblRatingAsc"/>

<div class="${resolvedWrapperClass}">
    <label for="sortSelect_<c:out value='${resolvedSortParam}'/>" class="text-secondary small fw-medium mb-0"><c:out value="${sortLabel}"/>:</label>
    <select id="sortSelect_<c:out value='${resolvedSortParam}'/>" class="form-select form-select-sm w-auto"
            onchange="window.location.href = this.value">
        <option value="<c:out value='${baseUrl}' escapeXml='false'/>${sep}<c:out value='${resolvedSortParam}' escapeXml='false'/>=date,desc&amp;<c:out value='${resolvedPageParam}' escapeXml='false'/>=0"
                ${empty currentSort || currentSort == 'date,desc' ? 'selected' : ''}><c:out value="${lblDateDesc}"/></option>
        <option value="<c:out value='${baseUrl}' escapeXml='false'/>${sep}<c:out value='${resolvedSortParam}' escapeXml='false'/>=date,asc&amp;<c:out value='${resolvedPageParam}' escapeXml='false'/>=0"
                ${currentSort == 'date,asc' ? 'selected' : ''}><c:out value="${lblDateAsc}"/></option>
        <option value="<c:out value='${baseUrl}' escapeXml='false'/>${sep}<c:out value='${resolvedSortParam}' escapeXml='false'/>=price,asc&amp;<c:out value='${resolvedPageParam}' escapeXml='false'/>=0"
                ${currentSort == 'price,asc' ? 'selected' : ''}><c:out value="${lblPriceAsc}"/></option>
        <option value="<c:out value='${baseUrl}' escapeXml='false'/>${sep}<c:out value='${resolvedSortParam}' escapeXml='false'/>=price,desc&amp;<c:out value='${resolvedPageParam}' escapeXml='false'/>=0"
                ${currentSort == 'price,desc' ? 'selected' : ''}><c:out value="${lblPriceDesc}"/></option>
        <option value="<c:out value='${baseUrl}' escapeXml='false'/>${sep}<c:out value='${resolvedSortParam}' escapeXml='false'/>=rating,desc&amp;<c:out value='${resolvedPageParam}' escapeXml='false'/>=0"
                ${currentSort == 'rating,desc' ? 'selected' : ''}><c:out value="${lblRatingDesc}"/></option>
        <option value="<c:out value='${baseUrl}' escapeXml='false'/>${sep}<c:out value='${resolvedSortParam}' escapeXml='false'/>=rating,asc&amp;<c:out value='${resolvedPageParam}' escapeXml='false'/>=0"
                ${currentSort == 'rating,asc' ? 'selected' : ''}><c:out value="${lblRatingAsc}"/></option>
    </select>
</div>
