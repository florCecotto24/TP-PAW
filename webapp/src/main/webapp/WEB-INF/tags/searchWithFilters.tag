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
<%@ attribute name="allowFlexibleSearch" required="false" type="java.lang.Boolean" %>

<c:set var="resolvedFormId" value="${empty formId ? 'exploreSearchForm' : formId}"/>
<c:set var="resolvedFormClass" value="${empty formClass ? 'search-menu w-100' : formClass}"/>
<c:set var="resolvedActionPath" value="${empty actionPath ? '/search' : actionPath}"/>
<c:set var="resolvedShowFilters" value="${showFilters ne false}"/>
<c:set var="resolvedAutoSubmit" value="${autoSubmitOnFilterChange eq true}"/>
<c:set var="resolvedShowClear" value="${showClearFilters eq true}"/>
<c:set var="resolvedAllowFlex" value="${allowFlexibleSearch eq true}"/>
<c:set var="isFlexActive" value="${resolvedAllowFlex and param.flexible eq 'true'}"/>

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
            <div class="d-flex align-items-stretch gap-2 flex-wrap">
                <div class="flex-grow-1" style="min-width: 12rem;">
                    <spring:message code="searchBar.query.ariaLabel" var="queryAriaLabel"/>
                    <spring:message code="searchBar.query.label" var="queryLabel"/>
                    <label class="form-label small text-secondary text-uppercase mb-1" for="search_query_<c:out value='${resolvedFormId}'/>"><c:out value="${queryLabel}"/></label>
                    <input type="text" class="form-control form-control-sm border-0 shadow-none" aria-label="<c:out value='${queryAriaLabel}'/>" id="search_query_<c:out value='${resolvedFormId}'/>"
                           name="query" value="<c:out value='${param.query}'/>">
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

                <div class="flex-grow-1" style="min-width: 14rem;">
                    <div class="ryden-date-stack">
                        <%-- Exact date range --%>
                        <div class="js-exact-date-range<c:if test='${isFlexActive}'> js-date-mode-hidden</c:if>" style="display:grid;grid-template-columns:1fr 1fr;column-gap:.5rem;">
                            <div>
                                <label class="form-label small text-secondary mb-1" for="search_from_picker_<c:out value='${resolvedFormId}'/>"><spring:message code="searchBar.from"/></label>
                                <spring:message code="searchBar.date.placeholder" var="datePlaceholder"/>
                                <spring:message code="searchBar.from.ariaLabel" var="fromAriaLabel"/>
                                <input type="text" class="form-control form-control-sm border-0 shadow-none" id="search_from_picker_<c:out value='${resolvedFormId}'/>"
                                       readonly placeholder="<c:out value='${datePlaceholder}'/>" aria-label="<c:out value='${fromAriaLabel}'/>"/>
                                <input type="hidden" name="from" id="search_from_hidden_<c:out value='${resolvedFormId}'/>" value="<c:out value='${fromDateOnly}'/>"/>
                            </div>
                            <div>
                                <label class="form-label small text-secondary mb-1" for="search_until_picker_<c:out value='${resolvedFormId}'/>"><spring:message code="searchBar.until"/></label>
                                <spring:message code="searchBar.until.ariaLabel" var="untilAriaLabel"/>
                                <input type="text" class="form-control form-control-sm border-0 shadow-none" id="search_until_picker_<c:out value='${resolvedFormId}'/>"
                                       readonly placeholder="<c:out value='${datePlaceholder}'/>" aria-label="<c:out value='${untilAriaLabel}'/>"/>
                                <input type="hidden" name="until" id="search_until_hidden_<c:out value='${resolvedFormId}'/>" value="<c:out value='${untilDateOnly}'/>"/>
                            </div>
                        </div>
                        <c:if test="${resolvedAllowFlex}">
                            <%-- Flexible date controls --%>
                            <div class="js-flexible-controls<c:if test='${!isFlexActive}'> js-date-mode-hidden</c:if>" style="display:grid;grid-template-columns:1fr 1fr;column-gap:.5rem;">
                                <div>
                                    <spring:message code="search.flexible.month.label" var="flexMonthLabel"/>
                                    <label class="form-label small text-secondary mb-1" for="search_flexmonth_<c:out value='${resolvedFormId}'/>">
                                        <c:out value="${flexMonthLabel}"/>
                                    </label>
                                    <div class="dropdown js-flex-month-wrapper">
                                        <button type="button"
                                                id="search_flexmonth_<c:out value='${resolvedFormId}'/>"
                                                class="form-control form-control-sm border-0 shadow-none dropdown-toggle d-flex align-items-center w-100 text-start"
                                                data-bs-toggle="dropdown"
                                                aria-expanded="false">
                                            <span class="text-truncate min-w-0 js-flex-month-display">&mdash;</span>
                                        </button>
                                        <div class="dropdown-menu shadow js-flex-month-menu" style="max-height:14rem;overflow-y:auto;min-width:100%"></div>
                                        <input type="hidden" name="flexMonth" class="js-flex-month-hidden" value="<c:out value='${param.flexMonth}'/>"/>
                                    </div>
                                </div>
                                <div>
                                    <spring:message code="search.flexible.days.label" var="flexDaysLabel"/>
                                    <spring:message code="search.flexible.anyDays" var="anyDaysPlaceholder"/>
                                    <label class="form-label small text-secondary mb-1" for="search_flexdays_<c:out value='${resolvedFormId}'/>">
                                        <c:out value="${flexDaysLabel}"/>
                                    </label>
                                    <div class="d-flex align-items-center gap-0">
                                        <button type="button"
                                                class="btn rounded-circle border border-primary text-primary bg-transparent flex-shrink-0 d-flex align-items-center justify-content-center js-flexdays-dec d-none"
                                                style="width:1.75rem;height:1.75rem;padding:0;" aria-label="-">
                                            <i class="bi bi-dash" style="font-size:1rem;line-height:1;" aria-hidden="true"></i>
                                        </button>
                                        <input type="number" name="flexDays" id="search_flexdays_<c:out value='${resolvedFormId}'/>"
                                               min="1" max="31" step="1"
                                               class="form-control form-control-sm border-0 shadow-none js-flexdays-input ryden-no-spinner"
                                               placeholder="<c:out value='${anyDaysPlaceholder}'/>"
                                               value="<c:out value='${param.flexDays}'/>"/>
                                        <button type="button"
                                                class="btn rounded-circle border border-primary text-primary bg-transparent flex-shrink-0 d-flex align-items-center justify-content-center js-flexdays-inc d-none"
                                                style="width:1.75rem;height:1.75rem;padding:0;" aria-label="+">
                                            <i class="bi bi-plus" style="font-size:1rem;line-height:1;" aria-hidden="true"></i>
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </c:if>
                    </div>
                    <c:if test="${resolvedAllowFlex}">
                        <%-- Toggle below date inputs, inside date column --%>
                        <spring:message code="search.flexible.toggle" var="flexToggleLabel"/>
                        <div class="form-check form-switch d-flex align-items-center gap-2 mt-2 ps-0">
                            <input class="form-check-input flex-shrink-0 ms-0 js-flexible-toggle" type="checkbox" role="switch"
                                   id="search_flex_<c:out value='${resolvedFormId}'/>"
                                   name="flexible" value="true"
                                   style="cursor:pointer;"
                                   <c:if test="${isFlexActive}">checked</c:if>/>
                            <label class="form-check-label small text-secondary mb-0"
                                   for="search_flex_<c:out value='${resolvedFormId}'/>">
                                <c:out value="${flexToggleLabel}"/>
                            </label>
                        </div>
                    </c:if>
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

<style>
    .ryden-no-spinner::-webkit-outer-spin-button,
    .ryden-no-spinner::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
    .ryden-no-spinner { -moz-appearance: textfield; }
</style>

<script>
    (function () {
        function daysInMonth(yyyyMM) {
            var parts = yyyyMM ? yyyyMM.split('-') : [];
            if (parts.length < 2) { return 31; }
            return new Date(parseInt(parts[0], 10), parseInt(parts[1], 10), 0).getDate();
        }

        var monthWrappers = document.querySelectorAll('#<c:out value="${resolvedFormId}"/> .js-flex-month-wrapper');
        monthWrappers.forEach(function (wrapper) {
            var menu = wrapper.querySelector('.js-flex-month-menu');
            var display = wrapper.querySelector('.js-flex-month-display');
            var hidden = wrapper.querySelector('.js-flex-month-hidden');
            if (!menu || !display || !hidden) { return; }
            var selected = hidden.value || '';
            var now = new Date();
            var firstVal = null, firstLabel = null;
            for (var i = 0; i < 12; i++) {
                var d = new Date(now.getFullYear(), now.getMonth() + i, 1);
                var y = d.getFullYear();
                var mo = d.getMonth() + 1;
                var val = y + '-' + (mo < 10 ? '0' + mo : mo);
                var lbl = d.toLocaleDateString(navigator.language || 'es-AR', { month: 'long', year: 'numeric' });
                lbl = lbl.charAt(0).toUpperCase() + lbl.slice(1);
                if (i === 0) { firstVal = val; firstLabel = lbl; }
                var btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'dropdown-item small px-3 py-1';
                btn.setAttribute('data-val', val);
                btn.textContent = lbl;
                if (val === selected) { btn.classList.add('active'); display.textContent = lbl; }
                menu.appendChild(btn);
            }
            if (!selected && firstVal) {
                hidden.value = firstVal;
                display.textContent = firstLabel;
                var first = menu.querySelector('.dropdown-item');
                if (first) { first.classList.add('active'); }
            }
            menu.addEventListener('click', function (e) {
                var item = e.target.closest('.dropdown-item[data-val]');
                if (!item) { return; }
                hidden.value = item.getAttribute('data-val');
                display.textContent = item.textContent;
                menu.querySelectorAll('.dropdown-item').forEach(function (el) { el.classList.remove('active'); });
                item.classList.add('active');
                var toggler = wrapper.querySelector('[data-bs-toggle="dropdown"]');
                if (toggler && window.bootstrap && bootstrap.Dropdown) {
                    var inst = bootstrap.Dropdown.getInstance(toggler);
                    if (inst) { inst.hide(); }
                }
                var form = wrapper.closest('form');
                if (form) {
                    var daysInp = form.querySelector('.js-flexdays-input');
                    if (daysInp) {
                        var maxD = daysInMonth(hidden.value);
                        daysInp.max = maxD;
                        if (daysInp.value !== '' && parseInt(daysInp.value, 10) > maxD) {
                            daysInp.value = maxD;
                        }
                    }
                }
            });
            var initForm = wrapper.closest('form');
            if (initForm) {
                var initDaysInp = initForm.querySelector('.js-flexdays-input');
                if (initDaysInp && hidden.value) { initDaysInp.max = daysInMonth(hidden.value); }
            }
        });

        var flexForm = document.getElementById('<c:out value="${resolvedFormId}"/>');
        if (flexForm) {
            var toggle = flexForm.querySelector('.js-flexible-toggle');
            var exactRange = flexForm.querySelector('.js-exact-date-range');
            var flexControls = flexForm.querySelector('.js-flexible-controls');

            function syncFlexMode(isFlexible) {
                if (exactRange) { exactRange.classList.toggle('js-date-mode-hidden', isFlexible); }
                if (flexControls) { flexControls.classList.toggle('js-date-mode-hidden', !isFlexible); }
                if (!isFlexible) {
                    var flexMonth = flexForm.querySelector('[name="flexMonth"]');
                    var flexDays = flexForm.querySelector('[name="flexDays"]');
                    if (flexDays) { flexDays.value = ''; }
                } else {
                    var fromHid = flexForm.querySelector('[name="from"]');
                    var untilHid = flexForm.querySelector('[name="until"]');
                    if (fromHid) { fromHid.value = ''; }
                    if (untilHid) { untilHid.value = ''; }
                }
            }

            if (toggle) {
                toggle.addEventListener('change', function () {
                    syncFlexMode(toggle.checked);
                });
            }

            var flexDaysInput = flexForm ? flexForm.querySelector('.js-flexdays-input') : null;
            if (flexDaysInput) {
                var decBtn = flexForm.querySelector('.js-flexdays-dec');
                var incBtn = flexForm.querySelector('.js-flexdays-inc');
                function clampDays(val) {
                    var min = parseInt(flexDaysInput.min, 10) || 1;
                    var max = parseInt(flexDaysInput.max, 10) || 31;
                    return Math.min(max, Math.max(min, val));
                }
                function syncDaysButtons() {
                    var hasValue = flexDaysInput.value.trim() !== '';
                    if (decBtn) { decBtn.classList.toggle('d-none', !hasValue); }
                    if (incBtn) { incBtn.classList.toggle('d-none', !hasValue); }
                    flexDaysInput.classList.toggle('text-center', hasValue);
                    flexDaysInput.style.width = hasValue ? '3.5rem' : '';
                }
                syncDaysButtons();
                flexDaysInput.addEventListener('input', syncDaysButtons);
                if (decBtn) {
                    decBtn.addEventListener('click', function () {
                        var cur = parseInt(flexDaysInput.value, 10);
                        flexDaysInput.value = isNaN(cur) ? '' : clampDays(cur - 1);
                        syncDaysButtons();
                    });
                }
                if (incBtn) {
                    incBtn.addEventListener('click', function () {
                        var cur = parseInt(flexDaysInput.value, 10);
                        flexDaysInput.value = isNaN(cur) ? 1 : clampDays(cur + 1);
                        syncDaysButtons();
                    });
                }
            }
        }
    })();
</script>

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

            form.addEventListener('submit', function () {
                var btn = form.querySelector('button[type="submit"]');
                if (!btn) return;
                var icon = btn.querySelector('i');
                if (icon) {
                    icon.className = 'spinner-border spinner-border-sm';
                    icon.setAttribute('role', 'status');
                }
                btn.setAttribute('aria-busy', 'true');
                btn.disabled = true;
            });
        })();
    </script>
</c:if>
