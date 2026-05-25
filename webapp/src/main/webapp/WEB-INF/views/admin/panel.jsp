<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title>Ryden &mdash; Admin Panel</title>
    <%@include file="../header.jsp" %>
</head>
<body class="has-fixed-navbar">
<ryden:navbar/>

<div class="container py-5">
    <h1 class="mb-4 fw-bold">Admin Panel</h1>

    <div class="card shadow-sm">
        <div class="card-header p-0">
            <ul class="nav nav-tabs card-header-tabs px-3">
                <li class="nav-item">
                    <a class="nav-link ${param.tab == 'users' || empty param.tab ? 'active' : ''}"
                       href="?tab=users">Users</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link ${param.tab == 'cars' ? 'active' : ''}"
                       href="?tab=cars">Cars</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link ${param.tab == 'reservations' ? 'active' : ''}"
                       href="?tab=reservations">Reservations</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link ${param.tab == 'validate' ? 'active' : ''}"
                       href="?tab=validate">Validate Cars</a>
                </li>
            </ul>
        </div>

        <div class="card-body">
            <c:choose>
                <%-- USERS TAB --%>
                <c:when test="${param.tab == 'users' || empty param.tab}">
                    <h2 class="h5 mb-4">Manage Users</h2>
                    <form action="${pageContext.request.contextPath}/admin/searchUser" method="get" class="mb-4">
                        <input type="hidden" name="tab" value="users">
                        <div class="input-group" style="max-width: 480px;">
                            <input type="email" name="email" placeholder="User email" required
                                   class="form-control" value="<c:out value='${param.email}'/>">
                            <button type="submit" class="btn btn-primary">Search</button>
                        </div>
                    </form>

                    <c:if test="${not empty userSearchResult}">
                        <div class="border rounded p-3">
                            <p class="mb-1"><strong>ID:</strong> <c:out value="${userSearchResult.id}"/></p>
                            <p class="mb-1"><strong>Name:</strong>
                                <c:out value="${userSearchResult.forename}"/>
                                <c:out value="${userSearchResult.surname}"/>
                            </p>
                            <p class="mb-1"><strong>Email:</strong> <c:out value="${userSearchResult.email}"/></p>
                            <p class="mb-1"><strong>Role:</strong> <c:out value="${userSearchResult.userRole}"/></p>
                            <p class="mb-3"><strong>Blocked:</strong> <c:out value="${userSearchResult.blocked}"/></p>

                            <div class="d-flex gap-2 flex-wrap">
                                <c:if test="${userSearchResult.userRole != 'ADMIN'}">
                                    <form action="${pageContext.request.contextPath}/admin/promoteUser" method="post">
                                        <%@include file="../includes/csrfHidden.jspf" %>
                                        <input type="hidden" name="targetUserId" value="${userSearchResult.id}">
                                        <button type="submit" class="btn btn-success btn-sm">Promote to Admin</button>
                                    </form>
                                </c:if>
                                <c:if test="${!userSearchResult.blocked}">
                                    <form action="${pageContext.request.contextPath}/admin/blockUser" method="post">
                                        <%@include file="../includes/csrfHidden.jspf" %>
                                        <input type="hidden" name="targetUserId" value="${userSearchResult.id}">
                                        <button type="submit" class="btn btn-danger btn-sm">Block User</button>
                                    </form>
                                </c:if>
                            </div>
                        </div>
                    </c:if>
                </c:when>

                <%-- CARS TAB --%>
                <c:when test="${param.tab == 'cars'}">
                    <h2 class="h5 mb-4">Manage Cars</h2>
                    <form action="${pageContext.request.contextPath}/admin/searchCar" method="get" class="mb-4">
                        <input type="hidden" name="tab" value="cars">
                        <div class="input-group" style="max-width: 480px;">
                            <input type="number" name="carId" placeholder="Car ID" required
                                   class="form-control" value="<c:out value='${param.carId}'/>">
                            <button type="submit" class="btn btn-primary">Search</button>
                        </div>
                    </form>

                    <c:if test="${not empty carSearchResult}">
                        <div class="border rounded p-3">
                            <p class="mb-1"><strong>ID:</strong> <c:out value="${carSearchResult.id}"/></p>
                            <p class="mb-1"><strong>Brand:</strong> <c:out value="${carSearchResult.brand}"/></p>
                            <p class="mb-1"><strong>Model:</strong> <c:out value="${carSearchResult.model}"/></p>
                            <p class="mb-1"><strong>Plate:</strong> <c:out value="${carSearchResult.plate}"/></p>
                            <p class="mb-1"><strong>Status:</strong> <c:out value="${carSearchResult.status}"/></p>
                            <p class="mb-3"><strong>Owner:</strong> <c:out value="${carSearchResult.owner.email}"/></p>

                            <div class="d-flex gap-2 flex-wrap">
                                <c:choose>
                                    <c:when test="${carSearchResult.status == 'ADMIN_PAUSED'}">
                                        <form action="${pageContext.request.contextPath}/admin/resumeCar" method="post">
                                            <%@include file="../includes/csrfHidden.jspf" %>
                                            <input type="hidden" name="carId" value="${carSearchResult.id}">
                                            <button type="submit" class="btn btn-success btn-sm">Resume Listing</button>
                                        </form>
                                    </c:when>
                                    <c:when test="${carSearchResult.status == 'ACTIVE' || carSearchResult.status == 'PAUSED'}">
                                        <form action="${pageContext.request.contextPath}/admin/pauseCar" method="post">
                                            <%@include file="../includes/csrfHidden.jspf" %>
                                            <input type="hidden" name="carId" value="${carSearchResult.id}">
                                            <button type="submit" class="btn btn-warning btn-sm">Pause Listing</button>
                                        </form>
                                    </c:when>
                                </c:choose>
                            </div>
                        </div>
                    </c:if>
                </c:when>

                <%-- RESERVATIONS TAB --%>
                <c:when test="${param.tab == 'reservations'}">
                    <h2 class="h5 mb-4">Manage Reservations</h2>
                    <form action="${pageContext.request.contextPath}/admin/searchReservation" method="get" class="mb-4">
                        <input type="hidden" name="tab" value="reservations">
                        <div class="input-group" style="max-width: 480px;">
                            <input type="number" name="reservationId" placeholder="Reservation ID" required
                                   class="form-control" value="<c:out value='${param.reservationId}'/>">
                            <button type="submit" class="btn btn-primary">Search</button>
                        </div>
                    </form>

                    <c:if test="${not empty resSearchResult}">
                        <div class="border rounded p-3">
                            <p class="mb-1"><strong>ID:</strong> <c:out value="${resSearchResult.id}"/></p>
                            <p class="mb-1"><strong>Status:</strong> <c:out value="${resSearchResult.status}"/></p>
                            <p class="mb-1"><strong>Rider:</strong> <c:out value="${resSearchResult.rider.email}"/></p>
                            <p class="mb-3"><strong>Car owner:</strong> <c:out value="${resSearchResult.car.owner.email}"/></p>

                            <a href="${pageContext.request.contextPath}/admin/reservation/${resSearchResult.id}/chat"
                               class="btn btn-outline-primary btn-sm">View Chat</a>
                        </div>
                    </c:if>
                </c:when>

                <%-- VALIDATE TAB --%>
                <c:when test="${param.tab == 'validate'}">
                    <h2 class="h5 mb-4">Validate Cars</h2>
                    <div class="alert alert-warning">
                        <strong>Coming Soon!</strong> This feature is yet to be implemented.
                    </div>
                </c:when>
            </c:choose>

            <c:if test="${not empty error}">
                <div class="alert alert-danger mt-3">
                    <c:out value="${error}"/>
                </div>
            </c:if>
            <c:if test="${not empty success}">
                <div class="alert alert-success mt-3">
                    <c:out value="${success}"/>
                </div>
            </c:if>
        </div>
    </div>
</div>

<%@include file="../footer.jsp" %>
</body>
</html>
