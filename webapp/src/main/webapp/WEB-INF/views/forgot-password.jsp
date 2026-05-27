<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="forgotPassword.title"/> — <spring:message code="app.title"/></title>
    <%@include file="header.jsp" %>
</head>
<body class="has-fixed-navbar auth-page">
<ryden:navbar/>
<div class="auth-page__shell auth-page__shell--simple">
    <div class="auth-page__main">
        <div class="auth-page__card-wrap auth-page__card-wrap--wide">
            <div class="bg-white rounded-4 shadow-sm p-4 p-md-5">
    <h1 class="h4 mb-3"><spring:message code="forgotPassword.heading"/></h1>
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

    <form:form method="post" action="${pageContext.request.contextPath}/forgot-password" modelAttribute="forgotPasswordRequestForm" cssClass="needs-validation" novalidate="novalidate">
        <div class="mb-4">
            <label for="email" class="form-label"><spring:message code="forgotPassword.email"/></label>
            <form:input path="email" type="email" cssClass="form-control" id="email" required="required" maxlength="${registrationEmailMaxLength}" autocomplete="email"/>
            <form:errors path="email" cssClass="text-danger small d-block" element="div"/>
        </div>
        <button type="submit" class="btn btn-primary w-100"><spring:message code="forgotPassword.submit"/></button>
    </form:form>
    <p class="text-center mt-3 small">
        <a href="<c:url value='/login'/>"><spring:message code="common.back"/></a>
    </p>
            </div>
        </div>
    </div>
</div>
<%@ include file="includes/footerScripts.jspf" %>
</body>
</html>
