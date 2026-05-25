<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title>Ryden &mdash; Admin Chat View</title>
    <%@include file="../header.jsp" %>
</head>
<body class="has-fixed-navbar">
<ryden:navbar/>

<div class="container py-5" style="max-width: 860px;">
    <a href="${pageContext.request.contextPath}/admin?tab=reservations"
       class="btn btn-link ps-0 mb-3">&larr; Back to Admin Panel</a>

    <c:choose>
        <c:when test="${not empty reservation}">
            <h1 class="h4 fw-bold mb-4">Chat &mdash; Reservation #<c:out value="${reservation.id}"/></h1>

            <div class="card shadow-sm mb-4">
                <div class="card-header bg-light">
                    <h2 class="h6 mb-0">Participants</h2>
                </div>
                <div class="card-body">
                    <p class="mb-1">
                        <strong>Rider:</strong>
                        <c:out value="${reservation.rider.email}"/>
                    </p>
                    <p class="mb-0">
                        <strong>Car Owner:</strong>
                        <c:out value="${reservation.car.owner.email}"/>
                    </p>
                </div>
            </div>

            <div class="card shadow-sm">
                <div class="card-header bg-light">
                    <h2 class="h6 mb-0">Reservation Details</h2>
                </div>
                <div class="card-body">
                    <p class="mb-1"><strong>Status:</strong> <c:out value="${reservation.status}"/></p>
                    <p class="mb-1"><strong>Start Date:</strong> <c:out value="${reservation.startDate}"/></p>
                    <p class="mb-0"><strong>End Date:</strong> <c:out value="${reservation.endDate}"/></p>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="alert alert-danger">
                <c:out value="${error}"/>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@include file="../footer.jsp" %>
</body>
</html>
