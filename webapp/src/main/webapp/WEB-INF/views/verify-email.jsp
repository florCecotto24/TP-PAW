<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="verifyEmail.title"/> — <spring:message code="app.title"/></title>
    <%@include file="header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container" style="max-width: 440px; margin-top: 5rem;">
    <h1 class="h3 mb-3"><spring:message code="verifyEmail.heading"/></h1>
    <p class="text-muted small mb-4"><spring:message code="verifyEmail.intro"/></p>

    <c:if test="${not empty verifyErrorMessage}">
        <div class="alert alert-danger" role="alert"><c:out value="${verifyErrorMessage}"/></div>
    </c:if>
    <c:if test="${verifyFieldError}">
        <div class="alert alert-danger" role="alert"><spring:message code="verifyEmail.fieldRequired"/></div>
    </c:if>
    <c:if test="${verifyFromLogin}">
        <div class="alert alert-warning" role="alert"><spring:message code="verifyEmail.fromLoginBanner"/></div>
    </c:if>

    <c:set var="prefillEmail" value="${verifyEmail}"/>
    <c:if test="${empty prefillEmail}"><c:set var="prefillEmail" value="${verifyEmailHint}"/></c:if>
    <c:set var="emailLocked" value="${not empty verifyEmailHint or not empty verifyEmail}"/>

    <form method="post" action="${pageContext.request.contextPath}/verify-email" class="needs-validation" novalidate>
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <div class="mb-3">
            <label for="email" class="form-label"><spring:message code="verifyEmail.email"/></label>
            <input type="email" class="form-control<c:if test='${emailLocked}'> form-control-verify-email-locked</c:if>" id="email" name="email"
                   required maxlength="50" value="<c:out value='${prefillEmail}'/>"<c:if test="${emailLocked}"> readonly</c:if>/>
        </div>
        <div class="mb-4">
            <label for="code" class="form-label"><spring:message code="verifyEmail.code"/></label>
            <input type="text" class="form-control" id="code" name="code" required maxlength="6" pattern="[0-9]{6}"
                   inputmode="numeric" autocomplete="one-time-code" placeholder="000000"
                   oninput="this.value=this.value.replace(/\D/g,'').slice(0,6)"/>
        </div>
        <button type="submit" class="btn btn-primary w-100"><spring:message code="verifyEmail.submit"/></button>
    </form>
    <p class="text-center mt-3 small mb-1">
        <span class="text-muted"><spring:message code="verifyEmail.alreadyVerified"/></span>
        <a class="ms-1" href="<c:url value='/login'/>"><spring:message code="navbar.login"/></a>
    </p>
</div>
<%@include file="footer.jsp" %>
</body>
</html>
