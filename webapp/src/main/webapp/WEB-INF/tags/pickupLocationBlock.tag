<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="label" required="false" type="java.lang.String" %>
<%@ attribute name="address" required="true" type="java.lang.String" %>
<%@ attribute name="mapImageSrc" required="true" type="java.lang.String" %>
<%@ attribute name="mapLinkHref" required="false" type="java.lang.String" %>

<c:if test="${empty label}">
    <spring:message code="pickupLocationBlock.defaultLabel" var="defaultLabel"/>
    <c:set var="label" value="${defaultLabel}" />
</c:if>

<div class="pickup-location-block mt-5">
    <p class="text-uppercase small text-secondary fw-semibold mb-2 letter-spacing-tight"><c:out value="${label}"/></p>
    <h3 class="h4 fw-bold mb-2"><c:out value="${address}"/></h3>
    <c:choose>
        <c:when test="${not empty mapLinkHref}">
            <a href="<c:out value='${mapLinkHref}'/>" class="d-inline-flex align-items-center gap-1 text-decoration-none mb-3">
                <i class="bi bi-map" aria-hidden="true"></i>
                <spring:message code="pickupLocationBlock.viewMap"/>
            </a>
        </c:when>
        <c:otherwise>
            <span class="d-inline-flex align-items-center gap-1 text-primary mb-3">
                <i class="bi bi-map" aria-hidden="true"></i>
                <spring:message code="pickupLocationBlock.viewMap"/>
            </span>
        </c:otherwise>
    </c:choose>
    <div class="rounded-4 overflow-hidden border pickup-map-placeholder">
        <spring:message code="pickupLocationBlock.mapPreviewAlt" var="mapPreviewAlt"/>
        <img src="<c:out value='${mapImageSrc}'/>" class="w-100 d-block" alt="<c:out value='${mapPreviewAlt}'/>">
    </div>
</div>
