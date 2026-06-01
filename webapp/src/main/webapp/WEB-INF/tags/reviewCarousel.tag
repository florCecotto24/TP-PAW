<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%--
  Reviews carousel for the public car detail page.
  Groups review cards into slides (2 per slide on md+, 1 per slide on small screens) and lets the user
  navigate slides client-side via Bootstrap. Server-side pagination still applies: the caller passes the
  first page of reviews (e.g. PaginationPolicy#getCarPublicReviewsPageSize) and the "view all"
  control on the surrounding section bumps the user into the paginated list view.
--%>
<%@ attribute name="reviews" required="true" type="java.util.List" %>
<%@ attribute name="id"      required="true" type="java.lang.String" %>

<c:if test="${not empty reviews}">
    <div id="<c:out value='${id}'/>" class="carousel slide ryden-review-carousel" data-bs-ride="false">
        <div class="carousel-inner">
            <c:forEach items="${reviews}" var="review" varStatus="status">
                <c:if test="${status.index % 2 == 0}">
                    <c:choose>
                        <c:when test="${status.first}">
                            <div class="carousel-item active">
                        </c:when>
                        <c:otherwise>
                            </div><div class="carousel-item">
                        </c:otherwise>
                    </c:choose>
                    <div class="row row-cols-1 row-cols-md-2 g-3 pb-2 align-items-stretch">
                </c:if>

                <div class="col d-flex">
                    <ryden:reviewCard
                            forename="${review.reviewerForename}"
                            surname="${review.reviewerSurname}"
                            dateLabel="${review.dateText}"
                            rating="${review.rating}"
                            comment="${review.comment}"
                            imageId="${review.imageId}"/>
                </div>

                <c:if test="${status.index % 2 == 1 || status.last}">
                    </div>
                </c:if>
            </c:forEach>
        </div>
    </div>
</c:if>
