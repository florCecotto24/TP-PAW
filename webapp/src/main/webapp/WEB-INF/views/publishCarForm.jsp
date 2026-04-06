<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<head>
    <%@include file="header.jsp" %>
</head>
<body class="has-fixed-navbar">
<paw:navbar/>

<main class="container py-5">
    <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-body p-4 p-md-5">

                    <h4 class="mb-4">Publish my car</h4>
                    <form:form action="${pageContext.request.contextPath}/publish-car"
                               method="POST"
                               modelAttribute="publishCarForm"
                               enctype="multipart/form-data">

                        <form:errors element="div" cssClass="alert alert-danger"/>

                        <div class="mb-3">
                            <label class="form-label required-label">Your name</label>
                            <form:input path="ownerName" cssClass="form-control" type="text" autocomplete="name"/>
                            <form:errors path="ownerName" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label">Your surname</label>
                            <form:input path="ownerSurname" cssClass="form-control" type="text" autocomplete="surname"/>
                            <form:errors path="ownerSurname" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label">Email</label>
                            <form:input path="ownerEmail" cssClass="form-control" type="email" autocomplete="email"/>
                            <form:errors path="ownerEmail" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label">Brand</label>
                            <form:input path="brand" cssClass="form-control"/>
                            <form:errors path="brand" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label">Model</label>
                            <form:input path="model" cssClass="form-control"/>
                            <form:errors path="model" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label">Plate</label>
                            <form:input path="plate" cssClass="form-control"/>
                            <form:errors path="plate" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label">Type</label>
                            <form:select cssClass="form-select" path="type">
                                <form:option value="" label="Select a type" />
                                <form:options items="${carTypeOptions}"/>
                            </form:select>
                            <form:errors path="type" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label">Powertrain</label>
                            <form:select cssClass="form-select" path="powertrain">
                                <form:option value="" label="Select the powertrain"/>
                                <form:options items="${powertrainOptions}"/>
                            </form:select>
                            <form:errors path="powertrain" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label">Transmission</label>
                            <form:select cssClass="form-select" path="transmission">
                                <form:option value="" label="Select the transmission"/>
                                <form:options items="${transmissionOptions}"/>
                            </form:select>
                            <form:errors path="transmission" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label">Price per day</label>
                            <form:input path="pricePerDay" cssClass="form-control"/>
                            <form:errors path="pricePerDay" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label">Start Point</label>
                            <form:input path="startPoint" cssClass="form-control"/>
                            <form:errors path="startPoint" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">Description</label>
                            <form:input path="description" cssClass="form-control"/>
                            <form:errors path="description" cssClass="text-danger d-block"/>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label class="form-label required-label" for="checkInTime">Check-in time</label>
                                <form:input path="checkInTime" type="time" cssClass="form-control" id="checkInTime" step="60"/>
                                <form:errors path="checkInTime" cssClass="text-danger d-block"/>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label required-label" for="checkOutTime">Check-out time</label>
                                <form:input path="checkOutTime" type="time" cssClass="form-control" id="checkOutTime" step="60"/>
                                <form:errors path="checkOutTime" cssClass="text-danger d-block"/>
                            </div>
                        </div>

                        <div class="mb-4" id="publishAvailabilitySection">
                            <label class="form-label required-label">Availability</label>
                            <p class="small text-muted mb-2">Inclusive calendar days (Argentina). Pick-up and return times use check-in / check-out above. Select one range per row; add more periods as needed (max 10).</p>
                            <form:errors path="availabilityRows" cssClass="text-danger d-block mb-2"/>
                            <div id="publish_availability_rows">
                                <c:forEach items="${publishCarForm.availabilityRows}" var="row" varStatus="st">
                                    <div class="publish-avail-row border rounded-3 p-3 mb-2" data-publish-avail-row>
                                        <div class="d-flex justify-content-between align-items-center mb-2 gap-2">
                                            <span class="small text-secondary">Period <span class="publish-avail-index"><c:out value="${st.index + 1}"/></span></span>
                                            <button type="button" class="btn btn-sm btn-outline-danger publish-avail-remove" aria-label="Remove period">Remove</button>
                                        </div>
                                        <input type="text" class="form-control form-control-sm paw-avail-range-input" readonly placeholder="Select date range on calendar" aria-label="Availability date range"/>
                                        <form:hidden path="availabilityRows[${st.index}].from" cssClass="paw-avail-from"/>
                                        <form:hidden path="availabilityRows[${st.index}].until" cssClass="paw-avail-until"/>
                                    </div>
                                </c:forEach>
                            </div>
                            <button type="button" class="btn btn-outline-secondary btn-sm mt-1" id="publish_avail_add">
                                <i class="bi bi-plus-lg" aria-hidden="true"></i> Add period
                            </button>
                        </div>

                        <template id="publish_avail_row_template">
                            <div class="publish-avail-row border rounded-3 p-3 mb-2" data-publish-avail-row>
                                <div class="d-flex justify-content-between align-items-center mb-2 gap-2">
                                    <span class="small text-secondary">Period <span class="publish-avail-index">1</span></span>
                                    <button type="button" class="btn btn-sm btn-outline-danger publish-avail-remove" aria-label="Remove period">Remove</button>
                                </div>
                                <input type="text" class="form-control form-control-sm paw-avail-range-input" readonly placeholder="Select date range on calendar" aria-label="Availability date range"/>
                                <input type="hidden" class="paw-avail-from" name="availabilityRows[__IDX__].from" value=""/>
                                <input type="hidden" class="paw-avail-until" name="availabilityRows[__IDX__].until" value=""/>
                            </div>
                        </template>

                        <div class="mb-3">
                            <span class="form-label required-label d-block">Pictures</span>
                            <div class="d-flex flex-wrap align-items-center gap-2 mt-1">
                                <input id="picturesInput" type="file" name="pictures" class="visually-hidden" accept="image/*" multiple required aria-label="Vehicle photos"/>
                                <label for="picturesInput" class="btn btn-outline-secondary mb-0">Choose files</label>
                            </div>
                            <small class="text-muted d-block mt-2">Upload 1-8 images (max 5MB total). Accepted: JPG, PNG, WebP</small>
                            <form:errors path="pictures" cssClass="text-danger d-block"/>
                            <div id="picturesPreview" class="row g-2 mt-2"></div>
                        </div>

                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-check-lg"></i> Submit
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