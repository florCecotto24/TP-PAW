<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<head>
    <%@include file="header.jsp" %>
</head>
<body class="has-fixed-navbar">
<ryden:navbar/>

<main class="container py-5">
    <spring:message code="publishCar.form.title" var="publishFormTitle"/>
    <ryden:breadcrumbTrail currentLabel="${publishFormTitle}"/>
    <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
            <div class="card border-0 shadow-sm rounded-4 bg-white">
                <div class="card-body p-4 p-md-5">

                    <h4 class="fw-semibold mb-4"><c:out value="${publishFormTitle}"/></h4>

                    <form:form id="publishCarFormEl"
                               action="${pageContext.request.contextPath}/publish-car"
                               method="POST"
                               modelAttribute="publishCarForm"
                               enctype="multipart/form-data"
                               htmlEscape="true"
                               novalidate="novalidate"
                               data-publish-retained-count="${fn:length(retainedPictureTokens)}"
                               data-ryden-context-path="${pageContext.request.contextPath}"
                               data-ryden-user-has-cbu="true">

                        <form:errors element="div" cssClass="alert alert-danger mb-3"/>

                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.brand"/></label>
                            <form:input path="brand" required="required" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="brand" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.model"/></label>
                            <form:input path="model" required="required" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="model" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.plate"/></label>
                            <form:input path="plate" required="required" cssClass="form-control" cssErrorClass="form-control is-invalid"
                                        maxlength="10" data-ryden-plate="true" data-ryden-no-punctuation="true" style="text-transform:uppercase"/>
                            <form:errors path="plate" cssClass="text-danger d-block"/>
                        </div>

                        <spring:message code="publishCar.form.type.placeholder" var="typePlaceholder"/>
                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.type"/></label>
                            <input type="hidden" id="publishTypeHidden" name="type"
                                   value="<c:out value='${publishCarForm.type}'/>"
                                   data-ryden-required="true"
                                   data-ryden-dd-btn-id="publishTypeBtn"/>
                            <c:set var="currentTypeLabel" value="${typePlaceholder}"/>
                            <c:forEach var="entry" items="${carTypeOptions}">
                                <c:if test="${not empty publishCarForm.type and entry.key eq publishCarForm.type}">
                                    <c:set var="currentTypeLabel" value="${entry.value}"/>
                                </c:if>
                            </c:forEach>
                            <div class="dropdown">
                                <button type="button" id="publishTypeBtn"
                                        class="form-select dropdown-toggle ryden-select-btn text-start w-100"
                                        data-bs-toggle="dropdown"
                                        data-bs-auto-close="true"
                                        aria-expanded="false">
                                    <span id="publishTypeLbl"><c:out value="${currentTypeLabel}"/></span>
                                </button>
                                <ul class="dropdown-menu shadow ryden-select-menu p-1 w-100">
                                    <c:set var="isAct" value="${empty publishCarForm.type}"/>
                                    <li>
                                        <button type="button" class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                                                data-ryden-select-val=""
                                                data-ryden-select-text="<c:out value='${typePlaceholder}'/>"
                                                data-ryden-target-id="publishTypeHidden"
                                                data-ryden-label-id="publishTypeLbl"
                                                data-ryden-dd-btn-id="publishTypeBtn">
                                            <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                                            <c:out value="${typePlaceholder}"/>
                                        </button>
                                    </li>
                                    <c:forEach var="entry" items="${carTypeOptions}">
                                        <c:set var="isAct" value="${not empty publishCarForm.type and entry.key eq publishCarForm.type}"/>
                                        <li>
                                            <button type="button" class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                                                    data-ryden-select-val="<c:out value='${entry.key}'/>"
                                                    data-ryden-select-text="<c:out value='${entry.value}'/>"
                                                    data-ryden-target-id="publishTypeHidden"
                                                    data-ryden-label-id="publishTypeLbl"
                                                    data-ryden-dd-btn-id="publishTypeBtn">
                                                <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                                                <c:out value="${entry.value}"/>
                                            </button>
                                        </li>
                                    </c:forEach>
                                </ul>
                            </div>
                            <form:errors path="type" cssClass="text-danger d-block"/>
                        </div>

                        <spring:message code="publishCar.form.powertrain.placeholder" var="powertrainPlaceholder"/>
                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.powertrain"/></label>
                            <input type="hidden" id="publishPowertrainHidden" name="powertrain"
                                   value="<c:out value='${publishCarForm.powertrain}'/>"
                                   data-ryden-required="true"
                                   data-ryden-dd-btn-id="publishPowertrainBtn"/>
                            <c:set var="currentPowertrainLabel" value="${powertrainPlaceholder}"/>
                            <c:forEach var="entry" items="${powertrainOptions}">
                                <c:if test="${not empty publishCarForm.powertrain and entry.key eq publishCarForm.powertrain}">
                                    <c:set var="currentPowertrainLabel" value="${entry.value}"/>
                                </c:if>
                            </c:forEach>
                            <div class="dropdown">
                                <button type="button" id="publishPowertrainBtn"
                                        class="form-select dropdown-toggle ryden-select-btn text-start w-100"
                                        data-bs-toggle="dropdown"
                                        data-bs-auto-close="true"
                                        aria-expanded="false">
                                    <span id="publishPowertrainLbl"><c:out value="${currentPowertrainLabel}"/></span>
                                </button>
                                <ul class="dropdown-menu shadow ryden-select-menu p-1 w-100">
                                    <c:set var="isAct" value="${empty publishCarForm.powertrain}"/>
                                    <li>
                                        <button type="button" class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                                                data-ryden-select-val=""
                                                data-ryden-select-text="<c:out value='${powertrainPlaceholder}'/>"
                                                data-ryden-target-id="publishPowertrainHidden"
                                                data-ryden-label-id="publishPowertrainLbl"
                                                data-ryden-dd-btn-id="publishPowertrainBtn">
                                            <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                                            <c:out value="${powertrainPlaceholder}"/>
                                        </button>
                                    </li>
                                    <c:forEach var="entry" items="${powertrainOptions}">
                                        <c:set var="isAct" value="${not empty publishCarForm.powertrain and entry.key eq publishCarForm.powertrain}"/>
                                        <li>
                                            <button type="button" class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                                                    data-ryden-select-val="<c:out value='${entry.key}'/>"
                                                    data-ryden-select-text="<c:out value='${entry.value}'/>"
                                                    data-ryden-target-id="publishPowertrainHidden"
                                                    data-ryden-label-id="publishPowertrainLbl"
                                                    data-ryden-dd-btn-id="publishPowertrainBtn">
                                                <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                                                <c:out value="${entry.value}"/>
                                            </button>
                                        </li>
                                    </c:forEach>
                                </ul>
                            </div>
                            <form:errors path="powertrain" cssClass="text-danger d-block"/>
                        </div>

                        <spring:message code="publishCar.form.transmission.placeholder" var="transmissionPlaceholder"/>
                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.transmission"/></label>
                            <input type="hidden" id="publishTransmissionHidden" name="transmission"
                                   value="<c:out value='${publishCarForm.transmission}'/>"
                                   data-ryden-required="true"
                                   data-ryden-dd-btn-id="publishTransmissionBtn"/>
                            <c:set var="currentTransmissionLabel" value="${transmissionPlaceholder}"/>
                            <c:forEach var="entry" items="${transmissionOptions}">
                                <c:if test="${not empty publishCarForm.transmission and entry.key eq publishCarForm.transmission}">
                                    <c:set var="currentTransmissionLabel" value="${entry.value}"/>
                                </c:if>
                            </c:forEach>
                            <div class="dropdown">
                                <button type="button" id="publishTransmissionBtn"
                                        class="form-select dropdown-toggle ryden-select-btn text-start w-100"
                                        data-bs-toggle="dropdown"
                                        data-bs-auto-close="true"
                                        aria-expanded="false">
                                    <span id="publishTransmissionLbl"><c:out value="${currentTransmissionLabel}"/></span>
                                </button>
                                <ul class="dropdown-menu shadow ryden-select-menu p-1 w-100">
                                    <c:set var="isAct" value="${empty publishCarForm.transmission}"/>
                                    <li>
                                        <button type="button" class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                                                data-ryden-select-val=""
                                                data-ryden-select-text="<c:out value='${transmissionPlaceholder}'/>"
                                                data-ryden-target-id="publishTransmissionHidden"
                                                data-ryden-label-id="publishTransmissionLbl"
                                                data-ryden-dd-btn-id="publishTransmissionBtn">
                                            <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                                            <c:out value="${transmissionPlaceholder}"/>
                                        </button>
                                    </li>
                                    <c:forEach var="entry" items="${transmissionOptions}">
                                        <c:set var="isAct" value="${not empty publishCarForm.transmission and entry.key eq publishCarForm.transmission}"/>
                                        <li>
                                            <button type="button" class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                                                    data-ryden-select-val="<c:out value='${entry.key}'/>"
                                                    data-ryden-select-text="<c:out value='${entry.value}'/>"
                                                    data-ryden-target-id="publishTransmissionHidden"
                                                    data-ryden-label-id="publishTransmissionLbl"
                                                    data-ryden-dd-btn-id="publishTransmissionBtn">
                                                <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                                                <c:out value="${entry.value}"/>
                                            </button>
                                        </li>
                                    </c:forEach>
                                </ul>
                            </div>
                            <form:errors path="transmission" cssClass="text-danger d-block"/>
                        </div>

                        <%-- Photos --%>
                        <spring:message code="validation.image.fileTooLarge" arguments="${uploadMaxImageMegabytes}" var="publishImageTooLargeMsg" htmlEscape="true"/>
                        <spring:message code="validation.pictures.mustBeImage" var="publishMustBeImageMsg" htmlEscape="true"/>
                        <spring:message code="publishCar.form.removeImage" var="removeImageLabel"/>
                        <div class="mb-4">
                            <span class="form-label required-label d-block"><spring:message code="publishCar.form.pictures"/></span>
                            <div class="d-flex flex-wrap align-items-center gap-2 mt-1">
                                <input id="picturesInput" type="file" name="pictures" class="visually-hidden" accept="image/*" multiple aria-label="Vehicle photos"
                                       data-upload-max-image-bytes="<c:out value='${uploadMaxImageBytes}'/>"
                                       data-upload-image-too-large="<c:out value='${publishImageTooLargeMsg}'/>"
                                       data-upload-not-image-msg="<c:out value='${publishMustBeImageMsg}'/>"/>
                                <label id="picturesChooseLabel" for="picturesInput" class="btn btn-outline-secondary mb-0"><spring:message code="publishCar.form.chooseFiles"/></label>
                            </div>
                            <small class="text-muted d-block mt-2"><spring:message code="publishCar.form.pictures.hint" arguments="${uploadMaxImageMegabytes}"/></small>
                            <c:if test="${not empty retainedPictureTokens}">
                                <div id="publishRetainedPictures" class="row g-2 mt-2">
                                    <c:forEach var="rpToken" items="${retainedPictureTokens}">
                                        <div class="col-6 col-md-4" data-retained-picture-col>
                                            <div class="border rounded p-2 position-relative">
                                                <img class="img-fluid rounded"
                                                     style="height:130px;object-fit:cover;width:100%"
                                                     alt=""
                                                     src="${pageContext.request.contextPath}/publish-car/retained-picture/${rpToken}"/>
                                                <button type="button" class="btn btn-sm btn-danger position-absolute top-0 end-0 m-1 ryden-publish-remove-retained-btn"
                                                        aria-label="<c:out value='${removeImageLabel}'/>" data-remove-url="${pageContext.request.contextPath}/publish-car/retained-picture/${rpToken}/remove">
                                                    <i class="bi bi-trash" aria-hidden="true"></i>
                                                </button>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:if>
                            <div id="publishPicturesClientError" class="alert alert-danger d-none mt-2 py-2 small" role="alert"></div>
                            <form:errors path="pictures" cssClass="text-danger d-block"/>
                            <div id="picturesPreview" class="row g-2 mt-2"></div>
                        </div>

                        <div class="d-flex justify-content-end mt-2">
                            <button type="submit" class="btn btn-primary" id="publishCarSubmitBtn">
                                <span class="publish-submit-default">
                                    <i class="bi bi-check-lg"></i> <spring:message code="publishCar.form.submit"/>
                                </span>
                                <span class="publish-submit-loading d-none" aria-live="polite">
                                    <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                                    <spring:message code="publishCar.form.submit"/>
                                </span>
                            </button>
                        </div>

                    </form:form>
                </div>
            </div>
        </div>
    </div>
</main>
<%@include file="footer.jsp" %>
</body>
</html>
