<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@include file="header.jsp"%>
</head>
<body class="bg-light has-fixed-navbar">
<ryden:navbar/>

<main class="car-detail-page container pb-4">
    <ryden:breadcrumbTrail currentLabel="${listing.title}"/>

    <div class="d-flex flex-column flex-lg-row justify-content-between align-items-start gap-3 mb-4">
        <div class="flex-grow-1 min-w-0">
            <h1 class="h2 fw-bold mb-0"><c:out value="${listing.title}"/></h1>
            <ryden:detailListingMeta location="${listing.startPoint}"/>
        </div>
    </div>

    <div class="row g-4 align-items-start">
        <div class="col-lg-8 order-1 d-flex flex-column gap-4">
            <ryden:carDetailGalleryGrid
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
            <c:if test="${not empty listing.description}">
                <section>
                    <h2 class="h5 fw-bold mb-3"><spring:message code="carDetail.description"/></h2>
                    <p class="mb-0"><c:out value="${listing.description}"/></p>
                </section>
            </c:if>
            <section>
                <h2 class="h5 fw-bold mb-3"><spring:message code="carDetail.specification"/></h2>
                <div class="row row-cols-2 row-cols-md-4 g-3">
                    <div class="col">
                        <spring:message code="enum.car.transmission.${car.transmission.name()}" var="carTransmissionLabel"/>
                        <ryden:specCard icon="gear-wide-connected" label="${carTransmissionLabel}"/>
                    </div>
                    <div class="col">
                        <spring:message code="enum.car.powertrain.${car.powertrain.name()}" var="carPowertrainLabel"/>
                        <ryden:specCard icon="fuel-pump" label="${carPowertrainLabel}"/>
                    </div>
                </div>
            </section>

        </div>

        <div class="col-lg-4 order-2">
            <div class="detail-reservation-sticky">
                <ryden:detailReservationPanel
                        listingId="${listing.id}"
                        dailyPrice="${listing.dayPrice}"
                        deliveryLocation="${listing.startPoint}"
                        fromDateTimeValue="${reservationFromDefault}"
                        untilDateTimeValue="${reservationUntilDefault}"
                        bookableWallRangesJson="${bookableWallRangesJson}"
                        carName="${listing.title}"
                        pickupTime="${listing.checkInTime}"
                        returnTime="${listing.checkOutTime}"/>
            </div>
        </div>
    </div>

    <section class="similarVehiclesSection mt-5 pt-5 border-top border-secondary-subtle" id="similarVehiclesSection">
        <ryden:similarVehiclesHeader seeAllHref="${pageContext.request.contextPath}${similarSearchUrl}"/>
        <c:choose>
            <c:when test="${empty similarListings}">
                <p class="text-secondary text-center mb-0"><spring:message code="carDetail.similarListingsWhenAvailable"/></p>
            </c:when>
            <c:otherwise>
                <div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-3">
                    <c:forEach var="similar" items="${similarListings}">
                        <div class="col d-flex justify-content-center">
                            <c:choose>
                                <c:when test="${similar.imageId > 0}">
                                    <c:url var="similarImageUrl" value="/image/${similar.imageId}" />
                                </c:when>
                                <c:otherwise>
                                    <c:set var="similarImageUrl" value="" />
                                </c:otherwise>
                            </c:choose>

                            <c:url var="similarCarDetailUrl" value="/car-detail"><c:param name="listingId" value="${similar.listingId}"/></c:url>
                            <ryden:carCard
                                    model="${similar.model}"
                                    brand="${similar.brand}"
                                    price="${similar.price}"
                                    image="${similarImageUrl}"
                                    pricePeriod="day"
                                    href="${similarCarDetailUrl}"/>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</main>

<c:if test="${not empty carGalleryImagePaths}">
<ryden:carDetailGalleryModal
        modalId="carDetailGalleryModal"
        carouselId="carDetailCarousel"
        imageUrls="${carGalleryImagePaths}"
        vehicleLabel="${listing.title}"/>
</c:if>

<%@include file="footer.jsp"%>
<script src="<c:url value='/js/detailReservationForm.js'/>"></script>
</body>
</html>
