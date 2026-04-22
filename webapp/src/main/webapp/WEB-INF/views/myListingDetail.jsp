<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="myListingDetail.pageTitle" arguments="${listing.id}"/></title>
    <%@include file="header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="navbar.myListings" var="myListingsLabel"/>
    <c:url var="editListingUrl" value="/my-listings/${listing.id}/edit"/>
    <c:url var="toggleListingUrl" value="/my-listings/${listing.id}/toggle"/>
    <ryden:breadcrumbTrail
            homeLabel="${myListingsLabel}"
            homeHref="${pageContext.request.contextPath}/my-listings"
            currentLabel="${listing.id}"/>

    <section class="reservation-management-header mb-4">
        <h1 class="h3 fw-bold mb-2"><spring:message code="myListingDetail.heading"/></h1>
        <p class="text-secondary mb-0"><spring:message code="myListingDetail.subheading"/></p>
    </section>

    <div class="row g-4 align-items-start">
        <div class="col-lg-8">
            <article class="card border-0 shadow-sm rounded-4 mb-4">
                <div class="card-body p-4">
                    <h2 class="h5 fw-semibold mb-3"><spring:message code="myListingDetail.carSummary.title"/></h2>
                    <div class="d-flex flex-column flex-md-row gap-3 align-items-start">
                        <div class="reservation-detail-car-media rounded-3 overflow-hidden border">
                            <c:choose>
                                <c:when test="${carImageId > 0}">
                                    <c:url var="carImageUrl" value="/image/${carImageId}"/>
                                    <img src="<c:out value='${carImageUrl}'/>" alt="<c:out value='${car.brand} ${car.model}'/>" class="w-100 h-100" style="object-fit:cover;">
                                </c:when>
                                <c:otherwise>
                                    <div class="w-100 h-100 d-flex align-items-center justify-content-center text-secondary bg-body-tertiary">
                                        <i class="bi bi-car-front fs-1" aria-hidden="true"></i>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div>
                            <h3 class="h5 mb-1"><c:out value="${car.brand} ${car.model}"/></h3>
                            <p class="text-secondary mb-2"><c:out value="${listing.title}"/></p>
                            <div class="d-flex flex-wrap gap-2 mb-2">
                                <spring:message code="enum.car.transmission.${car.transmission.name()}" var="carTransmissionLabel"/>
                                <spring:message code="enum.car.powertrain.${car.powertrain.name()}" var="carPowertrainLabel"/>
                                <spring:message code="enum.listing.status.${statusKey}" var="listingStatusLabel"/>
                                <span class="badge text-bg-light border"><c:out value="${carTransmissionLabel}"/></span>
                                <span class="badge text-bg-light border"><c:out value="${carPowertrainLabel}"/></span>
                                <span class="badge text-bg-primary"><c:out value="${listingStatusLabel}"/></span>
                            </div>
                            <p class="mb-0 text-secondary small">
                                <spring:message code="myListingDetail.details.createdAt"/>: <c:out value="${listingCreatedAtDisplay}"/>
                            </p>
                        </div>
                    </div>
                </div>
            </article>

            <article class="card border-0 shadow-sm rounded-4 mb-4">
                <div class="card-body p-4">
                    <h2 class="h5 fw-semibold mb-3"><spring:message code="myListingDetail.edit.title"/></h2>
                    <form:form method="post"
                               action="${editListingUrl}"
                               modelAttribute="editForm"
                               class="row g-3">
                        <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>

                        <div class="col-sm-6">
                            <label for="pricePerDay" class="form-label required-label"><spring:message code="publishCar.form.pricePerDay"/></label>
                            <form:input path="pricePerDay" id="pricePerDay" type="number" step="0.01" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="pricePerDay" cssClass="text-danger d-block"/>
                        </div>
                        <div class="col-sm-6">
                            <label for="startPoint" class="form-label required-label"><spring:message code="publishCar.form.startPoint"/></label>
                            <form:input path="startPoint" id="startPoint" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="startPoint" cssClass="text-danger d-block"/>
                        </div>
                        <div class="col-sm-6">
                            <label for="checkInTime" class="form-label required-label"><spring:message code="publishCar.form.checkInTime"/></label>
                            <form:input path="checkInTime" id="checkInTime" type="time" step="60" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="checkInTime" cssClass="text-danger d-block"/>
                        </div>
                        <div class="col-sm-6">
                            <label for="checkOutTime" class="form-label required-label"><spring:message code="publishCar.form.checkOutTime"/></label>
                            <form:input path="checkOutTime" id="checkOutTime" type="time" step="60" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="checkOutTime" cssClass="text-danger d-block"/>
                        </div>
                        <div class="col-12">
                            <label for="description" class="form-label"><spring:message code="publishCar.form.description"/></label>
                            <form:textarea path="description" id="description" rows="3" cssClass="form-control" cssErrorClass="form-control is-invalid"/>
                            <form:errors path="description" cssClass="text-danger d-block"/>
                        </div>

                        <spring:message code="publishCar.form.remove" var="removeLabel"/>
                        <spring:message code="publishCar.form.dateRange.placeholder" var="dateRangePlaceholder"/>
                        <spring:message code="publishCar.form.period" var="periodLabel"/>
                        <div class="col-12" id="publishAvailabilitySection">
                            <label class="form-label required-label"><spring:message code="publishCar.form.availability"/></label>
                            <p class="small text-muted mb-2"><spring:message code="publishCar.form.availability.hint"/></p>
                            <form:errors path="availabilityRows" cssClass="text-danger d-block mb-2"/>
                            <div id="publish_availability_rows">
                                <c:forEach items="${editForm.availabilityRows}" var="row" varStatus="st">
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
                                <input type="text" class="form-control form-control-sm ryden-avail-range-input" readonly placeholder="<c:out value='${dateRangePlaceholder}'/>" aria-label="Availability date range"/>
                                <input type="hidden" class="ryden-avail-from" name="availabilityRows[__IDX__].from" value=""/>
                                <input type="hidden" class="ryden-avail-until" name="availabilityRows[__IDX__].until" value=""/>
                            </div>
                        </template>

                        <div class="col-12">
                            <button type="submit" class="btn btn-primary">
                                <spring:message code="myListingDetail.actions.modify"/>
                            </button>
                        </div>
                    </form:form>
                </div>
            </article>

            <article class="card border-0 shadow-sm rounded-4 mb-4">
                <div class="card-body p-4">
                    <h2 class="h5 fw-semibold mb-3"><spring:message code="myListingDetail.availability.title"/></h2>
                    <c:choose>
                        <c:when test="${empty availabilities}">
                            <p class="text-secondary mb-0"><spring:message code="myListingDetail.availability.empty"/></p>
                        </c:when>
                        <c:otherwise>
                            <div class="d-flex flex-column gap-2">
                                <c:forEach var="availability" items="${availabilities}">
                                    <div class="p-2 border rounded-3 bg-body-tertiary small">
                                        <c:out value="${availability.startInclusive}"/> - <c:out value="${availability.endInclusive}"/>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </article>
        </div>

        <div class="col-lg-4">
            <article class="card border-0 shadow-sm rounded-4 reservation-detail-sticky">
                <div class="card-body p-4">
                    <div class="reservation-price-compact mb-3">
                        <span class="reservation-card__meta-label mb-0"><spring:message code="myListingDetail.pricePerDay"/></span>
                        <span class="h2 fw-bold text-primary mb-0">$<c:out value="${listing.dayPrice}"/></span>
                    </div>

                    <div class="d-grid gap-2">
                        <form method="post" action="<c:out value='${toggleListingUrl}'/>">
                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                            <c:choose>
                                <c:when test="${statusKey == 'ACTIVE'}">
                                    <button type="submit" class="btn w-100" style="background-color:#e4960b; color:#ffffff; border-color:#e4960b;" aria-label="<spring:message code='myListingDetail.actions.pause'/>">
                                        <spring:message code="myListingDetail.actions.pause"/>
                                    </button>
                                </c:when>
                                <c:when test="${statusKey == 'PAUSED'}">
                                    <button type="submit" class="btn btn-success w-100" aria-label="<spring:message code='myListingDetail.actions.activate'/>">
                                        <spring:message code="myListingDetail.actions.activate"/>
                                    </button>
                                </c:when>
                            </c:choose>
                        </form>
                    </div>

                    <hr class="my-4">
                    <c:url var="listingUrl" value="/car-detail">
                        <c:param name="listingId"><c:out value="${listing.id}"/></c:param>
                    </c:url>
                    <a href="<c:out value='${listingUrl}'/>" class="btn btn-light border w-100">
                        <spring:message code="myListingDetail.actions.viewListing"/>
                    </a>
                </div>
            </article>
        </div>
    </div>
</main>

<%@include file="footer.jsp"%>
</body>
</html>


