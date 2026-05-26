<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="admin.cars.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container py-5 mt-5">
    <h1 class="h2 fw-bold mb-4"><spring:message code="admin.cars.title"/></h1>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success rounded-3"><c:out value="${successMessage}"/></div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger rounded-3"><c:out value="${errorMessage}"/></div>
    </c:if>

    <c:choose>
        <c:when test="${empty cars}">
            <p class="text-secondary">No admin-paused cars.</p>
        </c:when>
        <c:otherwise>
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>ID</th>
                                    <th>Plate</th>
                                    <th><spring:message code="admin.cars.owner"/></th>
                                    <th><spring:message code="admin.cars.status"/></th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="car" items="${cars}">
                                    <tr>
                                        <td><c:out value="${car.id}"/></td>
                                        <td><c:out value="${car.plate}"/></td>
                                        <td><c:out value="${car.owner.forename} ${car.owner.surname}"/></td>
                                        <td><span class="badge text-bg-warning"><c:out value="${car.status}"/></span></td>
                                        <td class="text-end">
                                            <form action="${pageContext.request.contextPath}/admin/cars/${car.id}/resume" method="post" class="d-inline">
                                                <button type="submit" class="btn btn-sm btn-outline-success rounded-3">
                                                    <spring:message code="admin.cars.resume"/>
                                                </button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="../footer.jsp" %>
</body>
</html>
