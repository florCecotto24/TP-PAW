<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="myListingReservations.pageTitle" arguments="${listing.title}"/></title>
    <%@include file="header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="navbar.myListings" var="myListingsLabel"/>
    <ryden:breadcrumbTrail
            homeLabel="${myListingsLabel}"
            homeHref="${pageContext.request.contextPath}/my-listings"
            currentLabel="${listing.title}"/>

    <section class="reservation-management-header mt-4 pt-5 mb-4">
        <h1 class="h3 fw-bold mb-2"><spring:message code="myListingReservations.heading"/></h1>
        <p class="text-secondary mb-0">
            <c:out value="${car.brand} ${car.model}"/> &mdash; <c:out value="${listing.title}"/>
        </p>
    </section>

    <c:url var="listingReservationsPaginationBaseUrl" value="/my-listings/${listing.id}/reservations">
        <c:if test="${not empty statusFilter}">
            <c:param name="reservationStatus" value="${statusFilter}"/>
        </c:if>
    </c:url>

    <c:if test="${not empty reservations or not empty statusFilter}">
        <spring:message code="validation.dropdown.invalid" var="listingResDropdownInvalid" htmlEscape="true"/>
        <form id="listingResFilterForm" class="row g-2 align-items-end mb-3" method="get"
              action="${pageContext.request.contextPath}/my-listings/${listing.id}/reservations"
              data-ryden-dropdown-invalid="<c:out value='${listingResDropdownInvalid}'/>">
            <div class="col-md-5 col-lg-4">
                <label class="form-label small text-secondary mb-1" for="listingRes_status"><spring:message code="myReservations.filter.status"/></label>
                <select class="form-select" id="listingRes_status" name="reservationStatus">
                    <option value="" ${empty statusFilter ? 'selected="selected"' : ''}><spring:message code="myReservations.filter.status.any"/></option>
                    <option value="pending" ${statusFilter eq 'pending' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.pending"/></option>
                    <option value="accepted" ${statusFilter eq 'accepted' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.accepted"/></option>
                    <option value="started" ${statusFilter eq 'started' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.started"/></option>
                    <option value="cancelled" ${statusFilter eq 'cancelled' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.cancelled"/></option>
                    <option value="finished" ${statusFilter eq 'finished' ? 'selected="selected"' : ''}><spring:message code="enum.reservation.status.finished"/></option>
                </select>
            </div>
            <div class="col-auto d-flex flex-wrap gap-2">
                <button type="submit" class="btn btn-primary"><spring:message code="myListings.filter.search"/></button>
                <a href="${pageContext.request.contextPath}/my-listings/${listing.id}/reservations" class="btn btn-outline-secondary"><spring:message code="search.filters.clear"/></a>
            </div>
        </form>

        <div class="mb-3">
            <h3 class="h6 mb-0">
                <c:choose>
                    <c:when test="${listingReservationsPage.totalItems > 0}">
                        <spring:message code="myReservations.ownerResultsRange"
                                        arguments="${listingReservationsPage.firstItemNumber},${listingReservationsPage.lastItemNumber},${listingReservationsPage.totalItems}"/>
                    </c:when>
                    <c:otherwise>
                        <spring:message code="myReservations.ownerResultsCount" arguments="0"/>
                    </c:otherwise>
                </c:choose>
            </h3>
        </div>
    </c:if>

    <div class="mb-3 d-flex justify-content-end">
        <a href="${pageContext.request.contextPath}/my-listings/${listing.id}" class="btn btn-outline-secondary btn-sm">
            <i class="bi bi-arrow-left me-1" aria-hidden="true"></i>
            <spring:message code="common.back"/>
        </a>
    </div>

    <c:choose>
        <c:when test="${empty reservations}">
            <c:choose>
                <c:when test="${not empty statusFilter}">
                    <div class="search-empty-state text-center">
                        <h2 class="h4 fw-semibold mb-2">
                            <spring:message code="myListingReservations.noResults.title"/>
                        </h2>
                        <div class="search-empty-state__actions mt-4">
                            <a href="${pageContext.request.contextPath}/my-listings/${listing.id}/reservations" class="btn btn-outline-secondary">
                                <spring:message code="search.filters.clear"/>
                            </a>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="search-empty-state text-center">
                        <img src="${pageContext.request.contextPath}/assets/images/filmore-cars.png"
                             alt="" class="mb-4 img-fluid" style="max-width:260px"/>
                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myListingReservations.empty.title"/></h2>
                        <p class="text-secondary mb-0 search-empty-state__text">
                            <spring:message code="myListingReservations.empty.description"/>
                        </p>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <div class="d-flex flex-column gap-3 mb-4">
                <c:forEach var="reservation" items="${reservations}">
                    <c:url var="reservationDetailUrl" value="/my-reservations/${reservation.reservationId}">
                        <c:param name="role" value="owner"/>
                        <c:param name="fromListing" value="${listing.id}"/>
                    </c:url>
                    <a href="<c:out value='${reservationDetailUrl}'/>" class="reservation-card text-decoration-none text-reset">
                        <article class="card border-0 shadow-sm rounded-4 overflow-hidden reservation-card__surface position-relative">
                            <span class="position-absolute top-0 end-0 m-2 z-1">
                                <span class="badge ${reservation.statusKey eq 'accepted' ? 'bg-success' : fn:startsWith(reservation.statusKey, 'cancelled') ? 'bg-danger' : reservation.statusKey eq 'started' ? 'bg-info' : reservation.statusKey eq 'pending' ? 'bg-warning text-dark' : 'bg-secondary'}">
                                    <spring:message code="enum.reservation.status.${reservation.statusKey}"/>
                                </span>
                            </span>
                            <div class="row g-0 align-items-stretch">
                                <div class="col-12 col-md-3 reservation-card__media-wrap">
                                    <c:choose>
                                        <c:when test="${reservation.imageId > 0}">
                                            <c:url var="resImgUrl" value="/image/${reservation.imageId}"/>
                                            <img src="<c:out value='${resImgUrl}'/>" alt="<c:out value='${reservation.brand} ${reservation.model}'/>" class="reservation-card__media">
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
                                        <div>
                                            <h3 class="h5 fw-semibold mb-1"><c:out value="${reservation.brand} ${reservation.model}"/></h3>
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
                                                <span class="h5 mb-0 fw-bold text-primary"><c:out value="${reservation.totalPrice}"/></span>
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
                    currentPage="${listingReservationsPage.currentPage}"
                    totalPages="${listingReservationsPage.totalPages}"
                    baseUrl="${listingReservationsPaginationBaseUrl}"
                    pageParam="page"/>
        </c:otherwise>
    </c:choose>
</main>

<script>
    (function () {
        var allowed = ['', 'pending', 'accepted', 'started', 'cancelled', 'finished'];
        var form = document.getElementById('listingResFilterForm');
        var sel = document.getElementById('listingRes_status');
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
    })();
</script>

<%@include file="footer.jsp"%>
</body>
</html>
