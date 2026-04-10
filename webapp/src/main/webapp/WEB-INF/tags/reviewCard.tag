<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ attribute name="avatarUrl" required="true" type="java.lang.String" %>
<%@ attribute name="userName" required="true" type="java.lang.String" %>
<%@ attribute name="fullStars" required="true" type="java.lang.Integer" %>
<%@ attribute name="halfStar" required="false" type="java.lang.Boolean" %>
<%@ attribute name="quoteText" required="true" type="java.lang.String" %>

<c:if test="${halfStar eq null}">
    <c:set var="halfStar" value="${false}" />
</c:if>

<div class="reviewCard border rounded-4 p-3 bg-white h-100 shadow-sm">
    <div class="d-flex align-items-start gap-3 mb-3">
        <img src="<c:out value='${avatarUrl}'/>" alt="" width="48" height="48" class="rounded-circle object-fit-cover flex-shrink-0 reviewCard__avatar">
        <div class="min-w-0 flex-grow-1">
            <p class="fw-bold mb-2 mb-md-1"><c:out value="${userName}"/></p>
            <ryden:reviewStarsRow fullStars="${fullStars}" halfStar="${halfStar}" />
        </div>
    </div>
    <p class="reviewCard__quote fst-italic text-secondary mb-0 small">&ldquo;<c:out value="${quoteText}"/>&rdquo;</p>
</div>
