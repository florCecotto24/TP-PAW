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
                        identityValidated="${counterpartyIdentityValidated}"/>

                <ryden:counterpartyProfileReviews
                        comments="${recentReviewComments}"/>

                <c:if test="${showCounterpartyActiveListings}">
                    <section class="counterparty-section-card counterparty-reviews-card card border-0 shadow-sm rounded-4 mt-4">
                        <div class="card-body p-4">
                            <div class="mb-3">
                                <h2 class="h5 fw-semibold mb-1">
                                    <spring:message code="counterpartyProfile.activeListings.title"/>
                                </h2>
                            </div>
                            <c:choose>
                                <c:when test="${not empty counterpartyActiveListings}">
                                    <div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-3">
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
                                                    <c:param name="listingId"><c:out value="${car.listingId}"/></c:param>
                                                </c:url>
                                                <ryden:carCard
                                                        model="${car.model}"
                                                        brand="${car.brand}"
                                                        price="${car.price}"
                                                        image="${counterpartyListingImageUrl}"
                                                        pricePeriod="day"
                                                        ratingAvg="${car.ratingAvg}"
                                                        href="${counterpartyListingHref}"/>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <p class="mb-0 text-secondary small">
                                        <spring:message code="counterpartyProfile.activeListings.empty"/>
                                    </p>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </section>
                </c:if>
            </div>
        </div>
    </div>
</main>

<%@include file="footer.jsp" %>
</body>
</html>
