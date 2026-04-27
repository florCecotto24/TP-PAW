<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@include file="header.jsp"%>
    <title><spring:message code="reservationForm.title"/></title>
</head>
<body class="bg-light has-fixed-navbar">
<ryden:navbar/>

<main class="container py-5">
    <spring:message code="reservationForm.title" var="reservationFormLabel"/>
    <c:url var="carDetailBreadcrumbUrl" value="/car-detail">
        <c:param name="listingId" value="${reservationForm.listingId}"/>
    </c:url>
    <ryden:breadcrumbTrail midLabel="${reservationForm.carName}" midHref="${carDetailBreadcrumbUrl}" currentLabel="${reservationFormLabel}"/>
    <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-body p-4 p-md-5">
                    <h1 class="h4 fw-bold mb-2"><spring:message code="reservationForm.heading"/></h1>
                    <spring:message code="reservationForm.description.before"/> <strong><c:out value="${reservationForm.carName}"/></strong><spring:message code="reservationForm.description.after"/>

                    <p class="text-secondary small mt-3 mb-1">
                        <spring:message code="reservationForm.paymentProofNotice" arguments="${paymentProofUploadDeadlineHours}"/>
                    </p>

                    <c:if test="${not empty reservationError}">
                        <div class="alert alert-danger" role="alert"><c:out value="${reservationError}"/></div>
                    </c:if>

                    <div class="border rounded-3 p-3 bg-light-subtle mb-4 mt-1">
                        <h2 class="h6 fw-bold mb-2"><spring:message code="reservationForm.summary.title"/></h2>
                        <p class="mb-1"><strong><spring:message code="reservationForm.summary.car"/></strong> <c:out value="${reservationForm.carName}"/></p>
                        <p class="mb-1"><strong><spring:message code="reservationForm.summary.pickupReturn"/></strong>
                            <c:choose>
                                <c:when test="${not empty fromDateTimeDisplay}"><c:out value="${fromDateTimeDisplay}"/></c:when>
                                <c:otherwise><c:out value="${reservationForm.fromDateTime}"/></c:otherwise>
                            </c:choose>
                            →
                            <c:choose>
                                <c:when test="${not empty untilDateTimeDisplay}"><c:out value="${untilDateTimeDisplay}"/></c:when>
                                <c:otherwise><c:out value="${reservationForm.untilDateTime}"/></c:otherwise>
                            </c:choose>
                        </p>
                        <p class="mb-1"><strong><spring:message code="reservationForm.summary.location"/></strong> <c:out value="${reservationForm.deliveryLocation}"/></p>
                        <c:if test="${not empty reservationTotal}">
                            <p class="mb-0"><strong><spring:message code="reservationForm.summary.total"/></strong> $<c:out value="${reservationTotal}"/></p>
                        </c:if>
                    </div>

                    <form:form action="${pageContext.request.contextPath}/reservation" method="post" modelAttribute="reservationForm" cssClass="d-flex flex-column gap-2">
                        <form:errors path="fromDateTime" cssClass="text-danger d-block mb-2"/>
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

                        <div class="border rounded-3 p-3 bg-light-subtle mb-3">
                            <h2 class="h6 fw-bold mb-2"><spring:message code="reservationForm.account.title"/></h2>
                            <p class="mb-1">
                                <strong><spring:message code="reservationForm.account.name"/></strong>
                                <c:out value="${riderForename}"/> <c:out value="${riderSurname}"/>
                            </p>
                            <p class="mb-0">
                                <strong><spring:message code="reservationForm.account.email"/></strong>
                                <c:out value="${riderEmail}"/>
                            </p>
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



