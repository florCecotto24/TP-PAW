<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
<div class="container mt-5 pt-4" style="max-width: 520px;">
    <h1 class="h3 mb-3"><spring:message code="profile.heading"/></h1>

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
    <c:if test="${not empty profilePictureErrorMessage}">
        <div class="alert alert-danger" role="alert">
            <c:out value="${profilePictureErrorMessage}"/>
        </div>
    </c:if>
    <c:if test="${not empty profilePictureErrorCode}">
        <div class="alert alert-danger" role="alert">
            <spring:message code="${profilePictureErrorCode}"/>
        </div>
    </c:if>

    <p class="text-muted mb-1"><spring:message code="profile.name"/></p>
    <p class="fw-semibold mb-3"><c:out value="${userForename}"/> <c:out value="${userSurname}"/></p>
    <p class="text-muted mb-1"><spring:message code="profile.email"/></p>
    <p class="fw-semibold mb-4"><c:out value="${userEmail}"/></p>

    <h2 class="h5 mb-3"><spring:message code="profile.picture.sectionTitle"/></h2>
    <c:if test="${not empty profilePictureImageId}">
        <div class="mb-3">
            <img src="<c:url value='/image/${profilePictureImageId}'/>" alt="" class="rounded-circle border object-fit-cover" width="96" height="96"/>
        </div>
    </c:if>
    <spring:message code="validation.image.fileTooLarge" arguments="${uploadMaxImageMegabytes}" var="profileImageTooLargeMsg" htmlEscape="true"/>
    <spring:message code="validation.pictures.mustBeImage" var="profileMustBeImageMsg" htmlEscape="true"/>
    <form id="profilePictureForm" method="post" action="<c:url value='/profile/picture'/>" enctype="multipart/form-data">
        <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
        <div class="mb-3">
            <label for="profilePictureInput" class="form-label"><spring:message code="profile.picture.label"/></label>
            <input type="file" class="form-control" id="profilePictureInput" name="profilePicture" accept="image/*"
                   data-upload-max-image-bytes="<c:out value='${uploadMaxImageBytes}'/>"
                   data-upload-image-too-large="<c:out value='${profileImageTooLargeMsg}'/>"
                   data-upload-not-image-msg="<c:out value='${profileMustBeImageMsg}'/>"/>
            <div class="form-text"><spring:message code="profile.picture.hint" arguments="${uploadMaxImageMegabytes}"/></div>
        </div>
        <button type="submit" class="btn btn-outline-primary mb-4"><spring:message code="profile.picture.submit"/></button>
    </form>

    <h2 class="h5 mb-3"><spring:message code="profile.optionalSection"/></h2>
    <spring:message code="profile.phone.placeholder" var="profilePhonePlaceholder" htmlEscape="true"/>
    <form:form modelAttribute="profileForm" method="post" cssClass="needs-validation" novalidate="novalidate"
               action="${pageContext.request.contextPath}/profile">
        <form:errors path="*" element="div" cssClass="alert alert-danger" delimiter=" "/>

        <div class="mb-3">
            <label for="phoneNumber" class="form-label"><spring:message code="profile.phone"/></label>
            <input type="text" class="form-control" id="phoneNumber" name="profileForm.phoneNumber" autocomplete="tel"
                   maxlength="<c:out value='${profilePhoneMaxLength}'/>"
                   value="<c:out value='${profileForm.phoneNumber}'/>"
                   placeholder="<c:out value='${profilePhonePlaceholder}'/>"/>
            <form:errors path="phoneNumber" cssClass="text-danger small d-block" element="div"/>
            <div class="form-text"><spring:message code="profile.phone.hint"/></div>
        </div>
        <div class="mb-4">
            <label for="birthDate" class="form-label"><spring:message code="profile.birthDate"/></label>
            <input type="date" class="form-control" id="birthDate" name="profileForm.birthDate"
                   max="<c:out value='${profileBirthDateMax}'/>"
                   value="<c:out value='${profileForm.birthDate}'/>"/>
            <form:errors path="birthDate" cssClass="text-danger small d-block" element="div"/>
            <div class="form-text"><spring:message code="profile.birthDate.hint"/></div>
        </div>
        <button type="submit" class="btn btn-primary"><spring:message code="profile.save"/></button>
    </form:form>
</div>
<%@include file="footer.jsp" %>
</body>
</html>
