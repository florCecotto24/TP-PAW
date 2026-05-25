<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="counterpartyProfile.title"/> — <spring:message code="app.title"/></title>
    <%@include file="header.jsp" %>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/counterparty-profile.css">
</head>
<body class="has-fixed-navbar bg-light counterparty-profile-body">
<ryden:navbar/>

<main class="counterparty-profile-page">
    <div class="counterparty-profile-container">
        <div class="row g-4">
            <div class="col-12">
                <ryden:counterpartyProfileHeader
                        forename="${counterpartyForename}"
                        surname="${counterpartySurname}"
                        about="${counterpartyAbout}"
                        memberSinceDisplay="${counterpartyMemberSinceDisplay}"
                        profileImageId="${counterpartyProfileImageId}"
                        averageRating="${counterpartyAverageRating}"
                        licenseValidated="${counterpartyLicenseValidated}"
                        identityValidated="${counterpartyIdentityValidated}"
                        ratingFloor="${counterpartyRatingFloor}"/>

                <ryden:counterpartyProfileReviews
                        reviews="${recentReviewComments}"/>

                <c:if test="${showCounterpartyActiveListings}">
                    <section class="counterparty-section-card counterparty-reviews-card card border-0 shadow-sm rounded-4 mt-4"
                             data-counterparty-listings-endpoint="${pageContext.request.contextPath}/counterparty-profile/active-listings-page">
                        <div class="card-body p-4">
                            <div class="mb-3">
                                <h2 class="h5 fw-semibold mb-1">
                                    <spring:message code="counterpartyProfile.activeListings.title"/>
                                </h2>
                            </div>
                            <span id="counterpartyListingsLoadErr" class="d-none"><spring:message code="counterpartyProfile.activeListings.loadError"/></span>
                            <span id="counterpartyListingsLoadLoadingText" class="d-none"><spring:message code="counterpartyProfile.activeListings.loading"/></span>
                            <c:choose>
                                <c:when test="${not empty counterpartyActiveListings}">
                                    <div id="counterpartyActiveListingsRow" class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-3 gy-4">
                                        <c:forEach var="car" items="${counterpartyActiveListings}">
                                            <div class="col d-flex justify-content-center">
                                                <c:choose>
                                                    <c:when test="${car.imageId > 0}">
                                                        <c:url var="counterpartyListingImageUrl" value="/image/${car.imageId}"/>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <c:set var="counterpartyListingImageUrl" value=""/>
                                                    </c:otherwise>
                                                </c:choose>
                                                <c:url var="counterpartyListingHref" value="/car-detail">
                                                    <c:param name="carId"><c:out value="${car.carId}"/></c:param>
                                                </c:url>
                                                <ryden:consumerCarCard card="${car}"
                                                                       image="${counterpartyListingImageUrl}"
                                                                       href="${counterpartyListingHref}"/>
                                            </div>
                                        </c:forEach>
                                    </div>
                                    <c:if test="${counterpartyActiveListingsLoadMore.hasNext}">
                                        <div class="text-center mt-4">
                                            <spring:message code="counterpartyProfile.activeListings.viewMore" var="counterpartyViewMoreListingsLabel" htmlEscape="true"/>
                                            <button type="button"
                                                    class="btn btn-outline-primary"
                                                    id="counterpartyListingsLoadMoreBtn"
                                                    data-owner-user-id="${counterpartyActiveListingsLoadMore.ownerUserId}"
                                                    data-exclude-car-id="<c:out value='${counterpartyActiveListingsLoadMore.excludeCarId}'/>"
                                                    data-next-page="${counterpartyActiveListingsLoadMore.nextPageToLoad}"
                                                    data-default-label="<c:out value='${counterpartyViewMoreListingsLabel}'/>">
                                                <spring:message code="counterpartyProfile.activeListings.viewMore"/>
                                            </button>
                                        </div>
                                    </c:if>
                                </c:when>
                                <c:otherwise>
                                    <p class="mb-0 text-secondary small">
                                        <spring:message code="counterpartyProfile.activeListings.empty"/>
                                    </p>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </section>
                    <script defer src="${pageContext.request.contextPath}/js/counterparty-profile.js"></script>
                </c:if>
            </div>
        </div>
    </div>
</main>

<%@include file="footer.jsp" %>
</body>
</html>
