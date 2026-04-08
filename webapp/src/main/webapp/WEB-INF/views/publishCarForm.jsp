<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html>
<head>
    <%@include file="header.jsp" %>
</head>
<body class="has-fixed-navbar">
<paw:navbar/>

<main class="container py-5">
    <spring:message code="publishCar.form.title" var="publishFormTitle"/>
    <paw:breadcrumbTrail currentLabel="${publishFormTitle}"/>
    <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-body p-4 p-md-5">

                    <h4 class="mb-4"><spring:message code="publishCar.form.title"/></h4>
                    <form:form id="publishCarFormEl"
                               action="${pageContext.request.contextPath}/publish-car"
                               method="POST"
                               modelAttribute="publishCarForm"
                               enctype="multipart/form-data">

                        <form:errors element="div" cssClass="alert alert-danger"/>

                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.ownerName"/></label>
                            <form:input path="ownerName" cssClass="form-control" cssErrorClass="form-control is-invalid" type="text" autocomplete="name"/>
                            <form:errors path="ownerName" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.ownerSurname"/></label>
                            <form:input path="ownerSurname" cssClass="form-control" cssErrorClass="form-control is-invalid" type="text" autocomplete="surname"/>
                            <form:errors path="ownerSurname" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.email"/></label>
                            <form:input path="ownerEmail" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="ownerEmail" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.brand"/></label>
                            <form:input path="brand" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="brand" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.model"/></label>
                            <form:input path="model" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="model" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.plate"/></label>
                            <form:input path="plate" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="plate" cssClass="text-danger d-block"/>
                        </div>

                        <spring:message code="publishCar.form.type.placeholder" var="typePlaceholder"/>
                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.type"/></label>
                            <form:select cssClass="form-select" path="type">
                                <form:option value="" label="${typePlaceholder}" />
                                <form:options items="${carTypeOptions}"/>
                            </form:select>
                            <form:errors path="type" cssClass="text-danger d-block"/>
                        </div>

                        <spring:message code="publishCar.form.powertrain.placeholder" var="powertrainPlaceholder"/>
                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.powertrain"/></label>
                            <form:select cssClass="form-select" path="powertrain">
                                <form:option value="" label="${powertrainPlaceholder}"/>
                                <form:options items="${powertrainOptions}"/>
                            </form:select>
                            <form:errors path="powertrain" cssClass="text-danger d-block"/>
                        </div>

                        <spring:message code="publishCar.form.transmission.placeholder" var="transmissionPlaceholder"/>
                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.transmission"/></label>
                            <form:select cssClass="form-select" path="transmission">
                                <form:option value="" label="${transmissionPlaceholder}"/>
                                <form:options items="${transmissionOptions}"/>
                            </form:select>
                            <form:errors path="transmission" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.pricePerDay"/></label>
                            <form:input path="pricePerDay" cssClass="form-control" cssErrorClass="form-control is-invalid" type="number" step="0.01"/>
                            <form:errors path="pricePerDay" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label"><spring:message code="publishCar.form.startPoint"/></label>
                            <form:input path="startPoint" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="startPoint" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label"><spring:message code="publishCar.form.description"/></label>
                            <form:input path="description" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="description" cssClass="text-danger d-block"/>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label class="form-label required-label" for="checkInTime"><spring:message code="publishCar.form.checkInTime"/></label>
                                <form:input path="checkInTime" type="time" cssClass="form-control" cssErrorClass="form-control is-invalid" id="checkInTime" step="60"/>
                                <form:errors path="checkInTime" cssClass="text-danger d-block"/>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label required-label" for="checkOutTime"><spring:message code="publishCar.form.checkOutTime"/></label>
                                <form:input path="checkOutTime" type="time" cssClass="form-control" cssErrorClass="form-control is-invalid" id="checkOutTime" step="60"/>
                                <form:errors path="checkOutTime" cssClass="text-danger d-block"/>
                            </div>
                        </div>

                        <spring:message code="publishCar.form.period" var="periodLabel"/>
                        <spring:message code="publishCar.form.remove" var="removeLabel"/>
                        <spring:message code="publishCar.form.dateRange.placeholder" var="dateRangePlaceholder"/>
                        <div class="mb-4" id="publishAvailabilitySection">
                            <label class="form-label required-label"><spring:message code="publishCar.form.availability"/></label>
                            <p class="small text-muted mb-2"><spring:message code="publishCar.form.availability.hint"/></p>
                            <form:errors path="availabilityRows" cssClass="text-danger d-block mb-2"/>
                            <div id="publish_availability_rows">
                                <c:forEach items="${publishCarForm.availabilityRows}" var="row" varStatus="st">
                                    <div class="publish-avail-row border rounded-3 p-3 mb-2" data-publish-avail-row>
                                        <div class="d-flex justify-content-between align-items-center mb-2 gap-2">
                                            <span class="small text-secondary"><c:out value="${periodLabel}"/> <span class="publish-avail-index"><c:out value="${st.index + 1}"/></span></span>
                                            <button type="button" class="btn btn-sm btn-outline-danger publish-avail-remove ${st.first ? "d-none" : "" }" aria-label="<c:out value='${removeLabel}'/>"><c:out value="${removeLabel}"/></button>
                                        </div>
                                        <input type="text" class="form-control form-control-sm paw-avail-range-input" readonly placeholder="<c:out value='${dateRangePlaceholder}'/>" aria-label="Availability date range"/>
                                        <form:hidden path="availabilityRows[${st.index}].from" cssClass="paw-avail-from"/>
                                        <form:hidden path="availabilityRows[${st.index}].until" cssClass="paw-avail-until"/>
                                        <form:errors path="availabilityRows[${st.index}].from" cssClass="text-danger d-block"/>
                                        <form:errors path="availabilityRows[${st.index}].until" cssClass="text-danger d-block"/>
                                    </div>
                                </c:forEach>
                            </div>
                            <button type="button" class="btn btn-outline-secondary btn-sm mt-1" id="publish_avail_add">
                                <i class="bi bi-plus-lg" aria-hidden="true"></i> <spring:message code="publishCar.form.addPeriod"/>
                            </button>
                        </div>

                        <template id="publish_avail_row_template">
                            <div class="publish-avail-row border rounded-3 p-3 mb-2" data-publish-avail-row>
                                <div class="d-flex justify-content-between align-items-center mb-2 gap-2">
                                    <span class="small text-secondary"><c:out value="${periodLabel}"/> <span class="publish-avail-index">1</span></span>
                                    <button type="button" class="btn btn-outline-danger btn-action btn-action-sm publish-avail-remove" aria-label="<c:out value='${removeLabel}'/>"><c:out value="${removeLabel}"/></button>
                                </div>
                                <input type="text" class="form-control form-control-sm paw-avail-range-input" readonly placeholder="<c:out value='${dateRangePlaceholder}'/>" aria-label="Availability date range"/>
                                <input type="hidden" class="paw-avail-from" name="availabilityRows[__IDX__].from" value=""/>
                                <input type="hidden" class="paw-avail-until" name="availabilityRows[__IDX__].until" value=""/>
                            </div>
                        </template>

                        <div class="mb-3">
                            <span class="form-label required-label d-block"><spring:message code="publishCar.form.pictures"/></span>
                            <div class="d-flex flex-wrap align-items-center gap-2 mt-1">
                                <input id="picturesInput" type="file" name="pictures" class="visually-hidden" accept="image/*" multiple required aria-label="Vehicle photos"/>
                                <label for="picturesInput" class="btn btn-outline-secondary mb-0"><spring:message code="publishCar.form.chooseFiles"/></label>
                            </div>
                            <small class="text-muted d-block mt-2"><spring:message code="publishCar.form.pictures.hint"/></small>
                            <form:errors path="pictures" cssClass="text-danger d-block"/>
                            <div id="picturesPreview" class="row g-2 mt-2"></div>
                        </div>

                        <button type="submit" class="btn btn-primary" id="publishCarSubmitBtn">
                            <span class="publish-submit-default">
                                <i class="bi bi-check-lg"></i> <spring:message code="publishCar.form.submit"/>
                            </span>
                            <span class="publish-submit-loading d-none" aria-live="polite">
                                <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                                <spring:message code="publishCar.form.submit"/>
                            </span>
                        </button>

                    </form:form>
                </div>
            </div>
        </div>
    </div>
</main>
<%@include file="footer.jsp" %>
</body>
</html>