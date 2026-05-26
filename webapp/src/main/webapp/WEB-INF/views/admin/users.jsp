<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="admin.users.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container py-5 mt-5">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h1 class="h2 fw-bold mb-0"><spring:message code="admin.users.title"/></h1>
        <a href="${pageContext.request.contextPath}/admin/users/create" class="btn btn-primary rounded-3">
            <spring:message code="admin.users.createAdmin"/>
        </a>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success rounded-3"><c:out value="${successMessage}"/></div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger rounded-3"><c:out value="${errorMessage}"/></div>
    </c:if>

    <div class="card border-0 shadow-sm rounded-4">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th>ID</th>
                            <th>Name</th>
                            <th>Email</th>
                            <th><spring:message code="admin.users.role"/></th>
                            <th><spring:message code="admin.users.blocked"/></th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="user" items="${users.content}">
                            <tr>
                                <td><c:out value="${user.id}"/></td>
                                <td><c:out value="${user.forename} ${user.surname}"/></td>
                                <td><c:out value="${user.email}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${user.admin}">
                                            <span class="badge text-bg-primary">ADMIN</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge text-bg-secondary">USER</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${user.blocked}">
                                            <span class="badge text-bg-danger"><spring:message code="admin.users.blocked"/></span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge text-bg-success"><spring:message code="admin.users.active"/></span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="text-end">
                                    <c:if test="${not user.admin}">
                                        <form action="${pageContext.request.contextPath}/admin/users/${user.id}/promote" method="post" class="d-inline">
                                            <button type="submit" class="btn btn-sm btn-outline-primary rounded-3 me-1">
                                                <spring:message code="admin.users.promote"/>
                                            </button>
                                        </form>
                                    </c:if>
                                    <c:choose>
                                        <c:when test="${(currentAdminGrantorId != null and user.id eq currentAdminGrantorId) or user.id eq currentAdminId}">
                                            <!-- No block/unblock actions for the admin who granted the current role. -->
                                        </c:when>
                                        <c:when test="${user.blocked}">
                                            <form action="${pageContext.request.contextPath}/admin/users/${user.id}/unblock" method="post" class="d-inline">
                                                <button type="submit" class="btn btn-sm btn-outline-success rounded-3">
                                                    <spring:message code="admin.users.unblock"/>
                                                </button>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <form action="${pageContext.request.contextPath}/admin/users/${user.id}/block" method="post" class="d-inline">
                                                <button type="submit" class="btn btn-sm btn-outline-danger rounded-3">
                                                    <spring:message code="admin.users.block"/>
                                                </button>
                                            </form>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <c:if test="${users.totalPages > 1}">
        <nav class="mt-4">
            <ul class="pagination justify-content-center">
                <c:if test="${users.hasPrevious}">
                    <li class="page-item">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/users?page=${users.currentPage - 1}">
                            &laquo;
                        </a>
                    </li>
                </c:if>
                <c:if test="${users.hasNext}">
                    <li class="page-item">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/users?page=${users.currentPage + 1}">
                            &raquo;
                        </a>
                    </li>
                </c:if>
            </ul>
        </nav>
    </c:if>
</div>
<%@ include file="../footer.jsp" %>
</body>
</html>
