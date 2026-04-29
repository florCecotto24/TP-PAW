<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="comments" required="false" type="java.util.List" %>

<section class="counterparty-section-card counterparty-reviews-card card border-0 shadow-sm rounded-4">
    <div class="card-body p-4">
        <div class="mb-3">
            <h2 class="h5 fw-semibold mb-1">Recent reviews</h2>
        </div>

        <c:choose>
            <c:when test="${not empty comments}">
                <ul class="list-group list-group-flush">
                    <c:forEach var="comment" items="${comments}">
                        <li class="list-group-item px-0"><c:out value="${comment}"/></li>
                    </c:forEach>
                </ul>
            </c:when>
            <c:otherwise>
                <p class="mb-0 text-secondary small"><spring:message code="reviewCard.noComment"/></p>
            </c:otherwise>
        </c:choose>
    </div>
</section>

