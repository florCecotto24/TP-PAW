<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>

<%@ attribute name="viewAllListingsUrl" required="false" type="java.lang.String" %>

<c:if test="${empty viewAllListingsUrl}">
    <c:set var="viewAllListingsUrl" value="#"/>
</c:if>

<aside class="card border-0 shadow-sm rounded-4 counterparty-context-card">
    <div class="card-body p-4">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <h3 class="h6 fw-semibold mb-0">Listings</h3>
            <a href="${viewAllListingsUrl}" class="counterparty-link">View all listings</a>
        </div>

        <div class="counterparty-context-list">
            <div class="counterparty-context-item">
                <ryden:counterpartyContextCarImage
                        imageUrl="${pageContext.request.contextPath}/assets/images/mercedes-interior.png"
                        altText="Toyota Corolla"/>
                <div class="counterparty-context-item__body">
                    <div class="d-flex align-items-center justify-content-between gap-2">
                        <div class="fw-semibold">Toyota Corolla</div>
                        <ryden:counterpartyContextPriceChip label="$58 / day"/>
                    </div>
                    <div class="counterparty-tag-row">
                        <ryden:counterpartyContextTag label="Automatic"/>
                        <ryden:counterpartyContextTag label="Hybrid"/>
                    </div>
                </div>
            </div>

            <div class="counterparty-context-item">
                <ryden:counterpartyContextCarImage
                        imageUrl="${pageContext.request.contextPath}/assets/images/mercedes-rear-view.png"
                        altText="Ford Bronco"/>
                <div class="counterparty-context-item__body">
                    <div class="d-flex align-items-center justify-content-between gap-2">
                        <div class="fw-semibold">Ford Bronco</div>
                        <ryden:counterpartyContextPriceChip label="$82 / day"/>
                    </div>
                    <div class="counterparty-tag-row">
                        <ryden:counterpartyContextTag label="Manual"/>
                        <ryden:counterpartyContextTag label="Diesel"/>
                    </div>
                </div>
            </div>

            <div class="counterparty-context-item">
                <ryden:counterpartyContextCarImage
                        imageUrl="${pageContext.request.contextPath}/assets/images/mercedes-exterior.png"
                        altText="Mini Cooper"/>
                <div class="counterparty-context-item__body">
                    <div class="d-flex align-items-center justify-content-between gap-2">
                        <div class="fw-semibold">Mini Cooper</div>
                        <ryden:counterpartyContextPriceChip label="$69 / day"/>
                    </div>
                    <div class="counterparty-tag-row">
                        <ryden:counterpartyContextTag label="Automatic"/>
                        <ryden:counterpartyContextTag label="Gasoline"/>
                    </div>
                </div>
            </div>
        </div>
    </div>
</aside>

