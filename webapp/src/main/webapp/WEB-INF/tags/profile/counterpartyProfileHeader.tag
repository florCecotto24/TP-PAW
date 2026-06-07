<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="forename" required="true" type="java.lang.String" %>
<%@ attribute name="surname" required="true" type="java.lang.String" %>
<%@ attribute name="about" required="false" type="java.lang.String" %>
<%@ attribute name="memberSinceDisplay" required="false" type="java.lang.String" %>
<%@ attribute name="profileImageId" required="false" type="java.lang.Long" %>
<%@ attribute name="averageRating" required="false" type="java.math.BigDecimal" %>
<%@ attribute name="reviewCount" required="false" type="java.lang.Long" %>
<%@ attribute name="licenseValidated" required="false" type="java.lang.Boolean" %>
<%@ attribute name="identityValidated" required="false" type="java.lang.Boolean" %>
<%@ attribute name="ratingFloor" required="false" type="java.lang.Long" %>
<c:set var="initials" value="${fn:substring(forename, 0, 1)}${fn:substring(surname, 0, 1)}"/>

<section class="card border-0 shadow-sm rounded-4 counterparty-section-card counterparty-header-card">
    <div class="card-body p-4">
        <div class="d-flex flex-column flex-md-row align-items-start gap-4">
            <div class="counterparty-avatar">
                <c:choose>
                    <c:when test="${profileImageId != null && profileImageId > 0}">
                        <img src="${pageContext.request.contextPath}/image/${profileImageId}" alt="<c:out value='${forename} ${surname}'/>" class="counterparty-avatar__img"/>
                    </c:when>
                    <c:otherwise>
                        <div class="counterparty-avatar__placeholder">
                            <span><c:out value="${initials}"/></span>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="flex-grow-1 min-w-0">
                <div class="d-flex flex-wrap align-items-center gap-2">
                    <h1 class="h4 fw-semibold mb-0 ryden-text-break"><c:out value="${forename} ${surname}"/></h1>
                </div>
                <div class="d-flex align-items-center gap-2 mt-2">
                    <c:choose>
                        <c:when test="${averageRating != null}">
                            <span class="counterparty-rating-value">
                                <fmt:formatNumber value="${averageRating}" minFractionDigits="1" maxFractionDigits="1"/>
                            </span>
                            <div class="d-inline-flex align-items-center gap-1" aria-label="<spring:message code='counterpartyProfile.rating.ariaLabel'/>">
                                <c:set var="ratingFraction" value="${averageRating - ratingFloor}"/>
                                <c:forEach begin="1" end="5" var="star">
                                    <c:choose>
                                        <c:when test="${star <= ratingFloor}">
                                            <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
                                        </c:when>
                                        <c:when test="${star == ratingFloor + 1 && ratingFraction >= 0.4 && ratingFraction <= 0.6}">
                                            <i class="bi bi-star-half text-warning" aria-hidden="true"></i>
                                        </c:when>
                                        <c:otherwise>
                                            <i class="bi bi-star text-secondary-subtle" aria-hidden="true"></i>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>
                            </div>
                            <c:if test="${reviewCount != null && reviewCount > 0}">
                                <span class="text-secondary small">
                                    <spring:message code="counterpartyProfile.reviews.count" arguments="${reviewCount}"/>
                                </span>
                            </c:if>
                        </c:when>
                        <c:otherwise>
                            <span class="counterparty-rating-value">
                                <spring:message code="counterpartyProfile.reviews.emptyShort"/>
                            </span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <p class="counterparty-about mt-3 mb-2 ryden-multiline-plaintext">
                    <c:choose>
                        <c:when test="${not empty about}">
                            <c:out value="${about}"/>
                        </c:when>
                        <c:otherwise>
                            <spring:message code="counterpartyProfile.about.empty"/>
                        </c:otherwise>
                    </c:choose>
                </p>
                <c:if test="${not empty memberSinceDisplay}">
                    <p class="counterparty-member-since mb-0">
                        <span class="text-secondary"><spring:message code="profile.memberSince"/></span>
                        <span class="fw-semibold"><c:out value="${memberSinceDisplay}"/></span>
                    </p>
                </c:if>
            </div>
        </div>
    </div>
</section>

<section class="counterparty-section-card counterparty-reviews-card card border-0 shadow-sm rounded-4 mt-4">
    <div class="card-body p-4">
        <h2 class="h5 fw-semibold mb-3">
            <spring:message code="profile.documents.sectionTitle"/>
        </h2>
        <ul class="list-unstyled mb-0">
            <li class="mb-2">
                <c:choose>
                    <c:when test="${licenseValidated}">
                        <i class="bi bi-check-circle-fill text-success" aria-hidden="true"></i>
                        <span class="visually-hidden"><spring:message code="profile.documents.status.validated"/></span>
                    </c:when>
                    <c:otherwise>
                        <i class="bi bi-x-circle-fill text-danger" aria-hidden="true"></i>
                        <span class="visually-hidden"><spring:message code="profile.documents.status.notValidated"/></span>
                    </c:otherwise>
                </c:choose>
                <span class="fw-semibold ms-1"><spring:message code="profile.documents.license"/></span>
            </li>
            <li>
                <c:choose>
                    <c:when test="${identityValidated}">
                        <i class="bi bi-check-circle-fill text-success" aria-hidden="true"></i>
                        <span class="visually-hidden"><spring:message code="profile.documents.status.validated"/></span>
                    </c:when>
                    <c:otherwise>
                        <i class="bi bi-x-circle-fill text-danger" aria-hidden="true"></i>
                        <span class="visually-hidden"><spring:message code="profile.documents.status.notValidated"/></span>
                    </c:otherwise>
                </c:choose>
                <span class="fw-semibold ms-1"><spring:message code="profile.documents.identity"/></span>
            </li>
        </ul>
    </div>
</section>

