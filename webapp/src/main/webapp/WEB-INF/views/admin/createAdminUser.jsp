<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

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
            <form:form action="${pageContext.request.contextPath}/admin/users/create" method="post"
                       modelAttribute="createAdminUserForm" cssClass="needs-validation" novalidate="novalidate">
                <form:errors path="*" element="div" cssClass="alert alert-danger" delimiter=" "/>
                <div class="mb-3">
                    <label for="forename" class="form-label fw-semibold">
                        <spring:message code="admin.createAdmin.forename"/>
                    </label>
                    <form:input path="forename" cssClass="form-control rounded-3" id="forename" required="required"
                                maxlength="${registrationDisplayNamePartMaxLength}" autocomplete="given-name"
                                data-ryden-no-punctuation="true"/>
                    <form:errors path="forename" cssClass="text-danger small d-block" element="div"/>
                </div>
                <div class="mb-3">
                    <label for="surname" class="form-label fw-semibold">
                        <spring:message code="admin.createAdmin.surname"/>
                    </label>
                    <form:input path="surname" cssClass="form-control rounded-3" id="surname" required="required"
                                maxlength="${registrationDisplayNamePartMaxLength}" autocomplete="family-name"
                                data-ryden-no-punctuation="true"/>
                    <form:errors path="surname" cssClass="text-danger small d-block" element="div"/>
                </div>
                <div class="mb-3">
                    <label for="email" class="form-label fw-semibold">
                        <spring:message code="admin.createAdmin.email"/>
                    </label>
                    <form:input path="email" type="email" cssClass="form-control rounded-3" id="email" required="required"
                                maxlength="${registrationEmailMaxLength}" autocomplete="email"/>
                    <form:errors path="email" cssClass="text-danger small d-block" element="div"/>
                </div>
                <div class="mb-4">
                    <label for="password" class="form-label fw-semibold">
                        <spring:message code="admin.createAdmin.password"/>
                    </label>
                    <form:password path="password" cssClass="form-control rounded-3" id="password" required="required"
                                   maxlength="${registrationPasswordMaxLength}" autocomplete="new-password"/>
                    <form:errors path="password" cssClass="text-danger small d-block" element="div"/>
                </div>
                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-primary rounded-3 flex-grow-1">
                        <spring:message code="admin.createAdmin.submit"/>
                    </button>
                    <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-light border rounded-3">
                        <spring:message code="common.cancel"/>
                    </a>
                </div>
            </form:form>
        </div>
    </div>
</div>
<%@ include file="../footer.jsp" %>
</body>
</html>
