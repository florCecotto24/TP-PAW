<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="seeAllHref" required="false" type="java.lang.String" %>
<%@ attribute name="title" required="false" type="java.lang.String" %>
<%@ attribute name="subtitle" required="false" type="java.lang.String" %>

<c:if test="${empty seeAllHref}">
    <c:set var="seeAllHref" value="${pageContext.request.contextPath}/search" />
</c:if>
<c:if test="${empty title}">
    <spring:message code="similarVehiclesHeader.defaultTitle" var="defaultTitle"/>
    <c:set var="title" value="${defaultTitle}" />
</c:if>
<c:if test="${empty subtitle}">
    <spring:message code="similarVehiclesHeader.defaultSubtitle" var="defaultSubtitle"/>
    <c:set var="subtitle" value="${defaultSubtitle}" />
</c:if>

<div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4 similarVehiclesHeader">
    <div>
        <h2 class="h5 fw-bold mb-1"><c:out value="${title}"/></h2>
        <p class="text-secondary small mb-0"><c:out value="${subtitle}"/></p>
    </div>
    <a href="<c:out value='${seeAllHref}' escapeXml='false'/>" class="d-inline-flex align-items-center gap-1 text-decoration-none fw-semibold similarVehiclesSeeAll">
        <spring:message code="common.seeAll"/>
        <i class="bi bi-arrow-right" aria-hidden="true"></i>
    </a>
</div>
