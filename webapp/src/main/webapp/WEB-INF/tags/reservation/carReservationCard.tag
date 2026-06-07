<%@ tag language="java" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="reservation" required="true" type="ar.edu.itba.paw.models.dto.reservation.ReservationCardDisplayRow" %>
<%@ attribute name="href" required="true" type="java.lang.String" %>
<%@ attribute name="showRefundBadge" required="false" type="java.lang.Boolean" %>

<a href="<c:out value='${href}'/>" class="reservation-card text-decoration-none text-reset">
    <article class="card border-0 shadow-sm rounded-4 overflow-hidden reservation-card__surface position-relative">
        <div class="position-absolute top-0 end-0 m-2 z-1 d-flex flex-column align-items-end gap-2"
             style="max-width: 55%;">
            <span class="badge ${reservation.statusKey eq 'accepted' ? 'bg-success' : fn:startsWith(reservation.statusKey, 'cancelled') ? 'bg-danger' : reservation.statusKey eq 'started' ? 'bg-info' : reservation.statusKey eq 'pending' ? 'bg-warning text-dark' : 'bg-secondary'}">
                <spring:message code="enum.reservation.status.${reservation.statusKey}"/>
            </span>
            <c:if test="${showRefundBadge}">
                <span class="d-inline-flex align-items-center gap-2 text-end small fw-semibold"
                      style="background-color:#b91c1c; color:#ffffff; padding:.4rem .75rem; border-radius:.5rem; max-width:100%; white-space:normal; word-break:break-word; line-height:1.25;">
                    <i class="bi bi-cash-coin flex-shrink-0" aria-hidden="true"></i>
                    <span><spring:message code="ownerReservations.badge.refundProofPending"/></span>
                </span>
            </c:if>
        </div>
        <div class="row g-0 align-items-stretch">
            <div class="col-12 col-md-3 reservation-card__media-wrap">
                <c:choose>
                    <c:when test="${reservation.imageId > 0}">
                        <c:url var="reservationImgUrl" value="/image/${reservation.imageId}"/>
                        <img src="<c:out value='${reservationImgUrl}'/>" alt="<c:out value='${reservation.brand} ${reservation.model}'/>" class="reservation-card__media">
                    </c:when>
                    <c:otherwise>
                        <div class="reservation-card__media reservation-card__media--placeholder d-flex align-items-center justify-content-center text-secondary">
                            <i class="bi bi-car-front fs-1" aria-hidden="true"></i>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="col-12 col-md-9 min-w-0">
                <div class="card-body p-3 p-md-4 h-100 d-flex flex-column justify-content-between gap-3">
                    <div class="d-flex flex-wrap align-items-start gap-2">
                        <div class="min-w-0 flex-grow-1">
                            <h3 class="h5 fw-semibold mb-1 ryden-text-break"><c:out value="${reservation.brand} ${reservation.model}"/></h3>
                        </div>
                    </div>

                    <div class="row g-3">
                        <div class="col-12 col-sm-6">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservations.card.pickup"/></p>
                            <p class="mb-0 fw-medium"><c:out value="${reservation.pickupDateTime}"/></p>
                        </div>
                        <div class="col-12 col-sm-6">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservations.card.return"/></p>
                            <p class="mb-0 fw-medium"><c:out value="${reservation.returnDateTime}"/></p>
                        </div>
                    </div>

                    <div class="pt-1">
                        <div class="reservation-price-compact">
                            <span class="reservation-card__meta-label mb-0"><spring:message code="myReservations.card.totalPrice"/></span>
                            <span class="h5 mb-0 fw-bold text-primary"><c:out value="${reservation.totalPrice}"/></span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </article>
</a>
