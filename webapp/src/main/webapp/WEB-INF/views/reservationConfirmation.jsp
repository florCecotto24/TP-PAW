<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@include file="header.jsp"%>
    <title><spring:message code="reservationConfirmation.title"/></title>
</head>
<body class="bg-light has-fixed-navbar">
<paw:navbar/>

<spring:message code="common.notSpecified" var="notSpecified"/>
<c:set var="summaryFromDate" value="${empty fromDateTime ? notSpecified : fromDateTime}"/>
<c:set var="summaryUntilDate" value="${empty untilDateTime ? notSpecified : untilDateTime}"/>
<c:set var="summaryLocation" value="${empty deliveryLocation ? notSpecified : deliveryLocation}"/>

<main class="container py-5">
    <div class="row justify-content-center">
        <div class="col-md-9 col-lg-7">
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-body p-4 p-md-5 text-center">
                    <h1 class="h3 fw-bold mb-3"><spring:message code="reservationConfirmation.heading"/></h1>
                    <p class="mb-2"><spring:message code="reservationConfirmation.greeting" arguments="${name},${surname}"/></p>
                    <p class="text-secondary mb-4">
                        <spring:message code="reservationConfirmation.contactMessage" arguments="${carName},${email}"/>
                    </p>

                    <a href="<c:url value='/search'/>" class="btn btn-primary btn-action btn-action-md"><spring:message code="common.backToSearch"/></a>
                </div>
            </div>
        </div>
    </div>
</main>

<%@include file="footer.jsp"%>
</body>
</html>



