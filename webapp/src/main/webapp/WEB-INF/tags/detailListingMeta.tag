<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="verified" required="false" type="java.lang.Boolean" %>
<%@ attribute name="rating" required="true" type="java.lang.String" %>
<%@ attribute name="reviewCount" required="true" type="java.lang.String" %>
<%@ attribute name="location" required="true" type="java.lang.String" %>

<c:if test="${verified eq null}">
    <c:set var="verified" value="${true}" />
</c:if>

<div class="d-flex flex-wrap align-items-center gap-3 detail-listing-meta mt-2">
    <c:if test="${verified}">
        <span class="badge rounded-pill detail-verified-badge d-inline-flex align-items-center gap-1">
            <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
            Verified
        </span>
    </c:if>
    <span class="d-inline-flex align-items-center gap-1 text-secondary">
        <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
        <span class="text-dark fw-semibold">${rating}</span>
        <span class="text-muted">(${reviewCount} reviews)</span>
    </span>
    <span class="d-inline-flex align-items-center gap-1 text-secondary">
        <i class="bi bi-geo-alt" aria-hidden="true"></i>
        <span class="text-dark">${location}</span>
    </span>
</div>
