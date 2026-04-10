<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="register.title"/> — <spring:message code="app.title"/></title>
    <%@include file="header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container" style="max-width: 440px; margin-top: 5rem;">
    <h1 class="h3 mb-3"><spring:message code="register.heading"/></h1>

    <c:if test="${registerEmailTaken}">
        <div class="alert alert-danger" role="alert"><spring:message code="register.emailTaken"/></div>
    </c:if>
    <c:if test="${registerFieldError}">
        <div class="alert alert-danger" role="alert"><spring:message code="register.fieldRequired"/></div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/register" class="needs-validation" novalidate>
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <div class="mb-3">
            <label for="forename" class="form-label"><spring:message code="register.forename"/></label>
            <input type="text" class="form-control" id="forename" name="forename" required maxlength="50"
                   value="<c:out value='${forename}'/>"/>
        </div>
        <div class="mb-3">
            <label for="surname" class="form-label"><spring:message code="register.surname"/></label>
            <input type="text" class="form-control" id="surname" name="surname" required maxlength="50"
                   value="<c:out value='${surname}'/>"/>
        </div>
        <div class="mb-4">
            <label for="email" class="form-label"><spring:message code="register.email"/></label>
            <input type="email" class="form-control" id="email" name="email" required maxlength="50"
                   value="<c:out value='${email}'/>"/>
        </div>
        <button type="submit" class="btn btn-primary w-100"><spring:message code="register.submit"/></button>
    </form>
    <p class="text-center mt-3 small">
        <a href="<c:url value='/login'/>"><spring:message code="navbar.login"/></a>
    </p>
</div>
<%@include file="footer.jsp" %>
</body>
</html>
