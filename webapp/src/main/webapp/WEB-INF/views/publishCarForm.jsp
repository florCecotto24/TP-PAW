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

                        <div class="mb-3">
                            <label class="form-label required-label">Pictures</label>
                            <input id="picturesInput" type="file" name="pictures" class="form-control" accept="image/*" multiple required/>
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