<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="pickerId" required="true" type="java.lang.String" %>
<%@ attribute name="neighborhoodList" required="true" type="java.util.List" %>
<%@ attribute name="anyLabel" required="true" type="java.lang.String" %>
<%@ attribute name="searchPlaceholder" required="true" type="java.lang.String" %>
<%@ attribute name="selectFieldLabel" required="true" type="java.lang.String" %>
<%@ attribute name="toggleAriaLabel" required="true" type="java.lang.String" %>
<%@ attribute name="mode" required="true" type="java.lang.String" %>
<%@ attribute name="allowMultiple" required="false" type="java.lang.Boolean" %>
<%@ attribute name="selectName" required="false" type="java.lang.String" %>
<%@ attribute name="selectedNeighborhoodId" required="false" type="java.lang.Object" %>
<%@ attribute name="selectedNeighborhoodIds" required="false" type="java.util.List" %>
<%@ attribute name="springPath" required="false" type="java.lang.String" %>
<%@ attribute name="optionEmptyLabel" required="false" type="java.lang.String" %>
<%@ attribute name="cssClass" required="false" type="java.lang.String" %>
<%@ attribute name="cssErrorClass" required="false" type="java.lang.String" %>
<%@ attribute name="htmlEscapeForm" required="false" type="java.lang.Boolean" %>
<%@ attribute name="required" required="false" type="java.lang.Boolean" %>
<%@ attribute name="outerLabel" required="false" type="java.lang.String" %>
<%@ attribute name="outerLabelRequired" required="false" type="java.lang.Boolean" %>
<%@ attribute name="wrapExtraClass" required="false" type="java.lang.String" %>
<%@ attribute name="formId" required="false" type="java.lang.String" %>
<%@ attribute name="searchBarInline" required="false" type="java.lang.Boolean" %>
<%@ attribute name="nbRequiredMessage" required="false" type="java.lang.String" %>
<%-- GET + single selection + springPath: the page with form:form must include a <form:hidden path="..." id="nb_hid_${pickerId}"/> (not inside this tag). --%>

<c:set var="resolvedSelectName" value="${empty selectName ? 'neighborhoodId' : selectName}"/>
<c:set var="nbSearchBar" value="${searchBarInline eq true}"/>
<c:set var="nbAllowMultiple" value="${mode eq 'get' and (allowMultiple ne false)}"/>
<c:set var="nbSpringBoundSingle" value="${mode eq 'get' and not nbAllowMultiple and not empty springPath}"/>
<c:set var="nbRadioGroupName" value="${resolvedSelectName}"/>
<c:if test="${nbSpringBoundSingle}">
    <c:set var="nbRadioGroupName" value="nb_ui_${pickerId}"/>
</c:if>
<c:set var="nbDdBtnId" value="nb_dd_btn_${pickerId}"/>
<c:set var="nbDdTextId" value="nb_dd_text_${pickerId}"/>
<c:set var="nbDdWrapId" value="nb_dd_wrap_${pickerId}"/>
<c:set var="nbFilterId" value="nb_filter_${pickerId}"/>
<c:set var="nbSelId" value="nb_sel_${pickerId}"/>
<c:set var="nbSrcId" value="nb_src_${pickerId}"/>
<c:set var="nbListId" value="nb_list_${pickerId}"/>
<c:set var="nbChosenName" value=""/>
<c:forEach items="${neighborhoodList}" var="nb">
    <c:if test="${selectedNeighborhoodId != null && selectedNeighborhoodId eq nb.id}">
        <c:set var="nbChosenName" value="${nb.name}"/>
    </c:if>
</c:forEach>
<c:set var="nbMultiCount" value="0"/>
<c:choose>
    <c:when test="${mode eq 'get' and nbAllowMultiple}">
        <c:set var="nbMultiCount" value="${empty selectedNeighborhoodIds ? 0 : fn:length(selectedNeighborhoodIds)}"/>
        <c:if test="${nbMultiCount eq 1 and empty nbChosenName}">
            <c:forEach items="${neighborhoodList}" var="nb">
                <c:forEach items="${selectedNeighborhoodIds}" var="sid">
                    <c:if test="${sid == nb.id}"><c:set var="nbChosenName" value="${nb.name}"/></c:if>
                </c:forEach>
            </c:forEach>
        </c:if>
        <c:if test="${nbMultiCount gt 1}">
            <spring:message code="search.filter.neighborhood.multiCount" arguments="${nbMultiCount}" var="nbChosenName" htmlEscape="true"/>
        </c:if>
    </c:when>
    <c:when test="${mode eq 'get' and not nbAllowMultiple}">
        <c:set var="nbMultiCount" value="${selectedNeighborhoodId != null ? 1 : 0}"/>
    </c:when>
</c:choose>
<c:if test="${mode eq 'get' and nbAllowMultiple}">
    <spring:message code="search.filter.neighborhood.multiCount" var="nbMultiFmt" htmlEscape="true"/>
</c:if>

<div class="neighborhood-picker ${wrapExtraClass}<c:if test='${nbSearchBar}'> neighborhood-picker--search-bar</c:if>"<c:if test="${not empty nbRequiredMessage}"> data-ryden-nb-required="<c:out value='${nbRequiredMessage}'/>"</c:if>>
    <c:if test="${not empty outerLabel and not nbSearchBar}">
        <label class="form-label<c:if test="${outerLabelRequired eq true}"> required-label</c:if> mb-1" for="<c:out value='${nbDdBtnId}'/>"><c:out value="${outerLabel}"/></label>
    </c:if>
    <c:choose>
        <c:when test="${mode eq 'get'}">
            <c:if test="${nbSearchBar}">
                <label class="form-label small text-secondary mb-1" for="<c:out value='${nbDdBtnId}'/>"><c:out value="${selectFieldLabel}"/></label>
            </c:if>
            <div id="<c:out value='${nbDdWrapId}'/>"
                 class="dropdown explore-filter-dropdown neighborhood-picker__dropdown w-100"
                 data-nb-any="<c:out value='${anyLabel}'/>"
                 <c:if test="${nbAllowMultiple and not empty nbMultiFmt}">data-nb-multi-fmt="<c:out value='${nbMultiFmt}'/>"</c:if>>
                <button type="button"
                        class="<c:choose><c:when test='${nbSearchBar}'>form-control form-control-sm border-0 shadow-none dropdown-toggle neighborhood-picker__toggle neighborhood-picker__toggle--search-bar d-flex align-items-center gap-2 w-100 text-start</c:when><c:otherwise>form-select dropdown-toggle neighborhood-picker__toggle neighborhood-picker__toggle--form-select text-start</c:otherwise></c:choose>"
                        id="<c:out value='${nbDdBtnId}'/>"
                        data-bs-toggle="dropdown"
                        data-bs-auto-close="outside"
                        aria-expanded="false"
                        aria-haspopup="true"
                        aria-label="<c:out value='${toggleAriaLabel}'/>">
                    <span class="<c:choose><c:when test='${nbSearchBar}'>text-truncate min-w-0</c:when><c:otherwise>text-truncate</c:otherwise></c:choose>" id="<c:out value='${nbDdTextId}'/>"><c:out value="${not empty nbChosenName ? nbChosenName : anyLabel}"/></span>
                    <c:if test="${nbAllowMultiple}">
                        <span id="nb_dd_badge_<c:out value='${pickerId}'/>"
                              class="badge text-bg-primary rounded-pill flex-shrink-0 <c:if test='${nbMultiCount eq 0}'>d-none</c:if>"
                              data-nb-count="true"><c:out value="${nbMultiCount}"/></span>
                    </c:if>
                </button>
                <div class="dropdown-menu dropdown-menu-end shadow explore-filter-dropdown__panel p-0 w-100 neighborhood-picker__menu"
                     style="min-width: 0;"
                     aria-labelledby="<c:out value='${nbDdBtnId}'/>">
                    <div class="px-3 pt-2 pb-1">
                        <label class="visually-hidden" for="<c:out value='${nbFilterId}'/>"><c:out value="${searchPlaceholder}"/></label>
                        <input type="search" class="form-control form-control-sm" id="<c:out value='${nbFilterId}'/>"
                               autocomplete="off" placeholder="<c:out value='${searchPlaceholder}'/>"
                               aria-label="<c:out value='${searchPlaceholder}'/>"/>
                    </div>
                    <div class="neighborhood-picker__scroll px-2 pb-2">
                        <ul class="list-unstyled mb-0" id="<c:out value='${nbListId}'/>">
                            <c:forEach items="${neighborhoodList}" var="nb" varStatus="nbSt">
                                <c:choose>
                                    <c:when test="${nbAllowMultiple}">
                                        <c:set var="nbChecked" value="false"/>
                                        <c:if test="${not empty selectedNeighborhoodIds}">
                                            <c:forEach items="${selectedNeighborhoodIds}" var="sid">
                                                <c:if test="${sid eq nb.id}"><c:set var="nbChecked" value="true"/></c:if>
                                            </c:forEach>
                                        </c:if>
                                    </c:when>
                                    <c:otherwise>
                                        <c:set var="nbChecked" value="${selectedNeighborhoodId != null && selectedNeighborhoodId eq nb.id}"/>
                                    </c:otherwise>
                                </c:choose>
                                <li class="neighborhood-picker__row mb-0"
                                    data-nb-lookup="<c:out value='${fn:toLowerCase(nb.name)}'/>">
                                    <c:choose>
                                        <c:when test="${nbAllowMultiple}">
                                            <label class="dropdown-item d-flex gap-2 align-items-center py-2 px-2 mb-0 rounded-2"
                                                   for="nb_cb_<c:out value='${pickerId}'/>_<c:out value='${nb.id}'/>">
                                                <input type="checkbox"
                                                       class="form-check-input flex-shrink-0 js-neighborhood-pick mt-0"
                                                       name="<c:out value='${resolvedSelectName}'/>"
                                                       value="<c:out value='${nb.id}'/>"
                                                       id="nb_cb_<c:out value='${pickerId}'/>_<c:out value='${nb.id}'/>"
                                                       data-picker-id="<c:out value='${pickerId}'/>"
                                                       <c:if test="${nbChecked}">checked="checked"</c:if> />
                                                <span class="small js-nb-row-name"><c:out value="${nb.name}"/></span>
                                            </label>
                                        </c:when>
                                        <c:otherwise>
                                            <label class="dropdown-item d-flex gap-2 align-items-center py-2 px-2 mb-0 rounded-2"
                                                   for="nb_rb_<c:out value='${pickerId}'/>_<c:out value='${nb.id}'/>">
                                                <input type="radio"
                                                       class="form-check-input flex-shrink-0 js-neighborhood-pick mt-0"
                                                       name="<c:out value='${nbRadioGroupName}'/>"
                                                       value="<c:out value='${nb.id}'/>"
                                                       id="nb_rb_<c:out value='${pickerId}'/>_<c:out value='${nb.id}'/>"
                                                       data-picker-id="<c:out value='${pickerId}'/>"
                                                       <c:if test="${nbChecked}">checked="checked"</c:if>
                                                       <c:if test="${required eq true and nbSt.first and not nbSpringBoundSingle}">required="required"</c:if> />
                                                <span class="small js-nb-row-name"><c:out value="${nb.name}"/></span>
                                            </label>
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                        </ul>
                    </div>
                </div>
            </div>
            <c:if test="${nbSpringBoundSingle}">
                <div id="nb_err_<c:out value='${pickerId}'/>" class="small text-danger mt-1 d-none" role="alert"></div>
            </c:if>
        </c:when>
        <c:otherwise>
            <div id="<c:out value='${nbDdWrapId}'/>"
                 class="dropdown explore-filter-dropdown neighborhood-picker__dropdown w-100"
                 data-nb-any="<c:out value='${anyLabel}'/>">
                <button type="button"
                        class="form-select dropdown-toggle neighborhood-picker__toggle neighborhood-picker__toggle--form-select text-start"
                        id="<c:out value='${nbDdBtnId}'/>"
                        data-bs-toggle="dropdown"
                        data-bs-auto-close="outside"
                        aria-expanded="false"
                        aria-haspopup="true"
                        aria-label="<c:out value='${toggleAriaLabel}'/>">
                    <span class="text-truncate" id="<c:out value='${nbDdTextId}'/>"><c:out value="${not empty nbChosenName ? nbChosenName : anyLabel}"/></span>
                </button>
                <div class="dropdown-menu dropdown-menu-end shadow explore-filter-dropdown__panel p-0 w-100 neighborhood-picker__menu"
                     style="min-width: 0;"
                     aria-labelledby="<c:out value='${nbDdBtnId}'/>">
                    <div class="px-3 pt-2 pb-1">
                        <label class="visually-hidden" for="<c:out value='${nbFilterId}'/>"><c:out value="${searchPlaceholder}"/></label>
                        <input type="search" class="form-control form-control-sm" id="<c:out value='${nbFilterId}'/>"
                               autocomplete="off" placeholder="<c:out value='${searchPlaceholder}'/>"
                               aria-label="<c:out value='${searchPlaceholder}'/>"/>
                    </div>
                    <div class="neighborhood-picker__scroll px-2 pb-2">
                        <c:choose>
                            <c:when test="${mode eq 'spring' and required eq true}">
                                <form:select path="${springPath}" id="<c:out value='${nbSelId}'/>" size="8"
                                             cssClass="form-select form-select-sm border bg-body-secondary bg-opacity-10 ${empty cssClass ? '' : cssClass}"
                                             cssErrorClass="form-select form-select-sm border is-invalid bg-body-secondary bg-opacity-10 ${empty cssErrorClass ? '' : cssErrorClass}"
                                             htmlEscape="${htmlEscapeForm ne false}"
                                             required="required"
                                             aria-label="<c:out value='${selectFieldLabel}'/>"
                                             data-ryden-neighborhood-select="true">
                                    <form:option value="" label="${optionEmptyLabel}"/>
                                    <form:options items="${neighborhoodList}" itemValue="id" itemLabel="name"/>
                                </form:select>
                            </c:when>
                            <c:when test="${mode eq 'spring'}">
                                <form:select path="${springPath}" id="<c:out value='${nbSelId}'/>" size="8"
                                             cssClass="form-select form-select-sm border bg-body-secondary bg-opacity-10 ${empty cssClass ? '' : cssClass}"
                                             cssErrorClass="form-select form-select-sm border is-invalid bg-body-secondary bg-opacity-10 ${empty cssErrorClass ? '' : cssErrorClass}"
                                             htmlEscape="${htmlEscapeForm ne false}"
                                             aria-label="<c:out value='${selectFieldLabel}'/>"
                                             data-ryden-neighborhood-select="true">
                                    <form:option value="" label="${optionEmptyLabel}"/>
                                    <form:options items="${neighborhoodList}" itemValue="id" itemLabel="name"/>
                                </form:select>
                            </c:when>
                        </c:choose>
                    </div>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
    <div id="<c:out value='${nbSrcId}'/>" class="d-none" aria-hidden="true">
        <c:forEach items="${neighborhoodList}" var="nb">
            <span data-nid="<c:out value='${nb.id}'/>" data-name="<c:out value='${nb.name}'/>"></span>
        </c:forEach>
    </div>
    <c:if test="${not empty springPath and (mode eq 'spring' or mode eq 'get')}">
        <form:errors path="${springPath}" cssClass="text-danger d-block mt-1"/>
    </c:if>
</div>

<c:if test="${empty requestScope['rydenNeighborhoodPickerLib']}">
    <c:set var="rydenNeighborhoodPickerLib" value="1" scope="request"/>
    <script>
        (function () {
            if (window.rydenInitNeighborhoodPicker) {
                return;
            }
            function levenshtein(a, b) {
                if (!a.length) {
                    return b.length;
                }
                if (!b.length) {
                    return a.length;
                }
                var i, j, cost;
                var row = [];
                for (j = 0; j <= b.length; j++) {
                    row[j] = j;
                }
                for (i = 1; i <= a.length; i++) {
                    var prev = row[0];
                    row[0] = i;
                    for (j = 1; j <= b.length; j++) {
                        var cur = row[j];
                        cost = a.charAt(i - 1) === b.charAt(j - 1) ? 0 : 1;
                        row[j] = Math.min(prev + cost, Math.min(row[j] + 1, row[j - 1] + 1));
                        prev = cur;
                    }
                }
                return row[b.length];
            }
            window.rydenInitNeighborhoodPicker = function (cfg) {
                var pickerId = cfg.pickerId;
                var formId = cfg.formId || '';
                var listUi = !!cfg.listUi;
                var multiple = !!cfg.multiple;
                var nbSrc = document.getElementById('nb_src_' + pickerId);
                var nbSel = document.getElementById('nb_sel_' + pickerId);
                var nbFlt = document.getElementById('nb_filter_' + pickerId);
                var nbDdWrap = document.getElementById('nb_dd_wrap_' + pickerId);
                var nbDdText = document.getElementById('nb_dd_text_' + pickerId);
                var nbDdBadge = document.getElementById('nb_dd_badge_' + pickerId);
                var nbList = document.getElementById('nb_list_' + pickerId);
                var anyLabel = (nbDdWrap && nbDdWrap.getAttribute('data-nb-any')) || '';
                var multiFmt = (nbDdWrap && nbDdWrap.getAttribute('data-nb-multi-fmt')) || '';
                var allNb = [];
                function readNbRows() {
                    var out = [];
                    if (!nbSrc) {
                        return out;
                    }
                    nbSrc.querySelectorAll('[data-nid]').forEach(function (el) {
                        out.push({
                            id: String(el.getAttribute('data-nid')),
                            name: el.getAttribute('data-name') || ''
                        });
                    });
                    return out;
                }
                function nameFromCatalog(idStr) {
                    if (!idStr || !nbSrc) {
                        return '';
                    }
                    var el = nbSrc.querySelector('[data-nid="' + idStr + '"]');
                    if (!el) {
                        return '';
                    }
                    return String(el.getAttribute('data-name') || '').trim();
                }
                function syncNeighborhoodDropdownLabel() {
                    if (!nbDdText || !nbSel) {
                        return;
                    }
                    var v = nbSel.value;
                    if (!v) {
                        nbDdText.textContent = anyLabel;
                        return;
                    }
                    var vStr = String(v);
                    var label = anyLabel;
                    Array.prototype.forEach.call(nbSel.options, function (opt) {
                        if (String(opt.value) === vStr) {
                            /* Spring form:option may keep option text empty and use the label attribute. */
                            var t = String(opt.label || opt.textContent || '').trim();
                            if (t) {
                                label = t;
                            }
                        }
                    });
                    if (label === anyLabel) {
                        var fromCat = nameFromCatalog(vStr);
                        if (fromCat) {
                            label = fromCat;
                        } else {
                            for (var bi = 0; bi < allNb.length; bi++) {
                                if (allNb[bi].id === vStr) {
                                    label = allNb[bi].name;
                                    break;
                                }
                            }
                        }
                    }
                    nbDdText.textContent = label;
                }
                function nameMatchesQuery(q, nameLower) {
                    if (!q) {
                        return true;
                    }
                    if (nameLower.indexOf(q) !== -1) {
                        return true;
                    }
                    return levenshtein(q, nameLower) <= 2;
                }
                /** Keeps Spring-rendered options; only toggles hidden so the bound value is never wiped. */
                function applySelectNeighborhoodFilter() {
                    if (!nbSel || !nbFlt) {
                        return;
                    }
                    var q = (nbFlt.value || '').trim().toLowerCase();
                    var prevVal = nbSel.value;
                    Array.prototype.forEach.call(nbSel.options, function (opt) {
                        if (!opt.value) {
                            opt.hidden = false;
                            return;
                        }
                        var nm = String(opt.textContent || '').trim().toLowerCase();
                        var ok = nameMatchesQuery(q, nm);
                        opt.hidden = !ok && opt.value !== prevVal;
                    });
                    syncNeighborhoodDropdownLabel();
                }
                function applyNeighborhoodRowFilter() {
                    if (!nbList || !nbFlt) {
                        return;
                    }
                    var q = (nbFlt.value || '').trim().toLowerCase();
                    nbList.querySelectorAll('.neighborhood-picker__row').forEach(function (li) {
                        var key = (li.getAttribute('data-nb-lookup') || '').toLowerCase();
                        var ok = nameMatchesQuery(q, key);
                        li.classList.toggle('d-none', !ok);
                    });
                }
                function syncMultiUi() {
                    if (!multiple || !nbList || !nbDdText) {
                        return;
                    }
                    var checked = nbList.querySelectorAll('.js-neighborhood-pick:checked');
                    var cnt = checked.length;
                    if (nbDdBadge) {
                        nbDdBadge.textContent = String(cnt);
                        nbDdBadge.classList.toggle('d-none', cnt === 0);
                    }
                    if (cnt === 0) {
                        nbDdText.textContent = anyLabel;
                    } else if (cnt === 1) {
                        var nm = checked[0].closest('label');
                        var span = nm ? nm.querySelector('.js-nb-row-name') : null;
                        nbDdText.textContent = span ? String(span.textContent || '').trim() : anyLabel;
                    } else {
                        nbDdText.textContent = multiFmt ? multiFmt.replace(/\{0\}/g, String(cnt)) : String(cnt);
                    }
                }
                var nbHid = document.getElementById('nb_hid_' + pickerId);
                function syncSingleListUi() {
                    if (!nbList || !nbDdText) {
                        return;
                    }
                    var picked = nbList.querySelector('.js-neighborhood-pick:checked');
                    if (!picked) {
                        nbDdText.textContent = anyLabel;
                        if (nbHid) {
                            nbHid.value = '';
                        }
                        return;
                    }
                    var rowLabel = picked.closest('label');
                    var span = rowLabel ? rowLabel.querySelector('.js-nb-row-name') : null;
                    nbDdText.textContent = span ? String(span.textContent || '').trim() : anyLabel;
                    if (nbHid) {
                        nbHid.value = picked.value;
                    }
                }
                allNb = readNbRows();
                if (listUi && multiple) {
                    if (!nbList) {
                        return;
                    }
                    if (nbFlt) {
                        nbFlt.addEventListener('input', applyNeighborhoodRowFilter);
                    }
                    nbList.querySelectorAll('.js-neighborhood-pick').forEach(function (cb) {
                        cb.addEventListener('change', syncMultiUi);
                    });
                    if (nbDdWrap && nbFlt) {
                        nbDdWrap.addEventListener('shown.bs.dropdown', function () {
                            nbFlt.focus();
                        });
                    }
                    applyNeighborhoodRowFilter();
                    syncMultiUi();
                    if (formId) {
                        var formEl = document.getElementById(formId);
                        if (formEl && formEl.getAttribute('data-ryden-nb-submit-bound') !== '1') {
                            formEl.setAttribute('data-ryden-nb-submit-bound', '1');
                            formEl.addEventListener('submit', function (ev) {
                                var bad = false;
                                nbList.querySelectorAll('.js-neighborhood-pick:checked').forEach(function (cb) {
                                    var v = cb.value;
                                    if (!v) {
                                        return;
                                    }
                                    var ok = allNb.some(function (n) { return n.id === v; });
                                    if (!ok) {
                                        bad = true;
                                    }
                                });
                                if (bad) {
                                    ev.preventDefault();
                                    var inv = formEl.getAttribute('data-ryden-search-nb-invalid')
                                        || formEl.getAttribute('data-ryden-nb-invalid') || '';
                                    window.alert(inv);
                                }
                            });
                        }
                    }
                    return;
                }
                if (listUi && !multiple) {
                    if (!nbList) {
                        return;
                    }
                    if (nbFlt) {
                        nbFlt.addEventListener('input', applyNeighborhoodRowFilter);
                    }
                    var nbDdBtn = document.getElementById('nb_dd_btn_' + pickerId);
                    nbList.querySelectorAll('.js-neighborhood-pick').forEach(function (rb) {
                        rb.addEventListener('change', function () {
                            syncSingleListUi();
                            var errElCh = document.getElementById('nb_err_' + pickerId);
                            if (nbDdBtn) {
                                nbDdBtn.classList.remove('is-invalid');
                            }
                            if (errElCh) {
                                errElCh.classList.add('d-none');
                                errElCh.textContent = '';
                            }
                            if (nbDdBtn && window.bootstrap && bootstrap.Dropdown) {
                                var inst = bootstrap.Dropdown.getInstance(nbDdBtn);
                                if (inst) { inst.hide(); }
                            }
                        });
                    });
                    if (nbDdWrap && nbFlt) {
                        nbDdWrap.addEventListener('shown.bs.dropdown', function () {
                            nbFlt.focus();
                        });
                    }
                    if (nbDdWrap) {
                        nbDdWrap.addEventListener('hidden.bs.dropdown', function () {
                            /* After close, the radio 'change' may not have run yet; syncing on the next frame avoids
                             * clearing the hidden while the radio is already selected. */
                            if (window.requestAnimationFrame) {
                                window.requestAnimationFrame(function () {
                                    syncSingleListUi();
                                });
                            } else {
                                syncSingleListUi();
                            }
                        });
                    }
                    applyNeighborhoodRowFilter();
                    if (nbHid && nbHid.value) {
                        nbList.querySelectorAll('.js-neighborhood-pick').forEach(function (rb) {
                            rb.checked = String(rb.value) === String(nbHid.value);
                        });
                    }
                    syncSingleListUi();
                    if (formId) {
                        var formElSingle = document.getElementById(formId);
                        if (formElSingle && formElSingle.getAttribute('data-ryden-nb-submit-bound') !== '1') {
                            formElSingle.setAttribute('data-ryden-nb-submit-bound', '1');
                            formElSingle.addEventListener('submit', function (ev) {
                                var pickedSubmit = nbList.querySelector('.js-neighborhood-pick:checked');
                                if (pickedSubmit && nbHid) {
                                    nbHid.value = pickedSubmit.value;
                                } else {
                                    syncSingleListUi();
                                    pickedSubmit = nbList.querySelector('.js-neighborhood-pick:checked');
                                    if (pickedSubmit && nbHid) {
                                        nbHid.value = pickedSubmit.value;
                                    }
                                }
                                var v = (pickedSubmit && String(pickedSubmit.value || '').trim()) || (nbHid ? String(nbHid.value || '').trim() : '');
                                if (!v) {
                                    ev.preventDefault();
                                    var pickerRoot = nbDdWrap ? nbDdWrap.closest('.neighborhood-picker') : null;
                                    var reqSingle = (pickerRoot && pickerRoot.getAttribute('data-ryden-nb-required'))
                                        || formElSingle.getAttribute('data-ryden-nb-required') || '';
                                    var errEl = document.getElementById('nb_err_' + pickerId);
                                    if (errEl) {
                                        errEl.textContent = reqSingle || '';
                                        errEl.classList.toggle('d-none', !reqSingle);
                                    }
                                    if (nbDdBtn) {
                                        nbDdBtn.classList.add('is-invalid');
                                        nbDdBtn.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                        try {
                                            if (window.bootstrap && bootstrap.Dropdown) {
                                                bootstrap.Dropdown.getOrCreateInstance(nbDdBtn).show();
                                            }
                                        } catch (e1) { /* ignore */ }
                                    }
                                    return;
                                }
                                if (nbDdBtn) {
                                    nbDdBtn.classList.remove('is-invalid');
                                }
                                var errElOk = document.getElementById('nb_err_' + pickerId);
                                if (errElOk) {
                                    errElOk.classList.add('d-none');
                                }
                                var okSingle = allNb.some(function (n) { return n.id === v; });
                                if (!okSingle) {
                                    ev.preventDefault();
                                    var invSingle = formElSingle.getAttribute('data-ryden-search-nb-invalid')
                                        || formElSingle.getAttribute('data-ryden-nb-invalid') || '';
                                    window.alert(invSingle);
                                }
                            });
                        }
                    }
                    return;
                }
                if (!nbSrc || !nbSel) {
                    return;
                }
                if (nbFlt) {
                    nbFlt.addEventListener('input', applySelectNeighborhoodFilter);
                }
                nbSel.addEventListener('change', function () {
                    applySelectNeighborhoodFilter();
                });
                nbSel.addEventListener('blur', function () {
                    syncNeighborhoodDropdownLabel();
                });
                if (nbDdWrap && nbFlt) {
                    nbDdWrap.addEventListener('shown.bs.dropdown', function () {
                        nbFlt.focus();
                    });
                }
                if (nbDdWrap) {
                    nbDdWrap.addEventListener('hidden.bs.dropdown', function () {
                        syncNeighborhoodDropdownLabel();
                    });
                }
                applySelectNeighborhoodFilter();
                window.requestAnimationFrame(function () {
                    syncNeighborhoodDropdownLabel();
                });
                if (formId) {
                    var formEl2 = document.getElementById(formId);
                    if (formEl2 && formEl2.getAttribute('data-ryden-nb-submit-bound') !== '1') {
                        formEl2.setAttribute('data-ryden-nb-submit-bound', '1');
                        formEl2.addEventListener('submit', function (ev) {
                            if (!nbSel || !nbSrc) {
                                return;
                            }
                            var v = nbSel.value;
                            if (!v) {
                                nbSel.setCustomValidity('');
                                return;
                            }
                            var ok2 = allNb.some(function (n) { return n.id === v; });
                            if (!ok2) {
                                ev.preventDefault();
                                var inv2 = formEl2.getAttribute('data-ryden-search-nb-invalid')
                                    || formEl2.getAttribute('data-ryden-nb-invalid') || '';
                                nbSel.setCustomValidity(inv2);
                                nbSel.reportValidity();
                            } else {
                                nbSel.setCustomValidity('');
                            }
                        });
                    }
                }
            };
        })();
    </script>
</c:if>
<script>
    (function () {
        function boot() {
            if (!window.rydenInitNeighborhoodPicker) {
                return;
            }
            window.rydenInitNeighborhoodPicker({
                pickerId: '<c:out value="${pickerId}"/>',
                formId: '<c:out value="${empty formId ? '' : formId}"/>',
                listUi: ${mode eq 'get'},
                multiple: ${mode eq 'get' and nbAllowMultiple}
            });
        }
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', boot);
        } else {
            boot();
        }
    })();
</script>
