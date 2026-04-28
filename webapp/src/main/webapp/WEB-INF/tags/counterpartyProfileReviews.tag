<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%@ attribute name="ratingValue" required="false" type="java.lang.String" %>
<%@ attribute name="reviewCount" required="false" type="java.lang.String" %>

<c:if test="${empty ratingValue}">
    <c:set var="ratingValue" value="4.8"/>
</c:if>
<c:if test="${empty reviewCount}">
    <c:set var="reviewCount" value="18"/>
</c:if>
<c:set var="ratingSteps" value="5,4,3,2,1"/>

<section class="counterparty-section-card counterparty-reviews-card card border-0 shadow-sm rounded-4">
    <div class="card-body p-4">
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
            <div>
                <h2 class="h5 fw-semibold mb-1">Recent reviews</h2>
                <p class="text-secondary small mb-0">Last 12 months</p>
            </div>
            <div class="d-flex align-items-center gap-2">
                <span class="counterparty-rating-value"><c:out value="${ratingValue}"/></span>
                <div class="d-inline-flex align-items-center gap-1" aria-label="Rating">
                    <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
                    <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
                    <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
                    <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
                    <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
                </div>
                <span class="text-secondary small">(<c:out value="${reviewCount}"/> reviews)</span>
            </div>
        </div>

        <div class="counterparty-rating-breakdown">
            <c:forEach var="star" items="${fn:split(ratingSteps, ',')}">
                <div class="counterparty-rating-row">
                    <span class="counterparty-rating-label"><c:out value="${star}"/> stars</span>
                    <div class="progress counterparty-rating-progress" role="progressbar" aria-label="${star} stars">
                        <div class="progress-bar bg-warning" style="width: ${star * 16}%;"></div>
                    </div>
                    <span class="counterparty-rating-count">${star * 3}</span>
                </div>
            </c:forEach>
        </div>

        <div class="row g-3 mt-3">
            <div class="col-md-6">
                <ryden:reviewCard forename="Sofia" surname="Perez" dateLabel="Mar 2026" rating="5" comment="Great communication and spotless car."/>
            </div>
            <div class="col-md-6">
                <ryden:reviewCard forename="Lucas" surname="Gomez" dateLabel="Feb 2026" rating="4" comment="Smooth pickup and return, would book again."/>
            </div>
            <div class="col-md-6">
                <ryden:reviewCard forename="Valentina" surname="Diaz" dateLabel="Jan 2026" rating="5" comment="Very friendly and on time."/>
            </div>
        </div>

        <div class="mt-3">
            <a href="#" class="counterparty-link">View all reviews</a>
        </div>
    </div>
</section>

