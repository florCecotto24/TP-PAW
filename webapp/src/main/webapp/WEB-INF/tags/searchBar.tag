<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

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
            <input type="text" class="form-control border-0 shadow-none" aria-label="Search by brand, model, or location" id="search_query"
                   name="query" value="<c:out value='${param.query}'/>" placeholder=" ">
            <label for="search_query">Brand, model, or location</label>
        </div>

        <div class="vr flex-shrink-0 d-none d-md-block"></div>

        <div class="d-flex flex-wrap gap-2 flex-grow-1 align-items-end" style="min-width: 14rem;">
            <div class="flex-grow-1" style="min-width: 7rem;">
                <label class="form-label small text-secondary mb-1" for="search_from_picker">From</label>
                <input type="text" class="form-control form-control-sm border-0 shadow-none" id="search_from_picker"
                       readonly placeholder="Date" aria-label="Availability from date"/>
                <input type="hidden" name="from" id="search_from_hidden" value="<c:out value='${fromDateOnly}'/>"/>
            </div>
            <div class="flex-grow-1" style="min-width: 7rem;">
                <label class="form-label small text-secondary mb-1" for="search_until_picker">Until</label>
                <input type="text" class="form-control form-control-sm border-0 shadow-none" id="search_until_picker"
                       readonly placeholder="Date" aria-label="Availability until date"/>
                <input type="hidden" name="until" id="search_until_hidden" value="<c:out value='${untilDateOnly}'/>"/>
            </div>
        </div>

        <div class="vr flex-shrink-0 d-none d-md-block"></div>

        <button type="submit" class="btn btn-primary rounded-3 ms-md-3 p-2 flex-shrink-0" aria-label="Search">
            <i class="bi bi-search fs-5 search-btn" aria-hidden="true"></i>
        </button>
    </div>
</div>
