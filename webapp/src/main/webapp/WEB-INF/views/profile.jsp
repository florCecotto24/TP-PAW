<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="profile.title"/> — <spring:message code="app.title"/></title>
    <%@include file="header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container profile-container">
    <spring:message code="profile.heading" var="profileLabel"/>
    <ryden:breadcrumbTrail currentLabel="${profileLabel}"/>
    <div class="profile-header">
        <h1 class="profile-header__title"><spring:message code="profile.heading"/></h1>
        <div class="profile-header__actions">
            <button id="editProfileBtn" type="button" class="btn btn-primary" style="display: none;">
                <spring:message code="profile.edit"/>
            </button>
            <div id="editingActions" style="display: none;">
                <button type="button" id="cancelEditBtn" class="btn btn-outline-secondary">
                    <spring:message code="common.cancel"/>
                </button>
                <button type="submit" form="profileForm" class="btn btn-primary">
                    <spring:message code="profile.save"/>
                </button>
            </div>
        </div>
    </div>

    <c:if test="${profileSaved}">
        <div class="alert alert-success" role="alert">
            <spring:message code="profile.saved"/>
        </div>
    </c:if>
    <c:if test="${profilePictureSaved}">
        <div class="alert alert-success" role="alert">
            <spring:message code="profile.picture.saved"/>
        </div>
    </c:if>
    <c:if test="${profilePictureDeleted}">
        <div class="alert alert-success" role="alert">
            <spring:message code="profile.picture.deleted"/>
        </div>
    </c:if>
    <c:if test="${profilePasswordSaved}">
        <div class="alert alert-success" role="alert">
            <spring:message code="profile.password.savedBanner"/>
        </div>
    </c:if>
    <c:if test="${not empty profilePictureErrorMessage}">
        <div class="alert alert-danger" role="alert">
            <c:out value="${profilePictureErrorMessage}"/>
        </div>
    </c:if>
    <c:if test="${not empty profilePictureErrorCode}">
        <div class="alert alert-danger" role="alert">
            <%-- Códigos fijados en servidor; no usar code="${...}" sin acotar (riesgo de inyección en atributo). --%>
            <c:choose>
                <c:when test="${profilePictureErrorCode eq 'profile.picture.required'}"><spring:message code="profile.picture.required"/></c:when>
                <c:when test="${profilePictureErrorCode eq 'profile.picture.notImage'}"><spring:message code="profile.picture.notImage"/></c:when>
                <c:when test="${profilePictureErrorCode eq 'profile.picture.readFailed'}"><spring:message code="profile.picture.readFailed"/></c:when>
                <c:otherwise><spring:message code="profile.picture.readFailed"/></c:otherwise>
            </c:choose>
        </div>
    </c:if>

    <div class="profile-card">
        <div class="profile-card__header">
            <div class="profile-card__avatar">
                <c:if test="${not empty profilePictureImageId}">
                    <img src="<c:url value='/image/${profilePictureImageId}'/>" alt="<c:out value='${userForename} ${userSurname}'/>" class="profile-card__avatar-img"/>
                </c:if>
                <c:if test="${empty profilePictureImageId}">
                    <div class="profile-card__avatar-placeholder">
                        <c:set var="initials" value="${fn:substring(userForename, 0, 1)}${fn:substring(userSurname, 0, 1)}"/>
                        <span><c:out value="${initials}"/></span>
                    </div>
                </c:if>
            </div>
            <div class="profile-card__info">
                <h2 class="profile-card__name"><c:out value="${userForename} ${userSurname}"/></h2>
                <p class="profile-card__email"><c:out value="${userEmail}"/></p>
            </div>
        </div>


        <div class="profile-card__section" id="profilePictureSection" style="display: none;">
            <div id="profilePictureClientError" class="alert alert-danger d-none" role="alert"></div>
            <spring:message code="validation.image.fileTooLarge" arguments="${uploadMaxImageMegabytes}" var="profileImageTooLargeMsg" htmlEscape="true"/>
            <spring:message code="validation.pictures.mustBeImage" var="profileMustBeImageMsg" htmlEscape="true"/>
            <form id="profilePictureForm" method="post" action="<c:url value='/profile/picture'/>" enctype="multipart/form-data">
                <%@ include file="includes/csrfHidden.jspf" %>
                <div class="mb-3">
                    <label for="profilePictureInput" class="form-label"><spring:message code="profile.picture.label"/></label>
                    <input type="file" class="form-control" id="profilePictureInput" name="profilePicture" accept="image/*"
                           data-upload-max-image-bytes="<c:out value='${uploadMaxImageBytes}'/>"
                           data-upload-image-too-large="<c:out value='${profileImageTooLargeMsg}'/>"
                           data-upload-not-image-msg="<c:out value='${profileMustBeImageMsg}'/>"/>
                    <div class="form-text"><spring:message code="profile.picture.hint" arguments="${uploadMaxImageMegabytes}"/></div>
                </div>
                <button type="submit" class="btn btn-outline-primary"><spring:message code="profile.picture.submit"/></button>
            </form>
            <c:if test="${not empty profilePictureImageId}">
                <form method="post" action="<c:url value='/profile/picture/delete'/>" class="d-inline">
                    <%@ include file="includes/csrfHidden.jspf" %>
                    <button type="submit" class="btn btn-outline-danger"><spring:message code="profile.picture.delete"/></button>
                </form>
            </c:if>
        </div>
    </div>

    <div class="profile-card profile-card--section" id="profileViewSection">
        <h2 class="profile-section-title"><spring:message code="profile.optionalSection"/></h2>
        <hr class="profile-card__divider">
        <div class="profile-fields-grid">
            <div class="mb-3">
                <label class="form-label"><spring:message code="profile.forename"/></label>
                <input type="text" class="form-control" value="<c:out value='${profileForm.forename}'/>" readonly/>
            </div>
            <div class="mb-3">
                <label class="form-label"><spring:message code="profile.surname"/></label>
                <input type="text" class="form-control" value="<c:out value='${profileForm.surname}'/>" readonly/>
            </div>
            <div class="mb-3">
                <label class="form-label"><spring:message code="profile.phone"/></label>
                <input type="text" class="form-control" value="<c:out value='${profileForm.phoneNumber}'/>" readonly/>
            </div>
            <div class="mb-3">
                <label class="form-label"><spring:message code="profile.birthDate"/></label>
                <input type="text" class="form-control" value="<c:out value='${profileForm.birthDate}'/>" readonly/>
            </div>
        </div>
    </div>

    <div class="profile-card profile-card--section" id="profileEditingSection" style="display: none;">
        <h2 class="profile-section-title"><spring:message code="profile.optionalSection"/></h2>
        <hr class="profile-card__divider">
        <spring:message code="profile.phone.placeholder" var="profilePhonePlaceholder" htmlEscape="true"/>
        <form:form modelAttribute="profileForm" method="post" cssClass="needs-validation" novalidate="novalidate"
                   action="${pageContext.request.contextPath}/profile" id="profileForm">
            <%@ include file="includes/csrfHidden.jspf" %>
            <form:errors path="*" element="div" cssClass="alert alert-danger" delimiter=" "/>

            <div class="profile-fields-grid">
                <div class="mb-3">
                    <label for="forename" class="form-label"><spring:message code="profile.forename"/></label>
                    <form:input path="forename" id="forename" cssClass="form-control" maxlength="50" autocomplete="given-name" data-ryden-no-punctuation="true"/>
                    <form:errors path="forename" cssClass="text-danger small d-block" element="div"/>
                </div>
                <div class="mb-3">
                    <label for="surname" class="form-label"><spring:message code="profile.surname"/></label>
                    <form:input path="surname" id="surname" cssClass="form-control" maxlength="50" autocomplete="family-name" data-ryden-no-punctuation="true"/>
                    <form:errors path="surname" cssClass="text-danger small d-block" element="div"/>
                </div>
                <div class="mb-3">
                    <label for="phoneNumber" class="form-label"><spring:message code="profile.phone"/></label>
                    <form:input path="phoneNumber" id="phoneNumber" cssClass="form-control" maxlength="${profilePhoneMaxLength}"
                                autocomplete="tel" inputmode="tel" pattern="[0-9+]*" data-ryden-phone="true"
                                placeholder="${profilePhonePlaceholder}"/>
                    <form:errors path="phoneNumber" cssClass="text-danger small d-block" element="div"/>
                    <div class="form-text"><spring:message code="profile.phone.hint"/></div>
                </div>
                <div class="mb-3">
                    <label for="profileBirthDateInput" class="form-label"><spring:message code="profile.birthDate"/></label>
                    <%-- Texto + Flatpickr (mismo stack que búsqueda / reservas); valor ISO yyyy-MM-dd para el servidor --%>
                    <form:input path="birthDate" id="profileBirthDateInput" cssClass="form-control" autocomplete="bday"
                                readonly="true" data-max-ymd="${profileBirthDateMax}"/>
                    <form:errors path="birthDate" cssClass="text-danger small d-block" element="div"/>
                    <div class="form-text"><spring:message code="profile.birthDate.hint"/></div>
                </div>
            </div>
        </form:form>
    </div>

    <div class="profile-card profile-card--section">
        <h2 class="profile-section-title"><spring:message code="profile.password.sectionTitle"/></h2>
        <hr class="profile-card__divider">
        <p class="text-muted small mb-3"><spring:message code="profile.password.sectionHint"/></p>
        <a class="btn btn-outline-primary mb-4" href="<c:url value='/profile/password'/>"><spring:message code="profile.password.goToChange"/></a>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function() {
    const editProfileBtn = document.getElementById('editProfileBtn');
    const cancelEditBtn = document.getElementById('cancelEditBtn');
    const editingActions = document.getElementById('editingActions');
    const profileViewSection = document.getElementById('profileViewSection');
    const profileEditingSection = document.getElementById('profileEditingSection');
    const profilePictureSection = document.getElementById('profilePictureSection');

    // Mostrar modo lectura por defecto
    editProfileBtn.style.display = 'inline-block';

    editProfileBtn.addEventListener('click', function() {
        // Entrar en modo edición
        editProfileBtn.style.display = 'none';
        editingActions.style.display = 'flex';
        profileViewSection.style.display = 'none';
        profileEditingSection.style.display = 'block';
        profilePictureSection.style.display = 'block';
    });

    cancelEditBtn.addEventListener('click', function() {
        // Volver a modo lectura
        editProfileBtn.style.display = 'inline-block';
        editingActions.style.display = 'none';
        profileViewSection.style.display = 'block';
        profileEditingSection.style.display = 'none';
        profilePictureSection.style.display = 'none';
    });
});
</script>

<%@include file="footer.jsp" %>
</body>
</html>
