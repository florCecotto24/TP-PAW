<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="baseUrl"     required="true" type="java.lang.String" %>
<%@ attribute name="currentSort" required="false" type="java.lang.String" %>

<c:set var="sep" value="${fn:contains(baseUrl, '?') ? '&amp;' : '?'}"/>

<spring:message code="search.sort.label"    var="sortLabel"/>
<spring:message code="search.sort.dateDesc" var="lblDateDesc"/>
<spring:message code="search.sort.dateAsc"  var="lblDateAsc"/>
<spring:message code="search.sort.priceAsc" var="lblPriceAsc"/>
<spring:message code="search.sort.priceDesc" var="lblPriceDesc"/>

<div class="d-flex align-items-center gap-2 flex-wrap mb-3">
    <span class="text-secondary small fw-medium"><c:out value="${sortLabel}"/>:</span>
    <div class="btn-group btn-group-sm" role="group" aria-label="${sortLabel}">
        <a href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}sort=date,desc&amp;page=0"
           class="btn btn-outline-secondary${empty currentSort || currentSort == 'date,desc' ? ' active' : ''}">
            <c:out value="${lblDateDesc}"/>
        </a>
        <a href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}sort=date,asc&amp;page=0"
           class="btn btn-outline-secondary${currentSort == 'date,asc' ? ' active' : ''}">
            <c:out value="${lblDateAsc}"/>
        </a>
        <a href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}sort=price,asc&amp;page=0"
           class="btn btn-outline-secondary${currentSort == 'price,asc' ? ' active' : ''}">
            <c:out value="${lblPriceAsc}"/>
        </a>
        <a href="<c:out value='${baseUrl}' escapeXml='false'/>${sep}sort=price,desc&amp;page=0"
           class="btn btn-outline-secondary${currentSort == 'price,desc' ? ' active' : ''}">
            <c:out value="${lblPriceDesc}"/>
        </a>
    </div>
</div>
