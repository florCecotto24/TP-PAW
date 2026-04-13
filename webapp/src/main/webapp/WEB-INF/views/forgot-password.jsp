<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="forgotPassword.title"/> — <spring:message code="app.title"/></title>
    <%@include file="header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container" style="max-width: 440px; margin-top: 5rem;">
    <h1 class="h3 mb-3"><spring:message code="forgotPassword.heading"/></h1>
    <p class="text-muted small mb-4"><spring:message code="forgotPassword.intro"/></p>

    <c:if test="${forgotSessionExpired}">
        <div class="alert alert-warning" role="alert"><spring:message code="forgotPassword.sessionExpired"/></div>
    </c:if>
    <c:if test="${not empty forgotErrorMessage}">
        <div class="alert alert-danger" role="alert"><c:out value="${forgotErrorMessage}"/></div>
    </c:if>
    <c:if test="${forgotFieldError}">
        <div class="alert alert-danger" role="alert"><spring:message code="forgotPassword.fieldRequired"/></div>
    </c:if>
    <c:if test="${forgotGenericHint}">
        <div class="alert alert-info" role="alert"><spring:message code="forgotPassword.genericHint"/></div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/forgot-password" class="needs-validation" novalidate>
        <%@ include file="includes/csrfHidden.jspf" %>
        <div class="mb-4">
            <label for="email" class="form-label"><spring:message code="forgotPassword.email"/></label>
            <input type="email" class="form-control" id="email" name="email" required maxlength="50"
                   value="<c:out value='${forgotEmail}'/>"/>
        </div>
        <button type="submit" class="btn btn-primary w-100"><spring:message code="forgotPassword.submit"/></button>
    </form>
    <p class="text-center mt-3 small">
        <a href="<c:url value='/login'/>"><spring:message code="common.back"/></a>
    </p>
</div>
<%@ include file="includes/footerScripts.jspf" %>
</body>
</html>
