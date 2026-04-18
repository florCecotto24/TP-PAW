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
    <section class="reservation-management-header mt-4 pt-5 mb-4">
        <h1 class="h3 fw-bold mb-2"><spring:message code="myReservations.heading"/></h1>
        <p class="text-secondary mb-0"><spring:message code="myReservations.subheading"/></p>
    </section>

    <div class="mb-3 d-flex flex-wrap align-items-center justify-content-between gap-2">
        <h2 class="h5 mb-0">
            <c:choose>
                <c:when test="${myReservationsPage.totalItems > 0}">
                    <spring:message code="myReservations.resultsRange"
                                    arguments="${myReservationsPage.firstItemNumber},${myReservationsPage.lastItemNumber},${myReservationsPage.totalItems}"/>
                </c:when>
                <c:otherwise>
                    <spring:message code="myReservations.resultsCount" arguments="0"/>
                </c:otherwise>
            </c:choose>
        </h2>
    </div>

    <c:url var="myReservationsBaseUrl" value="/my-reservations"/>
    <c:url var="reserveCarUrl" value="/search"/>

    <c:choose>
        <c:when test="${empty results}">
            <div class="search-empty-state text-center">
                <div class="search-empty-state__icon" aria-hidden="true">
                    <i class="bi bi-calendar2-check"></i>
                </div>
                <h2 class="h4 fw-semibold mb-2"><spring:message code="myReservations.empty.title"/></h2>
                <p class="text-secondary mb-0 search-empty-state__text">
                    <spring:message code="myReservations.empty.description"/>
                </p>
                <div class="search-empty-state__actions">
                    <a href="<c:out value='${reserveCarUrl}'/>" class="btn btn-primary btn-action btn-action-md">
                        <spring:message code="myReservations.empty.reserve"/>
                    </a>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="d-flex flex-column gap-3">
                <c:forEach var="reservation" items="${results}">
                    <c:url var="reservationDetailUrl" value="/my-reservations/${reservation.reservationId}"/>
                    <a href="<c:out value='${reservationDetailUrl}'/>" class="reservation-card text-decoration-none text-reset">
                        <article class="card border-0 shadow-sm rounded-4 overflow-hidden reservation-card__surface">
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
                    currentPage="${myReservationsPage.currentPage}"
                    totalPages="${myReservationsPage.totalPages}"
                    baseUrl="${myReservationsBaseUrl}"/>
        </c:otherwise>
    </c:choose>
</main>

<%@include file="footer.jsp"%>
</body>
</html>

