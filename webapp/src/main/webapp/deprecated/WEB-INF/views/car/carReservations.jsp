<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
<head>
    <title><spring:message code="myCarReservations.pageTitle" arguments="${car.brand} ${car.model}"/></title>
    <%@include file="../header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="navbar.myCars" var="myListingsLabel"/>
    <ryden:breadcrumbTrail
            homeLabel="${myListingsLabel}"
            homeHref="${pageContext.request.contextPath}/my-cars"
            currentLabel="${car.brand} ${car.model}"/>

    <section class="reservation-management-header mt-4 pt-5 mb-4">
        <h1 class="h3 fw-bold mb-2"><spring:message code="myCarReservations.heading"/></h1>
        <p class="text-secondary mb-0">
            <c:out value="${car.brand} ${car.model}"/>
        </p>
    </section>

    <c:url var="carReservationsPaginationBaseUrl" value="/my-cars/car/${car.id}/reservations">
        <c:if test="${not empty statusFilter}">
            <c:param name="reservationStatus" value="${statusFilter}"/>
        </c:if>
    </c:url>

    <c:if test="${not empty reservations or not empty statusFilter}">
        <spring:message code="validation.dropdown.invalid" var="carResDropdownInvalid"/>
        <form id="carResFilterForm" class="row g-2 align-items-end mb-3" method="get"
              action="${pageContext.request.contextPath}/my-cars/car/${car.id}/reservations"
              data-ryden-dropdown-invalid="<c:out value='${carResDropdownInvalid}'/>">
            <div class="col-md-5 col-lg-4">
                <spring:message code="myReservations.filter.status.any"    var="rsAny"/>
                <spring:message code="enum.reservation.status.pending"     var="rsPending"/>
                <spring:message code="enum.reservation.status.accepted"    var="rsAccepted"/>
                <spring:message code="enum.reservation.status.started"     var="rsStarted"/>
                <spring:message code="enum.reservation.status.cancelled"   var="rsCancelled"/>
                <spring:message code="enum.reservation.status.finished"    var="rsFinished"/>
                <c:choose>
                    <c:when test="${statusFilter eq 'pending'}">  <c:set var="rsActiveLabel" value="${rsPending}"/></c:when>
                    <c:when test="${statusFilter eq 'accepted'}"> <c:set var="rsActiveLabel" value="${rsAccepted}"/></c:when>
                    <c:when test="${statusFilter eq 'started'}">  <c:set var="rsActiveLabel" value="${rsStarted}"/></c:when>
                    <c:when test="${statusFilter eq 'cancelled'}"><c:set var="rsActiveLabel" value="${rsCancelled}"/></c:when>
                    <c:when test="${statusFilter eq 'finished'}"> <c:set var="rsActiveLabel" value="${rsFinished}"/></c:when>
                    <c:otherwise><c:set var="rsActiveLabel" value="${rsAny}"/></c:otherwise>
                </c:choose>
                <input type="hidden" id="carRes_status" name="reservationStatus" value="<c:out value='${statusFilter}'/>"/>
                <label class="form-label small text-secondary mb-1"><spring:message code="myReservations.filter.status"/></label>
                <div class="dropdown">
                    <button type="button" id="carResStatusBtn"
                            class="form-select dropdown-toggle ryden-select-btn text-start w-100"
                            data-bs-toggle="dropdown"
                            data-bs-auto-close="true"
                            aria-expanded="false">
                        <span id="carResStatusLbl"><c:out value="${rsActiveLabel}"/></span>
                    </button>
                    <ul class="dropdown-menu shadow ryden-select-menu p-1 w-100">
                        <c:set var="isAct" value="${empty statusFilter}"/>
                        <li>
                            <button type="button" class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                                    data-ryden-select-val=""
                                    data-ryden-select-text="<c:out value='${rsAny}'/>"
                                    data-ryden-target-id="carRes_status"
                                    data-ryden-label-id="carResStatusLbl"
                                    data-ryden-dd-btn-id="carResStatusBtn">
                                <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                                <c:out value="${rsAny}"/>
                            </button>
                        </li>
                        <c:set var="isAct" value="${statusFilter eq 'pending'}"/>
                        <li>
                            <button type="button" class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                                    data-ryden-select-val="pending"
                                    data-ryden-select-text="<c:out value='${rsPending}'/>"
                                    data-ryden-target-id="carRes_status"
                                    data-ryden-label-id="carResStatusLbl"
                                    data-ryden-dd-btn-id="carResStatusBtn">
                                <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                                <c:out value="${rsPending}"/>
                            </button>
                        </li>
                        <c:set var="isAct" value="${statusFilter eq 'accepted'}"/>
                        <li>
                            <button type="button" class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                                    data-ryden-select-val="accepted"
                                    data-ryden-select-text="<c:out value='${rsAccepted}'/>"
                                    data-ryden-target-id="carRes_status"
                                    data-ryden-label-id="carResStatusLbl"
                                    data-ryden-dd-btn-id="carResStatusBtn">
                                <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                                <c:out value="${rsAccepted}"/>
                            </button>
                        </li>
                        <c:set var="isAct" value="${statusFilter eq 'started'}"/>
                        <li>
                            <button type="button" class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                                    data-ryden-select-val="started"
                                    data-ryden-select-text="<c:out value='${rsStarted}'/>"
                                    data-ryden-target-id="carRes_status"
                                    data-ryden-label-id="carResStatusLbl"
                                    data-ryden-dd-btn-id="carResStatusBtn">
                                <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                                <c:out value="${rsStarted}"/>
                            </button>
                        </li>
                        <c:set var="isAct" value="${statusFilter eq 'cancelled'}"/>
                        <li>
                            <button type="button" class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                                    data-ryden-select-val="cancelled"
                                    data-ryden-select-text="<c:out value='${rsCancelled}'/>"
                                    data-ryden-target-id="carRes_status"
                                    data-ryden-label-id="carResStatusLbl"
                                    data-ryden-dd-btn-id="carResStatusBtn">
                                <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                                <c:out value="${rsCancelled}"/>
                            </button>
                        </li>
                        <c:set var="isAct" value="${statusFilter eq 'finished'}"/>
                        <li>
                            <button type="button" class="dropdown-item ryden-select-item${isAct ? ' ryden-select-item--active' : ''}"
                                    data-ryden-select-val="finished"
                                    data-ryden-select-text="<c:out value='${rsFinished}'/>"
                                    data-ryden-target-id="carRes_status"
                                    data-ryden-label-id="carResStatusLbl"
                                    data-ryden-dd-btn-id="carResStatusBtn">
                                <i class="bi bi-check2 ryden-sel-check${isAct ? '' : ' invisible'}" aria-hidden="true"></i>
                                <c:out value="${rsFinished}"/>
                            </button>
                        </li>
                    </ul>
                </div>
            </div>
            <div class="col-auto d-flex flex-wrap gap-2">
                <button type="submit" class="btn btn-primary"><spring:message code="myCars.filter.search"/></button>
                <a href="${pageContext.request.contextPath}/my-cars/car/${car.id}/reservations" class="btn btn-outline-secondary"><spring:message code="search.filters.clear"/></a>
            </div>
        </form>

        <div class="mb-3">
            <h3 class="h6 mb-0">
                <c:choose>
                    <c:when test="${carReservationsPage.totalItems > 0}">
                        <spring:message code="myReservations.ownerResultsRange"
                                        arguments="${carReservationsPage.firstItemNumber},${carReservationsPage.lastItemNumber},${carReservationsPage.totalItems}"/>
                    </c:when>
                    <c:otherwise>
                        <spring:message code="myReservations.ownerResultsCount" arguments="0"/>
                    </c:otherwise>
                </c:choose>
            </h3>
        </div>
    </c:if>

    <div class="mb-3 d-flex justify-content-end">
        <a href="${pageContext.request.contextPath}/my-cars/car/${car.id}" class="btn btn-outline-secondary btn-sm">
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
                            <spring:message code="myCarReservations.noResults.title"/>
                        </h2>
                        <div class="search-empty-state__actions mt-4">
                            <a href="${pageContext.request.contextPath}/my-cars/car/${car.id}/reservations" class="btn btn-outline-secondary">
                                <spring:message code="search.filters.clear"/>
                            </a>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="search-empty-state text-center">
                        <img src="${pageContext.request.contextPath}/assets/images/filmore-cars.png"
                             alt="" class="mb-4 img-fluid" style="max-width:260px"/>
                        <h2 class="h4 fw-semibold mb-2"><spring:message code="myCarReservations.empty.title"/></h2>
                        <p class="text-secondary mb-0 search-empty-state__text">
                            <spring:message code="myCarReservations.empty.description"/>
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
                        <c:param name="fromCar" value="${car.id}"/>
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
                    currentPage="${carReservationsPage.currentPage}"
                    totalPages="${carReservationsPage.totalPages}"
                    baseUrl="${carReservationsPaginationBaseUrl}"
                    pageParam="page"/>
        </c:otherwise>
    </c:choose>
</main>

<script>
    (function () {
        var allowed = ['', 'pending', 'accepted', 'started', 'cancelled', 'finished'];
        var form = document.getElementById('carResFilterForm');
        var sel = document.getElementById('carRes_status');
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

<%@include file="../footer.jsp"%>
</body>
</html>
