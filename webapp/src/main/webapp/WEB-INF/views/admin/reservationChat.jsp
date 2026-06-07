<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
<head>
    <title><spring:message code="admin.reservationChat.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container py-5 mt-5" style="max-width: 720px;">
    <spring:message code="admin.panel.title" var="adminPanelLabel"/>
    <spring:message code="admin.reservations.title" var="adminReservationsLabel"/>
    <spring:message code="admin.reservationChat.title" var="adminReservationChatLabel"/>
    <ryden:breadcrumbTrail
            homeLabel="${adminPanelLabel}"
            homeHref="${pageContext.request.contextPath}/admin"
            midLabel="${adminReservationsLabel}"
            midHref="${pageContext.request.contextPath}/admin/reservations"
            currentLabel="${adminReservationChatLabel}"/>
    <h1 class="h2 fw-bold mb-4">
        <spring:message code="admin.reservationChat.title"/>
        <small class="fs-6 text-secondary ms-2">#<c:out value="${reservation.id}"/></small>
    </h1>

    <c:choose>
        <c:when test="${empty messages}">
            <p class="text-secondary"><spring:message code="admin.reservationChat.noMessages"/></p>
        </c:when>
        <c:otherwise>
            <div class="d-flex flex-column gap-3">
                <c:forEach var="msg" items="${messages}">
                    <div class="d-flex flex-column">
                        <div class="d-flex align-items-center gap-2 mb-1">
                            <span class="fw-semibold small">
                                <c:out value="${msg.sender.forename} ${msg.sender.surname}"/>
                            </span>
                            <span class="text-secondary small">
                                <c:out value="${msg.createdAt}"/>
                            </span>
                        </div>
                        <div class="bg-light rounded-3 px-3 py-2">
                            <p class="mb-0"><c:out value="${msg.body}"/></p>
                        </div>
                    </div>
                </c:forEach>
            </div>

            <c:set var="totalPages" value="${(totalMessages + pageSize - 1) / pageSize}"/>
            <ryden:pagination
                    currentPage="${currentPage}"
                    totalPages="${totalPages}"
                    baseUrl="${pageContext.request.contextPath}/admin/reservations/${reservation.id}/chat"/>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="../footer.jsp" %>
</body>
</html>
