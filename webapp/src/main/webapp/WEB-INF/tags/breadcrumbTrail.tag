<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ attribute name="currentLabel" required="true" type="java.lang.String" %>
<%@ attribute name="homeLabel" required="false" type="java.lang.String" %>
<%@ attribute name="homeHref" required="false" type="java.lang.String" %>
<%@ attribute name="midLabel" required="false" type="java.lang.String" %>
<%@ attribute name="midHref" required="false" type="java.lang.String" %>
<%@ attribute name="mid2Label" required="false" type="java.lang.String" %>
<%@ attribute name="mid2Href" required="false" type="java.lang.String" %>
<%@ attribute name="showHome" required="false" type="java.lang.Boolean" %>

<c:if test="${showHome == null}">
    <c:set var="showHome" value="${true}"/>
</c:if>
<c:if test="${showHome and empty homeLabel}">
    <spring:message code="breadcrumbTrail.defaultHomeLabel" var="defaultHomeLabel"/>
    <c:set var="homeLabel" value="${defaultHomeLabel}" />
</c:if>
<c:if test="${showHome and empty homeHref}">
    <c:set var="homeHref" value="${pageContext.request.contextPath}/" />
</c:if>

<spring:message code="breadcrumbTrail.ariaLabel" var="breadcrumbAria"/>
<nav aria-label="<c:out value='${breadcrumbAria}'/>">
    <ol class="breadcrumb mb-2 small">
        <c:if test="${showHome}">
            <li class="breadcrumb-item">
                <a href="<c:out value='${homeHref}' escapeXml='false'/>" class="text-decoration-none"><c:out value="${homeLabel}"/></a>
            </li>
        </c:if>
        <c:if test="${not empty midLabel}">
            <li class="breadcrumb-item">
                <c:choose>
                    <c:when test="${not empty midHref}">
                        <a href="<c:out value='${midHref}' escapeXml='false'/>" class="text-decoration-none"><c:out value="${midLabel}"/></a>
                    </c:when>
                    <c:otherwise><c:out value="${midLabel}"/></c:otherwise>
                </c:choose>
            </li>
        </c:if>
        <c:if test="${not empty mid2Label}">
            <li class="breadcrumb-item">
                <c:choose>
                    <c:when test="${not empty mid2Href}">
                        <a href="<c:out value='${mid2Href}' escapeXml='false'/>" class="text-decoration-none"><c:out value="${mid2Label}"/></a>
                    </c:when>
                    <c:otherwise><c:out value="${mid2Label}"/></c:otherwise>
                </c:choose>
            </li>
        </c:if>
        <li class="breadcrumb-item active text-muted" aria-current="page"><c:out value="${currentLabel}"/></li>
    </ol>
</nav>
