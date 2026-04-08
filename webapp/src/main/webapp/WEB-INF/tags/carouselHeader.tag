<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="seeAllHref" required="false" type="java.lang.String" %>
<%@ attribute name="title" required="false" type="java.lang.String" %>
<%@ attribute name="subtitle" required="false" type="java.lang.String" %>
<%@ attribute name="id" required="false" type="java.lang.String" %>

<c:if test="${empty seeAllHref}">
    <c:set var="seeAllHref" value="${pageContext.request.contextPath}/search" />
</c:if>
<c:if test="${empty title}">
    <spring:message code="carouselHeader.defaultTitle" var="defaultTitle"/>
    <c:set var="title" value="${defaultTitle}" />
</c:if>
<c:if test="${empty subtitle}">
    <spring:message code="carouselHeader.defaultSubtitle" var="defaultSubtitle"/>
    <c:set var="subtitle" value="${defaultSubtitle}" />
</c:if>
<c:if test="${empty id}">
    <c:set var="id" value="cheapestCarsCarousel" />
</c:if>

<div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4 carouselHeader">
    <div>
        <h4 class="font-semibold mb-1"><c:out value="${title}"/></h4>
        <p class="text-secondary small mb-0"><c:out value="${subtitle}"/></p>
    </div>
    <div class="d-flex gap-2">
        <button class="btn btn-outline-secondary btn-action btn-action-sm carouselHeader__navBtn" type="button" data-bs-target="#<c:out value='${id}'/>" data-bs-slide="prev">
            <i class="bi bi-chevron-left"></i>
        </button>
        <button class="btn btn-outline-secondary btn-action btn-action-sm carouselHeader__navBtn" type="button" data-bs-target="#<c:out value='${id}'/>" data-bs-slide="next">
            <i class="bi bi-chevron-right"></i>
        </button>
    </div>
</div>
