<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="myReservations.pageTitle"/></title>
    <%@include file="header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="myReservations.heading" var="myReservationsLabel"/>
    <ryden:breadcrumbTrail currentLabel="${myReservationsLabel}"/>
    <section class="reservation-management-header mt-4 pt-5 mb-4">
        <h1 class="h3 fw-bold mb-2"><spring:message code="myReservations.heading"/></h1>
        <p class="text-secondary mb-0"><spring:message code="myReservations.subheading"/></p>
    </section>

    <c:url var="reserveCarUrl" value="/search"/>
    <c:url var="myReservationsRiderPaginationBaseUrl" value="/my-reservations">
        <c:param name="tab" value="rider"/>
        <c:if test="${not empty riderStatusFilter}">
            <c:param name="riderStatus" value="${riderStatusFilter}"/>
        </c:if>
        <c:if test="${not empty ownerStatusFilter}">
            <c:param name="ownerStatus" value="${ownerStatusFilter}"/>
        </c:if>
    </c:url>
    <c:url var="myReservationsOwnerPaginationBaseUrl" value="/my-reservations">
        <c:param name="tab" value="owner"/>
        <c:if test="${not empty riderStatusFilter}">
            <c:param name="riderStatus" value="${riderStatusFilter}"/>
        </c:if>
        <c:if test="${not empty ownerStatusFilter}">
            <c:param name="ownerStatus" value="${ownerStatusFilter}"/>
        </c:if>
    </c:url>

    <spring:message code="validation.dropdown.invalid" var="myReservationsDropdownInvalid" htmlEscape="true"/>

    <ul class="nav nav-tabs mb-4" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link ${selectedReservationsTab eq 'rider' ? 'active' : ''}" id="rider-tab" data-bs-toggle="tab" data-bs-target="#rider-pane" type="button" role="tab" aria-controls="rider-pane" aria-selected="${selectedReservationsTab eq 'rider' ? 'true' : 'false'}">
                <spring:message code="myReservations.section.myReservations"/>
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link ${selectedReservationsTab eq 'owner' ? 'active' : ''}" id="owner-tab" data-bs-toggle="tab" data-bs-target="#owner-pane" type="button" role="tab" aria-controls="owner-pane" aria-selected="${selectedReservationsTab eq 'owner' ? 'true' : 'false'}">
                <spring:message code="myReservations.section.ownerReservations"/>
            </button>
        </li>
    </ul>

    <div class="tab-content">
        <div class="tab-pane fade ${selectedReservationsTab eq 'rider' ? 'show active' : ''}" id="rider-pane" role="tabpanel" aria-labelledby="rider-tab">
            <form id="myReservationsRiderFilterForm" class="row g-2 align-items-end mb-3" method="get" action="${pageContext.request.contextPath}/my-reservations"
                  data-ryden-dropdown-invalid="<c:out value='${myReservationsDropdownInvalid}'/>">
                <input type="hidden" name="tab" value="rider"/>
                <c:if test="${not empty ownerStatusFilter}">
                    <input type="hidden" name="ownerStatus" value="<c:out value='${ownerStatusFilter}'/>"/>
                </c:if>
                <div class="col-md-5 col-lg-4">
                    <label class="form-label small text-secondary mb-1" for="rider_res_status"><spring:message code="myReservations.filter.status"/></label>
                    <select class="form-select" id="rider_res_status" name="riderStatus">
                        <option value="" ${empty riderStatusFilter ? 'selected="selected"' : ''}><spring:message code="myReservations.filter.status.any"/></option>
                        <option value="pending" ${riderStatusFilter eq 'pending' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.pending"/></option>
                        <option value="accepted" ${riderStatusFilter eq 'accepted' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.accepted"/></option>
                        <option value="started" ${riderStatusFilter eq 'started' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.started"/></option>
                        <option value="cancelled" ${riderStatusFilter eq 'cancelled' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.cancelled"/></option>
                        <option value="finished" ${riderStatusFilter eq 'finished' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.finished"/></option>
                    </select>
                </div>
                <div class="col-auto d-flex flex-wrap gap-2">
                    <button type="submit" class="btn btn-primary"><spring:message code="myListings.filter.search"/></button>
                    <a href="${pageContext.request.contextPath}/my-reservations?tab=rider" class="btn btn-outline-secondary"><spring:message code="search.filters.clear"/></a>
                </div>
            </form>
            <div class="mb-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
                <h3 class="h6 mb-0">
                    <c:choose>
                        <c:when test="${riderReservationsPage.totalItems > 0}">
                            <spring:message code="myReservations.resultsRange"
                                            arguments="${riderReservationsPage.firstItemNumber},${riderReservationsPage.lastItemNumber},${riderReservationsPage.totalItems}"/>
                        </c:when>
                        <c:otherwise>
                            <spring:message code="myReservations.resultsCount" arguments="0"/>
                        </c:otherwise>
                    </c:choose>
                </h3>
            </div>

            <c:choose>
                <c:when test="${empty riderReservations}">
                    <div class="search-empty-state text-center">
                        <div class="search-empty-state__icon" aria-hidden="true">
                            <i class="bi bi-calendar2-check"></i>
                        </div>
                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myReservations.empty.title"/></h2>
                        <p class="text-secondary mb-0 search-empty-state__text">
                            <spring:message code="myReservations.empty.description"/>
                        </p>
                        <div class="search-empty-state__actions mt-4">
                            <a href="<c:out value='${reserveCarUrl}'/>" class="btn btn-primary btn-action btn-action-md">
                                <spring:message code="myReservations.empty.reserve"/>
                            </a>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="d-flex flex-column gap-3 mb-4">
                        <c:forEach var="reservation" items="${riderReservations}">
                            <c:url var="reservationDetailUrl" value="/my-reservations/${reservation.reservationId}"/>
                            <a href="<c:out value='${reservationDetailUrl}'/>" class="reservation-card text-decoration-none text-reset">
                                <article class="card border-0 shadow-sm rounded-4 overflow-hidden reservation-card__surface position-relative">
                                    <span class="position-absolute top-0 end-0 m-2 z-1">
                                        <span class="badge ${reservation.statusKey eq 'accepted' ? 'bg-success' : reservation.statusKey eq 'cancelled' ? 'bg-danger' : reservation.statusKey eq 'started' ? 'bg-info' : reservation.statusKey eq 'pending' ? 'bg-warning text-dark' : 'bg-secondary'}">
                                            <spring:message code="enum.reservation.status.${reservation.statusKey}"/>
                                        </span>
                                    </span>
                                    <div class="row g-0 align-items-stretch">
                                        <div class="col-12 col-md-3 reservation-card__media-wrap">
                                            <c:choose>
                                                <c:when test="${reservation.imageId > 0}">
                                                    <c:url var="reservationImgUrl" value="/image/${reservation.imageId}"/>
                                                    <img src="<c:out value='${reservationImgUrl}'/>" alt="<c:out value='${reservation.brand} ${reservation.model}'/>" class="reservation-card__media">
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="reservation-card__media reservation-card__media--placeholder d-flex align-items-center justify-content-center text-secondary">
                                                        <i class="bi bi-car-front fs-1" aria-hidden="true"></i>
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                        <div class="col-12 col-md-9">
                                            <div class="card-body p-3 p-md-4 h-100 d-flex flex-column justify-content-between gap-3">
                                                <div class="d-flex flex-wrap align-items-start gap-2">
                                                    <div>
                                                        <h3 class="h5 fw-semibold mb-1"><c:out value="${reservation.brand} ${reservation.model}"/></h3>
                                                    </div>
                                                </div>

                                                <div class="row g-3">
                                                    <div class="col-12 col-sm-6">
                                                        <p class="reservation-card__meta-label mb-1"><spring:message code="myReservations.card.pickup"/></p>
                                                        <p class="mb-0 fw-medium"><c:out value="${reservation.pickupDateTime}"/></p>
                                                    </div>
                                                    <div class="col-12 col-sm-6">
                                                        <p class="reservation-card__meta-label mb-1"><spring:message code="myReservations.card.return"/></p>
                                                        <p class="mb-0 fw-medium"><c:out value="${reservation.returnDateTime}"/></p>
                                                    </div>
                                                </div>

                                                <div class="pt-1">
                                                    <div class="reservation-price-compact">
                                                        <span class="reservation-card__meta-label mb-0"><spring:message code="myReservations.card.totalPrice"/></span>
                                                        <span class="h5 mb-0 fw-bold text-primary">$<c:out value="${reservation.totalPrice}"/></span>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </article>
                            </a>
                        </c:forEach>
                    </div>

                    <ryden:pagination
                            currentPage="${riderReservationsPage.currentPage}"
                            totalPages="${riderReservationsPage.totalPages}"
                            baseUrl="${myReservationsRiderPaginationBaseUrl}"
                            pageParam="riderPage"/>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="tab-pane fade ${selectedReservationsTab eq 'owner' ? 'show active' : ''}" id="owner-pane" role="tabpanel" aria-labelledby="owner-tab">
            <form id="myReservationsOwnerFilterForm" class="row g-2 align-items-end mb-3" method="get" action="${pageContext.request.contextPath}/my-reservations"
                  data-ryden-dropdown-invalid="<c:out value='${myReservationsDropdownInvalid}'/>">
                <input type="hidden" name="tab" value="owner"/>
                <c:if test="${not empty riderStatusFilter}">
                    <input type="hidden" name="riderStatus" value="<c:out value='${riderStatusFilter}'/>"/>
                </c:if>
                <div class="col-md-5 col-lg-4">
                    <label class="form-label small text-secondary mb-1" for="owner_res_status"><spring:message code="myReservations.filter.status"/></label>
                    <select class="form-select" id="owner_res_status" name="ownerStatus">
                        <option value="" ${empty ownerStatusFilter ? 'selected="selected"' : ''}><spring:message code="myReservations.filter.status.any"/></option>
                        <option value="pending" ${ownerStatusFilter eq 'pending' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.pending"/></option>
                        <option value="accepted" ${ownerStatusFilter eq 'accepted' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.accepted"/></option>
                        <option value="started" ${ownerStatusFilter eq 'started' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.started"/></option>
                        <option value="cancelled" ${ownerStatusFilter eq 'cancelled' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.cancelled"/></option>
                        <option value="finished" ${ownerStatusFilter eq 'finished' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.finished"/></option>
                    </select>
                </div>
                <div class="col-auto d-flex flex-wrap gap-2">
                    <button type="submit" class="btn btn-primary"><spring:message code="myListings.filter.search"/></button>
                    <a href="${pageContext.request.contextPath}/my-reservations?tab=owner" class="btn btn-outline-secondary"><spring:message code="search.filters.clear"/></a>
                </div>
            </form>
            <div class="mb-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
                <h3 class="h6 mb-0">
                    <c:choose>
                        <c:when test="${ownerReservationsPage.totalItems > 0}">
                            <spring:message code="myReservations.ownerResultsRange"
                                            arguments="${ownerReservationsPage.firstItemNumber},${ownerReservationsPage.lastItemNumber},${ownerReservationsPage.totalItems}"/>
                        </c:when>
                        <c:otherwise>
                            <spring:message code="myReservations.ownerResultsCount" arguments="0"/>
                        </c:otherwise>
                    </c:choose>
                </h3>
            </div>

            <c:choose>
                <c:when test="${empty ownerReservations}">
                    <div class="search-empty-state text-center">
                        <div class="search-empty-state__icon" aria-hidden="true">
                            <i class="bi bi-calendar-check"></i>
                        </div>
                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myReservations.ownerEmpty.title"/></h2>
                        <p class="text-secondary mb-0 search-empty-state__text">
                            <spring:message code="myReservations.ownerEmpty.description"/>
                        </p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="d-flex flex-column gap-3 mb-4">
                        <c:forEach var="reservation" items="${ownerReservations}">
                            <c:url var="reservationDetailUrl" value="/my-reservations/${reservation.reservationId}">
                                <c:param name="role" value="owner"/>
                            </c:url>
                            <a href="<c:out value='${reservationDetailUrl}'/>" class="reservation-card text-decoration-none text-reset">
                                <article class="card border-0 shadow-sm rounded-4 overflow-hidden reservation-card__surface position-relative">
                                    <span class="position-absolute top-0 end-0 m-2 z-1">
                                        <span class="badge ${reservation.statusKey eq 'accepted' ? 'bg-success' : reservation.statusKey eq 'cancelled' ? 'bg-danger' : reservation.statusKey eq 'started' ? 'bg-info' : reservation.statusKey eq 'pending' ? 'bg-warning text-dark' : 'bg-secondary'}">
                                            <spring:message code="enum.reservation.status.${reservation.statusKey}"/>
                                        </span>
                                    </span>
                                    <div class="row g-0 align-items-stretch">
                                        <div class="col-12 col-md-3 reservation-card__media-wrap">
                                            <c:choose>
                                                <c:when test="${reservation.imageId > 0}">
                                                    <c:url var="reservationImgUrl" value="/image/${reservation.imageId}"/>
                                                    <img src="<c:out value='${reservationImgUrl}'/>" alt="<c:out value='${reservation.brand} ${reservation.model}'/>" class="reservation-card__media">
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="reservation-card__media reservation-card__media--placeholder d-flex align-items-center justify-content-center text-secondary">
                                                        <i class="bi bi-car-front fs-1" aria-hidden="true"></i>
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                        <div class="col-12 col-md-9">
                                            <div class="card-body p-3 p-md-4 h-100 d-flex flex-column justify-content-between gap-3">
                                                <div class="d-flex flex-wrap align-items-start gap-2">
                                                    <div>
                                                        <h3 class="h5 fw-semibold mb-1"><c:out value="${reservation.brand} ${reservation.model}"/></h3>
                                                    </div>
                                                </div>

                                                <div class="row g-3">
                                                    <div class="col-12 col-sm-6">
                                                        <p class="reservation-card__meta-label mb-1"><spring:message code="myReservations.card.pickup"/></p>
                                                        <p class="mb-0 fw-medium"><c:out value="${reservation.pickupDateTime}"/></p>
                                                    </div>
                                                    <div class="col-12 col-sm-6">
                                                        <p class="reservation-card__meta-label mb-1"><spring:message code="myReservations.card.return"/></p>
                                                        <p class="mb-0 fw-medium"><c:out value="${reservation.returnDateTime}"/></p>
                                                    </div>
                                                </div>

                                                <div class="pt-1">
                                                    <div class="reservation-price-compact">
                                                        <span class="reservation-card__meta-label mb-0"><spring:message code="myReservations.card.totalPrice"/></span>
                                                        <span class="h5 mb-0 fw-bold text-primary">$<c:out value="${reservation.totalPrice}"/></span>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </article>
                            </a>
                        </c:forEach>
                    </div>

                    <ryden:pagination
                            currentPage="${ownerReservationsPage.currentPage}"
                            totalPages="${ownerReservationsPage.totalPages}"
                            baseUrl="${myReservationsOwnerPaginationBaseUrl}"
                            pageParam="ownerPage"/>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</main>

<script>
    (function () {
        var allowed = ['', 'pending', 'accepted', 'started', 'cancelled', 'finished'];
        function wire(formId, selId) {
            var form = document.getElementById(formId);
            var sel = document.getElementById(selId);
            if (!form || !sel) return;
            var msg = form.getAttribute('data-ryden-dropdown-invalid') || '';
            form.addEventListener('submit', function (ev) {
                var v = (sel.value || '').trim().toLowerCase();
                if (allowed.indexOf(v) < 0) {
                    ev.preventDefault();
                    sel.setCustomValidity(msg);
                    sel.reportValidity();
                    return;
                }
                sel.setCustomValidity('');
            });
        }
        wire('myReservationsRiderFilterForm', 'rider_res_status');
        wire('myReservationsOwnerFilterForm', 'owner_res_status');
    })();
</script>

<%@include file="footer.jsp"%>
</body>
</html>


