<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
<head>
    <title><spring:message code="reservationCancelled.pageTitle"/></title>
    <%@include file="../header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="navbar.myReservations" var="myReservationsLabel"/>
    <spring:message code="reservationCancelled.pageTitle" var="reservationCancelledLabel"/>
    <ryden:breadcrumbTrail homeLabel="${myReservationsLabel}" homeHref="${pageContext.request.contextPath}/my-reservations" currentLabel="${reservationCancelledLabel}"/>
    <div class="row justify-content-center">
        <div class="col-lg-6">
            <div class="text-center py-5">
                <div class="mb-4">
                    <i class="bi bi-check-circle text-success" style="font-size: 3rem;"></i>
                </div>
                <h1 class="h2 fw-bold mb-2"><spring:message code="reservationCancelled.heading"/></h1>
                <p class="text-secondary mb-4"><spring:message code="reservationCancelled.subheading"/></p>
                
                <c:url var="backUrl" value="/my-reservations">
                    <c:if test="${reservationRole eq 'owner'}">
                        <c:param name="tab" value="owner"/>
                    </c:if>
                </c:url>
                <a href="<c:out value='${backUrl}'/>" class="btn btn-primary">
                    <spring:message code="reservationCancelled.backButton"/>
                </a>
            </div>
        </div>
    </div>
</main>

<%@include file="../footer.jsp"%>
</body>
</html>
