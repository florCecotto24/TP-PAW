<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<head>
    <%@include file="../header.jsp" %>
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

                        <%-- ── Brand picker ────────────────────────────────────────────────────── --%>
                        <spring:message code="publishCar.form.brand.placeholder" var="brandPickerPlaceholder"/>
                        <spring:message code="publishCar.form.search" var="brandSearchPlaceholder"/>
                        <spring:message code="publishCar.form.other" var="otherLabel"/>
                        <spring:message code="publishCar.form.brand.otherPlaceholder" var="brandOtherPlaceholder"/>
                        <c:set var="brandIsOther" value="${not empty publishCarForm.brandId and publishCarForm.brandId eq 0}"/>
                        <%-- Determine current button label --%>
                        <c:set var="currentBrandLabel" value="${brandPickerPlaceholder}"/>
                        <c:if test="${brandIsOther}">
                            <c:set var="currentBrandLabel" value="${otherLabel}"/>
                        </c:if>
                        <c:if test="${not empty publishCarForm.brandId and publishCarForm.brandId gt 0}">
                            <c:forEach var="b" items="${allBrands}">
                                <c:if test="${b.id eq publishCarForm.brandId}">
                                    <c:set var="currentBrandLabel" value="${b.name}"/>
                                </c:if>
                            </c:forEach>
                        </c:if>
                        <div class="mb-3">
                            <label class="form-label required-label" for="publishBrandBtn">
                                <spring:message code="publishCar.form.brand"/>
                            </label>
                            <%-- Spring-bound hidden: carries the string value for validation + dual-write --%>
                            <form:hidden path="brand" id="publishBrandHidden"/>
                            <%-- Plain hidden: carries the catalog ID (0 = other) --%>
                            <input type="hidden" id="publishBrandIdHidden" name="brandId"
                                   value="<c:out value='${publishCarForm.brandId}'/>"/>
                            <div class="dropdown" id="publishBrandDd">
                                <button type="button" id="publishBrandBtn"
                                        class="form-select dropdown-toggle ryden-select-btn text-start w-100"
                                        data-bs-toggle="dropdown" data-bs-auto-close="outside"
                                        aria-expanded="false" aria-haspopup="true">
                                    <span id="publishBrandLbl" class="text-truncate"><c:out value="${currentBrandLabel}"/></span>
                                </button>
                                <div class="dropdown-menu shadow p-0 w-100" style="min-width:0"
                                     aria-labelledby="publishBrandBtn">
                                    <div class="px-3 pt-2 pb-1">
                                        <input type="search" id="publishBrandFilter"
                                               class="form-control form-control-sm" autocomplete="off"
                                               placeholder="<c:out value='${brandSearchPlaceholder}'/>"/>
                                    </div>
                                    <div class="ryden-catalog-scroll px-2 pb-2" style="max-height:220px;overflow-y:auto">
                                        <ul class="list-unstyled mb-0" id="publishBrandList">
                                            <li>
                                                <label class="dropdown-item d-flex gap-2 align-items-center py-2 px-2 mb-0 rounded-2">
                                                    <input type="radio" class="form-check-input flex-shrink-0 js-brand-pick mt-0"
                                                           name="_brandPick" value="0"
                                                           data-catname=""
                                                           <c:if test="${brandIsOther}">checked="checked"</c:if>/>
                                                    <span class="small"><c:out value="${otherLabel}"/></span>
                                                </label>
                                            </li>
                                            <c:forEach var="b" items="${allBrands}">
                                                <li data-catlookup="${fn:toLowerCase(b.name)}">
                                                    <label class="dropdown-item d-flex gap-2 align-items-center py-2 px-2 mb-0 rounded-2">
                                                        <input type="radio" class="form-check-input flex-shrink-0 js-brand-pick mt-0"
                                                               name="_brandPick" value="${b.id}"
                                                               data-catname="<c:out value='${b.name}'/>"
                                                               <c:if test="${not empty publishCarForm.brandId and publishCarForm.brandId eq b.id}">checked="checked"</c:if>/>
                                                        <span class="small"><c:out value="${b.name}"/></span>
                                                    </label>
                                                </li>
                                            </c:forEach>
                                        </ul>
                                    </div>
                                </div>
                            </div>
                            <%-- "Other" text input: visible when brandId == 0 --%>
                            <div id="publishBrandOtherRow" class="mt-2<c:if test="${not brandIsOther}"> d-none</c:if>">
                                <input type="text" id="publishBrandOtherInput"
                                       class="form-control" maxlength="50"
                                       placeholder="<c:out value='${brandOtherPlaceholder}'/>"
                                       value="<c:if test='${brandIsOther}'><c:out value='${publishCarForm.brand}'/></c:if>"/>
                            </div>
                            <form:errors path="brand" cssClass="text-danger d-block"/>
                        </div>

                        <%-- ── Model picker ────────────────────────────────────────────────────── --%>
                        <spring:message code="publishCar.form.model.placeholder" var="modelPickerPlaceholder"/>
                        <spring:message code="publishCar.form.model.selectBrandFirst" var="modelSelectBrandFirst"/>
                        <spring:message code="publishCar.form.model.otherPlaceholder" var="modelOtherPlaceholder"/>
                        <c:set var="modelIsOther" value="${not empty publishCarForm.modelId and publishCarForm.modelId eq 0}"/>
                        <c:set var="currentModelLabel" value="${modelPickerPlaceholder}"/>
                        <c:if test="${modelIsOther}">
                            <c:set var="currentModelLabel" value="${otherLabel}"/>
                        </c:if>
                        <c:if test="${not empty publishCarForm.modelId and publishCarForm.modelId gt 0}">
                            <c:forEach var="m" items="${allModels}">
                                <c:if test="${m.id eq publishCarForm.modelId}">
                                    <c:set var="currentModelLabel" value="${m.name}"/>
                                </c:if>
                            </c:forEach>
                        </c:if>
                        <div class="mb-3">
                            <label class="form-label required-label" for="publishModelBtn">
                                <spring:message code="publishCar.form.model"/>
                            </label>
                            <form:hidden path="model" id="publishModelHidden"/>
                            <input type="hidden" id="publishModelIdHidden" name="modelId"
                                   value="<c:out value='${publishCarForm.modelId}'/>"/>
                            <div class="dropdown<c:if test="${brandIsOther}"> d-none</c:if>" id="publishModelDd">
                                <button type="button" id="publishModelBtn"
                                        class="form-select dropdown-toggle ryden-select-btn text-start w-100"
                                        data-bs-toggle="dropdown" data-bs-auto-close="outside"
                                        aria-expanded="false" aria-haspopup="true"
                                        <c:if test="${empty publishCarForm.brandId}">disabled="disabled"</c:if>>
                                    <span id="publishModelLbl" class="text-truncate">
                                        <c:choose>
                                            <c:when test="${empty publishCarForm.brandId}"><c:out value="${modelSelectBrandFirst}"/></c:when>
                                            <c:otherwise><c:out value="${currentModelLabel}"/></c:otherwise>
                                        </c:choose>
                                    </span>
                                </button>
                                <div class="dropdown-menu shadow p-0 w-100" style="min-width:0"
                                     aria-labelledby="publishModelBtn">
                                    <div class="px-3 pt-2 pb-1">
                                        <input type="search" id="publishModelFilter"
                                               class="form-control form-control-sm" autocomplete="off"
                                               placeholder="<c:out value='${brandSearchPlaceholder}'/>"/>
                                    </div>
                                    <div class="ryden-catalog-scroll px-2 pb-2" style="max-height:220px;overflow-y:auto">
                                        <ul class="list-unstyled mb-0" id="publishModelList">
                                            <li id="publishModelOtherLi">
                                                <label class="dropdown-item d-flex gap-2 align-items-center py-2 px-2 mb-0 rounded-2">
                                                    <input type="radio" class="form-check-input flex-shrink-0 js-model-pick mt-0"
                                                           name="_modelPick" value="0"
                                                           data-catname=""
                                                           <c:if test="${modelIsOther}">checked="checked"</c:if>/>
                                                    <span class="small"><c:out value="${otherLabel}"/></span>
                                                </label>
                                            </li>
                                            <c:forEach var="m" items="${allModels}">
                                                <li data-catlookup="${fn:toLowerCase(m.name)}"
                                                    data-brandid="${m.brandId}">
                                                    <label class="dropdown-item d-flex gap-2 align-items-center py-2 px-2 mb-0 rounded-2">
                                                        <input type="radio" class="form-check-input flex-shrink-0 js-model-pick mt-0"
                                                               name="_modelPick" value="${m.id}"
                                                               data-catname="<c:out value='${m.name}'/>"
                                                               <c:if test="${not empty publishCarForm.modelId and publishCarForm.modelId eq m.id}">checked="checked"</c:if>/>
                                                        <span class="small"><c:out value="${m.name}"/></span>
                                                    </label>
                                                </li>
                                            </c:forEach>
                                        </ul>
                                    </div>
                                </div>
                            </div>
                            <%-- "Other" model text input --%>
                            <div id="publishModelOtherRow" class="mt-2<c:if test="${not brandIsOther and not modelIsOther}"> d-none</c:if>">
                                <input type="text" id="publishModelOtherInput"
                                       class="form-control" maxlength="50"
                                       placeholder="<c:out value='${modelOtherPlaceholder}'/>"
                                       value="<c:if test='${modelIsOther}'><c:out value='${publishCarForm.model}'/></c:if>"/>
                            </div>
                            <form:errors path="model" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.plate"/></label>
                            <form:input path="plate" required="required" cssClass="form-control" cssErrorClass="form-control is-invalid"
                                        maxlength="10" data-ryden-plate="true" data-ryden-no-punctuation="true" style="text-transform:uppercase"/>
                            <form:errors path="plate" cssClass="text-danger d-block"/>
                        </div>

                        <%-- Type dropdown: only required/visible when creating a new model ("Other"); when an
                             existing catalog model is picked, its type is taken from car_models. --%>
                        <spring:message code="publishCar.form.type.placeholder" var="typePlaceholder"/>
                        <div id="publishTypeWrap" class="mb-3<c:if test="${not modelIsOther}"> d-none</c:if>">
                            <label class="form-label required-label"><spring:message code="publishCar.form.type"/></label>
                            <input type="hidden" id="publishTypeHidden" name="type"
                                   value="<c:out value='${publishCarForm.type}'/>"
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

                        <%-- Optional manufacture year (>= 1886, <= current year). Rendered as type=text (with
                             inputmode=numeric and digits-only JS) to prevent the browser from silently clamping
                             out-of-range values to {min,max} on submit, which would mask the rule from the user.
                             The min message uses the Hibernate Validator {value} template (Spring/MessageFormat
                             ignores non-positional placeholders); we substitute it client-side. --%>
                        <spring:message code="publishCar.form.year.placeholder" var="yearPlaceholder"/>
                        <spring:message code="validation.year.min" var="yearMinClientTemplate" htmlEscape="true"/>
                        <spring:message code="validation.year.max" arguments="${carYearMax}" var="yearMaxClientMsg" htmlEscape="true"/>
                        <div class="mb-3">
                            <label class="form-label" for="publishCarYear"><spring:message code="publishCar.form.year"/></label>
                            <form:input path="year" id="publishCarYear" type="text" cssClass="form-control"
                                        cssErrorClass="form-control is-invalid"
                                        inputmode="numeric" autocomplete="off" maxlength="4"
                                        placeholder="${yearPlaceholder}"
                                        data-ryden-digits-only="true"
                                        data-year-min="${carYearMin}"
                                        data-year-max="${carYearMax}"
                                        data-year-min-template="${yearMinClientTemplate}"
                                        data-year-max-msg="${yearMaxClientMsg}"/>
                            <div id="publishCarYearClientError" class="text-danger d-block small mt-1 d-none" aria-live="polite"></div>
                            <form:errors path="year" cssClass="text-danger d-block"/>
                        </div>

                        <%-- Description --%>
                        <div class="mb-3">
                            <label class="form-label"><spring:message code="publishCar.form.description"/></label>
                            <form:textarea path="description" rows="3" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="description" cssClass="text-danger d-block"/>
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

                        <%-- Optional insurance document; if missing, the car is created in LACK_DOC and can be uploaded later from the car detail page.
                             When a previous submission attached one, the file is preserved server-side (browser cannot repopulate <input type="file">). --%>
                        <spring:message code="publishCar.form.removeFile" var="removeFileLabel"/>
                        <div class="mb-4">
                            <label for="publishInsuranceFileInput" class="form-label d-block">
                                <spring:message code="publishCar.form.insurance"/>
                            </label>
                            <input id="publishInsuranceFileInput" type="file" name="insuranceFile"
                                   accept="application/pdf,image/*"
                                   class="form-control"/>
                            <div id="publishInsuranceSelected" class="d-none d-flex align-items-center gap-2 mt-2">
                                <i class="bi bi-file-earmark text-secondary" aria-hidden="true"></i>
                                <span id="publishInsuranceFileName" class="small text-truncate" style="max-width:260px"></span>
                                <button type="button" id="publishInsuranceClearBtn"
                                        class="btn btn-sm btn-outline-danger ms-1"
                                        aria-label="<c:out value='${removeFileLabel}'/>">
                                    <i class="bi bi-trash" aria-hidden="true"></i>
                                </button>
                            </div>
                            <c:if test="${not empty retainedInsuranceFilename}">
                                <div id="publishRetainedInsurance" class="d-flex align-items-center gap-2 mt-2"
                                     data-remove-url="${pageContext.request.contextPath}/publish-car/retained-insurance/remove">
                                    <i class="bi bi-file-earmark-check text-success" aria-hidden="true"></i>
                                    <a href="${pageContext.request.contextPath}/publish-car/retained-insurance"
                                       class="small text-truncate" style="max-width:260px"
                                       target="_blank" rel="noopener noreferrer">
                                        <c:out value="${retainedInsuranceFilename}"/>
                                    </a>
                                    <button type="button" id="publishRetainedInsuranceRemoveBtn"
                                            class="btn btn-sm btn-outline-danger ms-1"
                                            aria-label="<c:out value='${removeFileLabel}'/>">
                                        <i class="bi bi-trash" aria-hidden="true"></i>
                                    </button>
                                </div>
                            </c:if>
                            <small class="text-muted d-block mt-2">
                                <spring:message code="publishCar.form.insurance.hint" arguments="${uploadMaxProfileDocumentMegabytes}"/>
                            </small>
                            <form:errors path="insuranceFile" cssClass="text-danger d-block"/>
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
<%@include file="../footer.jsp" %>
<script>
(function () {
    'use strict';

    /* ── Helpers ─────────────────────────────────────────────────────────── */
    function levenshtein(a, b) {
        if (!a.length) { return b.length; }
        if (!b.length) { return a.length; }
        var row = [];
        for (var j = 0; j <= b.length; j++) { row[j] = j; }
        for (var i = 1; i <= a.length; i++) {
            var prev = row[0];
            row[0] = i;
            for (var j2 = 1; j2 <= b.length; j2++) {
                var cur = row[j2];
                row[j2] = Math.min(prev + (a[i-1] === b[j2-1] ? 0 : 1), row[j2] + 1, row[j2-1] + 1);
                prev = cur;
            }
        }
        return row[b.length];
    }
    function nameMatches(q, nameLower) {
        if (!q) { return true; }
        if (nameLower.indexOf(q) !== -1) { return true; }
        return levenshtein(q, nameLower) <= 2;
    }

    /* ── Brand picker ────────────────────────────────────────────────────── */
    var brandHid    = document.getElementById('publishBrandHidden');
    var brandIdHid  = document.getElementById('publishBrandIdHidden');
    var brandBtn    = document.getElementById('publishBrandBtn');
    var brandLbl    = document.getElementById('publishBrandLbl');
    var brandList   = document.getElementById('publishBrandList');
    var brandFilter = document.getElementById('publishBrandFilter');
    var brandOtherRow   = document.getElementById('publishBrandOtherRow');
    var brandOtherInput = document.getElementById('publishBrandOtherInput');

    /* ── Model picker ────────────────────────────────────────────────────── */
    var modelHid    = document.getElementById('publishModelHidden');
    var modelIdHid  = document.getElementById('publishModelIdHidden');
    var modelBtn    = document.getElementById('publishModelBtn');
    var modelLbl    = document.getElementById('publishModelLbl');
    var modelList   = document.getElementById('publishModelList');
    var modelFilter = document.getElementById('publishModelFilter');
    var modelOtherRow   = document.getElementById('publishModelOtherRow');
    var modelOtherInput = document.getElementById('publishModelOtherInput');

    /* ── Type wrapper (only visible when creating a new car model "Other") ── */
    var typeWrap   = document.getElementById('publishTypeWrap');
    var typeHidden = document.getElementById('publishTypeHidden');
    var typeLbl    = document.getElementById('publishTypeLbl');
    function setTypeRequired(visible) {
        if (!typeWrap) { return; }
        typeWrap.classList.toggle('d-none', !visible);
        if (!visible && typeHidden) {
            typeHidden.value = '';
            if (typeLbl) {
                typeLbl.textContent = '${fn:escapeXml(typePlaceholder)}';
            }
        }
    }

    var brandSelectBrandFirst = modelBtn ? (modelBtn.getAttribute('data-select-brand-first') || '') : '';

    /* ── Brand filter ─────────────────────────────────────────────────────── */
    function applyBrandFilter() {
        if (!brandList || !brandFilter) { return; }
        var q = brandFilter.value.trim().toLowerCase();
        brandList.querySelectorAll('li[data-catlookup]').forEach(function (li) {
            var key = li.getAttribute('data-catlookup') || '';
            li.classList.toggle('d-none', !!q && !nameMatches(q, key));
        });
    }
    if (brandFilter) {
        brandFilter.addEventListener('input', applyBrandFilter);
        document.getElementById('publishBrandDd') && document.getElementById('publishBrandDd')
            .addEventListener('shown.bs.dropdown', function () { brandFilter.focus(); });
    }

    /* ── Brand selection ─────────────────────────────────────────────────── */
    function getCurrentBrandId() {
        return brandIdHid ? (brandIdHid.value || '') : '';
    }

    function onBrandPicked(radio) {
        var id   = radio.value;
        var name = radio.getAttribute('data-catname') || '';
        if (brandIdHid) { brandIdHid.value = id; }
        if (brandLbl)   { brandLbl.textContent = id === '0' ? brandOtherInput.closest('label') ? '' : '' : name; }
        // determine button label
        var lbl = '';
        if (id === '0') {
            // get the "other" span text from the radio's parent label
            var otherLi = radio.closest('li');
            var otherSpan = otherLi ? otherLi.querySelector('span.small') : null;
            lbl = otherSpan ? otherSpan.textContent.trim() : name;
        } else {
            lbl = name;
        }
        if (brandLbl) { brandLbl.textContent = lbl; }
        // sync brand string field
        if (id === '0') {
            // "Other" selected: clear brand string (will be filled by text input)
            if (brandHid) { brandHid.value = brandOtherInput ? brandOtherInput.value : ''; }
            if (brandOtherRow) { brandOtherRow.classList.remove('d-none'); }
            if (brandOtherInput) { brandOtherInput.focus(); }
        } else {
            if (brandHid) { brandHid.value = name; }
            if (brandOtherRow) { brandOtherRow.classList.add('d-none'); }
            if (brandOtherInput) { brandOtherInput.value = ''; }
        }
        // Reset model picker when brand changes
        resetModelPicker(id);
        // close dropdown
        if (brandBtn && window.bootstrap && bootstrap.Dropdown) {
            var inst = bootstrap.Dropdown.getInstance(brandBtn);
            if (inst) { inst.hide(); }
        }
    }

    if (brandList) {
        brandList.querySelectorAll('.js-brand-pick').forEach(function (rb) {
            rb.addEventListener('change', function () { onBrandPicked(this); });
        });
    }
    if (brandOtherInput) {
        brandOtherInput.addEventListener('input', function () {
            if (brandHid) { brandHid.value = this.value; }
        });
    }

    /* ── Model filter ─────────────────────────────────────────────────────── */
    function applyModelFilter() {
        if (!modelList || !modelFilter) { return; }
        var q   = modelFilter.value.trim().toLowerCase();
        var bid = getCurrentBrandId();
        modelList.querySelectorAll('li[data-catlookup]').forEach(function (li) {
            var lisBrand = li.getAttribute('data-brandid') || '';
            var key = li.getAttribute('data-catlookup') || '';
            // Show only catalog models matching the selected brand.
            // When bid is empty (no brand yet) or bid='0' (other), hide all catalog items.
            var brandMatch = !!bid && bid !== '0' && lisBrand === bid;
            li.classList.toggle('d-none', !brandMatch || (!!q && !nameMatches(q, key)));
        });
    }
    if (modelFilter) {
        modelFilter.addEventListener('input', applyModelFilter);
        document.getElementById('publishModelDd') && document.getElementById('publishModelDd')
            .addEventListener('shown.bs.dropdown', function () { modelFilter.focus(); });
    }

    /* ── Model selection ─────────────────────────────────────────────────── */
    function onModelPicked(radio) {
        var id   = radio.value;
        var name = radio.getAttribute('data-catname') || '';
        if (modelIdHid) { modelIdHid.value = id; }
        var lbl = '';
        if (id === '0') {
            var otherLi = radio.closest('li');
            var otherSpan = otherLi ? otherLi.querySelector('span.small') : null;
            lbl = otherSpan ? otherSpan.textContent.trim() : name;
        } else {
            lbl = name;
        }
        if (modelLbl) { modelLbl.textContent = lbl; }
        if (id === '0') {
            if (modelHid) { modelHid.value = modelOtherInput ? modelOtherInput.value : ''; }
            if (modelOtherRow) { modelOtherRow.classList.remove('d-none'); }
            if (modelOtherInput) { modelOtherInput.focus(); }
        } else {
            if (modelHid) { modelHid.value = name; }
            if (modelOtherRow) { modelOtherRow.classList.add('d-none'); }
            if (modelOtherInput) { modelOtherInput.value = ''; }
        }
        // Show body-type dropdown only when creating a new catalog model.
        setTypeRequired(id === '0');
        if (modelBtn && window.bootstrap && bootstrap.Dropdown) {
            var inst = bootstrap.Dropdown.getInstance(modelBtn);
            if (inst) { inst.hide(); }
        }
    }

    if (modelList) {
        modelList.querySelectorAll('.js-model-pick').forEach(function (rb) {
            rb.addEventListener('change', function () { onModelPicked(this); });
        });
    }
    if (modelOtherInput) {
        modelOtherInput.addEventListener('input', function () {
            if (modelHid) { modelHid.value = this.value; }
        });
    }

    /* ── Reset model picker (called when brand changes) ──────────────────── */
    function resetModelPicker(newBrandId) {
        // Uncheck all model radios
        if (modelList) {
            modelList.querySelectorAll('.js-model-pick').forEach(function (rb) { rb.checked = false; });
        }
        if (modelIdHid) { modelIdHid.value = ''; }
        if (modelHid)   { modelHid.value = ''; }
        if (modelOtherInput) { modelOtherInput.value = ''; }
        if (modelFilter) { modelFilter.value = ''; }
        var publishModelDd = document.getElementById('publishModelDd');
        if (newBrandId === '0') {
            // "Other" brand: bypass dropdown and show text input directly
            if (publishModelDd) { publishModelDd.classList.add('d-none'); }
            if (modelOtherRow) { modelOtherRow.classList.remove('d-none'); }
            if (modelIdHid) { modelIdHid.value = '0'; }
            // New model implies type is required.
            setTypeRequired(true);
        } else {
            // Normal brand: restore dropdown, hide text input
            if (publishModelDd) { publishModelDd.classList.remove('d-none'); }
            if (modelOtherRow) { modelOtherRow.classList.add('d-none'); }
            if (modelBtn) {
                var hasValidBrand = newBrandId && newBrandId !== '';
                modelBtn.disabled = !hasValidBrand;
                if (modelLbl) {
                    modelLbl.textContent = hasValidBrand
                        ? modelBtn.getAttribute('data-placeholder') || ''
                        : (modelBtn.getAttribute('data-select-brand-first') || '');
                }
            }
            // No model picked yet → hide body-type dropdown.
            setTypeRequired(false);
        }
        applyModelFilter();
    }

    /* ── Store placeholder texts on the model button for JS reuse ────────── */
    if (modelBtn) {
        modelBtn.setAttribute('data-placeholder', modelLbl ? modelLbl.textContent.trim() : '');
        modelBtn.setAttribute('data-select-brand-first', '${fn:escapeXml(modelSelectBrandFirst)}');
    }

    /* ── Initial model filter (page load with pre-selected brand) ─────────── */
    (function initFilters() {
        applyModelFilter();
        var bid = getCurrentBrandId();
        if (bid === '0') {
            // Brand is "Other" on page load: ensure model text input is shown, modelId is 0
            if (modelOtherRow) { modelOtherRow.classList.remove('d-none'); }
            if (modelIdHid && !modelIdHid.value) { modelIdHid.value = '0'; }
        } else if (modelBtn && bid && bid !== '') {
            // On page re-display after a validation error, reconstruct model button label
            modelBtn.disabled = false;
        }
    }());

    /* ── Year input: validate range [min, max] on blur/input/change and block submit if out-of-range. ──
       We do NOT silently clamp the value; the user must correct it explicitly so the backend rule
       (PublishCarFormValidator: year <= currentYear, @Min(1886)) is mirrored visibly on the client.
       The submit listener runs in the capture phase + stopImmediatePropagation to ensure other submit
       handlers (e.g. spinner / availability checks) cannot override the block. */
    (function initYearGuard() {
        var yearInput = document.getElementById('publishCarYear');
        var errorBox  = document.getElementById('publishCarYearClientError');
        if (!yearInput) { return; }
        function readBound(attr) {
            var n = parseInt(yearInput.getAttribute(attr), 10);
            return isNaN(n) ? null : n;
        }
        function showError(msg) {
            yearInput.classList.add('is-invalid');
            if (errorBox) {
                errorBox.textContent = msg || '';
                errorBox.classList.remove('d-none');
            }
        }
        function clearError() {
            yearInput.classList.remove('is-invalid');
            if (errorBox) {
                errorBox.textContent = '';
                errorBox.classList.add('d-none');
            }
        }
        function validate() {
            var raw = (yearInput.value || '').trim();
            if (!raw) { clearError(); return true; }
            var v = parseInt(raw, 10);
            if (isNaN(v)) { clearError(); return true; }
            var lo = readBound('data-year-min');
            var hi = readBound('data-year-max');
            if (lo !== null && v < lo) {
                var tpl = yearInput.getAttribute('data-year-min-template') || '';
                showError(tpl.replace('{value}', String(lo)));
                return false;
            }
            if (hi !== null && v > hi) {
                showError(yearInput.getAttribute('data-year-max-msg') || '');
                return false;
            }
            clearError();
            return true;
        }
        yearInput.addEventListener('input', validate);
        yearInput.addEventListener('change', validate);
        yearInput.addEventListener('blur', validate);
        var formEl = document.getElementById('publishCarFormEl');
        if (formEl) {
            formEl.addEventListener('submit', function (e) {
                if (!validate()) {
                    e.preventDefault();
                    e.stopImmediatePropagation();
                    yearInput.focus();
                }
            }, true);
        }
    }());

    /* ── Insurance file clear button + retained-from-previous-submit cleanup ─ */
    (function initInsuranceClear() {
        var insuranceInput    = document.getElementById('publishInsuranceFileInput');
        var insuranceSelected = document.getElementById('publishInsuranceSelected');
        var insuranceFileName = document.getElementById('publishInsuranceFileName');
        var insuranceClearBtn = document.getElementById('publishInsuranceClearBtn');
        var retainedBox       = document.getElementById('publishRetainedInsurance');
        var retainedRemoveBtn = document.getElementById('publishRetainedInsuranceRemoveBtn');
        if (!insuranceInput && !retainedBox) { return; }

        if (insuranceInput) {
            insuranceInput.addEventListener('change', function () {
                if (this.files && this.files.length > 0) {
                    if (insuranceFileName) { insuranceFileName.textContent = this.files[0].name; }
                    if (insuranceSelected) { insuranceSelected.classList.remove('d-none'); }
                    // A fresh upload will replace the stashed one server-side on submit; hide the chip now.
                    if (retainedBox) { retainedBox.classList.add('d-none'); }
                } else {
                    if (insuranceSelected) { insuranceSelected.classList.add('d-none'); }
                }
            });
        }
        if (insuranceClearBtn) {
            insuranceClearBtn.addEventListener('click', function () {
                if (insuranceInput) { insuranceInput.value = ''; }
                if (insuranceSelected) { insuranceSelected.classList.add('d-none'); }
            });
        }
        if (retainedRemoveBtn && retainedBox) {
            retainedRemoveBtn.addEventListener('click', function () {
                var url = retainedBox.getAttribute('data-remove-url');
                if (!url) { return; }
                retainedRemoveBtn.disabled = true;
                var xhr = new XMLHttpRequest();
                xhr.open('POST', url, true);
                xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
                xhr.onreadystatechange = function () {
                    if (xhr.readyState !== 4) { return; }
                    if (xhr.status >= 200 && xhr.status < 400) {
                        retainedBox.parentNode.removeChild(retainedBox);
                    } else {
                        retainedRemoveBtn.disabled = false;
                    }
                };
                xhr.send(null);
            });
        }
    }());

})();
</script>
</body>
</html>
