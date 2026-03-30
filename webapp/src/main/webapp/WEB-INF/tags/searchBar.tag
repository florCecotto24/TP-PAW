<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="fromRaw" value="${param.from}"/>
<c:set var="untilRaw" value="${param.until}"/>
<c:choose>
    <c:when test="${not empty fromRaw and fn:length(fromRaw) == 10}">
        <c:set var="fromVal" value="${fromRaw}T00:00"/>
    </c:when>
    <c:otherwise>
        <c:set var="fromVal" value="${fromRaw}"/>
    </c:otherwise>
</c:choose>
<c:choose>
    <c:when test="${not empty untilRaw and fn:length(untilRaw) == 10}">
        <c:set var="untilVal" value="${untilRaw}T23:59"/>
    </c:when>
    <c:otherwise>
        <c:set var="untilVal" value="${untilRaw}"/>
    </c:otherwise>
</c:choose>

<div class="container mb-4">
    <div class="d-flex align-items-center bg-white rounded-4 px-3 py-2 shadow gap-2 flex-wrap">

        <div class="form-floating flex-grow-1" style="min-width: 12rem;">
            <input type="text" class="form-control border-0 shadow-none" aria-label="Search by brand, model, or location" id="search_query"
                   name="query" value="<c:out value='${param.query}'/>" placeholder=" ">
            <label for="search_query">Brand, model, or location</label>
        </div>

        <div class="vr flex-shrink-0 d-none d-md-block"></div>

        <div class="flex-grow-1" style="min-width: 13rem;">
            <label class="form-label small text-secondary mb-1" for="search_from_d">From</label>
            <div class="input-group input-group-sm shadow-none" data-paw-dtpair-wrap data-paw-hidden="search_from_hidden" data-paw-date="search_from_d" data-paw-time="search_from_t">
                <input type="hidden" name="from" id="search_from_hidden" value="<c:out value='${fromVal}'/>"/>
                <span class="input-group-text border-0 bg-light"><i class="bi bi-calendar3" aria-hidden="true"></i></span>
                <input type="date" class="form-control border-0 shadow-none" id="search_from_d" aria-label="From date"/>
                <span class="input-group-text border-0 bg-light"><i class="bi bi-clock" aria-hidden="true"></i></span>
                <input type="time" class="form-control border-0 shadow-none" id="search_from_t" step="60" value="00:00" aria-label="From time"/>
            </div>
        </div>

        <div class="vr flex-shrink-0 d-none d-md-block"></div>

        <div class="flex-grow-1" style="min-width: 13rem;">
            <label class="form-label small text-secondary mb-1" for="search_until_d">Until</label>
            <div class="input-group input-group-sm shadow-none" data-paw-dtpair-wrap data-paw-hidden="search_until_hidden" data-paw-date="search_until_d" data-paw-time="search_until_t">
                <input type="hidden" name="until" id="search_until_hidden" value="<c:out value='${untilVal}'/>"/>
                <span class="input-group-text border-0 bg-light"><i class="bi bi-calendar3" aria-hidden="true"></i></span>
                <input type="date" class="form-control border-0 shadow-none" id="search_until_d" aria-label="Until date"/>
                <span class="input-group-text border-0 bg-light"><i class="bi bi-clock" aria-hidden="true"></i></span>
                <input type="time" class="form-control border-0 shadow-none" id="search_until_t" step="60" value="00:00" aria-label="Until time"/>
            </div>
        </div>

        <button type="submit" class="btn btn-primary rounded-3 ms-md-3 p-2 flex-shrink-0" aria-label="Search">
            <i class="bi bi-search fs-5 search-btn" aria-hidden="true"></i>
        </button>
    </div>
</div>
