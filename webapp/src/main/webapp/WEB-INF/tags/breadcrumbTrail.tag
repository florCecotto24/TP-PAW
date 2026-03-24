<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="currentLabel" required="true" type="java.lang.String" %>
<%@ attribute name="homeLabel" required="false" type="java.lang.String" %>
<%@ attribute name="homeHref" required="false" type="java.lang.String" %>

<c:if test="${empty homeLabel}">
    <c:set var="homeLabel" value="Home" />
</c:if>
<c:if test="${empty homeHref}">
    <c:set var="homeHref" value="${pageContext.request.contextPath}/" />
</c:if>

<nav aria-label="breadcrumb">
    <ol class="breadcrumb mb-2 small">
        <li class="breadcrumb-item">
            <a href="${homeHref}" class="text-decoration-none">${homeLabel}</a>
        </li>
        <li class="breadcrumb-item active text-muted" aria-current="page">${currentLabel}</li>
    </ol>
</nav>
