<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%@ attribute name="forename" required="true" type="java.lang.String" %>
<%@ attribute name="surname" required="true" type="java.lang.String" %>
<%@ attribute name="about" required="false" type="java.lang.String" %>
<%@ attribute name="memberSinceDisplay" required="false" type="java.lang.String" %>
<%@ attribute name="profileImageId" required="false" type="java.lang.Long" %>
<%@ attribute name="badgeLabel" required="false" type="java.lang.String" %>
<%@ attribute name="ratingValue" required="false" type="java.lang.String" %>
<%@ attribute name="reviewCount" required="false" type="java.lang.String" %>

<c:if test="${empty badgeLabel}">
    <c:set var="badgeLabel" value="Verified Owner"/>
</c:if>
<c:if test="${empty ratingValue}">
    <c:set var="ratingValue" value="4.8"/>
</c:if>
<c:if test="${empty reviewCount}">
    <c:set var="reviewCount" value="18"/>
</c:if>
<c:set var="initials" value="${fn:substring(forename, 0, 1)}${fn:substring(surname, 0, 1)}"/>

<section class="card border-0 shadow-sm rounded-4 counterparty-section-card counterparty-header-card">
    <div class="card-body p-4">
        <div class="d-flex flex-column flex-md-row align-items-start gap-4">
            <div class="counterparty-avatar">
                <c:choose>
                    <c:when test="${profileImageId != null && profileImageId > 0}">
                        <img src="${pageContext.request.contextPath}/image/${profileImageId}" alt="${forename} ${surname}" class="counterparty-avatar__img"/>
                    </c:when>
                    <c:otherwise>
                        <div class="counterparty-avatar__placeholder">
                            <span><c:out value="${initials}"/></span>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="flex-grow-1">
                <div class="d-flex flex-wrap align-items-center gap-2">
                    <h1 class="h4 fw-semibold mb-0"><c:out value="${forename} ${surname}"/></h1>
                    <span class="badge counterparty-badge"><c:out value="${badgeLabel}"/></span>
                </div>
                <div class="d-flex align-items-center gap-2 mt-2">
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
                <p class="counterparty-about mt-3 mb-2">
                    <c:choose>
                        <c:when test="${not empty about}">
                            <c:out value="${about}"/>
                        </c:when>
                        <c:otherwise>
                            No description provided yet.
                        </c:otherwise>
                    </c:choose>
                </p>
                <c:if test="${not empty memberSinceDisplay}">
                    <p class="counterparty-member-since mb-0">
                        <span class="text-secondary">Member since</span>
                        <span class="fw-semibold"><c:out value="${memberSinceDisplay}"/></span>
                    </p>
                </c:if>
            </div>
        </div>
    </div>
</section>

