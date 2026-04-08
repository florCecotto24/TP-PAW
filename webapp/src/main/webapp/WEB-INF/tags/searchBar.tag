<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="fromRaw" value="${param.from}"/>
<c:set var="untilRaw" value="${param.until}"/>
<c:set var="fromDateOnly" value=""/>
<c:if test="${not empty fromRaw}">
    <c:set var="fromDateOnly" value="${fn:length(fromRaw) ge 10 ? fn:substring(fromRaw, 0, 10) : fromRaw}"/>
</c:if>
<c:set var="untilDateOnly" value=""/>
<c:if test="${not empty untilRaw}">
    <c:set var="untilDateOnly" value="${fn:length(untilRaw) ge 10 ? fn:substring(untilRaw, 0, 10) : untilRaw}"/>
</c:if>

<div class="container mb-4">
    <div class="d-flex align-items-center bg-white rounded-4 px-3 py-2 shadow gap-2 flex-wrap">

        <div class="form-floating flex-grow-1" style="min-width: 12rem;">
            <spring:message code="searchBar.query.ariaLabel" var="queryAriaLabel"/>
            <spring:message code="searchBar.query.label" var="queryLabel"/>
            <input type="text" class="form-control border-0 shadow-none" aria-label="<c:out value='${queryAriaLabel}'/>" id="search_query"
                   name="query" value="<c:out value='${param.query}'/>" placeholder=" ">
            <label for="search_query"><c:out value="${queryLabel}"/></label>
        </div>

        <div class="vr flex-shrink-0 d-none d-md-block"></div>

        <div class="d-flex flex-wrap gap-2 flex-grow-1 align-items-end" style="min-width: 14rem;">
            <div class="flex-grow-1" style="min-width: 7rem;">
                <label class="form-label small text-secondary mb-1" for="search_from_picker"><spring:message code="searchBar.from"/></label>
                <spring:message code="searchBar.date.placeholder" var="datePlaceholder"/>
                <spring:message code="searchBar.from.ariaLabel" var="fromAriaLabel"/>
                <input type="text" class="form-control form-control-sm border-0 shadow-none" id="search_from_picker"
                       readonly placeholder="<c:out value='${datePlaceholder}'/>" aria-label="<c:out value='${fromAriaLabel}'/>"/>
                <input type="hidden" name="from" id="search_from_hidden" value="<c:out value='${fromDateOnly}'/>"/>
            </div>
            <div class="flex-grow-1" style="min-width: 7rem;">
                <label class="form-label small text-secondary mb-1" for="search_until_picker"><spring:message code="searchBar.until"/></label>
                <spring:message code="searchBar.until.ariaLabel" var="untilAriaLabel"/>
                <input type="text" class="form-control form-control-sm border-0 shadow-none" id="search_until_picker"
                       readonly placeholder="<c:out value='${datePlaceholder}'/>" aria-label="<c:out value='${untilAriaLabel}'/>"/>
                <input type="hidden" name="until" id="search_until_hidden" value="<c:out value='${untilDateOnly}'/>"/>
            </div>
        </div>

        <div class="vr flex-shrink-0 d-none d-md-block"></div>

        <spring:message code="searchBar.submit.ariaLabel" var="submitAriaLabel"/>
        <button type="submit" class="btn btn-primary rounded-3 ms-md-3 p-2 flex-shrink-0" aria-label="<c:out value='${submitAriaLabel}'/>">
            <i class="bi bi-search fs-5 search-btn" aria-hidden="true"></i>
        </button>
    </div>
</div>
