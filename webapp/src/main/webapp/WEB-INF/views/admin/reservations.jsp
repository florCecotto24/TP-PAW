<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="admin.reservations.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container py-5 mt-5">
    <h1 class="h2 fw-bold mb-4"><spring:message code="admin.reservations.title"/></h1>

    <div class="card border-0 shadow-sm rounded-4">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th><spring:message code="admin.reservations.table.id"/></th>
                            <th><spring:message code="admin.reservations.table.car"/></th>
                            <th><spring:message code="admin.reservations.table.start"/></th>
                            <th><spring:message code="admin.reservations.table.end"/></th>
                            <th><spring:message code="admin.reservations.table.status"/></th>
                            <th><spring:message code="admin.reservations.table.total"/></th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="r" items="${reservations.content}">
                            <tr>
                                <td><c:out value="${r.reservationId}"/></td>
                                <td><c:out value="${r.brand} ${r.model}"/></td>
                                <td><c:out value="${r.startDate}"/></td>
                                <td><c:out value="${r.endDate}"/></td>
                                <td><span class="badge text-bg-secondary"><c:out value="${r.status}"/></span></td>
                                <td>$<c:out value="${r.totalPrice}"/></td>
                                <td class="text-end">
                                    <a href="${pageContext.request.contextPath}/admin/reservations/${r.reservationId}/chat"
                                       class="btn btn-sm btn-outline-primary rounded-3">
                                        <spring:message code="admin.reservations.viewChat"/>
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <c:if test="${reservations.totalPages > 1}">
        <nav class="mt-4">
            <ul class="pagination justify-content-center">
                <c:if test="${reservations.hasPrevious}">
                    <li class="page-item">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/reservations?page=${reservations.currentPage - 1}">&laquo;</a>
                    </li>
                </c:if>
                <c:if test="${reservations.hasNext}">
                    <li class="page-item">
                        <a class="page-link" href="${pageContext.request.contextPath}/admin/reservations?page=${reservations.currentPage + 1}">&raquo;</a>
                    </li>
                </c:if>
            </ul>
        </nav>
    </c:if>
</div>
<%@ include file="../footer.jsp" %>
</body>
</html>
