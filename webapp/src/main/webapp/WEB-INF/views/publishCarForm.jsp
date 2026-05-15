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

                    <%-- Wizard progress bar --%>
                    <spring:message code="publishWizard.step1.label" var="wStep1Label"/>
                    <spring:message code="publishWizard.step2.label" var="wStep2Label"/>
                    <spring:message code="publishWizard.step3.label" var="wStep3Label"/>
                    <spring:message code="publishWizard.step4.label" var="wStep4Label"/>
                    <spring:message code="publishWizard.step1.title" var="wStep1Title"/>
                    <spring:message code="publishWizard.step2.title" var="wStep2Title"/>
                    <spring:message code="publishWizard.step3.title" var="wStep3Title"/>
                    <spring:message code="publishWizard.step4.title" var="wStep4Title"/>
                    <spring:message code="publishWizard.nav.next"    var="wNavNext"/>
                    <spring:message code="publishWizard.nav.prev"    var="wNavPrev"/>
                    <div id="publishWizardProgress" class="mb-4">
                        <div class="d-flex align-items-center justify-content-between mb-3">
                            <h5 class="wizard-step-title mb-0 fw-semibold" id="wizardStepTitle"></h5>
                            <span class="text-muted small" id="wizardStepCounter" style="font-variant-numeric: tabular-nums;"></span>
                        </div>
                        <div class="wizard-steps-track">
                            <div class="wizard-step-node" data-step="1">
                                <div class="wizard-node-circle">
                                    <span class="wizard-node-num">1</span>
                                    <i class="bi bi-check2 wizard-node-check" aria-hidden="true"></i>
                                </div>
                                <span class="wizard-node-label" data-title="<c:out value='${wStep1Title}'/>"><c:out value="${wStep1Label}"/></span>
                            </div>
                            <div class="wizard-connector" data-connector="1"></div>
                            <div class="wizard-step-node" data-step="2">
                                <div class="wizard-node-circle">
                                    <span class="wizard-node-num">2</span>
                                    <i class="bi bi-check2 wizard-node-check" aria-hidden="true"></i>
                                </div>
                                <span class="wizard-node-label" data-title="<c:out value='${wStep2Title}'/>"><c:out value="${wStep2Label}"/></span>
                            </div>
                            <div class="wizard-connector" data-connector="2"></div>
                            <div class="wizard-step-node" data-step="3">
                                <div class="wizard-node-circle">
                                    <span class="wizard-node-num">3</span>
                                    <i class="bi bi-check2 wizard-node-check" aria-hidden="true"></i>
                                </div>
                                <span class="wizard-node-label" data-title="<c:out value='${wStep3Title}'/>"><c:out value="${wStep3Label}"/></span>
                            </div>
                            <div class="wizard-connector" data-connector="3"></div>
                            <div class="wizard-step-node" data-step="4">
                                <div class="wizard-node-circle">
                                    <span class="wizard-node-num">4</span>
                                    <i class="bi bi-check2 wizard-node-check" aria-hidden="true"></i>
                                </div>
                                <span class="wizard-node-label" data-title="<c:out value='${wStep4Title}'/>"><c:out value="${wStep4Label}"/></span>
                            </div>
                        </div>
                    </div>

                    <spring:message code="publishCar.form.pictures.clientRequired" var="publishPicturesClientRequired" htmlEscape="true"/>
                    <spring:message code="validation.neighborhood.invalid" var="publishNbInvalidMsg" htmlEscape="true"/>
                    <spring:message code="validation.neighborhood.notNull" var="publishNbRequiredMsg" htmlEscape="true"/>
                    <spring:message code="publishCar.missingCbu.invalid" var="publishMissingCbuInvalidMsg" htmlEscape="false" arguments="${cbuRequiredDigits}"/>
                    <spring:message code="publishCar.missingCbu.saveFailed" var="publishMissingCbuSaveFailedMsg" htmlEscape="false"/>
                    <form:form id="publishCarFormEl"
                               action="${pageContext.request.contextPath}/publish-car"
                               method="POST"
                               modelAttribute="publishCarForm"
                               enctype="multipart/form-data"
                               htmlEscape="true"
                               data-ryden-user-has-cbu="${userHasCbu ? 'true' : 'false'}"
                               data-ryden-context-path="${pageContext.request.contextPath}"
                               data-ryden-quick-cbu-url="${pageContext.request.contextPath}/publish-car/quick-cbu"
                               data-ryden-cbu-invalid="${fn:escapeXml(publishMissingCbuInvalidMsg)}"
                               data-ryden-cbu-save-failed="${fn:escapeXml(publishMissingCbuSaveFailedMsg)}"
                               data-publish-retained-count="${fn:escapeXml(retainedPicturesCount)}"
                               data-ryden-nb-invalid="${fn:escapeXml(publishNbInvalidMsg)}"
                               data-ryden-nb-required="${fn:escapeXml(publishNbRequiredMsg)}">

                        <%-- ── STEP 1: Vehicle info ─────────────────────────────────── --%>
                        <div class="publish-wizard-step" data-step="1">
                            <form:errors element="div" cssClass="alert alert-danger mb-3"/>

                            <div class="mb-4 p-3 rounded-3 bg-light border">
                                <p class="mb-1 small text-muted"><spring:message code="publishCar.form.publishingAs"/></p>
                                <p class="mb-0 fw-semibold"><c:out value="${publisherDisplayName}"/></p>
                                <p class="mb-0 text-secondary small"><c:out value="${publisherEmail}"/></p>
                            </div>

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

                            <div class="d-flex justify-content-end mt-4">
                                <button type="button" class="btn btn-primary" data-wizard-next>
                                    <c:out value="${wNavNext}"/>
                                </button>
                            </div>
                        </div>

                        <%-- ── STEP 2: Location & price ─────────────────────────────── --%>
                        <div class="publish-wizard-step" data-step="2">
                            <div class="mb-3">
                                <label class="form-label required-label"><spring:message code="publishCar.form.pricePerDay"/></label>
                                <form:input path="pricePerDay" required="required" cssClass="form-control js-no-number-wheel-step js-listing-price-decimal" cssErrorClass="form-control is-invalid js-no-number-wheel-step js-listing-price-decimal" type="number" step="0.01" max="99999999.99" data-max-int="8" data-max-frac="2"/>
                                <form:errors path="pricePerDay" cssClass="text-danger d-block"/>
                            </div>

                            <spring:message code="publishCar.form.neighborhood.placeholder" var="publishNbPh"/>
                            <spring:message code="publishCar.form.neighborhood" var="publishNbFieldLabel"/>
                            <spring:message code="publishCar.form.neighborhood.search" var="publishNbSearchPh"/>
                            <form:hidden path="neighborhoodId" id="nb_hid_publish"/>
                            <div class="mb-3">
                                <ryden:neighborhoodPicker
                                        pickerId="publish"
                                        mode="get"
                                        allowMultiple="false"
                                        springPath="neighborhoodId"
                                        selectedNeighborhoodId="${publishCarForm.neighborhoodId}"
                                        neighborhoodList="${allNeighborhoods}"
                                        anyLabel="${publishNbPh}"
                                        searchPlaceholder="${publishNbSearchPh}"
                                        selectFieldLabel="${publishNbFieldLabel}"
                                        toggleAriaLabel="${publishNbFieldLabel}"
                                        outerLabel="${publishNbFieldLabel}"
                                        outerLabelRequired="true"
                                        required="true"
                                        nbRequiredMessage="${publishNbRequiredMsg}"
                                        formId="publishCarFormEl"/>
                            </div>

                            <div class="row g-3 mb-1">
                                <div class="col-md-8">
                                    <label class="form-label required-label" for="publish_start_point_street"><spring:message code="publishCar.form.pickupStreet"/></label>
                                    <form:input path="startPointStreet" id="publish_start_point_street" required="required" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                                    <form:errors path="startPointStreet" cssClass="text-danger d-block"/>
                                </div>
                                <div class="col-md-4">
                                    <label class="form-label required-label" for="publish_start_point_number"><spring:message code="publishCar.form.pickupStreetNumber"/></label>
                                    <form:input path="startPointNumber" id="publish_start_point_number" maxlength="10" inputmode="numeric" autocomplete="off"
                                                required="required" data-ryden-digits-only="true" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                                    <form:errors path="startPointNumber" cssClass="text-danger d-block"/>
                                </div>
                            </div>
                            <p class="small text-muted mb-3"><spring:message code="publishCar.form.samePickupReturnHint"/></p>

                            <div class="mb-3">
                                <label class="form-label"><spring:message code="publishCar.form.description"/></label>
                                <form:input path="description" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                                <form:errors path="description" cssClass="text-danger d-block"/>
                            </div>

                            <div class="d-flex justify-content-between mt-4">
                                <button type="button" class="btn btn-outline-secondary" data-wizard-prev>
                                    <c:out value="${wNavPrev}"/>
                                </button>
                                <button type="button" class="btn btn-primary" data-wizard-next>
                                    <c:out value="${wNavNext}"/>
                                </button>
                            </div>
                        </div>

                        <%-- ── STEP 3: Schedule & availability ──────────────────────── --%>
                        <div class="publish-wizard-step" data-step="3">
                            <div class="row g-3 mb-3">
                                <div class="col-md-6">
                                    <label class="form-label required-label" for="checkInTime"><spring:message code="publishCar.form.checkInTime"/></label>
                                    <form:input path="checkInTime" type="time" required="required" cssClass="form-control" cssErrorClass="form-control is-invalid" id="checkInTime" step="60"/>
                                    <form:errors path="checkInTime" cssClass="text-danger d-block"/>
                                </div>
                                <div class="col-md-6">
                                    <label class="form-label required-label" for="checkOutTime"><spring:message code="publishCar.form.checkOutTime"/></label>
                                    <form:input path="checkOutTime" type="time" required="required" cssClass="form-control" cssErrorClass="form-control is-invalid" id="checkOutTime" step="60"/>
                                    <form:errors path="checkOutTime" cssClass="text-danger d-block"/>
                                </div>
                            </div>

                            <spring:message code="publishCar.form.period" var="periodLabel"/>
                            <spring:message code="publishCar.form.remove" var="removeLabel"/>
                            <spring:message code="publishCar.form.dateRange.placeholder" var="dateRangePlaceholder"/>
                            <spring:message code="listing.availability.beyondPublishHorizon" arguments="${maxAvailabilityForwardWallDays}" var="beyondHorizonClientErrMsg" htmlEscape="true"/>
                            <spring:message code="listing.availability.required" var="availRequiredClientMsg" htmlEscape="true"/>
                            <div class="mb-4" id="publishAvailabilitySection"
                                 data-publish-avail-required="<c:out value='${availRequiredClientMsg}'/>"
                                 data-publish-min-avail-ymd="<c:out value='${publishMinAvailabilityFrom}'/>"
                                 data-publish-max-avail-wall-ymd="<c:out value='${publishMaxAvailabilityWallInclusive}'/>"
                                 data-publish-availability-beyond-msg="<c:out value='${beyondHorizonClientErrMsg}'/>">
                                <label class="form-label required-label"><spring:message code="publishCar.form.availability"/></label>
                                <p class="small text-muted mb-2"><spring:message code="publishCar.form.availability.hint" arguments="${pickupLeadHours}"/></p>
                                <p class="small text-muted mb-2"><spring:message code="publishCar.form.availability.forwardHorizonHint" arguments="${maxAvailabilityForwardWallDays}"/></p>
                                <form:errors path="availabilityRows" cssClass="text-danger d-block mb-2"/>
                                <div id="publish_availability_rows">
                                    <c:forEach items="${publishCarForm.availabilityRows}" var="row" varStatus="st">
                                        <div class="publish-avail-row border rounded-3 p-3 mb-2" data-publish-avail-row>
                                            <div class="d-flex justify-content-between align-items-center mb-2 gap-2">
                                                <span class="small text-secondary"><c:out value="${periodLabel}"/> <span class="publish-avail-index"><c:out value="${st.index + 1}"/></span></span>
                                                <button type="button" class="btn btn-sm btn-outline-danger publish-avail-remove" aria-label="<c:out value='${removeLabel}'/>"><c:out value="${removeLabel}"/></button>
                                            </div>
                                            <input type="text" class="form-control form-control-sm ryden-avail-range-input" readonly placeholder="<c:out value='${dateRangePlaceholder}'/>" aria-label="Availability date range"/>
                                            <form:hidden path="availabilityRows[${st.index}].from" cssClass="ryden-avail-from"/>
                                            <form:hidden path="availabilityRows[${st.index}].until" cssClass="ryden-avail-until"/>
                                            <form:errors path="availabilityRows[${st.index}].from" cssClass="text-danger d-block"/>
                                            <form:errors path="availabilityRows[${st.index}].until" cssClass="text-danger d-block"/>
                                        </div>
                                    </c:forEach>
                                </div>
                                <div id="publishClientAvailError" class="text-danger small d-none mb-1" role="alert"></div>
                                <button type="button" class="btn btn-outline-secondary btn-sm mt-1" id="publish_avail_add">
                                    <i class="bi bi-plus-lg" aria-hidden="true"></i> <spring:message code="publishCar.form.addPeriod"/>
                                </button>
                            </div>

                            <template id="publish_avail_row_template">
                                <div class="publish-avail-row border rounded-3 p-3 mb-2" data-publish-avail-row>
                                    <div class="d-flex justify-content-between align-items-center mb-2 gap-2">
                                        <span class="small text-secondary"><c:out value="${periodLabel}"/> <span class="publish-avail-index">1</span></span>
                                        <button type="button" class="btn btn-sm btn-outline-danger publish-avail-remove" aria-label="<c:out value='${removeLabel}'/>"><c:out value="${removeLabel}"/></button>
                                    </div>
                                    <input type="text" class="form-control form-control-sm ryden-avail-range-input" readonly placeholder="<c:out value='${dateRangePlaceholder}'/>" aria-label="Availability date range"/>
                                    <input type="hidden" class="ryden-avail-from" name="availabilityRows[__IDX__].from" value=""/>
                                    <input type="hidden" class="ryden-avail-until" name="availabilityRows[__IDX__].until" value=""/>
                                </div>
                            </template>

                            <div class="d-flex justify-content-between mt-4">
                                <button type="button" class="btn btn-outline-secondary" data-wizard-prev>
                                    <c:out value="${wNavPrev}"/>
                                </button>
                                <button type="button" class="btn btn-primary" data-wizard-next>
                                    <c:out value="${wNavNext}"/>
                                </button>
                            </div>
                        </div>

                        <%-- ── STEP 4: Photos & submit ──────────────────────────────── --%>
                        <div class="publish-wizard-step" data-step="4">
                            <spring:message code="validation.image.fileTooLarge" arguments="${uploadMaxImageMegabytes}" var="publishImageTooLargeMsg" htmlEscape="true"/>
                            <spring:message code="validation.pictures.mustBeImage" var="publishMustBeImageMsg" htmlEscape="true"/>
                            <spring:message code="publishCar.form.removeImage" var="removeImageLabel"/>
                            <div class="mb-3">
                                <span class="form-label required-label d-block"><spring:message code="publishCar.form.pictures"/></span>
                                <div class="d-flex flex-wrap align-items-center gap-2 mt-1">
                                    <input id="picturesInput" type="file" name="pictures" class="visually-hidden" accept="image/*" multiple aria-label="Vehicle photos"
                                           data-publish-pictures-required="${publishPicturesClientRequired}"
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

                            <div class="d-flex justify-content-between align-items-center mt-4">
                                <button type="button" class="btn btn-outline-secondary" data-wizard-prev>
                                    <c:out value="${wNavPrev}"/>
                                </button>
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
                        </div>

                    </form:form>

                    <c:if test="${not userHasCbu}">
                        <spring:message code="publishCar.missingCbu.modalTitle" var="pubMissingCbuTitle"/>
                        <spring:message code="publishCar.missingCbu.fieldLabel" var="pubMissingCbuFieldLabel" arguments="${cbuRequiredDigits}"/>
                        <spring:message code="publishCar.missingCbu.save" var="pubMissingCbuSave"/>
                        <spring:message code="publishCar.missingCbu.cancel" var="pubMissingCbuCancel"/>
                        <ryden:dataPromptModal
                                id="publishMissingCbuModal"
                                title="${pubMissingCbuTitle}"
                                fieldId="publishMissingCbuInput"
                                fieldLabel="${pubMissingCbuFieldLabel}"
                                errorId="publishMissingCbuError"
                                confirmId="publishMissingCbuSaveBtn"
                                openButtonId="rydenPublishMissingCbuModalOpen"
                                includeOpenTrigger="true"
                                inputType="text"
                                maxlength="${cbuRequiredDigits}"
                                inputmode="numeric"
                                inputPattern="[0-9]*"
                                digitsOnly="true"
                                cancelLabel="${pubMissingCbuCancel}"
                                confirmLabel="${pubMissingCbuSave}">
                            <p class="mb-3 text-secondary"><spring:message code="publishCar.missingCbu.modalBody" arguments="${cbuRequiredDigits}"/></p>
                        </ryden:dataPromptModal>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
</main>
<script>window.rydenPublishAvailMinFromUrl = '<c:url value="/publish-car/availability-min-from"/>';</script>
<%@include file="footer.jsp" %>
<script src="${pageContext.request.contextPath}/js/publish-wizard.js"></script>
</body>
</html>
