<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="profile.password.title"/> — <spring:message code="app.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container mt-5 pt-4" style="max-width: 520px;">
    <spring:message code="profile.heading" var="profileLabel"/>
    <spring:message code="profile.password.heading" var="profilePwLabel"/>
    <ryden:breadcrumbTrail homeLabel="${profileLabel}" homeHref="${pageContext.request.contextPath}/profile" currentLabel="${profilePwLabel}"/>
    <h1 class="h3 mb-3"><spring:message code="profile.password.heading"/></h1>
    <p class="text-muted mb-4"><spring:message code="profile.password.intro"/></p>

    <spring:message code="common.password.show" var="lblPwShow" htmlEscape="true"/>
    <spring:message code="common.password.hide" var="lblPwHide" htmlEscape="true"/>

    <form:form modelAttribute="profilePasswordForm" method="post" cssClass="needs-validation" novalidate="novalidate"
               action="${pageContext.request.contextPath}/profile/password">
        <%@ include file="../includes/csrfHidden.jspf" %>
        <form:errors path="*" element="div" cssClass="alert alert-danger" delimiter=" "/>

        <div class="mb-3">
            <label for="currentPassword" class="form-label"><spring:message code="profile.password.current"/></label>
            <div class="position-relative ryden-pw-wrap">
                <form:password path="currentPassword" id="currentPassword" cssClass="form-control pe-5" autocomplete="current-password"/>
                <button type="button" class="btn btn-sm border-0 bg-transparent text-secondary position-absolute top-50 end-0 translate-middle-y me-1 ryden-password-toggle" aria-pressed="false"
                        data-label-show="<c:out value='${lblPwShow}'/>" data-label-hide="<c:out value='${lblPwHide}'/>"
                        aria-label="<c:out value='${lblPwShow}'/>">
                    <i class="bi bi-eye" aria-hidden="true"></i>
                </button>
            </div>
            <form:errors path="currentPassword" cssClass="text-danger small d-block" element="div"/>
        </div>
        <div class="mb-3">
            <label for="password" class="form-label"><spring:message code="profile.password.new"/></label>
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
            <label for="passwordConfirm" class="form-label"><spring:message code="profile.password.newConfirm"/></label>
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
        <button type="submit" class="btn btn-primary me-2"><spring:message code="profile.password.submit"/></button>
        <a class="btn btn-outline-secondary" href="<c:url value='/profile'/>"><spring:message code="common.back"/></a>
    </form:form>
</div>
<%@include file="../footer.jsp" %>
</body>
</html>
