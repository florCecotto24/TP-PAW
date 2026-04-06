<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="filterLabel" required="true" type="java.lang.String" %>
<%@ attribute name="helperText" required="false" type="java.lang.String" %>
<%@ attribute name="paramName" required="true" type="java.lang.String" %>
<%@ attribute name="ariaGroup" required="true" type="java.lang.String" %>
<%@ attribute name="options" required="true" type="java.util.Map" %>

<c:set var="pn" value="${paramName}"/>
<c:set var="pv" value="${paramValues[pn]}"/>
<c:set var="selCount" value="${empty pv ? 0 : fn:length(pv)}"/>

<div class="dropdown explore-filter-dropdown mx-1 my-1">
    <button class="btn btn-light border dropdown-toggle rounded-4 d-inline-flex align-items-center gap-1"
            type="button"
            id="explore_dd_${ariaGroup}"
            data-bs-toggle="dropdown"
            data-bs-auto-close="outside"
            aria-expanded="false"
            aria-haspopup="true"
            <spring:message code="exploreFilterDropdown.ariaLabel" arguments="${filterLabel}" var="ddAriaLabel"/>
            aria-label="${ddAriaLabel}">
        <span class="explore-filter-dropdown__label"><c:out value="${filterLabel}"/></span>
        <c:if test="${selCount gt 0}">
            <span class="badge text-bg-primary rounded-pill"><c:out value="${selCount}"/></span>
        </c:if>
    </button>
    <ul class="dropdown-menu shadow explore-filter-dropdown__panel p-0"
        aria-labelledby="explore_dd_${ariaGroup}">
        <li>
            <h6 class="dropdown-header mb-0"><c:out value="${filterLabel}"/></h6>
        </li>
        <c:if test="${not empty helperText}">
            <li>
                <span class="dropdown-item-text small text-body-secondary px-3 pb-2 d-block"><c:out value="${helperText}"/></span>
            </li>
        </c:if>
        <li><hr class="dropdown-divider my-0"></li>
        <c:forEach var="e" items="${options}">
            <c:set var="isOptSel" value="false"/>
            <c:if test="${not empty pv}">
                <c:forEach items="${pv}" var="v">
                    <c:if test="${v eq e.key}"><c:set var="isOptSel" value="true"/></c:if>
                </c:forEach>
            </c:if>
            <li>
                <label class="dropdown-item d-flex gap-2 align-items-center py-2 px-3 mb-0" for="explore_${ariaGroup}_${e.key}">
                    <input class="form-check-input flex-shrink-0 js-explore-filter mt-0" type="checkbox"
                           name="${paramName}" value="${e.key}"
                           id="explore_${ariaGroup}_${e.key}"
                           <c:if test="${isOptSel}">checked="checked"</c:if> />
                    <span class="small"><c:out value="${e.value}"/></span>
                </label>
            </li>
        </c:forEach>
    </ul>
</div>
