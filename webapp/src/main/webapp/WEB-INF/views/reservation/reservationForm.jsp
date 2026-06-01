<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@include file="../header.jsp"%>
    <title><spring:message code="reservationForm.title"/></title>
</head>
<body class="bg-light has-fixed-navbar">
<ryden:navbar/>

<main class="container py-5">
    <spring:message code="reservationForm.title" var="reservationFormLabel"/>
    <c:url var="carDetailBreadcrumbUrl" value="/cars/${reservationForm.carId}"/>
    <ryden:breadcrumbTrail midLabel="${reservationForm.carName}" midHref="${carDetailBreadcrumbUrl}" currentLabel="${reservationFormLabel}"/>
    <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
            <div class="card border-0 shadow-sm rounded-4 bg-white">
                <div class="card-body p-4 p-md-5">
                    <h1 class="h4 fw-bold mb-2"><spring:message code="reservationForm.heading"/></h1>
                    <spring:message code="reservationForm.description.before"/> <strong><c:out value="${reservationForm.carName}"/></strong><spring:message code="reservationForm.description.after"/>

                    <p class="text-secondary small mt-3 mb-1">
                        <spring:message code="reservationForm.paymentProofNotice" arguments="${paymentProofUploadDeadlineHours}"/>
                    </p>

                    <c:if test="${not empty reservationError}">
                        <div class="alert alert-danger" role="alert"><c:out value="${reservationError}"/></div>
                    </c:if>

                    <spring:message code="reservationForm.missingDocs.needBoth" var="resMissingDocsNeedBothMsg" htmlEscape="false"/>
                    <spring:message code="reservationForm.missingDocs.needLicense" var="resMissingDocsNeedLicenseMsg" htmlEscape="false"/>
                    <spring:message code="reservationForm.missingDocs.needIdentity" var="resMissingDocsNeedIdentityMsg" htmlEscape="false"/>
                    <spring:message code="reservationForm.missingDocs.saveFailed" var="resMissingDocsSaveFailedMsg" htmlEscape="false"/>

                    <div class="border rounded-3 p-3 bg-cream mb-4 mt-1">
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
                            <p class="mb-0"><strong><spring:message code="reservationForm.summary.total"/></strong> <c:out value="${reservationTotal}"/></p>
                        </c:if>
                    </div>

                    <form:form id="reservationFormEl"
                               action="${pageContext.request.contextPath}/reservation" method="post" modelAttribute="reservationForm" cssClass="d-flex flex-column gap-2"
                               data-ryden-rider-has-booking-docs="${riderHasBookingDocuments ? 'true' : 'false'}"
                               data-ryden-booking-docs-url="${pageContext.request.contextPath}/reservation/booking-documents"
                               data-ryden-needs-license="${riderMissingLicenseDocument ? 'true' : 'false'}"
                               data-ryden-needs-identity="${riderMissingIdentityDocument ? 'true' : 'false'}"
                               data-ryden-booking-docs-need-both="${fn:escapeXml(resMissingDocsNeedBothMsg)}"
                               data-ryden-booking-docs-need-license="${fn:escapeXml(resMissingDocsNeedLicenseMsg)}"
                               data-ryden-booking-docs-need-identity="${fn:escapeXml(resMissingDocsNeedIdentityMsg)}"
                               data-ryden-booking-docs-save-failed="${fn:escapeXml(resMissingDocsSaveFailedMsg)}">
                        <form:errors path="fromDateTime" cssClass="text-danger d-block mb-2"/>
                        <form:hidden path="carId"/>
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

                        <div class="border rounded-3 p-3 bg-cream mb-3">
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
                                <c:when test="${not empty reservationForm.carId}">
                                    <a href="<c:url value='/cars/${reservationForm.carId}'/>" class="btn btn-outline-secondary w-50"><spring:message code="common.back"/></a>
                                </c:when>
                                <c:otherwise>
                                    <a href="<c:url value='/search'/>" class="btn btn-outline-secondary w-50"><spring:message code="common.back"/></a>
                                </c:otherwise>
                            </c:choose>
                            <button type="submit" class="btn btn-primary w-50"><spring:message code="reservationForm.confirm"/></button>
                        </div>
                    </form:form>

                    <c:if test="${not riderHasBookingDocuments}">
                        <spring:message code="reservationForm.missingDocs.modalTitle" var="resMissingDocsTitle"/>
                        <spring:message code="reservationForm.missingDocs.licenseLabel" var="resMissingDocsLicenseLabel"/>
                        <spring:message code="reservationForm.missingDocs.identityLabel" var="resMissingDocsIdentityLabel"/>
                        <spring:message code="reservationForm.missingDocs.uploadedOnFile" var="resMissingDocsUploadedOnFile"/>
                        <spring:message code="reservationForm.missingDocs.save" var="resMissingDocsSave"/>
                        <spring:message code="reservationForm.missingDocs.cancel" var="resMissingDocsCancel"/>
                        <ryden:documentPromptModal
                                id="reservationMissingDocsModal"
                                title="${resMissingDocsTitle}"
                                licenseInputId="reservationMissingDocsLicense"
                                identityInputId="reservationMissingDocsIdentity"
                                licenseLabel="${resMissingDocsLicenseLabel}"
                                identityLabel="${resMissingDocsIdentityLabel}"
                                uploadedSlotMessage="${resMissingDocsUploadedOnFile}"
                                licensePending="${riderMissingLicenseDocument}"
                                identityPending="${riderMissingIdentityDocument}"
                                errorId="reservationMissingDocsError"
                                confirmId="reservationMissingDocsSaveBtn"
                                openButtonId="rydenReservationMissingDocsModalOpen"
                                includeOpenTrigger="true"
                                cancelLabel="${resMissingDocsCancel}"
                                confirmLabel="${resMissingDocsSave}">
                            <p class="mb-2 text-secondary text-center">
                                <spring:message code="reservationForm.missingDocs.modalBody"/>
                            </p>
                            <p class="mb-3 text-secondary text-center">
                                <spring:message code="reservationForm.missingDocs.modalBodyFormats" arguments="${uploadMaxProfileDocumentMegabytes}"/>
                            </p>
                        </ryden:documentPromptModal>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
</main>

<%@include file="../footer.jsp"%>
</body>
</html>



