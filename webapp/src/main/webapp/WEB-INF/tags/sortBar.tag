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
    <label for="sortSelect" class="text-secondary small fw-medium mb-0"><c:out value="${sortLabel}"/>:</label>
    <select id="sortSelect" class="form-select form-select-sm w-auto"
            onchange="window.location.href = this.value">
        <option value="<c:out value='${baseUrl}' escapeXml='false'/>${sep}sort=date,desc&amp;page=0"
                ${empty currentSort || currentSort == 'date,desc' ? 'selected' : ''}><c:out value="${lblDateDesc}"/></option>
        <option value="<c:out value='${baseUrl}' escapeXml='false'/>${sep}sort=date,asc&amp;page=0"
                ${currentSort == 'date,asc' ? 'selected' : ''}><c:out value="${lblDateAsc}"/></option>
        <option value="<c:out value='${baseUrl}' escapeXml='false'/>${sep}sort=price,asc&amp;page=0"
                ${currentSort == 'price,asc' ? 'selected' : ''}><c:out value="${lblPriceAsc}"/></option>
        <option value="<c:out value='${baseUrl}' escapeXml='false'/>${sep}sort=price,desc&amp;page=0"
                ${currentSort == 'price,desc' ? 'selected' : ''}><c:out value="${lblPriceDesc}"/></option>
    </select>
</div>
