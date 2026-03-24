<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="seeAllHref" required="false" type="java.lang.String" %>
<%@ attribute name="title" required="false" type="java.lang.String" %>
<%@ attribute name="subtitle" required="false" type="java.lang.String" %>

<c:if test="${empty seeAllHref}">
    <c:set var="seeAllHref" value="${pageContext.request.contextPath}/search" />
</c:if>
<c:if test="${empty title}">
    <c:set var="title" value="Similar vehicles" />
</c:if>
<c:if test="${empty subtitle}">
    <c:set var="subtitle" value="Explore other premium options in the area." />
</c:if>

<div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4 similarVehiclesHeader">
    <div>
        <h2 class="h5 fw-bold mb-1">${title}</h2>
        <p class="text-secondary small mb-0">${subtitle}</p>
    </div>
    <a href="${seeAllHref}" class="d-inline-flex align-items-center gap-1 text-decoration-none fw-semibold similarVehiclesSeeAll">
        See all
        <i class="bi bi-arrow-right" aria-hidden="true"></i>
    </a>
</div>
