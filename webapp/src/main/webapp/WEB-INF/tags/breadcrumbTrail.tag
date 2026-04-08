<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="currentLabel" required="true" type="java.lang.String" %>
<%@ attribute name="homeLabel" required="false" type="java.lang.String" %>
<%@ attribute name="homeHref" required="false" type="java.lang.String" %>

<c:if test="${empty homeLabel}">
    <spring:message code="breadcrumbTrail.defaultHomeLabel" var="defaultHomeLabel"/>
    <c:set var="homeLabel" value="${defaultHomeLabel}" />
</c:if>
<c:if test="${empty homeHref}">
    <c:set var="homeHref" value="${pageContext.request.contextPath}/" />
</c:if>

<spring:message code="breadcrumbTrail.ariaLabel" var="breadcrumbAria"/>
<nav aria-label="<c:out value='${breadcrumbAria}'/>">
    <ol class="breadcrumb mb-2 small">
        <li class="breadcrumb-item">
            <a href="<c:out value='${homeHref}' escapeXml='false'/>" class="text-decoration-none"><c:out value="${homeLabel}"/></a>
        </li>
        <li class="breadcrumb-item active text-muted" aria-current="page"><c:out value="${currentLabel}"/></li>>
    </ol>
</nav>
