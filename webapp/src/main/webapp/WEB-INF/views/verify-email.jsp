<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="verifyEmail.title"/> — <spring:message code="app.title"/></title>
    <%@include file="header.jsp" %>
</head>
<body class="has-fixed-navbar auth-page">
<ryden:navbar/>
<div class="auth-page__shell auth-page__shell--simple auth-page__shell--tall-form">
    <div class="auth-page__main">
        <div class="auth-page__card-wrap auth-page__card-wrap--wide">
            <div class="bg-white rounded-4 shadow-sm p-4 p-md-5">
    <h1 class="h4 mb-3"><spring:message code="verifyEmail.heading"/></h1>
    <p class="text-muted small mb-4"><spring:message code="verifyEmail.intro"/></p>

    <spring:message code="verifyEmail.code.placeholder" var="verifyEmailCodePlaceholder" htmlEscape="true"/>

    <c:if test="${not empty verifyErrorMessage}">
        <div class="alert alert-danger" role="alert"><c:out value="${verifyErrorMessage}"/></div>
    </c:if>
    <c:if test="${verifyFieldError}">
        <div class="alert alert-danger" role="alert"><spring:message code="verifyEmail.fieldRequired"/></div>
    </c:if>
    <c:if test="${verifyFromLogin}">
        <div class="alert alert-warning" role="alert"><spring:message code="verifyEmail.fromLoginBanner"/></div>
    </c:if>
    <c:if test="${verifyResent}">
        <div class="alert alert-success" role="alert"><spring:message code="verifyEmail.resent"/></div>
    </c:if>

    <c:set var="emailLocked" value="${not empty verifyEmailHint or not empty verifyEmail}"/>

    <form:form method="post" action="${pageContext.request.contextPath}/verify-email" modelAttribute="verifyEmailForm" cssClass="needs-validation" novalidate="novalidate">
        <div class="mb-3">
            <label for="email" class="form-label"><spring:message code="verifyEmail.email"/></label>
            <form:input path="email" type="email" cssClass="form-control${emailLocked ? ' form-control-verify-email-locked' : ''}" id="email"
                        required="required" maxlength="${registrationEmailMaxLength}" readonly="${emailLocked ? 'readonly' : ''}" autocomplete="email"/>
            <form:errors path="email" cssClass="text-danger small d-block" element="div"/>
        </div>
        <div class="mb-3">
            <label for="code" class="form-label"><spring:message code="verifyEmail.code"/></label>
            <form:input path="code" type="text" cssClass="form-control" id="code" required="required" maxlength="6" pattern="[0-9]{6}"
                        inputmode="numeric" autocomplete="one-time-code" placeholder="${verifyEmailCodePlaceholder}"
                        data-ryden-digits-only="true" data-max-len="6"/>
            <form:errors path="code" cssClass="text-danger small d-block" element="div"/>
        </div>
        <button type="submit" class="btn btn-primary w-100 mb-2"><spring:message code="verifyEmail.submit"/></button>
    </form:form>

    <form:form method="post" action="${pageContext.request.contextPath}/verify-email/resend" modelAttribute="verifyEmailForm" cssClass="mb-3">
        <form:hidden path="email"/>
        <button type="submit" class="btn btn-outline-secondary w-100"><spring:message code="verifyEmail.resend"/></button>
    </form:form>

    <p class="text-center mt-3 small mb-1">
        <span class="text-muted"><spring:message code="verifyEmail.alreadyVerified"/></span>
        <a class="ms-1" href="<c:url value='/login'/>"><spring:message code="navbar.login"/></a>
    </p>
            </div>
        </div>
    </div>
</div>
<%@ include file="includes/footerScripts.jspf" %>
</body>
</html>
