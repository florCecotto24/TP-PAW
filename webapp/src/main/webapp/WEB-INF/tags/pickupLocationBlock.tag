<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="label" required="false" type="java.lang.String" %>
<%@ attribute name="address" required="true" type="java.lang.String" %>
<%@ attribute name="mapImageSrc" required="true" type="java.lang.String" %>
<%@ attribute name="mapLinkHref" required="false" type="java.lang.String" %>

<c:if test="${empty label}">
    <c:set var="label" value="PICK-UP / RETURN POINT" />
</c:if>

<div class="pickup-location-block mt-5">
    <p class="text-uppercase small text-secondary fw-semibold mb-2 letter-spacing-tight">${label}</p>
    <h3 class="h4 fw-bold mb-2">${address}</h3>
    <c:choose>
        <c:when test="${not empty mapLinkHref}">
            <a href="${mapLinkHref}" class="d-inline-flex align-items-center gap-1 text-decoration-none mb-3">
                <i class="bi bi-map" aria-hidden="true"></i>
                View map
            </a>
        </c:when>
        <c:otherwise>
            <span class="d-inline-flex align-items-center gap-1 text-primary mb-3">
                <i class="bi bi-map" aria-hidden="true"></i>
                View map
            </span>
        </c:otherwise>
    </c:choose>
    <div class="rounded-4 overflow-hidden border pickup-map-placeholder">
        <img src="${mapImageSrc}" class="w-100 d-block" alt="Map preview">
    </div>
</div>
