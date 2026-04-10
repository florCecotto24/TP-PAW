<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="registerPassword.title"/> — <spring:message code="app.title"/></title>
    <%@include file="header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container" style="max-width: 440px; margin-top: 5rem;">
    <h1 class="h3 mb-3"><spring:message code="registerPassword.heading"/></h1>
    <p class="text-muted small mb-4"><spring:message code="registerPassword.hint" arguments="${registrationPasswordMinLength}"/></p>

    <c:if test="${not empty passwordErrorMessage}">
        <div class="alert alert-danger" role="alert"><c:out value="${passwordErrorMessage}"/></div>
    </c:if>

    <form:form modelAttribute="registrationPasswordForm" method="post"
               action="${pageContext.request.contextPath}/register/password" cssClass="needs-validation" novalidate="novalidate">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <form:errors path="*" element="div" cssClass="alert alert-danger" delimiter=" "/>

        <div class="mb-3">
            <label for="password" class="form-label"><spring:message code="registerPassword.password"/></label>
            <form:password path="password" id="password" cssClass="form-control"
                           autocomplete="new-password" maxlength="200" htmlEscape="true"/>
            <form:errors path="password" cssClass="text-danger small d-block" element="div"/>
        </div>
        <div class="mb-4">
            <label for="passwordConfirm" class="form-label"><spring:message code="registerPassword.passwordConfirm"/></label>
            <form:password path="passwordConfirm" id="passwordConfirm" cssClass="form-control"
                           autocomplete="new-password" maxlength="200" htmlEscape="true"/>
            <form:errors path="passwordConfirm" cssClass="text-danger small d-block" element="div"/>
        </div>
        <button type="submit" class="btn btn-primary w-100"><spring:message code="registerPassword.submit"/></button>
    </form:form>
</div>
<%@include file="footer.jsp" %>
<script>
    (function () {
        var min = <c:out value="${registrationPasswordMinLength}"/>;
        var p = document.getElementById('password');
        var c = document.getElementById('passwordConfirm');
        if (p) { p.setAttribute('minlength', String(min)); }
        if (c) { c.setAttribute('minlength', String(min)); }
    })();
</script>
</body>
</html>
