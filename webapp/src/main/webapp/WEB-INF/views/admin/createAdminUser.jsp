<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="admin.createAdmin.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container py-5 mt-5" style="max-width: 520px;">
    <h1 class="h2 fw-bold mb-4"><spring:message code="admin.createAdmin.title"/></h1>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger rounded-3"><c:out value="${errorMessage}"/></div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-4">
        <div class="card-body p-4">
            <form action="${pageContext.request.contextPath}/admin/users/create" method="post">
                <div class="mb-3">
                    <label for="forename" class="form-label fw-semibold">
                        <spring:message code="admin.createAdmin.forename"/>
                    </label>
                    <input type="text" class="form-control rounded-3" id="forename" name="forename" required/>
                </div>
                <div class="mb-3">
                    <label for="surname" class="form-label fw-semibold">
                        <spring:message code="admin.createAdmin.surname"/>
                    </label>
                    <input type="text" class="form-control rounded-3" id="surname" name="surname" required/>
                </div>
                <div class="mb-3">
                    <label for="email" class="form-label fw-semibold">
                        <spring:message code="admin.createAdmin.email"/>
                    </label>
                    <input type="email" class="form-control rounded-3" id="email" name="email" required/>
                </div>
                <div class="mb-4">
                    <label for="password" class="form-label fw-semibold">
                        <spring:message code="admin.createAdmin.password"/>
                    </label>
                    <input type="password" class="form-control rounded-3" id="password" name="password" required/>
                </div>
                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-primary rounded-3 flex-grow-1">
                        <spring:message code="admin.createAdmin.submit"/>
                    </button>
                    <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-light border rounded-3">
                        <spring:message code="common.cancel"/>
                    </a>
                </div>
            </form>
        </div>
    </div>
</div>
<%@ include file="../footer.jsp" %>
</body>
</html>
