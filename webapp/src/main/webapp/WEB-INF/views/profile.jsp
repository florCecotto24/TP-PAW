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
    <c:if test="${profileDocumentSaved}">
        <div class="alert alert-success" role="alert">
            <spring:message code="profile.document.saved"/>
        </div>
    </c:if>
    <c:if test="${profileDocumentDeleted}">
        <div class="alert alert-success" role="alert">
            <spring:message code="profile.document.deleted"/>
        </div>
    </c:if>
    <c:if test="${not empty profilePictureErrorMessage}">
        <div class="alert alert-danger" role="alert">
            <c:out value="${profilePictureErrorMessage}"/>
        </div>
    </c:if>
    <c:if test="${not empty profileDocumentError}">
        <div class="alert alert-danger" role="alert">
            <c:out value="${profileDocumentError}"/>
        </div>
    </c:if>
    <c:if test="${not empty profilePictureErrorCode}">
        <div class="alert alert-danger" role="alert">
            <%-- Server-defined codes; do not use code="${...}" unbounded (attribute injection risk). --%>
            <c:choose>
                <c:when test="${profilePictureErrorCode eq 'profile.picture.required'}"><spring:message code="profile.picture.required"/></c:when>
                <c:when test="${profilePictureErrorCode eq 'profile.picture.notImage'}"><spring:message code="profile.picture.notImage"/></c:when>
                <c:when test="${profilePictureErrorCode eq 'profile.picture.readFailed'}"><spring:message code="profile.picture.readFailed"/></c:when>
                <c:otherwise><spring:message code="profile.picture.readFailed"/></c:otherwise>
            </c:choose>
        </div>
    </c:if>

    <c:set var="profileBindingResult" value="${requestScope['org.springframework.validation.BindingResult.profileForm']}"/>
    <c:set var="hasProfileErrors" value="${profileBindingResult != null and profileBindingResult.errorCount > 0}"/>

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
                <button type="button" class="profile-card__avatar-edit-btn" id="avatarEditBtn"
                        aria-label="<spring:message code='profile.picture.editPhoto'/>">
                    <i class="bi bi-pencil-fill" aria-hidden="true"></i>
                </button>
                <div class="profile-avatar-menu" id="avatarMenu" aria-hidden="true">
                    <button type="button" class="profile-avatar-menu__item" id="avatarMenuUpload">
                        <i class="bi bi-upload" aria-hidden="true"></i>
                        <spring:message code="profile.picture.submit"/>
                    </button>
                    <c:if test="${not empty profilePictureImageId}">
                        <button type="button" class="profile-avatar-menu__item profile-avatar-menu__item--danger" id="avatarMenuDelete">
                            <i class="bi bi-trash" aria-hidden="true"></i>
                            <spring:message code="profile.picture.delete"/>
                        </button>
                    </c:if>
                </div>
            </div>
            <div class="profile-card__info">
                <h2 class="profile-card__name"><c:out value="${userForename} ${userSurname}"/></h2>
                <c:if test="${not empty profileMemberSinceDisplay}">
                    <p class="profile-card__member-since">
                        <span class="profile-card__member-since-label"><spring:message code="profile.memberSince"/></span>
                        <span class="profile-card__member-since-value"><c:out value="${profileMemberSinceDisplay}"/></span>
                    </p>
                </c:if>
                <p class="profile-card__email"><c:out value="${userEmail}"/></p>
            </div>
        </div>

        <div id="profilePictureClientError" class="alert alert-danger d-none" role="alert"></div>
        <spring:message code="validation.image.fileTooLarge" arguments="${uploadMaxImageMegabytes}" var="profileImageTooLargeMsg" htmlEscape="true"/>
        <spring:message code="validation.pictures.mustBeImage" var="profileMustBeImageMsg" htmlEscape="true"/>
        <form id="profilePictureForm" method="post" action="<c:url value='/profile/picture'/>" enctype="multipart/form-data" style="display:none;">
            <%@ include file="includes/csrfHidden.jspf" %>
            <input type="file" id="profilePictureInput" name="profilePicture" accept="image/*"
                   data-upload-max-image-bytes="<c:out value='${uploadMaxImageBytes}'/>"
                   data-upload-image-too-large="<c:out value='${profileImageTooLargeMsg}'/>"
                   data-upload-not-image-msg="<c:out value='${profileMustBeImageMsg}'/>"/>
        </form>
        <c:if test="${not empty profilePictureImageId}">
            <form id="profilePictureDeleteForm" method="post" action="<c:url value='/profile/picture/delete'/>" style="display:none;">
                <%@ include file="includes/csrfHidden.jspf" %>
            </form>
        </c:if>
    </div>

    <div class="profile-card profile-card--section" id="profileViewSection" style="${hasProfileErrors ? 'display:none;' : ''}">
        <div class="profile-card__section-header">
            <h2 class="profile-section-title"><spring:message code="profile.optionalSection"/></h2>
            <button id="editProfileBtn" type="button" class="btn btn-outline-primary btn-sm">
                <spring:message code="profile.edit"/>
            </button>
        </div>
        <hr class="profile-card__divider">
        <div class="profile-fields-grid">
            <div class="profile-field-view">
                <span class="profile-section-label"><spring:message code="profile.forename"/></span>
                <span class="profile-field-value"><c:out value="${profileForm.forename}"/></span>
            </div>
            <div class="profile-field-view">
                <span class="profile-section-label"><spring:message code="profile.surname"/></span>
                <span class="profile-field-value"><c:out value="${profileForm.surname}"/></span>
            </div>
            <div class="profile-field-view">
                <span class="profile-section-label"><spring:message code="profile.phone"/></span>
                <span class="profile-field-value">
                    <c:choose>
                        <c:when test="${not empty profileForm.phoneNumber}"><c:out value="${profileForm.phoneNumber}"/></c:when>
                        <c:otherwise><spring:message code="common.notSpecified"/></c:otherwise>
                    </c:choose>
                </span>
            </div>
            <div class="profile-field-view">
                <span class="profile-section-label"><spring:message code="profile.birthDate"/></span>
                <span class="profile-field-value"><c:out value="${profileBirthDateDisplay}"/></span>
            </div>
            <div class="profile-field-view">
                <span class="profile-section-label"><spring:message code="profile.cbu"/></span>
                <span class="profile-field-value">
                    <c:choose>
                        <c:when test="${not empty profileForm.cbu}"><c:out value="${profileForm.cbu}"/></c:when>
                        <c:otherwise><spring:message code="common.notSpecified"/></c:otherwise>
                    </c:choose>
                </span>
            </div>
            <div class="profile-field-view">
                <span class="profile-section-label"><spring:message code="profile.about"/></span>
                <span class="profile-field-value">
                    <c:choose>
                        <c:when test="${not empty profileForm.about}">
                            <c:out value="${profileForm.about}"/>
                        </c:when>
                        <c:otherwise><spring:message code="common.notSpecified"/></c:otherwise>
                    </c:choose>
                </span>
            </div>
        </div>
    </div>

    <div class="profile-card profile-card--section" id="profileEditingSection" style="${hasProfileErrors ? 'display:block;' : 'display:none;'}">
        <h2 class="profile-section-title"><spring:message code="profile.optionalSection"/></h2>
        <hr class="profile-card__divider">
        <spring:message code="profile.phone.placeholder" var="profilePhonePlaceholder" htmlEscape="true"/>
        <spring:message code="profile.cbu.placeholder" var="profileCbuPlaceholder" htmlEscape="true" arguments="${cbuRequiredDigits}"/>
        <spring:message code="profile.birthDate.clearSelection" var="profileBirthDateClearLabel" htmlEscape="false"/>
        <form:form modelAttribute="profileForm" method="post" cssClass="needs-validation" novalidate="novalidate"
                   action="${pageContext.request.contextPath}/profile" id="profileForm">
            <%@ include file="includes/csrfHidden.jspf" %>
            <form:errors path="*" element="div" cssClass="alert alert-danger" delimiter=" "/>

            <div class="profile-fields-grid">
                <div class="mb-3">
                    <label for="forename" class="form-label"><spring:message code="profile.forename"/></label>
                    <form:input path="forename" id="forename" cssClass="form-control" maxlength="${profileDisplayNamePartMaxLength}" autocomplete="given-name" data-ryden-no-punctuation="true"/>
                    <form:errors path="forename" cssClass="text-danger small d-block" element="div"/>
                </div>
                <div class="mb-3">
                    <label for="surname" class="form-label"><spring:message code="profile.surname"/></label>
                    <form:input path="surname" id="surname" cssClass="form-control" maxlength="${profileDisplayNamePartMaxLength}" autocomplete="family-name" data-ryden-no-punctuation="true"/>
                    <form:errors path="surname" cssClass="text-danger small d-block" element="div"/>
                </div>
                <div class="mb-3">
                    <label for="phoneNumber" class="form-label"><spring:message code="profile.phone"/></label>
                    <form:input path="phoneNumber" id="phoneNumber" cssClass="form-control" maxlength="${profilePhoneMaxLength}"
                                autocomplete="tel" inputmode="tel" pattern="[0-9+]*" data-ryden-phone="true"
                                placeholder="${profilePhonePlaceholder}"/>
                    <form:errors path="phoneNumber" cssClass="text-danger small d-block" element="div"/>
                    <div class="form-text"><spring:message code="profile.phone.hint" arguments="${profilePhoneMaxLength}"/></div>
                </div>
                <div class="mb-3">
                    <label for="profileBirthDateInput" class="form-label"><spring:message code="profile.birthDate"/></label>
                    <%-- Flatpickr single date; ISO yyyy-MM-dd for the server --%>
                    <form:input path="birthDate" id="profileBirthDateInput" cssClass="form-control" autocomplete="bday"
                                readonly="true" data-max-ymd="${profileBirthDateMax}"
                                data-clear-label="${profileBirthDateClearLabel}"/>
                    <form:errors path="birthDate" cssClass="text-danger small d-block" element="div"/>
                </div>
                <div class="mb-3">
                    <label for="cbu" class="form-label"><spring:message code="profile.cbu"/></label>
                    <form:input path="cbu" id="cbu" cssClass="form-control"
                                type="text"
                                inputmode="numeric"
                                maxlength="${cbuRequiredDigits}"
                                pattern="\d*"
                                data-ryden-digits-only="true"
                                placeholder="${profileCbuPlaceholder}"/>
                    <form:errors path="cbu" cssClass="text-danger small d-block" element="div"/>
                    <div class="form-text"><spring:message code="profile.cbu.hint" arguments="${cbuRequiredDigits}"/></div>
                </div>
                <div class="mb-3">
                    <label for="about" class="form-label"><spring:message code="profile.about"/></label>
                    <form:textarea path="about" id="about" cssClass="form-control" rows="4" maxlength="${profileAboutMaxLength}"/>
                    <form:errors path="about" cssClass="text-danger small d-block" element="div"/>
                </div>
            </div>
            <div class="profile-card__form-actions">
                <button type="button" id="cancelEditBtn" class="btn btn-outline-secondary">
                    <spring:message code="common.cancel"/>
                </button>
                <button type="submit" form="profileForm" class="btn btn-primary">
                    <spring:message code="profile.save"/>
                </button>
            </div>
        </form:form>
    </div>

    <div class="profile-card profile-card--section">
        <h2 class="profile-section-title"><spring:message code="profile.documents.sectionTitle"/></h2>
        <hr class="profile-card__divider">
        <p class="text-muted small mb-3">
            <spring:message code="profile.documents.allowedTypesAndSize" arguments="${uploadMaxProfileDocumentMegabytes}"/>
        </p>
        <form method="post" action="<c:url value='/profile/documents'/>" enctype="multipart/form-data">
            <%@ include file="includes/csrfHidden.jspf" %>
            <div class="profile-fields-grid">
                <div class="mb-3">
                    <c:choose>
                        <c:when test="${empty licenseFileName}">
                            <label for="licenseFileInput" class="form-label"><spring:message code="profile.documents.license"/></label>
                        </c:when>
                        <c:otherwise>
                            <div class="form-label"><spring:message code="profile.documents.license"/></div>
                        </c:otherwise>
                    </c:choose>
                    <p class="small mb-2">
                        <c:choose>
                            <c:when test="${licenseValidated}">
                                <i class="bi bi-check-circle-fill text-success" aria-hidden="true"></i>
                            </c:when>
                            <c:otherwise>
                                <i class="bi bi-x-circle-fill text-danger" aria-hidden="true"></i>
                            </c:otherwise>
                        </c:choose>
                        <span class="ms-1">
                            <c:choose>
                                <c:when test="${licenseValidated}">
                                    <spring:message code="profile.documents.status.validated"/>
                                </c:when>
                                <c:otherwise>
                                    <spring:message code="profile.documents.status.notValidated"/>
                                </c:otherwise>
                            </c:choose>
                        </span>
                    </p>
                    <c:if test="${not empty licenseFileName}">
                        <p class="small mb-2">
                            <a class="link-primary text-break"
                               href="<c:url value='/profile/document/view'><c:param name='documentType' value='LICENSE'/></c:url>"
                               target="_blank" rel="noopener noreferrer">
                                <c:out value="${licenseFileName}"/>
                            </a>
                        </p>
                        <button type="submit"
                                class="btn btn-outline-danger btn-sm mb-2"
                                formaction="<c:url value='/profile/document/delete'/>"
                                formmethod="post"
                                name="documentType"
                                value="LICENSE">
                            <spring:message code="profile.documents.remove"/>
                        </button>
                    </c:if>
                    <c:if test="${empty licenseFileName}">
                        <input id="licenseFileInput" class="form-control form-control-sm" type="file" name="licenseFile" accept="image/*,application/pdf"/>
                    </c:if>
                </div>
                <div class="mb-3">
                    <c:choose>
                        <c:when test="${empty insuranceFileName}">
                            <label for="insuranceFileInput" class="form-label"><spring:message code="profile.documents.insurance"/></label>
                        </c:when>
                        <c:otherwise>
                            <div class="form-label"><spring:message code="profile.documents.insurance"/></div>
                        </c:otherwise>
                    </c:choose>
                    <p class="small mb-2">
                        <c:choose>
                            <c:when test="${insuranceValidated}">
                                <i class="bi bi-check-circle-fill text-success" aria-hidden="true"></i>
                            </c:when>
                            <c:otherwise>
                                <i class="bi bi-x-circle-fill text-danger" aria-hidden="true"></i>
                            </c:otherwise>
                        </c:choose>
                        <span class="ms-1">
                            <c:choose>
                                <c:when test="${insuranceValidated}">
                                    <spring:message code="profile.documents.status.validated"/>
                                </c:when>
                                <c:otherwise>
                                    <spring:message code="profile.documents.status.notValidated"/>
                                </c:otherwise>
                            </c:choose>
                        </span>
                    </p>
                    <c:if test="${not empty insuranceFileName}">
                        <p class="small mb-2">
                            <a class="link-primary text-break"
                               href="<c:url value='/profile/document/view'><c:param name='documentType' value='INSURANCE'/></c:url>"
                               target="_blank" rel="noopener noreferrer">
                                <c:out value="${insuranceFileName}"/>
                            </a>
                        </p>
                        <button type="submit"
                                class="btn btn-outline-danger btn-sm mb-2"
                                formaction="<c:url value='/profile/document/delete'/>"
                                formmethod="post"
                                name="documentType"
                                value="INSURANCE">
                            <spring:message code="profile.documents.remove"/>
                        </button>
                    </c:if>
                    <c:if test="${empty insuranceFileName}">
                        <input id="insuranceFileInput" class="form-control form-control-sm" type="file" name="insuranceFile" accept="image/*,application/pdf"/>
                    </c:if>
                </div>
                <div class="mb-3">
                    <c:choose>
                        <c:when test="${empty identityFileName}">
                            <label for="identityFileInput" class="form-label"><spring:message code="profile.documents.identity"/></label>
                        </c:when>
                        <c:otherwise>
                            <div class="form-label"><spring:message code="profile.documents.identity"/></div>
                        </c:otherwise>
                    </c:choose>
                    <p class="small mb-2">
                        <c:choose>
                            <c:when test="${identityValidated}">
                                <i class="bi bi-check-circle-fill text-success" aria-hidden="true"></i>
                            </c:when>
                            <c:otherwise>
                                <i class="bi bi-x-circle-fill text-danger" aria-hidden="true"></i>
                            </c:otherwise>
                        </c:choose>
                        <span class="ms-1">
                            <c:choose>
                                <c:when test="${identityValidated}">
                                    <spring:message code="profile.documents.status.validated"/>
                                </c:when>
                                <c:otherwise>
                                    <spring:message code="profile.documents.status.notValidated"/>
                                </c:otherwise>
                            </c:choose>
                        </span>
                    </p>
                    <c:if test="${not empty identityFileName}">
                        <p class="small mb-2">
                            <a class="link-primary text-break"
                               href="<c:url value='/profile/document/view'><c:param name='documentType' value='IDENTITY'/></c:url>"
                               target="_blank" rel="noopener noreferrer">
                                <c:out value="${identityFileName}"/>
                            </a>
                        </p>
                        <button type="submit"
                                class="btn btn-outline-danger btn-sm mb-2"
                                formaction="<c:url value='/profile/document/delete'/>"
                                formmethod="post"
                                name="documentType"
                                value="IDENTITY">
                            <spring:message code="profile.documents.remove"/>
                        </button>
                    </c:if>
                    <c:if test="${empty identityFileName}">
                        <input id="identityFileInput" class="form-control form-control-sm" type="file" name="identityFile" accept="image/*,application/pdf"/>
                    </c:if>
                </div>
            </div>
            <c:if test="${empty licenseFileName or empty insuranceFileName or empty identityFileName}">
                <button type="submit" class="btn btn-outline-primary btn-sm"><spring:message code="profile.documents.upload"/></button>
            </c:if>
        </form>
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
    var hasProfileErrors = ${hasProfileErrors ? 'true' : 'false'};
    var editProfileBtn = document.getElementById('editProfileBtn');
    var cancelEditBtn = document.getElementById('cancelEditBtn');
    var profileViewSection = document.getElementById('profileViewSection');
    var profileEditingSection = document.getElementById('profileEditingSection');
    var avatarEditBtn = document.getElementById('avatarEditBtn');
    var avatarMenu = document.getElementById('avatarMenu');
    var avatarMenuUpload = document.getElementById('avatarMenuUpload');
    var avatarMenuDelete = document.getElementById('avatarMenuDelete');
    var profilePictureInput = document.getElementById('profilePictureInput');
    var profilePictureForm = document.getElementById('profilePictureForm');
    var profilePictureDeleteForm = document.getElementById('profilePictureDeleteForm');

    function setProfileEditMode(isEditing) {
        profileViewSection.style.display = isEditing ? 'none' : 'block';
        profileEditingSection.style.display = isEditing ? 'block' : 'none';
    }

    setProfileEditMode(hasProfileErrors);

    editProfileBtn.addEventListener('click', function() {
        setProfileEditMode(true);
    });

    cancelEditBtn.addEventListener('click', function() {
        setProfileEditMode(false);
    });

    if (avatarEditBtn && avatarMenu) {
        avatarEditBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            var isOpen = avatarMenu.classList.contains('is-open');
            avatarMenu.classList.toggle('is-open', !isOpen);
            avatarMenu.setAttribute('aria-hidden', isOpen ? 'true' : 'false');
        });

        document.addEventListener('click', function() {
            avatarMenu.classList.remove('is-open');
            avatarMenu.setAttribute('aria-hidden', 'true');
        });

        if (avatarMenuUpload && profilePictureInput && profilePictureForm) {
            avatarMenuUpload.addEventListener('click', function(e) {
                e.stopPropagation();
                avatarMenu.classList.remove('is-open');
                avatarMenu.setAttribute('aria-hidden', 'true');
                profilePictureInput.click();
            });

            profilePictureInput.addEventListener('change', function() {
                setTimeout(function() {
                    var errEl = document.getElementById('profilePictureClientError');
                    var hasError = errEl && !errEl.classList.contains('d-none');
                    if (!hasError && profilePictureInput.files && profilePictureInput.files.length > 0) {
                        profilePictureForm.submit();
                    }
                }, 0);
            });
        }

        if (avatarMenuDelete && profilePictureDeleteForm) {
            avatarMenuDelete.addEventListener('click', function(e) {
                e.stopPropagation();
                avatarMenu.classList.remove('is-open');
                avatarMenu.setAttribute('aria-hidden', 'true');
                profilePictureDeleteForm.submit();
            });
        }
    }
});
</script>

<%@include file="footer.jsp" %>
</body>
</html>
