<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@include file="header.jsp"%>
    <title>Reservation confirmation</title>
</head>
<body class="bg-light has-fixed-navbar">
<paw:navbar/>

<c:set var="summaryFromDate" value="${empty fromDate ? 'Not specified' : fromDate}"/>
<c:set var="summaryUntilDate" value="${empty untilDate ? 'Not specified' : untilDate}"/>
<c:set var="summaryLocation" value="${empty deliveryLocation ? 'Not specified' : deliveryLocation}"/>

<main class="container py-5">
    <div class="row justify-content-center">
        <div class="col-md-9 col-lg-7">
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-body p-4 p-md-5 text-center">
                    <h1 class="h3 fw-bold mb-3">Reservation request sent</h1>
                    <p class="mb-2">Thank you, ${name} ${surname}.</p>
                    <p class="text-secondary mb-4">
                        The owner of <strong>${carName}</strong> will contact you at
                        <strong>${email}</strong> to coordinate the vehicle handoff.
                    </p>

                    <a href="<c:url value='/search'/>" class="btn btn-primary px-4">Back to search</a>
                </div>
            </div>
        </div>
    </div>
</main>

<%@include file="footer.jsp"%>
</body>
</html>



