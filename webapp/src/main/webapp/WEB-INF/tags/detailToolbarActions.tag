<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="shareLabel" required="false" type="java.lang.String" %>
<%@ attribute name="saveLabel" required="false" type="java.lang.String" %>

<c:if test="${empty shareLabel}">
    <c:set var="shareLabel" value="Share" />
</c:if>
<c:if test="${empty saveLabel}">
    <c:set var="saveLabel" value="Save" />
</c:if>

<div class="d-flex flex-shrink-0 gap-2 detail-toolbar-actions">
    <button type="button" class="btn btn-light border rounded-3 d-inline-flex align-items-center gap-2 px-3 py-2" id="detailShareBtn" aria-label="${shareLabel}">
        <i class="bi bi-share" aria-hidden="true"></i>
        <span>${shareLabel}</span>
    </button>
    <button type="button" class="btn btn-light border rounded-3 d-inline-flex align-items-center gap-2 px-3 py-2" id="detailSaveBtn" aria-label="${saveLabel}">
        <i class="bi bi-heart" aria-hidden="true"></i>
        <span>${saveLabel}</span>
    </button>
</div>
