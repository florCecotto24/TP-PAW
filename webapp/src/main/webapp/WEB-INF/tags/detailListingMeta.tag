<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="verified" required="false" type="java.lang.Boolean" %>
<%@ attribute name="rating" required="false" type="java.lang.String" %>
<%@ attribute name="reviewCount" required="false" type="java.lang.String" %>

<div class="d-flex flex-wrap align-items-center gap-3 detail-listing-meta mt-2">
    <c:if test="${not empty reviewCount}">
        <span class="d-inline-flex align-items-center gap-1 text-secondary">
            <c:if test="${not empty rating}">
                <span class="text-dark fw-semibold"><c:out value="${rating}"/></span>
                <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
            </c:if>
            <spring:message code="detailListingMeta.reviewsCount" arguments="${reviewCount}" var="reviewsCntText"/>
            <span class="text-muted"><c:out value="${reviewsCntText}"/></span>
        </span>
    </c:if>
</div>
