<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@include file="header.jsp"%>
</head>
<body class="bg-light has-fixed-navbar">
<paw:navbar/>

<main class="car-detail-page container pb-4">
    <paw:breadcrumbTrail currentLabel="${listing.title}"/>

    <div class="d-flex flex-column flex-lg-row justify-content-between align-items-start gap-3 mb-4">
        <div class="flex-grow-1 min-w-0">
            <h1 class="h2 fw-bold mb-0"><c:out value="${listing.title}"/></h1>
            <paw:detailListingMeta location="${listing.startPoint}"/>
        </div>
    </div>

    <div class="row g-4 align-items-start">
        <div class="col-lg-8 order-1 d-flex flex-column gap-4">
            <paw:carDetailGalleryGrid
                    modalId="carDetailGalleryModal"
                    imageUrls="${carGalleryImagePaths}"
                    vehicleLabel="${listing.title}"/>

            <!-- Owner contact info -->
            <div class="d-flex align-items-center gap-2">
                <span class="d-inline-flex align-items-center justify-content-center rounded-circle border bg-white text-secondary"
                      style="width:40px; height:40px;" aria-hidden="true">
                    <i class="bi bi-person-fill fs-5"></i>
                </span>
                <span class="fw-semibold" aria-label="Owner name">
                    <c:out value="${owner.forename}"/> <c:out value="${owner.surname}"/>
                </span>
            </div>
            <section>
                <h2 class="h5 fw-bold mb-3">Specifications</h2>
                <div class="row row-cols-2 row-cols-md-4 g-3">
                    <div class="col">
                        <paw:specCard icon="gear-wide-connected" label="${car.transmission.name()}"/>
                    </div>
                    <div class="col">
                        <paw:specCard icon="fuel-pump" label="${car.powertrain.name()}"/>
                    </div>
                </div>
            </section>
            <section id="availabilitySection">
                <h2 class="h5 fw-bold mb-3 d-flex align-items-center gap-2 flex-wrap">
                    Availability
                    <c:if test="${not empty listingAvailabilities}">
                        <span class="badge rounded-pill text-bg-secondary">${fn:length(listingAvailabilities)} window<c:if test="${fn:length(listingAvailabilities) ne 1}">s</c:if></span>
                    </c:if>
                </h2>
                <c:choose>
                    <c:when test="${empty availabilityLines}">
                        <p class="text-secondary mb-0">No availability windows published for this listing.</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="list-group list-group-flush border rounded-3 shadow-sm">
                            <c:forEach items="${availabilityLines}" var="line" varStatus="st">
                                <li class="list-group-item d-flex align-items-start gap-2 py-3">
                                    <i class="bi bi-calendar-range text-primary mt-1" aria-hidden="true"></i>
                                    <div>
                                        <span class="visually-hidden">Window </span>
                                        <span class="fw-medium">Period <c:out value="${st.count}"/></span>
                                        <div class="small text-body-secondary mt-1"><c:out value="${line}"/></div>
                                    </div>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </section>


        </div>

        <div class="col-lg-4 order-2">
            <div class="detail-reservation-sticky">
                <fmt:formatNumber var="dayPriceStr" value="${listing.dayPrice}" maxFractionDigits="2" minFractionDigits="0" groupingUsed="false"/>
                <paw:detailReservationPanel
                        listingId="${listing.id}"
                        pricePerDay="${dayPriceStr}"
                        days="3"
                        subtotal="360"
                        serviceFee="24"
                        total="384"
                        deliveryLocation="${listing.startPoint}"
                        fromDateTimeValue="${reservationFromDefault}"
                        untilDateTimeValue="${reservationUntilDefault}"
                        reservationPeriods="${reservationPeriods}"
                        carName="${listing.title}"/>
            </div>
        </div>
    </div>

    <section class="similarVehiclesSection mt-5 pt-5 border-top border-secondary-subtle" id="similarVehiclesSection">
        <paw:similarVehiclesHeader/>
        <p class="text-secondary text-center mb-0">Similar listings will appear here when available.</p>
    </section>
</main>

<c:if test="${not empty carGalleryImagePaths}">
<paw:carDetailGalleryModal
        modalId="carDetailGalleryModal"
        carouselId="carDetailCarousel"
        imageUrls="${carGalleryImagePaths}"
        vehicleLabel="${listing.title}"/>
</c:if>

<%@include file="footer.jsp"%>
</body>
</html>
