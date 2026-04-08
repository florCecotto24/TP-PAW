<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@include file="header.jsp"%>
    <title><spring:message code="reservationForm.title"/></title>
</head>
<body class="bg-light has-fixed-navbar">
<paw:navbar/>

<main class="container py-5">
    <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-body p-4 p-md-5">
                    <h1 class="h4 fw-bold mb-2"><spring:message code="reservationForm.heading"/></h1>
                    <spring:message code="reservationForm.description.before"/> <strong><c:out value="${reservationForm.carName}"/></strong><spring:message code="reservationForm.description.after"/>

                    <c:if test="${not empty reservationError}">
                        <div class="alert alert-danger" role="alert"><c:out value="${reservationError}"/></div>
                    </c:if>

                    <div class="border rounded-3 p-3 bg-light-subtle mb-4">
                        <h2 class="h6 fw-bold mb-2"><spring:message code="reservationForm.summary.title"/></h2>
                        <p class="mb-1"><strong><spring:message code="reservationForm.summary.car"/></strong> <c:out value="${reservationForm.carName}"/></p>
                        <p class="mb-1"><strong><spring:message code="reservationForm.summary.pickupReturn"/></strong> <c:out value="${reservationForm.fromDateTime}"/> → <c:out value="${reservationForm.untilDateTime}"/></p>
                        <p class="mb-1"><strong><spring:message code="reservationForm.summary.location"/></strong> <c:out value="${reservationForm.deliveryLocation}"/></p>
                        <c:if test="${not empty reservationTotal}">
                            <p class="mb-0"><strong><spring:message code="reservationForm.summary.total"/></strong> $<c:out value="${reservationTotal}"/></p>
                        </c:if>
                    </div>

                    <form:form action="${pageContext.request.contextPath}/reservation" method="post" modelAttribute="reservationForm" cssClass="d-flex flex-column gap-2">
                        <form:hidden path="listingId"/>
                        <c:if test="${not empty availabilityId}">
                            <input type="hidden" name="availabilityId" value="<c:out value='${availabilityId}'/>"/>
                        </c:if>

                        <form:hidden path="carName"/>
                        <form:hidden path="fromDateTime"/>
                        <form:hidden path="untilDateTime"/>
                        <form:hidden path="deliveryLocation"/>
                        <c:if test="${not empty clientReservationTotal}">
                            <input type="hidden" name="reservationTotal" value="<c:out value='${clientReservationTotal}'/>"/>
                        </c:if>

                        <div>
                            <label for="email" class="form-label required-label"><spring:message code="reservationForm.email"/></label>
                            <form:input path="email" cssClass="form-control" cssErrorClass="form-control is-invalid" placeholder="name@example.com" />
                            <form:errors path="email" cssClass="text-danger small d-block mt-1" />
                        </div>

                        <div>
                            <label for="name" class="form-label required-label"><spring:message code="reservationForm.firstName"/></label>
                            <form:input path="name" id="name" cssClass="form-control" cssErrorClass="form-control is-invalid" placeholder="Your first name" />
                            <form:errors path="name" cssClass="text-danger small d-block mt-1" />
                        </div>

                        <div>
                            <label for="surname" class="form-label required-label"><spring:message code="reservationForm.lastName"/></label>
                            <form:input path="surname" id="surname" cssClass="form-control" cssErrorClass="form-control is-invalid" placeholder="Your last name" />
                            <form:errors path="surname" cssClass="text-danger small d-block mt-1" />
                        </div>

                        <div class="d-flex gap-2 mt-2">
                            <c:choose>
                                <c:when test="${not empty reservationForm.listingId}">
                                    <a href="<c:url value='/car-detail'><c:param name='listingId' value='${reservationForm.listingId}'/></c:url>" class="btn btn-outline-secondary w-50"><spring:message code="common.back"/></a>
                                </c:when>
                                <c:otherwise>
                                    <a href="<c:url value='/car-detail'/>" class="btn btn-outline-secondary w-50"><spring:message code="common.back"/></a>
                                </c:otherwise>
                            </c:choose>
                            <button type="submit" class="btn btn-primary w-50"><spring:message code="reservationForm.confirm"/></button>
                        </div>
                    </form:form>
                </div>
            </div>
        </div>
    </div>
</main>

<%@include file="footer.jsp"%>
</body>
</html>



