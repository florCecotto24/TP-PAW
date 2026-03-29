<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@include file="header.jsp"%>
    <title>Reservation form</title>
</head>
<body class="bg-light has-fixed-navbar">
<paw:navbar/>

<c:set var="summaryFromDate" value="${empty fromDate ? 'Not specified' : fromDate}"/>
<c:set var="summaryUntilDate" value="${empty untilDate ? 'Not specified' : untilDate}"/>
<c:set var="summaryLocation" value="${empty deliveryLocation ? 'Not specified' : deliveryLocation}"/>

<main class="container py-5">
    <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-body p-4 p-md-5">
                    <h1 class="h4 fw-bold mb-2">Complete your reservation</h1>
                    <p class="text-secondary mb-4">
                        You are requesting <strong>${carName}</strong>. Fill in your details so the owner can contact you.
                    </p>

                    <div class="border rounded-3 p-3 bg-light-subtle mb-4">
                        <h2 class="h6 fw-bold mb-2">Reservation summary</h2>
                        <p class="mb-1"><strong>Car:</strong> ${carName}</p>
                        <p class="mb-1"><strong>Dates:</strong> ${summaryFromDate} to ${summaryUntilDate}</p>
                        <p class="mb-0"><strong>Location:</strong> ${summaryLocation}</p>
                    </div>

                    <c:if test="${not empty errorMessage}">
                        <div class="alert alert-danger" role="alert">${errorMessage}</div>
                    </c:if>

                    <form action="<c:url value='/reservation'/>" method="post" class="d-flex flex-column gap-2">
                        <input type="hidden" name="listingId" value="${listingId}"/>
                        <input type="hidden" name="carName" value="${carName}"/>
                        <input type="hidden" name="fromDate" value="${fromDate}"/>
                        <input type="hidden" name="untilDate" value="${untilDate}"/>
                        <input type="hidden" name="deliveryLocation" value="${deliveryLocation}"/>

                        <paw:input
                                name="email"
                                type="email"
                                label="Email"
                                placeholder="name@example.com"
                                required="${true}"/>

                        <paw:input
                                name="name"
                                label="First name"
                                placeholder="Your first name"
                                required="${true}"/>

                        <paw:input
                                name="surname"
                                label="Last name"
                                placeholder="Your last name"
                                required="${true}"/>

                        <div class="d-flex gap-2 mt-2">
                            <a href="<c:url value='/car-detail'/>" class="btn btn-outline-secondary w-50">Back</a>
                            <button type="submit" class="btn btn-primary w-50">Confirm reservation</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</main>

<%@include file="footer.jsp"%>
</body>
</html>



