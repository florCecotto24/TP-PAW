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
<%@ attribute name="clearFiltersHref" required="false" type="java.lang.String" %>
<%@ attribute name="showClearFilters" required="false" type="java.lang.Boolean" %>

<c:set var="resolvedFormId" value="${empty formId ? 'exploreSearchForm' : formId}"/>
<c:set var="resolvedFormClass" value="${empty formClass ? 'search-menu w-100' : formClass}"/>
<c:set var="resolvedActionPath" value="${empty actionPath ? '/search' : actionPath}"/>
<c:set var="resolvedShowFilters" value="${showFilters ne false}"/>
<c:set var="resolvedAutoSubmit" value="${autoSubmitOnFilterChange eq true}"/>
<c:set var="resolvedShowClear" value="${showClearFilters eq true}"/>

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
<spring:message code="search.filter.neighborhood.search" var="neighborhoodSearchPh"/>
<spring:message code="search.filter.neighborhood.any" var="neighborhoodAnyLabel"/>
<spring:message code="search.filter.neighborhood" var="neighborhoodLabel"/>
<spring:message code="search.validation.neighborhood.invalid" var="searchNbInvalidMsg" htmlEscape="true"/>

<form id="<c:out value='${resolvedFormId}'/>" class="<c:out value='${resolvedFormClass}'/>" method="get" action="${resolvedActionUrl}"
      data-form-suffix="<c:out value='${resolvedFormId}'/>"
      data-ryden-search-nb-invalid="<c:out value='${searchNbInvalidMsg}'/>">
    <div class="container">
        <div class="bg-white rounded-4 px-3 py-2 shadow-sm">
            <div class="d-flex align-items-end gap-2 flex-wrap">
                <div class="form-floating flex-grow-1" style="min-width: 12rem;">
                    <spring:message code="searchBar.query.ariaLabel" var="queryAriaLabel"/>
                    <spring:message code="searchBar.query.label" var="queryLabel"/>
                    <input type="text" class="form-control border-0 shadow-none" aria-label="<c:out value='${queryAriaLabel}'/>" id="search_query_<c:out value='${resolvedFormId}'/>"
                           name="query" value="<c:out value='${param.query}'/>" placeholder=" ">
                    <label for="search_query_<c:out value='${resolvedFormId}'/>"><c:out value="${queryLabel}"/></label>
                </div>

                <c:if test="${resolvedShowFilters}">
                    <div class="vr flex-shrink-0 d-none d-md-block"></div>
                    <div class="flex-grow-1" style="min-width: 10rem; max-width: 16rem;">
                        <ryden:neighborhoodPicker
                                pickerId="${resolvedFormId}"
                                neighborhoodList="${searchAllNeighborhoods}"
                                anyLabel="${neighborhoodAnyLabel}"
                                searchPlaceholder="${neighborhoodSearchPh}"
                                selectFieldLabel="${neighborhoodLabel}"
                                toggleAriaLabel="${neighborhoodLabel}"
                                mode="get"
                                allowMultiple="true"
                                selectedNeighborhoodIds="${searchSanitizedNeighborhoodIds}"
                                formId="${resolvedFormId}"
                                searchBarInline="true"
                                wrapExtraClass="w-100"/>
                    </div>
                </c:if>

                <div class="vr flex-shrink-0 d-none d-md-block"></div>

                <div class="d-flex flex-wrap gap-2 flex-grow-1 align-items-end" style="min-width: 14rem;">
                    <div class="flex-grow-1" style="min-width: 7rem;">
                        <label class="form-label small text-secondary mb-1" for="search_from_picker_<c:out value='${resolvedFormId}'/>"><spring:message code="searchBar.from"/></label>
                        <spring:message code="searchBar.date.placeholder" var="datePlaceholder"/>
                        <spring:message code="searchBar.from.ariaLabel" var="fromAriaLabel"/>
                        <input type="text" class="form-control form-control-sm border-0 shadow-none" id="search_from_picker_<c:out value='${resolvedFormId}'/>"
                               readonly placeholder="<c:out value='${datePlaceholder}'/>" aria-label="<c:out value='${fromAriaLabel}'/>"/>
                        <input type="hidden" name="from" id="search_from_hidden_<c:out value='${resolvedFormId}'/>" value="<c:out value='${fromDateOnly}'/>"/>
                    </div>
                    <div class="flex-grow-1" style="min-width: 7rem;">
                        <label class="form-label small text-secondary mb-1" for="search_until_picker_<c:out value='${resolvedFormId}'/>"><spring:message code="searchBar.until"/></label>
                        <spring:message code="searchBar.until.ariaLabel" var="untilAriaLabel"/>
                        <input type="text" class="form-control form-control-sm border-0 shadow-none" id="search_until_picker_<c:out value='${resolvedFormId}'/>"
                               readonly placeholder="<c:out value='${datePlaceholder}'/>" aria-label="<c:out value='${untilAriaLabel}'/>"/>
                        <input type="hidden" name="until" id="search_until_hidden_<c:out value='${resolvedFormId}'/>" value="<c:out value='${untilDateOnly}'/>"/>
                    </div>
                </div>

                <div class="vr flex-shrink-0 d-none d-md-block"></div>

                <c:if test="${resolvedShowClear and not empty clearFiltersHref}">
                    <spring:message code="search.empty.reset" var="clearFiltersLabel"/>
                    <a href="<c:out value='${clearFiltersHref}'/>" class="btn btn-outline-primary btn-action btn-action-md flex-shrink-0 align-self-center">
                        <c:out value="${clearFiltersLabel}"/>
                    </a>
                </c:if>

                <spring:message code="searchBar.submit.ariaLabel" var="submitAriaLabel"/>
                <button type="submit" class="btn btn-primary rounded-3 ms-md-3 p-2 flex-shrink-0 align-self-center" aria-label="<c:out value='${submitAriaLabel}'/>">
                    <i class="bi bi-search fs-5 search-btn" aria-hidden="true"></i>
                </button>
            </div>

            <c:if test="${resolvedShowFilters}">
                <spring:message code="search.filters.ariaLabel" var="filtersNavAria"/>
                <div class="border-top mt-2 pt-2 d-flex flex-wrap align-items-center justify-content-center gap-1"
                     role="navigation"
                     aria-label="<c:out value='${filtersNavAria}'/>">
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
                    <spring:message code="search.filter.price.min" var="priceMinLabel"/>
                    <spring:message code="search.filter.price.max" var="priceMaxLabel"/>
                    <c:set var="hasActivePrice" value="${not empty param.priceMin or not empty param.priceMax}"/>
                    <div class="dropdown explore-filter-dropdown mx-1 my-1">
                        <button class="btn btn-light border dropdown-toggle rounded-4 d-inline-flex align-items-center gap-1" type="button"
                                data-bs-toggle="dropdown" data-bs-auto-close="outside"
                                aria-expanded="false">
                            <span class="explore-filter-dropdown__label"><c:out value="${priceLabel}"/></span>
                            <span class="badge text-bg-primary rounded-pill <c:if test='${not hasActivePrice}'>d-none</c:if>" data-filter-count="true">1</span>
                        </button>
                        <div class="dropdown-menu p-3" style="min-width:200px">
                            <div class="mb-2">
                                <label class="form-label small mb-1"><c:out value="${priceMinLabel}"/></label>
                                <input type="number" class="form-control form-control-sm js-price-input" name="priceMin"
                                       min="0" step="1" value="<c:out value='${param.priceMin}'/>"/>
                            </div>
                            <div>
                                <label class="form-label small mb-1"><c:out value="${priceMaxLabel}"/></label>
                                <input type="number" class="form-control form-control-sm js-price-input" name="priceMax"
                                       min="0" step="1" value="<c:out value='${param.priceMax}'/>"/>
                            </div>
                        </div>
                    </div>
                    <spring:message code="search.filter.rating" var="ratingLabel"/>
                    <ryden:exploreFilterDropdown
                            filterLabel="${ratingLabel}"
                            paramName="rating"
                            ariaGroup="rating"
                            options="${ratingFilterOptions}"/>
                </div>
            </c:if>
        </div>
    </div>
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

            <c:if test="${resolvedAutoSubmit}">
            form.querySelectorAll('.js-price-input').forEach(function (input) {
                input.addEventListener('change', function () {
                    form.submit();
                });
            });
            </c:if>
        })();
    </script>
</c:if>
