<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="fullStars" required="true" type="java.lang.Integer" %>
<%@ attribute name="halfStar" required="false" type="java.lang.Boolean" %>

<c:if test="${halfStar eq null}">
    <c:set var="halfStar" value="${false}" />
</c:if>

<c:set var="extraHalf" value="0" />
<c:if test="${halfStar}">
    <c:set var="extraHalf" value="1" />
</c:if>
<c:set var="emptyCount" value="${5 - fullStars - extraHalf}" />

<div class="reviewStarsRow d-inline-flex align-items-center gap-0" role="img" aria-label="Rating ${fullStars} out of 5">
    <c:forEach begin="1" end="${fullStars}" var="ignored">
        <i class="bi bi-star-fill text-primary" aria-hidden="true"></i>
    </c:forEach>
    <c:if test="${halfStar}">
        <i class="bi bi-star-half text-primary" aria-hidden="true"></i>
    </c:if>
    <c:if test="${emptyCount > 0}">
        <c:forEach begin="1" end="${emptyCount}" var="ignored2">
            <i class="bi bi-star text-primary" aria-hidden="true"></i>
        </c:forEach>
    </c:if>
</div>
