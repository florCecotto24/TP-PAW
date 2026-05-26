<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="admin.panel.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container py-5 mt-5">
    <h1 class="h2 fw-bold mb-4"><spring:message code="admin.panel.title"/></h1>
    <div class="row g-3">
        <div class="col-md-3">
            <a href="${pageContext.request.contextPath}/admin/users" class="text-decoration-none">
                <div class="card h-100 border-0 shadow-sm rounded-4">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold"><spring:message code="admin.panel.users"/></h2>
                    </div>
                </div>
            </a>
        </div>
        <div class="col-md-3">
            <a href="${pageContext.request.contextPath}/admin/cars" class="text-decoration-none">
                <div class="card h-100 border-0 shadow-sm rounded-4">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold"><spring:message code="admin.panel.cars"/></h2>
                    </div>
                </div>
            </a>
        </div>
        <div class="col-md-3">
            <a href="${pageContext.request.contextPath}/admin/catalog" class="text-decoration-none">
                <div class="card h-100 border-0 shadow-sm rounded-4">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold"><spring:message code="admin.panel.catalog"/></h2>
                    </div>
                </div>
            </a>
        </div>
        <div class="col-md-3">
            <a href="${pageContext.request.contextPath}/admin/reservations" class="text-decoration-none">
                <div class="card h-100 border-0 shadow-sm rounded-4">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold"><spring:message code="admin.panel.reservations"/></h2>
                    </div>
                </div>
            </a>
        </div>
    </div>
</div>
<%@ include file="../footer.jsp" %>
</body>
</html>
