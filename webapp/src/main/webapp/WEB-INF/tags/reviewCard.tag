<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="forename" required="true" type="java.lang.String" %>
<%@ attribute name="surname" required="true" type="java.lang.String" %>
<%@ attribute name="dateLabel" required="true" type="java.lang.String" %>
<%@ attribute name="rating" required="true" type="java.lang.Integer" %>
<%@ attribute name="comment" required="false" type="java.lang.String" %>

<article class="card border-0 shadow-sm rounded-4 h-100 listing-review-card">
    <div class="card-body p-4 d-flex flex-column gap-2">
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-2">
            <div>
                <p class="fw-semibold mb-0">
                    <c:out value="${forename}"/> <c:out value="${surname}"/>
                </p>
                <p class="text-secondary small mb-0"><c:out value="${dateLabel}"/></p>
            </div>
            <div class="d-inline-flex align-items-center gap-1 text-secondary" aria-label="Rating">
                <c:forEach begin="1" end="5" var="star">
                    <c:choose>
                        <c:when test="${star <= rating}">
                            <i class="bi bi-star-fill text-warning" aria-hidden="true"></i>
                        </c:when>
                        <c:otherwise>
                            <i class="bi bi-star text-secondary-subtle" aria-hidden="true"></i>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </div>
        </div>
        <c:choose>
            <c:when test="${not empty comment}">
                <p class="mb-0 small ryden-multiline-plaintext"><c:out value="${comment}"/></p>
            </c:when>
            <c:otherwise>
                <p class="mb-0 small text-secondary"><spring:message code="reviewCard.noComment"/></p>
            </c:otherwise>
        </c:choose>
    </div>
</article>
