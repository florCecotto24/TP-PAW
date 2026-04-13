<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="login.title"/> — <spring:message code="app.title"/></title>
    <%@include file="header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container" style="max-width: 420px; margin-top: 6rem;">
    <h1 class="h3 mb-4"><spring:message code="login.heading"/></h1>

    <c:if test="${param.error != null && param.error != 'emailNotValidated'}">
        <div class="alert alert-danger" role="alert">
            <spring:message code="login.error.badCredentials"/>
        </div>
    </c:if>
    <c:if test="${param.registrationComplete != null}">
        <div class="alert alert-success" role="alert">
            <spring:message code="login.registrationComplete"/>
        </div>
    </c:if>
    <c:if test="${param.legacyPasswordSent != null}">
        <div class="alert alert-success" role="alert">
            <spring:message code="login.legacyPasswordSent"/>
        </div>
    </c:if>
    <c:if test="${param.passwordResetDone != null}">
        <div class="alert alert-success" role="alert">
            <spring:message code="login.passwordResetDone"/>
        </div>
    </c:if>
    <c:if test="${param.logout != null}">
        <div class="alert alert-success" role="alert">
            <spring:message code="login.logout.confirmation"/>
        </div>
    </c:if>

    <spring:message code="common.password.show" var="lblPwShow" htmlEscape="true"/>
    <spring:message code="common.password.hide" var="lblPwHide" htmlEscape="true"/>

    <form method="post" action="${pageContext.request.contextPath}/login" class="needs-validation" novalidate>
        <%@ include file="includes/csrfHidden.jspf" %>

        <div class="mb-3">
            <label for="email" class="form-label"><spring:message code="login.email"/></label>
            <input type="email" class="form-control" id="email" name="email" autocomplete="username" required/>
        </div>
        <div class="mb-3">
            <label for="password" class="form-label"><spring:message code="login.password"/></label>
            <div class="input-group">
                <input type="password" class="form-control" id="password" name="password" autocomplete="current-password" required/>
                <button type="button" class="btn btn-outline-secondary ryden-password-toggle" aria-pressed="false"
                        data-label-show="<c:out value='${lblPwShow}'/>" data-label-hide="<c:out value='${lblPwHide}'/>"
                        aria-label="<c:out value='${lblPwShow}'/>">
                    <i class="bi bi-eye" aria-hidden="true"></i>
                </button>
            </div>
        </div>
        <div class="mb-3 form-check">
            <input type="checkbox" class="form-check-input" id="remember-me" name="remember-me"/>
            <label class="form-check-label" for="remember-me"><spring:message code="login.rememberMe"/></label>
        </div>
        <button type="submit" class="btn btn-primary w-100"><spring:message code="login.submit"/></button>
    </form>
    <p class="text-center small mt-2 mb-1">
        <a href="<c:url value='/forgot-password'/>"><spring:message code="login.forgotPassword"/></a>
    </p>
    <p class="text-center text-muted small mt-3 mb-1">
        <spring:message code="login.createAccountPrompt"/>
        <a href="<c:url value='/register'/>"><spring:message code="login.createAccountLink"/></a>
    </p>
    <p class="text-center small">
        <a href="<c:url value='/verify-email'/>"><spring:message code="login.verifyEmailLink"/></a>
    </p>
</div>
<%@ include file="includes/footerScripts.jspf" %>
</body>
</html>
