<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="forgotPassword.reset.title"/> — <spring:message code="app.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body class="has-fixed-navbar auth-page">
<ryden:navbar/>
<div class="auth-page__shell auth-page__shell--simple auth-page__shell--tall-form">
    <div class="auth-page__main">
        <div class="auth-page__card-wrap auth-page__card-wrap--wide">
            <div class="bg-white rounded-4 shadow-sm p-4 p-md-5">
    <h1 class="h4 mb-3"><spring:message code="forgotPassword.reset.heading"/></h1>
    <p class="text-muted small mb-4"><spring:message code="forgotPassword.reset.intro"/></p>

    <spring:message code="common.password.show" var="lblPwShow" htmlEscape="true"/>
    <spring:message code="common.password.hide" var="lblPwHide" htmlEscape="true"/>

    <c:if test="${forgotCodeSent}">
        <div class="alert alert-success" role="alert"><spring:message code="forgotPassword.codeSent"/></div>
    </c:if>
    <c:if test="${not empty forgotResetErrorMessage}">
        <div class="alert alert-danger" role="alert"><c:out value="${forgotResetErrorMessage}"/></div>
    </c:if>

    <form:form modelAttribute="forgotPasswordResetForm" method="post" cssClass="needs-validation" novalidate="novalidate"
               action="${pageContext.request.contextPath}/forgot-password/reset">
        <%@ include file="../includes/csrfHidden.jspf" %>
        <form:errors path="*" element="div" cssClass="alert alert-danger" delimiter=" "/>

        <div class="mb-3">
            <label for="code" class="form-label"><spring:message code="forgotPassword.reset.code"/></label>
            <form:input path="code" id="code" cssClass="form-control" maxlength="6" inputmode="numeric" autocomplete="one-time-code"
                        placeholder="000000" data-ryden-digits-only="true" data-max-len="6" pattern="[0-9]{6}"/>
            <form:errors path="code" cssClass="text-danger small d-block" element="div"/>
        </div>
        <div class="mb-3">
            <label for="password" class="form-label"><spring:message code="forgotPassword.reset.newPassword"/></label>
            <div class="position-relative ryden-pw-wrap">
                <form:password path="password" id="password" cssClass="form-control pe-5" autocomplete="new-password" maxlength="${registrationPasswordMaxLength}"/>
                <button type="button" class="btn btn-sm border-0 bg-transparent text-secondary position-absolute top-50 end-0 translate-middle-y me-1 ryden-password-toggle" aria-pressed="false"
                        data-label-show="<c:out value='${lblPwShow}'/>" data-label-hide="<c:out value='${lblPwHide}'/>"
                        aria-label="<c:out value='${lblPwShow}'/>">
                    <i class="bi bi-eye" aria-hidden="true"></i>
                </button>
            </div>
            <form:errors path="password" cssClass="text-danger small d-block" element="div"/>
            <div class="form-text"><spring:message code="registerPassword.hint" arguments="${registrationPasswordMinLength},${registrationPasswordMaxLength}"/></div>
        </div>
        <div class="mb-4">
            <label for="passwordConfirm" class="form-label"><spring:message code="forgotPassword.reset.newPasswordConfirm"/></label>
            <div class="position-relative ryden-pw-wrap">
                <form:password path="passwordConfirm" id="passwordConfirm" cssClass="form-control pe-5" autocomplete="new-password" maxlength="${registrationPasswordMaxLength}"/>
                <button type="button" class="btn btn-sm border-0 bg-transparent text-secondary position-absolute top-50 end-0 translate-middle-y me-1 ryden-password-toggle" aria-pressed="false"
                        data-label-show="<c:out value='${lblPwShow}'/>" data-label-hide="<c:out value='${lblPwHide}'/>"
                        aria-label="<c:out value='${lblPwShow}'/>">
                    <i class="bi bi-eye" aria-hidden="true"></i>
                </button>
            </div>
            <form:errors path="passwordConfirm" cssClass="text-danger small d-block" element="div"/>
        </div>
        <button type="submit" class="btn btn-primary w-100"><spring:message code="forgotPassword.reset.submit"/></button>
    </form:form>
            </div>
        </div>
    </div>
</div>
<%@ include file="../includes/footerScripts.jspf" %>
</body>
</html>
