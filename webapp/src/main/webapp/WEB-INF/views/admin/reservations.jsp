<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
<head>
    <title><spring:message code="admin.reservations.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container py-5 mt-5">
    <spring:message code="admin.panel.title" var="adminPanelLabel"/>
    <spring:message code="admin.reservations.title" var="adminReservationsLabel"/>
    <ryden:breadcrumbTrail
            homeLabel="${adminPanelLabel}"
            homeHref="${pageContext.request.contextPath}/admin"
            currentLabel="${adminReservationsLabel}"/>
    <h1 class="h2 fw-bold mb-4"><spring:message code="admin.reservations.title"/></h1>

    <div class="card border-0 shadow-sm rounded-4" style="overflow: hidden;">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-primary">
                        <tr>
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
                                <td><c:out value="${r.brand} ${r.model}"/></td>
                                <td><c:out value="${r.startDate}"/></td>
                                <td><c:out value="${r.endDate}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${r.status == 'PENDING'}">
                                            <span class="badge text-bg-secondary"><c:out value="${r.status}"/></span>
                                        </c:when>
                                        <c:when test="${r.status == 'CANCELLED' || r.status == 'CANCELLED_BY_RIDER' || r.status == 'CANCELLED_BY_OWNER' || r.status == 'CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF'}">
                                            <span class="badge text-bg-warning"><c:out value="${r.status}"/></span>
                                        </c:when>
                                        <c:when test="${r.status == 'FINISHED'}">
                                            <span class="badge text-bg-secondary"><c:out value="${r.status}"/></span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge text-bg-success"><c:out value="${r.status}"/></span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
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

    <ryden:pagination
            currentPage="${reservations.currentPage}"
            totalPages="${reservations.totalPages}"
            baseUrl="${pageContext.request.contextPath}/admin/reservations"/>
</div>
<%@ include file="../footer.jsp" %>
</body>
</html>
