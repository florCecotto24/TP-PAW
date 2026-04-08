<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">

    <head>
        <title>Ryden - <spring:message code="error.pageTitle"/></title>
        <%@include file="header.jsp" %>
    </head>

    <body>
        <paw:navbar />

        <div class="d-flex flex-column align-items-center justify-content-center text-center py-5 px-3" style="min-height: calc(100vh - var(--navbar-height, 64px) - 80px);">
            <img src="${pageContext.request.contextPath}/assets/images/sad_mate.png"
                 alt="<spring:message code="error.imageAlt"/>"
                 style="width: min(320px, 80vw); height: auto; margin-bottom: 2rem; opacity: 0.9;">

            <h1 class="fw-bold mb-2" style="font-size: 4rem; color: var(--color-primary, #3b7be0);">${statusCode}</h1>

            <h2 class="fw-semibold mb-3">
                <spring:message code="${messageKey}.title"/>
            </h2>

            <p class="text-muted mb-4" style="max-width: 480px; font-size: 1.05rem;">
                <spring:message code="${messageKey}.desc"/>
            </p>

            <a href="${pageContext.request.contextPath}/"
               class="btn btn-primary btn-action btn-action-md">
                <spring:message code="error.goHome"/>
            </a>
        </div>

        <%@ include file="footer.jsp" %>
    </body>

</html>
