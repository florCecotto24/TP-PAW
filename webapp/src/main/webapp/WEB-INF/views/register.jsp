<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="register.title"/> — <spring:message code="app.title"/></title>
    <%@include file="header.jsp" %>
</head>
<body class="has-fixed-navbar auth-page">
<ryden:navbar/>
<div class="auth-page__shell auth-page__shell--tall-form">
    <div class="auth-page__brand">
        <a href="<c:url value='/'/>" class="text-decoration-none">
            <span class="auth-page__logo">Ryden</span>
        </a>
    </div>
    <div class="auth-page__main">
        <div class="auth-page__card-wrap auth-page__card-wrap--wide">
    <div class="bg-white rounded-4 shadow-sm p-4 p-md-5">
    <h1 class="h4 mb-3"><spring:message code="register.heading"/></h1>

    <c:if test="${registerEmailTaken}">
        <div class="alert alert-danger" role="alert"><spring:message code="register.emailTaken"/></div>
    </c:if>

    <spring:message code="common.password.show" var="lblPwShow" htmlEscape="true"/>
    <spring:message code="common.password.hide" var="lblPwHide" htmlEscape="true"/>

    <form:form modelAttribute="registrationAccountForm" method="post" cssClass="needs-validation" novalidate="novalidate"
               data-ryden-disable-submit-once="true"
               action="${pageContext.request.contextPath}/register">
        <%@ include file="includes/csrfHidden.jspf" %>
        <form:errors path="*" element="div" cssClass="alert alert-danger" delimiter=" "/>

        <div class="mb-3">
            <label for="forename" class="form-label"><spring:message code="register.forename"/></label>
            <form:input path="forename" id="forename" cssClass="form-control mb-1" maxlength="${registrationDisplayNamePartMaxLength}" autocomplete="given-name" data-ryden-no-punctuation="true"/>
            <form:errors path="forename" cssClass="text-danger small d-block" element="div"/>
        </div>
        <div class="mb-3">
            <label for="surname" class="form-label"><spring:message code="register.surname"/></label>
            <form:input path="surname" id="surname" cssClass="form-control mb-1" maxlength="${registrationDisplayNamePartMaxLength}" autocomplete="family-name" data-ryden-no-punctuation="true"/>
            <form:errors path="surname" cssClass="text-danger small d-block" element="div"/>
        </div>
        <div class="mb-3">
            <label for="email" class="form-label"><spring:message code="register.email"/></label>
            <form:input path="email" id="email" type="email" cssClass="form-control" maxlength="${registrationEmailMaxLength}" required="required"/>
            <form:errors path="email" cssClass="text-danger small d-block" element="div"/>
        </div>
        <div class="mb-3">
            <label for="password" class="form-label"><spring:message code="register.password"/></label>
            <div class="position-relative ryden-pw-wrap">
                <form:password path="password" id="password" cssClass="form-control pe-5" autocomplete="new-password" maxlength="${registrationPasswordMaxLength}" required="required"/>
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
            <label for="passwordConfirm" class="form-label"><spring:message code="register.passwordConfirm"/></label>
            <div class="position-relative ryden-pw-wrap">
                <form:password path="passwordConfirm" id="passwordConfirm" cssClass="form-control pe-5" autocomplete="new-password" maxlength="${registrationPasswordMaxLength}" required="required"/>
                <button type="button" class="btn btn-sm border-0 bg-transparent text-secondary position-absolute top-50 end-0 translate-middle-y me-1 ryden-password-toggle" aria-pressed="false"
                        data-label-show="<c:out value='${lblPwShow}'/>" data-label-hide="<c:out value='${lblPwHide}'/>"
                        aria-label="<c:out value='${lblPwShow}'/>">
                    <i class="bi bi-eye" aria-hidden="true"></i>
                </button>
            </div>
            <form:errors path="passwordConfirm" cssClass="text-danger small d-block" element="div"/>
        </div>
        <button type="submit" class="btn btn-primary w-100"><spring:message code="register.submit"/></button>
    </form:form>
    <p class="text-center mt-3 small">
        <a href="<c:url value='/login'/>"><spring:message code="navbar.login"/></a>
    </p>
    </div>
</div>
</div>
</div>
<%@ include file="includes/footerScripts.jspf" %>
</body>
</html>
