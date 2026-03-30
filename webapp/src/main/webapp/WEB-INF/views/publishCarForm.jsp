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
                                <form:options items="${carTypes}" />
                            </form:select>
                            <form:errors path="type" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label">Powertrain</label>
                            <form:select cssClass="form-select" path="powertrain">
                                <form:option value="" label="Select the powertrain"/>
                                <form:options items="${powertrains}" />
                            </form:select>
                            <form:errors path="powertrain" cssClass="text-danger d-block"/>
                        </div>

                        <div class="mb-3">
                            <label class="form-label required-label">Transmission</label>
                            <form:select cssClass="form-select" path="transmission">
                                <form:option value="" label="Select the transmission"/>
                                <form:options items="${transmissions}" />
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

                        <div class="mb-4">
                            <label class="form-label required-label">Availability (up to 5 periods)</label>
                            <p class="small text-muted mb-2">From / until are inclusive (minute precision). Times default to 00:00 when you pick a date. Zone: Argentina.</p>
                            <c:forEach items="${publishCarForm.availabilityRows}" var="row" varStatus="st">
                                <div class="row g-2 mb-3 align-items-end">
                                    <div class="col-md-6">
                                        <label class="form-label small mb-1" for="avail_from_d_${st.index}">From</label>
                                        <div class="input-group input-group-sm shadow-none"
                                             data-paw-dtpair-wrap
                                             data-paw-hidden="avail_from_h_${st.index}"
                                             data-paw-date="avail_from_d_${st.index}"
                                             data-paw-time="avail_from_t_${st.index}">
                                            <form:hidden path="availabilityRows[${st.index}].from" id="avail_from_h_${st.index}"/>
                                            <span class="input-group-text border-0 bg-light"><i class="bi bi-calendar3" aria-hidden="true"></i></span>
                                            <input type="date" class="form-control border-0 shadow-none" id="avail_from_d_${st.index}" aria-label="From date"/>
                                            <span class="input-group-text border-0 bg-light"><i class="bi bi-clock" aria-hidden="true"></i></span>
                                            <input type="time" class="form-control border-0 shadow-none" id="avail_from_t_${st.index}" step="60" aria-label="From time"/>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <label class="form-label small mb-1" for="avail_until_d_${st.index}">Until</label>
                                        <div class="input-group input-group-sm shadow-none"
                                             data-paw-dtpair-wrap
                                             data-paw-hidden="avail_until_h_${st.index}"
                                             data-paw-date="avail_until_d_${st.index}"
                                             data-paw-time="avail_until_t_${st.index}">
                                            <form:hidden path="availabilityRows[${st.index}].until" id="avail_until_h_${st.index}"/>
                                            <span class="input-group-text border-0 bg-light"><i class="bi bi-calendar3" aria-hidden="true"></i></span>
                                            <input type="date" class="form-control border-0 shadow-none" id="avail_until_d_${st.index}" aria-label="Until date"/>
                                            <span class="input-group-text border-0 bg-light"><i class="bi bi-clock" aria-hidden="true"></i></span>
                                            <input type="time" class="form-control border-0 shadow-none" id="avail_until_t_${st.index}" step="60" aria-label="Until time"/>
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>

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