<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="myReservationDetail.pageTitle" arguments="${reservation.id}"/></title>
    <%@include file="header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="navbar.myReservations" var="myReservationsLabel"/>
    <ryden:breadcrumbTrail
            homeLabel="${myReservationsLabel}"
            homeHref="${pageContext.request.contextPath}/my-reservations"
            currentLabel="${reservation.id}"/>

    <section class="reservation-management-header mb-4">
        <h1 class="h3 fw-bold mb-2"><spring:message code="myReservationDetail.heading" arguments="${reservation.id}"/></h1>
        <p class="text-secondary mb-0"><spring:message code="myReservationDetail.subheading"/></p>
    </section>

    <div class="row g-4 align-items-start">
        <div class="col-lg-8">
            <article class="card border-0 shadow-sm rounded-4 mb-4">
                <div class="card-body p-4">
                    <h2 class="h5 fw-semibold mb-3"><spring:message code="myReservationDetail.carSummary.title"/></h2>
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
                            <div class="d-flex flex-wrap gap-2">
                                <spring:message code="enum.car.transmission.${car.transmission.name()}" var="carTransmissionLabel"/>
                                <spring:message code="enum.car.powertrain.${car.powertrain.name()}" var="carPowertrainLabel"/>
                                <span class="badge text-bg-light border"><c:out value="${carTransmissionLabel}"/></span>
                                <span class="badge text-bg-light border"><c:out value="${carPowertrainLabel}"/></span>
                            </div>
                        </div>
                    </div>
                </div>
            </article>

            <article class="card border-0 shadow-sm rounded-4 mb-4">
                <div class="card-body p-4">
                    <h2 class="h5 fw-semibold mb-3"><spring:message code="myReservationDetail.details.title"/></h2>
                    <div class="row g-3">
                        <div class="col-sm-6">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationDetail.details.pickup"/></p>
                            <p class="mb-0 fw-medium"><c:out value="${pickupDateTime}"/></p>
                        </div>
                        <div class="col-sm-6">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationDetail.details.return"/></p>
                            <p class="mb-0 fw-medium"><c:out value="${returnDateTime}"/></p>
                        </div>
                        <div class="col-sm-6">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationDetail.details.status"/></p>
                            <span class="reservation-status-badge reservation-status-badge--${statusKey}">
                                <spring:message code="enum.reservation.status.${statusKey}"/>
                            </span>
                        </div>
                        <div class="col-sm-6">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationDetail.details.pickupLocation"/></p>
                            <p class="mb-0 fw-medium"><c:out value="${listing.startPoint}"/></p>
                        </div>
                    </div>
                </div>
            </article>
        </div>

        <div class="col-lg-4">
            <article class="card border-0 shadow-sm rounded-4 reservation-detail-sticky">
                <div class="card-body p-4">
                    <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationDetail.totalPrice"/></p>
                    <p class="h2 fw-bold text-primary mb-4">$<c:out value="${totalPrice}"/></p>

                    <div class="d-grid gap-2">
                        <button type="button" class="btn btn-primary" disabled>
                            <spring:message code="myReservationDetail.actions.modify"/>
                        </button>
                        <button type="button" class="btn btn-outline-danger" disabled>
                            <spring:message code="myReservationDetail.actions.cancel"/>
                        </button>
                        <c:if test="${not empty owner.email}">
                            <spring:message code="myReservationDetail.actions.contactOwner" var="contactOwnerLabel"/>
                            <a href="mailto:<c:out value='${owner.email}'/>" class="btn btn-outline-secondary">
                                <c:out value="${contactOwnerLabel}"/>
                            </a>
                        </c:if>
                    </div>

                    <hr class="my-4">
                    <c:url var="listingUrl" value="/car-detail">
                        <c:param name="listingId" value="${listing.id}"/>
                    </c:url>
                    <a href="<c:out value='${listingUrl}'/>" class="btn btn-light border w-100">
                        <spring:message code="myReservationDetail.actions.viewListing"/>
                    </a>
                </div>
            </article>
        </div>
    </div>
</main>

<%@include file="footer.jsp"%>
</body>
</html>


