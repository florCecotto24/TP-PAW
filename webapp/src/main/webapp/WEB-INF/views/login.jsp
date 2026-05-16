<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="app.title"/> — <spring:message code="login.title"/></title>
    <%@include file="header.jsp" %>
</head>
<body class="has-fixed-navbar auth-page">
<ryden:navbar/>
<div class="auth-page__shell">
    <div class="auth-page__brand">
        <a href="<c:url value='/'/>" class="text-decoration-none">
            <span class="auth-page__logo">Ryden</span>
        </a>
    </div>
    <div class="auth-page__main">
        <div class="auth-page__card-wrap">
    <div class="bg-white rounded-4 shadow-sm p-4 p-md-5">
    <h1 class="h4 mb-4"><spring:message code="login.heading"/></h1>

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

    <form method="post" action="${pageContext.request.contextPath}/login" class="needs-validation" novalidate
          data-ryden-disable-submit-once="true">
        <%@ include file="includes/csrfHidden.jspf" %>

        <div class="mb-3">
            <label for="email" class="form-label"><spring:message code="login.email"/></label>
            <input type="email" class="form-control" id="email" name="email" autocomplete="username" required/>
        </div>
        <div class="mb-3">
            <div class="d-flex justify-content-between align-items-baseline mb-1">
                <label for="password" class="form-label mb-0"><spring:message code="login.password"/></label>
                <a href="<c:url value='/forgot-password'/>" class="small text-muted"><spring:message code="login.forgotPassword"/></a>
            </div>
            <div class="position-relative ryden-pw-wrap">
                <input type="password" class="form-control pe-5" id="password" name="password" autocomplete="current-password" required/>
                <button type="button" class="btn btn-sm border-0 bg-transparent text-secondary position-absolute top-50 end-0 translate-middle-y me-1 ryden-password-toggle" aria-pressed="false"
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
    <p class="text-center text-muted small mt-4 mb-0">
        <spring:message code="login.createAccountPrompt"/>
        <a href="<c:url value='/register'/>"><spring:message code="login.createAccountLink"/></a>
    </p>
    <p class="text-center mt-2">
        <a href="<c:url value='/verify-email'/>" class="text-muted small"><spring:message code="login.verifyEmailLink"/></a>
    </p>
    </div>
        </div>
    </div>
</div>
<%@ include file="includes/footerScripts.jspf" %>
</body>
</html>
