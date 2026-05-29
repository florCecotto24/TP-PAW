<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="publishCar.prereq.title"/> — <spring:message code="app.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<spring:message code="publishCar.prereq.title" var="prereqTitle"/>
<spring:message code="publishCar.prereq.cbu.invalid" var="publishMissingCbuInvalidMsg" htmlEscape="false"/>
<spring:message code="publishCar.prereq.cbu.saveFailed" var="publishMissingCbuSaveFailedMsg" htmlEscape="false"/>
<spring:message code="publishCar.prereq.identity.required" var="publishMissingIdentityRequiredMsg" htmlEscape="false"/>
<spring:message code="publishCar.prereq.identity.saveFailed" var="publishMissingIdentitySaveFailedMsg" htmlEscape="false"/>

<main class="container py-5">
    <ryden:breadcrumbTrail currentLabel="${prereqTitle}"/>
    <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
            <article class="card border-0 shadow-sm rounded-4 bg-white"
                     id="publishPrereqRoot"
                     data-ryden-publisher-has-cbu="${publisherHasCbu ? 'true' : 'false'}"
                     data-ryden-publisher-has-identity="${publisherHasIdentity ? 'true' : 'false'}"
                     data-ryden-quick-cbu-url="${pageContext.request.contextPath}/publish-car/quick-cbu"
                     data-ryden-quick-identity-url="${pageContext.request.contextPath}/publish-car/quick-identity"
                     data-ryden-publish-url="${pageContext.request.contextPath}/publish-car"
                     data-ryden-cbu-invalid="${publishMissingCbuInvalidMsg}"
                     data-ryden-cbu-save-failed="${publishMissingCbuSaveFailedMsg}"
                     data-ryden-identity-required="${publishMissingIdentityRequiredMsg}"
                     data-ryden-identity-save-failed="${publishMissingIdentitySaveFailedMsg}">
                <div class="card-body p-4 p-md-5">
                    <h1 class="h4 fw-bold mb-3"><c:out value="${prereqTitle}"/></h1>
                    <p class="text-secondary mb-4"><spring:message code="publishCar.prereq.intro"/></p>

                    <ul class="list-unstyled mb-4">
                        <li class="d-flex align-items-center gap-2 mb-2">
                            <c:choose>
                                <c:when test="${publisherHasCbu}">
                                    <i class="bi bi-check-circle-fill text-success fs-5" aria-hidden="true"></i>
                                </c:when>
                                <c:otherwise>
                                    <i class="bi bi-exclamation-circle-fill text-warning fs-5" aria-hidden="true"></i>
                                </c:otherwise>
                            </c:choose>
                            <span><spring:message code="publishCar.prereq.requirement.cbu"/></span>
                        </li>
                        <li class="d-flex align-items-center gap-2">
                            <c:choose>
                                <c:when test="${publisherHasIdentity}">
                                    <i class="bi bi-check-circle-fill text-success fs-5" aria-hidden="true"></i>
                                </c:when>
                                <c:otherwise>
                                    <i class="bi bi-exclamation-circle-fill text-warning fs-5" aria-hidden="true"></i>
                                </c:otherwise>
                            </c:choose>
                            <span><spring:message code="publishCar.prereq.requirement.identity"/></span>
                        </li>
                    </ul>

                    <div class="d-flex flex-wrap gap-2">
                        <c:if test="${not publisherHasCbu}">
                            <button type="button" class="btn btn-primary" id="publishPrereqOpenCbuBtn">
                                <i class="bi bi-bank me-1" aria-hidden="true"></i>
                                <spring:message code="publishCar.prereq.button.cbu"/>
                            </button>
                        </c:if>
                        <c:if test="${not publisherHasIdentity}">
                            <button type="button" class="btn btn-primary" id="publishPrereqOpenIdentityBtn">
                                <i class="bi bi-person-vcard me-1" aria-hidden="true"></i>
                                <spring:message code="publishCar.prereq.button.identity"/>
                            </button>
                        </c:if>
                        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/profile">
                            <spring:message code="publishCar.prereq.button.goToProfile"/>
                        </a>
                        <a class="btn btn-link" href="${pageContext.request.contextPath}/">
                            <spring:message code="common.backToHome"/>
                        </a>
                    </div>

                    <p class="text-muted small mt-4 mb-0">
                        <spring:message code="publishCar.prereq.footnote"/>
                    </p>
                </div>
            </article>
        </div>
    </div>
</main>

<%-- ────── Modals ────── --%>

<c:if test="${not publisherHasCbu}">
    <spring:message code="publishCar.missingCbu.modalTitle" var="cbuMissingTitle"/>
    <spring:message code="publishCar.missingCbu.fieldLabel" var="cbuMissingFieldLabel" arguments="${cbuRequiredDigits}"/>
    <spring:message code="publishCar.missingCbu.save" var="cbuMissingSave"/>
    <spring:message code="publishCar.missingCbu.cancel" var="cbuMissingCancel"/>
    <ryden:dataPromptModal
            id="publishPrereqCbuModal"
            title="${cbuMissingTitle}"
            fieldId="publishPrereqCbuInput"
            fieldLabel="${cbuMissingFieldLabel}"
            errorId="publishPrereqCbuError"
            confirmId="publishPrereqCbuSaveBtn"
            openButtonId="rydenPublishPrereqCbuOpen"
            includeOpenTrigger="true"
            inputType="text"
            maxlength="${cbuRequiredDigits}"
            inputmode="numeric"
            inputPattern="[0-9]*"
            digitsOnly="true"
            cancelLabel="${cbuMissingCancel}"
            confirmLabel="${cbuMissingSave}">
        <p class="mb-3 text-secondary"><spring:message code="publishCar.missingCbu.modalBody" arguments="${cbuRequiredDigits}"/></p>
    </ryden:dataPromptModal>
</c:if>

<c:if test="${not publisherHasIdentity}">
    <spring:message code="publishCar.prereq.identity.modalTitle" var="prereqIdentityTitle"/>
    <spring:message code="publishCar.prereq.identity.fieldLabel" var="prereqIdentityLabel"/>
    <spring:message code="publishCar.prereq.identity.uploadedOnFile" var="prereqIdentityUploadedOnFile"/>
    <spring:message code="publishCar.prereq.identity.save" var="prereqIdentitySave"/>
    <spring:message code="publishCar.prereq.identity.cancel" var="prereqIdentityCancel"/>
    <ryden:documentPromptModal
            id="publishPrereqIdentityModal"
            title="${prereqIdentityTitle}"
            identityInputId="publishPrereqIdentityInput"
            identityLabel="${prereqIdentityLabel}"
            uploadedSlotMessage="${prereqIdentityUploadedOnFile}"
            hideLicenseSlot="true"
            identityPending="true"
            errorId="publishPrereqIdentityError"
            confirmId="publishPrereqIdentitySaveBtn"
            openButtonId="rydenPublishPrereqIdentityOpen"
            includeOpenTrigger="true"
            cancelLabel="${prereqIdentityCancel}"
            confirmLabel="${prereqIdentitySave}">
        <p class="mb-2 text-secondary text-center">
            <spring:message code="publishCar.prereq.identity.modalBody"/>
        </p>
        <p class="mb-3 text-secondary text-center">
            <spring:message code="publishCar.prereq.identity.modalBodyFormats" arguments="${uploadMaxProfileDocumentMegabytes}"/>
        </p>
    </ryden:documentPromptModal>
</c:if>

<%@include file="../footer.jsp" %>
</body>
</html>
