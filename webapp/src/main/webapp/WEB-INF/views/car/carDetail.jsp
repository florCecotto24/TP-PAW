<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@include file="../header.jsp"%>
</head>
<body class="bg-light has-fixed-navbar">
<ryden:navbar/>

<main class="car-detail-page container pb-4">
    <c:if test="${not empty successMessage}">
        <div class="alert alert-warning rounded-3 mt-3"><c:out value="${successMessage}"/></div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger rounded-3 mt-3"><c:out value="${errorMessage}"/></div>
    </c:if>
    <c:choose>
        <c:when test="${param.src eq 'search'}">
            <spring:message code="navbar.explore" var="exploreLabel"/>
            <c:url var="exploreHref" value="/search"/>
            <ryden:breadcrumbTrail midLabel="${exploreLabel}" midHref="${exploreHref}" currentLabel="${carTitle}"/>
        </c:when>
        <c:otherwise>
            <ryden:breadcrumbTrail currentLabel="${carTitle}"/>
        </c:otherwise>
    </c:choose>

    <div class="d-flex flex-column flex-lg-row justify-content-between align-items-start gap-3 mb-4">
        <div class="flex-grow-1 min-w-0">
            <h1 class="h2 fw-bold mb-0"><c:out value="${carTitle}"/></h1>
            <ryden:detailListingMeta
                    rating="${carRatingLabel}"
                    reviewCount="${carReviewCountLabel}"/>
        </div>
    </div>

    <div class="row g-4 align-items-start">
        <div class="col-lg-8 order-1 d-flex flex-column gap-4">
            <ryden:carDetailGalleryGrid
                    modalId="carDetailGalleryModal"
                    mediaItems="${carGalleryMedia}"
                    vehicleLabel="${carTitle}"/>

            <!-- Owner contact info -->
            <c:url var="ownerProfileUrl" value="/counterparty-profile">
                <c:param name="userId" value="${owner.id}"/>
                <c:param name="carId" value="${car.id}"/>
            </c:url>
            <div class="d-flex align-items-center gap-2">
                <a href="${ownerProfileUrl}" class="d-flex align-items-center gap-2 text-decoration-none text-reset">
                    <c:choose>
                        <c:when test="${ownerProfileImageId != null}">
                            <c:url var="ownerProfileImageUrl" value="/image/${ownerProfileImageId}"/>
                            <spring:message code="carDetail.owner.profileImageAlt" var="ownerProfileImageAlt"/>
                            <img
                                    src="${ownerProfileImageUrl}"
                                    alt="<c:out value='${ownerProfileImageAlt}'/>"
                                    class="rounded-circle border"
                                    style="width:40px; height:40px; object-fit:cover;"/>
                        </c:when>
                        <c:otherwise>
                            <span class="d-inline-flex align-items-center justify-content-center rounded-circle border bg-white text-secondary"
                                  style="width:40px; height:40px;" aria-hidden="true">
                                <i class="bi bi-person-fill fs-5"></i>
                            </span>
                        </c:otherwise>
                    </c:choose>
                    <spring:message code="carDetail.owner.nameAriaLabel" var="ownerNameAriaLabel"/>
                    <span class="fw-semibold text-decoration-underline" aria-label="<c:out value='${ownerNameAriaLabel}'/>">
                        <c:out value="${owner.forename}"/> <c:out value="${owner.surname}"/>
                    </span>
                </a>
            </div>
            <c:if test="${car.description.present}">
                <section>
                    <h2 class="h5 fw-bold mb-3"><spring:message code="carDetail.description"/></h2>
                    <p class="mb-0 ryden-multiline-plaintext"><c:out value="${car.description.get()}"/></p>
                </section>
            </c:if>
            <section>
                <h2 class="h5 fw-bold mb-3"><spring:message code="carDetail.specification"/></h2>
                <div class="row row-cols-2 row-cols-md-3 g-3">
                    <div class="col">
                        <spring:message code="enum.car.type.${car.type.name()}" var="carTypeLabel"/>
                        <ryden:specCard icon="car-front" label="${carTypeLabel}"/>
                    </div>
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

            <section class="mt-4 pt-4 border-top border-secondary-subtle" id="listing-reviews">
                <h2 class="h5 fw-bold mb-3"><spring:message code="carDetail.reviews.title"/></h2>
                <c:choose>
                    <c:when test="${empty carReviewPage.content}">
                        <p class="text-secondary mb-0"><spring:message code="carDetail.reviews.empty"/></p>
                    </c:when>
                    <c:otherwise>
                        <div class="row row-cols-1 row-cols-md-2 g-3 mb-3">
                            <c:forEach var="row" items="${carReviewPage.content}">
                                <div class="col">
                                    <ryden:reviewCard
                                            forename="${row.reviewerForename}"
                                            surname="${row.reviewerSurname}"
                                            dateLabel="${row.dateText}"
                                            rating="${row.rating}"
                                            comment="${row.comment}"
                                            imageId="${row.imageId}"/>
                                </div>
                            </c:forEach>
                        </div>
                        <c:if test="${carReviewPage.totalPages > 1}">
                            <div class="d-flex justify-content-between align-items-center gap-2 mt-3">
                                <c:url var="reviewsPrevUrl" value="/car-detail">
                                    <c:param name="carId" value="${car.id}"/>
                                    <c:param name="reviewPage" value="${carReviewPage.currentPage - 1}"/>
                                    <c:if test="${param.src eq 'search'}"><c:param name="src" value="search"/></c:if>
                                    <c:if test="${not empty param.from}"><c:param name="from"><c:out value="${param.from}"/></c:param></c:if>
                                    <c:if test="${not empty param.until}"><c:param name="until"><c:out value="${param.until}"/></c:param></c:if>
                                </c:url>
                                <c:url var="reviewsNextUrl" value="/car-detail">
                                    <c:param name="carId" value="${car.id}"/>
                                    <c:param name="reviewPage" value="${carReviewPage.currentPage + 1}"/>
                                    <c:if test="${param.src eq 'search'}"><c:param name="src" value="search"/></c:if>
                                    <c:if test="${not empty param.from}"><c:param name="from"><c:out value="${param.from}"/></c:param></c:if>
                                    <c:if test="${not empty param.until}"><c:param name="until"><c:out value="${param.until}"/></c:param></c:if>
                                </c:url>
                                <a class="btn btn-outline-secondary btn-sm${carReviewPage.hasPrevious ? '' : ' disabled'}"
                                   href="${carReviewPage.hasPrevious ? reviewsPrevUrl : '#'}"
                                   aria-disabled="${carReviewPage.hasPrevious ? 'false' : 'true'}">
                                    <spring:message code="carDetail.reviews.prev"/>
                                </a>
                                <span class="text-secondary small">
                                    <spring:message code="carDetail.reviews.pageIndicator"
                                                    arguments="${carReviewPage.currentPage + 1},${carReviewPage.totalPages}"/>
                                </span>
                                <a class="btn btn-outline-secondary btn-sm${carReviewPage.hasNext ? '' : ' disabled'}"
                                   href="${carReviewPage.hasNext ? reviewsNextUrl : '#'}"
                                   aria-disabled="${carReviewPage.hasNext ? 'false' : 'true'}">
                                    <spring:message code="carDetail.reviews.next"/>
                                </a>
                            </div>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </section>

        </div>

        <div class="col-lg-4 order-2">
            <c:if test="${currentUserIsAdmin and !owner.admin}">
                <div class="card bg-white border-0 shadow-sm rounded-4 mb-3 p-3">
                    <h6 class="fw-semibold mb-2"><spring:message code="carDetail.admin.sectionTitle"/></h6>
                    <c:if test="${car.status.name() eq 'ACTIVE'}">
                        <form:form action="${pageContext.request.contextPath}/admin/cars/${car.id}/pause"
                                   method="post" modelAttribute="adminActionForm" cssClass="d-inline">
                            <input type="hidden" name="fromCarDetail" value="true"/>
                            <input type="hidden" name="carDetailId" value="${car.id}"/>
                            <button type="submit" class="btn btn-warning btn-sm rounded-3 w-100">
                                <spring:message code="carDetail.admin.pause"/>
                            </button>
                        </form:form>
                    </c:if>
                    <c:if test="${car.status.name() eq 'ADMIN_PAUSED'}">
                        <form:form action="${pageContext.request.contextPath}/admin/cars/${car.id}/resume"
                                   method="post" modelAttribute="adminActionForm" cssClass="d-inline">
                            <input type="hidden" name="fromCarDetail" value="true"/>
                            <input type="hidden" name="carDetailId" value="${car.id}"/>
                            <button type="submit" class="btn btn-success btn-sm rounded-3 w-100">
                                <spring:message code="carDetail.admin.resume"/>
                            </button>
                        </form:form>
                    </c:if>
                </div>
            </c:if>
            <div class="detail-reservation-sticky">
                <ryden:detailReservationPanel
                        carId="${car.id}"
                        dailyPrice="${carMinEffectiveDayPrice}"
                        priceFrom="${carPriceIsVariable}"
                        fromDateTimeValue="${reservationFromDefault}"
                        untilDateTimeValue="${reservationUntilDefault}"
                        bookableWallRangesJson="${bookableWallRangesJson}"
                        carName="${carTitle}"
                        maxBillableDays="${maxReservationBillableDays}"
                        isOwnerRequesting="${isOwnerRequesting}"
                        searchNeighborhoodIds="${searchNeighborhoodIds}"
                        minimumRentalDays="${car.minimumRentalDays}"
                        defaultMonthFirstDay="${reservationDefaultMonthFirstDay}"/>
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

                            <c:url var="similarCarDetailUrl" value="/car-detail"><c:param name="carId" value="${similar.carId}"/></c:url>
                            <ryden:consumerCarCard card="${similar}" image="${similarImageUrl}" href="${similarCarDetailUrl}"/>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</main>

<c:if test="${not empty carGalleryMedia}">
<ryden:carDetailGalleryModal
        modalId="carDetailGalleryModal"
        carouselId="carDetailCarousel"
        mediaItems="${carGalleryMedia}"
        vehicleLabel="${carTitle}"/>
</c:if>

<%@include file="../footer.jsp"%>
<script src="<c:url value='/js/detailReservationForm.js'/>"></script>
</body>
</html>
