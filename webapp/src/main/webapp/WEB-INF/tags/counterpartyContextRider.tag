<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>

<%@ attribute name="reservationDetailUrl" required="false" type="java.lang.String" %>

<c:if test="${empty reservationDetailUrl}">
    <c:set var="reservationDetailUrl" value="#"/>
</c:if>

<aside class="card border-0 shadow-sm rounded-4 counterparty-context-card">
    <div class="card-body p-4">
        <h3 class="h6 fw-semibold mb-3">Upcoming reservation</h3>
        <div class="counterparty-context-vehicle">
            <ryden:counterpartyContextCarImage
                    imageUrl="${pageContext.request.contextPath}/assets/images/mercedes-exterior.png"
                    altText="Jeep Renegade"/>
            <div>
                <div class="fw-semibold">Jeep Renegade</div>
                <div class="counterparty-tag-row">
                    <ryden:counterpartyContextTag label="Automatic"/>
                    <ryden:counterpartyContextTag label="Gasoline"/>
                </div>
            </div>
        </div>
        <div class="counterparty-context-details">
            <ryden:counterpartyContextDateRow label="Pickup" value="Apr 30, 2026 · 10:00" iconClass="bi-calendar-check"/>
            <ryden:counterpartyContextDateRow label="Return" value="May 02, 2026 · 16:00" iconClass="bi-calendar2-check"/>
            <div class="counterparty-context-row">
                <i class="bi bi-geo-alt" aria-hidden="true"></i>
                <div>
                    <div class="counterparty-context-row__label">Location</div>
                    <div class="counterparty-context-row__value">Palermo, Buenos Aires</div>
                </div>
            </div>
        </div>
        <a href="${reservationDetailUrl}" class="btn btn-primary w-100">View reservation details</a>
    </div>
</aside>

