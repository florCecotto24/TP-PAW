<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>

<%@ attribute name="formId" required="false" type="java.lang.String" %>
<%@ attribute name="formClass" required="false" type="java.lang.String" %>
<%@ attribute name="actionPath" required="false" type="java.lang.String" %>
<%@ attribute name="showFilters" required="false" type="java.lang.Boolean" %>
<%@ attribute name="autoSubmitOnFilterChange" required="false" type="java.lang.Boolean" %>
<%@ attribute name="categoryFilterOptions" required="true" type="java.util.Map" %>
<%@ attribute name="transmissionFilterOptions" required="true" type="java.util.Map" %>
<%@ attribute name="powertrainFilterOptions" required="true" type="java.util.Map" %>
<%@ attribute name="priceFilterOptions" required="true" type="java.util.Map" %>

<c:set var="resolvedFormId" value="${empty formId ? 'exploreSearchForm' : formId}"/>
<c:set var="resolvedFormClass" value="${empty formClass ? 'search-menu w-100' : formClass}"/>
<c:set var="resolvedActionPath" value="${empty actionPath ? '/search' : actionPath}"/>
<c:set var="resolvedShowFilters" value="${showFilters ne false}"/>
<c:set var="resolvedAutoSubmit" value="${autoSubmitOnFilterChange eq true}"/>

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

<c:url var="resolvedActionUrl" value="${resolvedActionPath}"/>

<form id="<c:out value='${resolvedFormId}'/>" class="<c:out value='${resolvedFormClass}'/>" method="get" action="${resolvedActionUrl}">
    <div class="container">
        <div class="d-flex align-items-center bg-white rounded-4 px-3 py-2 shadow-sm gap-2 flex-wrap">
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

    <c:if test="${resolvedShowFilters}">
        <nav class="navbar-expand-lg d-flex justify-content-center align-items-center py-2 flex-wrap gap-1"
             aria-label="Search filters">
            <spring:message code="search.filter.category" var="categoryLabel"/>
            <spring:message code="search.filter.category.helper" var="categoryHelper"/>
            <ryden:exploreFilterDropdown
                    filterLabel="${categoryLabel}"
                    helperText="${categoryHelper}"
                    paramName="category"
                    ariaGroup="category"
                    options="${categoryFilterOptions}"/>
            <spring:message code="search.filter.transmission" var="transmissionLabel"/>
            <spring:message code="search.filter.transmission.helper" var="transmissionHelper"/>
            <ryden:exploreFilterDropdown
                    filterLabel="${transmissionLabel}"
                    helperText="${transmissionHelper}"
                    paramName="transmission"
                    ariaGroup="transmission"
                    options="${transmissionFilterOptions}"/>
            <spring:message code="search.filter.powertrain" var="powertrainLabel"/>
            <spring:message code="search.filter.powertrain.helper" var="powertrainHelper"/>
            <ryden:exploreFilterDropdown
                    filterLabel="${powertrainLabel}"
                    helperText="${powertrainHelper}"
                    paramName="powertrain"
                    ariaGroup="powertrain"
                    options="${powertrainFilterOptions}"/>
            <spring:message code="search.filter.price" var="priceLabel"/>
            <spring:message code="search.filter.price.helper" var="priceHelper"/>
            <ryden:exploreFilterDropdown
                    filterLabel="${priceLabel}"
                    helperText="${priceHelper}"
                    paramName="price"
                    ariaGroup="price"
                    options="${priceFilterOptions}"/>
        </nav>
    </c:if>
</form>

<c:if test="${resolvedShowFilters}">
    <script>
        (function () {
            var form = document.getElementById('<c:out value="${resolvedFormId}"/>');
            if (!form) return;

            function updateBadge(dropdown) {
                var count = dropdown.querySelectorAll('.js-explore-filter:checked').length;
                var badge = dropdown.querySelector('[data-filter-count="true"]');
                if (!badge) return;
                badge.textContent = String(count);
                badge.classList.toggle('d-none', count === 0);
            }

            var dropdowns = form.querySelectorAll('.explore-filter-dropdown');
            dropdowns.forEach(updateBadge);

            form.querySelectorAll('.js-explore-filter').forEach(function (cb) {
                cb.addEventListener('change', function () {
                    var dropdown = cb.closest('.explore-filter-dropdown');
                    if (dropdown) {
                        updateBadge(dropdown);
                    }
                    <c:if test="${resolvedAutoSubmit}">form.submit();</c:if>
                });
            });
        })();
    </script>
</c:if>

