<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="reviews" required="false" type="java.util.List" %>

<section class="counterparty-section-card counterparty-reviews-card card border-0 shadow-sm rounded-4">
    <div class="card-body p-4">
        <div class="mb-3">
            <h2 class="h5 fw-semibold mb-1"><spring:message code="counterpartyProfile.reviews.recentHeading"/></h2>
        </div>

        <c:choose>
            <c:when test="${not empty reviews}">
                <ul class="list-group list-group-flush">
                    <c:forEach var="review" items="${reviews}">
                        <c:if test="${not empty review.comment.orElse('')}">
                            <li class="list-group-item px-0 py-3 bg-transparent">
                                <div class="d-flex flex-column gap-2">
                                    <div class="d-flex align-items-center justify-content-between">
                                        <div>
                                            <p class="fw-semibold mb-1"><c:out value="${review.reviewerName}"/></p>
                                            <div class="d-inline-flex align-items-center gap-1">
                                                <c:forEach begin="1" end="5" var="star">
                                                    <c:choose>
                                                        <c:when test="${star <= review.rating}">
                                                            <i class="bi bi-star-fill text-warning" aria-hidden="true" style="font-size: 0.75rem;"></i>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <i class="bi bi-star text-secondary-subtle" aria-hidden="true" style="font-size: 0.75rem;"></i>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:forEach>
                                            </div>
                                        </div>
                                        <span class="text-secondary small"><c:out value="${review.reviewDate}"/></span>
                                    </div>
                                    <p class="mb-0 text-secondary ryden-multiline-plaintext"><c:out value="${review.comment.orElse('')}"/></p>
                                </div>
                            </li>
                        </c:if>
                    </c:forEach>
                </ul>
            </c:when>
            <c:otherwise>
                <p class="mb-0 text-secondary small">
                    <spring:message code="counterpartyProfile.reviews.empty"/>
                </p>
            </c:otherwise>
        </c:choose>
    </div>
</section>

