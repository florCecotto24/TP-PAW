<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="shareLabel" required="false" type="java.lang.String" %>
<%@ attribute name="saveLabel" required="false" type="java.lang.String" %>

<c:if test="${empty shareLabel}">
    <spring:message code="detailToolbarActions.share" var="defaultShareLabel"/>
    <c:set var="shareLabel" value="${defaultShareLabel}" />
</c:if>
<c:if test="${empty saveLabel}">
    <spring:message code="detailToolbarActions.save" var="defaultSaveLabel"/>
    <c:set var="saveLabel" value="${defaultSaveLabel}" />
</c:if>

<div class="d-flex flex-shrink-0 gap-2 detail-toolbar-actions">
    <button type="button" class="btn btn-light border rounded-3 d-inline-flex align-items-center gap-2 px-3 py-2" id="detailShareBtn" aria-label="<c:out value='${shareLabel}'/>">
        <i class="bi bi-share" aria-hidden="true"></i>
        <span><c:out value="${shareLabel}"/></span>
    </button>
    <button type="button" class="btn btn-light border rounded-3 d-inline-flex align-items-center gap-2 px-3 py-2" id="detailSaveBtn" aria-label="<c:out value='${saveLabel}'/>">
        <i class="bi bi-heart" aria-hidden="true"></i>
        <span><c:out value="${saveLabel}"/></span>
    </button>
</div>
