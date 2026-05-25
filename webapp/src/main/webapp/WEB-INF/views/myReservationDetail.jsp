<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="myReservationDetail.pageTitle" arguments="${listingTitle}"/></title>
    <%@include file="header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <c:choose>
        <c:when test="${not empty reservationDetailOwnerCarHubId}">
            <%-- 4-level: My Cars > Reservations for my cars > Brand Model > #reservationId --%>
            <spring:message code="navbar.myListings" var="bcHomeLabel"/>
            <spring:message code="ownerReservations.heading" var="bcMidLabel"/>
            <c:url var="bcMid2Href" value="/my-cars/reservations/${reservationDetailOwnerCarHubId}"/>
            <ryden:breadcrumbTrail
                    homeLabel="${bcHomeLabel}"
                    homeHref="${pageContext.request.contextPath}/my-cars"
                    midLabel="${bcMidLabel}"
                    midHref="${pageContext.request.contextPath}/my-cars/reservations"
                    mid2Label="${car.brand} ${car.model}"
                    mid2Href="${bcMid2Href}"
                    currentLabel="#${reservation.id}"/>
        </c:when>
        <c:otherwise>
            <spring:message code="navbar.myReservations" var="myReservationsLabel"/>
            <ryden:breadcrumbTrail
                    homeLabel="${myReservationsLabel}"
                    homeHref="${pageContext.request.contextPath}/my-reservations"
                    currentLabel="#${reservation.id}"/>
        </c:otherwise>
    </c:choose>

    <c:if test="${not empty cancelReservationError}">
        <div class="alert alert-danger" role="alert"><c:out value="${cancelReservationError}"/></div>
    </c:if>
    <c:if test="${not empty paymentReceiptError}">
        <div class="alert alert-danger" role="alert"><c:out value="${paymentReceiptError}"/></div>
    </c:if>
    <c:if test="${not empty paymentApprovalMessage}">
        <div class="alert alert-success" role="alert"><c:out value="${paymentApprovalMessage}"/></div>
    </c:if>
    <c:if test="${not empty refundReceiptError}">
        <div class="alert alert-danger" role="alert"><c:out value="${refundReceiptError}"/></div>
    </c:if>
    <c:if test="${not empty refundApprovalMessage}">
        <div class="alert alert-success" role="alert"><c:out value="${refundApprovalMessage}"/></div>
    </c:if>
    <c:if test="${not empty carReturnedMessage}">
        <div class="alert alert-success" role="alert"><c:out value="${carReturnedMessage}"/></div>
    </c:if>
    <c:if test="${not empty carReturnedError}">
        <div class="alert alert-danger" role="alert"><c:out value="${carReturnedError}"/></div>
    </c:if>
    <c:if test="${not empty reviewMessage}">
        <div class="alert alert-success" role="alert"><c:out value="${reviewMessage}"/></div>
    </c:if>
    <c:if test="${not empty reviewError}">
        <div class="alert alert-danger" role="alert"><c:out value="${reviewError}"/></div>
    </c:if>

    <section class="reservation-management-header mb-4">
                        <h1 class="h3 fw-bold mb-2"><spring:message code="myReservationDetail.heading"/></h1>
        <p class="text-secondary mb-0"><spring:message code="myReservationDetail.subheading"/></p>
    </section>

    <div class="row g-4 align-items-start">
        <div class="col-lg-8">
            <c:if test="${statusKey eq 'pending' and reservationRole eq 'rider'}">
                <article class="card border border-warning shadow-sm rounded-4 mb-4 bg-warning-subtle">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold mb-3"><i class="bi bi-clock text-primary me-1" aria-hidden="true"></i><spring:message code="myReservationDetail.payment.title"/></h2>
                        <p class="text-secondary mb-3"><spring:message code="myReservationDetail.payment.intro"/></p>
                        <p class="mb-3"><span class="fw-semibold"><spring:message code="myReservationDetail.payment.cbu"/></span>
                            <c:out value="${cbu}"/></p>
                        <c:if test="${not empty paymentProofDeadlineDisplay}">
                            <p class="mb-3"><span class="fw-semibold"><spring:message code="myReservationDetail.payment.deadline"/></span>
                                <c:out value="${paymentProofDeadlineDisplay}"/></p>
                        </c:if>
                        <spring:message code="validation.paymentReceipt.fileTooLarge" arguments="${uploadMaxPaymentReceiptMegabytes}" var="paymentReceiptTooLargeMsg"/>
                        <spring:message code="myReservationDetail.payment.invalidFile" var="paymentInvalidFileMsg"/>
                        <spring:message code="reservationConfirmation.paymentReceipt.chooseHint" var="paymentReceiptChooseHint"/>
                        <spring:message code="reservationConfirmation.paymentReceipt.uploadAria" var="paymentReceiptUploadAria"/>
                        <form id="paymentReceiptForm" method="post" enctype="multipart/form-data"
                              action="${pageContext.request.contextPath}/my-reservations/${reservation.id}/payment-receipt"
                              class="ryden-payment-receipt__form">
                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                            <%@ include file="includes/fromCarHubHidden.jspf" %>
                            <div class="d-flex align-items-stretch gap-2">
                                <label class="form-control d-flex align-items-center mb-0 flex-grow-1 min-w-0 position-relative ryden-payment-receipt__file-label">
                                    <span id="paymentReceiptFileText" class="text-truncate text-muted pe-1 flex-grow-1 min-w-0"><c:out value="${paymentReceiptChooseHint}"/></span>
                                    <input type="file" class="position-absolute top-0 start-0 w-100 h-100 opacity-0 ryden-payment-receipt__file-input"
                                           id="paymentReceipt" name="paymentReceipt" required
                                           accept="image/*,application/pdf"
                                           aria-label="<c:out value='${paymentReceiptChooseHint}'/>"
                                           title="<c:out value='${paymentReceiptChooseHint}'/>"
                                           data-upload-max-image-bytes="<c:out value='${uploadMaxPaymentReceiptBytes}'/>"
                                           data-upload-receipt-too-large="<c:out value='${paymentReceiptTooLargeMsg}'/>"
                                           data-invalid-file-msg="<c:out value='${paymentInvalidFileMsg}'/>"/>
                                </label>
                                <button type="submit" id="paymentReceiptSubmit"
                                        class="btn btn-sm btn-primary flex-shrink-0 d-inline-flex align-items-center justify-content-center gap-1 px-2"
                                        aria-label="<c:out value='${paymentReceiptUploadAria}'/>"
                                        title="<c:out value='${paymentReceiptUploadAria}'/>"
                                        disabled>
                                    <i class="bi bi-cloud-arrow-up" aria-hidden="true"></i>
                                    <spring:message code="myReservationDetail.payment.submit"/>
                                </button>
                            </div>
                            <div id="paymentReceiptClientErr" class="text-danger small d-none mt-2" role="alert"></div>
                        </form>
                        <p class="small text-muted mt-2 mb-0"><spring:message code="myReservationDetail.payment.hint" arguments="${uploadMaxPaymentReceiptMegabytes}"/></p>
                    </div>
                </article>
            </c:if>

            <c:if test="${reservationRole eq 'rider' and hasPaymentReceipt}">
                <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                    <div class="card-body p-4 d-flex flex-wrap align-items-center justify-content-between gap-2">
                        <div>
                            <h2 class="h6 fw-semibold mb-1"><spring:message code="myReservationDetail.payment.viewReceipt"/></h2>
                            <p class="text-secondary small mb-0"><spring:message code="myReservationDetail.payment.hint" arguments="${uploadMaxPaymentReceiptMegabytes}"/></p>
                        </div>
                        <c:url var="paymentReceiptViewUrl" value="/my-reservations/${reservation.id}/payment-receipt/view"/>
                        <a class="btn btn-outline-primary" href="<c:out value='${paymentReceiptViewUrl}'/>" target="_blank" rel="noopener noreferrer">
                            <spring:message code="myReservationDetail.payment.viewReceipt"/>
                        </a>
                    </div>
                </article>
            </c:if>

            <c:if test="${reservationRole eq 'owner' and hasPaymentReceipt and (statusKey eq 'accepted' or statusKey eq 'started' or statusKey eq 'finished')}">
                <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                    <div class="card-body p-4">
                        <div class="d-flex align-items-center gap-2 mb-3">
                            <h2 class="h5 fw-semibold mb-0"><spring:message code="myReservationDetail.payment.ownerTitle"/></h2>
                            <c:if test="${paymentReceiptApproved}">
                                <span class="badge text-bg-success"><spring:message code="myReservationDetail.payment.reviewedBadge"/></span>
                            </c:if>
                        </div>
                        <c:url var="ownerReceiptViewUrl" value="/my-reservations/${reservation.id}/payment-receipt/view"/>
                        <c:url var="approvalUrl" value="/my-reservations/${reservation.id}/payment-receipt/approval"/>
                        <div class="d-flex gap-2 flex-wrap">
                            <c:if test="${not paymentReceiptApproved}">
                                <spring:message code="myReservationDetail.payment.markReviewed" var="paymentApproveBtnLabel"/>
                                <spring:message code="myReservationDetail.payment.modal.title" var="paymentApproveModalTitle"/>
                                <spring:message code="myReservationDetail.payment.modal.message" var="paymentApproveModalMessage"/>
                                <spring:message code="myReservationDetail.payment.modal.confirm" var="paymentApproveModalConfirm"/>
                                <spring:message code="myReservationDetail.payment.modal.back" var="paymentApproveModalBack"/>
                                <ryden:confirmModal
                                        id="paymentReceiptApprovalModal"
                                        title="${paymentApproveModalTitle}"
                                        message="${paymentApproveModalMessage}"
                                        action="${approvalUrl}"
                                        cancelLabel="${paymentApproveModalBack}"
                                        confirmLabel="${paymentApproveModalConfirm}"
                                        triggerLabel="${paymentApproveBtnLabel}"
                                        triggerClass="btn btn-primary"
                                        confirmButtonClass="btn btn-primary">
                                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                                    <%@ include file="includes/fromCarHubHidden.jspf" %>
                                </ryden:confirmModal>
                            </c:if>
                            <a class="btn btn-outline-primary" href="<c:out value='${ownerReceiptViewUrl}'/>" target="_blank" rel="noopener noreferrer">
                                <spring:message code="myReservationDetail.payment.viewReceipt"/>
                            </a>
                        </div>
                    </div>
                </article>
            </c:if>

            <c:if test="${reservationRole eq 'owner' and reservation.paymentRefundRequired and not hasRefundReceipt and (statusKey eq 'cancelled_by_owner' or statusKey eq 'cancelled_by_rider')}">
                <article class="card border-0 shadow-sm rounded-4 mb-4 border-warning bg-white">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold mb-3"><spring:message code="myReservationDetail.refund.ownerTitle"/></h2>
                        <p class="text-secondary mb-3"><spring:message code="myReservationDetail.refund.ownerIntro"/></p>
                        <c:if test="${not empty refundProofDeadlineDisplay}">
                            <p class="mb-3"><span class="fw-semibold"><spring:message code="myReservationDetail.refund.deadline"/></span>
                                <c:out value="${refundProofDeadlineDisplay}"/></p>
                        </c:if>
                        <spring:message code="validation.paymentReceipt.fileTooLarge" arguments="${uploadMaxPaymentReceiptMegabytes}" var="refundReceiptTooLargeMsg"/>
                        <spring:message code="myReservationDetail.refund.invalidFile" var="refundInvalidFileMsg"/>
                        <spring:message code="myReservationDetail.refund.chooseHint" var="refundReceiptChooseHint"/>
                        <spring:message code="myReservationDetail.refund.uploadAria" var="refundReceiptUploadAria"/>
                        <form id="refundReceiptForm" method="post" enctype="multipart/form-data"
                              action="${pageContext.request.contextPath}/my-reservations/${reservation.id}/refund-receipt"
                              class="ryden-payment-receipt__form">
                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                            <%@ include file="includes/fromCarHubHidden.jspf" %>
                            <div class="d-flex align-items-stretch gap-2">
                                <label class="form-control d-flex align-items-center mb-0 flex-grow-1 min-w-0 position-relative ryden-payment-receipt__file-label">
                                    <span id="refundReceiptFileText" class="text-truncate text-muted pe-1 flex-grow-1 min-w-0"><c:out value="${refundReceiptChooseHint}"/></span>
                                    <input type="file" class="position-absolute top-0 start-0 w-100 h-100 opacity-0 ryden-payment-receipt__file-input"
                                           id="refundReceipt" name="refundReceipt" required
                                           accept="image/*,application/pdf"
                                           aria-label="<c:out value='${refundReceiptChooseHint}'/>"
                                           title="<c:out value='${refundReceiptChooseHint}'/>"
                                           data-upload-max-image-bytes="<c:out value='${uploadMaxPaymentReceiptBytes}'/>"
                                           data-upload-receipt-too-large="<c:out value='${refundReceiptTooLargeMsg}'/>"
                                           data-invalid-file-msg="<c:out value='${refundInvalidFileMsg}'/>"/>
                                </label>
                                <button type="submit" id="refundReceiptSubmit"
                                        class="btn btn-sm btn-primary flex-shrink-0 d-inline-flex align-items-center justify-content-center gap-1 px-2"
                                        aria-label="<c:out value='${refundReceiptUploadAria}'/>"
                                        title="<c:out value='${refundReceiptUploadAria}'/>"
                                        disabled>
                                    <i class="bi bi-cloud-arrow-up" aria-hidden="true"></i>
                                    <spring:message code="myReservationDetail.refund.submit"/>
                                </button>
                            </div>
                            <div id="refundReceiptClientErr" class="text-danger small d-none mt-2" role="alert"></div>
                        </form>
                        <p class="small text-muted mt-2 mb-0"><spring:message code="myReservationDetail.refund.hint" arguments="${uploadMaxPaymentReceiptMegabytes}"/></p>
                    </div>
                </article>
            </c:if>

            <c:if test="${reservationRole eq 'rider' and reservation.paymentRefundRequired and not hasRefundReceipt and (statusKey eq 'cancelled_by_owner' or statusKey eq 'cancelled_by_rider')}">
                <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold mb-2"><spring:message code="myReservationDetail.refund.riderWaitingTitle"/></h2>
                        <p class="text-secondary small mb-0"><spring:message code="myReservationDetail.refund.riderWaitingIntro"/></p>
                        <c:if test="${not empty refundProofDeadlineDisplay}">
                            <p class="small text-muted mt-3 mb-0"><span class="fw-semibold"><spring:message code="myReservationDetail.refund.deadline"/></span>
                                <c:out value="${refundProofDeadlineDisplay}"/></p>
                        </c:if>
                    </div>
                </article>
            </c:if>

            <c:if test="${reservationRole eq 'owner' and hasRefundReceipt and reservation.paymentRefundRequired and (statusKey eq 'cancelled_by_owner' or statusKey eq 'cancelled_by_rider')}">
                <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                    <div class="card-body p-4 d-flex flex-wrap align-items-center justify-content-between gap-2">
                        <div>
                            <h2 class="h6 fw-semibold mb-1"><spring:message code="myReservationDetail.refund.ownerUploadedTitle"/></h2>
                            <p class="text-secondary small mb-0"><spring:message code="myReservationDetail.refund.hint" arguments="${uploadMaxPaymentReceiptMegabytes}"/></p>
                        </div>
                        <c:url var="ownerRefundReceiptViewUrl" value="/my-reservations/${reservation.id}/refund-receipt/view"/>
                        <a class="btn btn-outline-primary" href="<c:out value='${ownerRefundReceiptViewUrl}'/>" target="_blank" rel="noopener noreferrer">
                            <spring:message code="myReservationDetail.refund.viewReceipt"/>
                        </a>
                    </div>
                </article>
            </c:if>

            <c:if test="${reservationRole eq 'rider' and hasRefundReceipt and reservation.paymentRefundRequired and (statusKey eq 'cancelled_by_owner' or statusKey eq 'cancelled_by_rider')}">
                <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold mb-2"><spring:message code="myReservationDetail.refund.riderTitle"/></h2>
                        <p class="text-secondary small mb-3"><spring:message code="myReservationDetail.refund.riderIntro"/></p>
                        <div class="d-flex flex-wrap align-items-center gap-2 mb-3">
                            <c:url var="riderRefundViewUrl" value="/my-reservations/${reservation.id}/refund-receipt/view"/>
                            <a class="btn btn-outline-primary" href="<c:out value='${riderRefundViewUrl}'/>" target="_blank" rel="noopener noreferrer">
                                <spring:message code="myReservationDetail.refund.viewReceipt"/>
                            </a>
                            <c:if test="${refundReceiptApproved}">
                                <span class="badge text-bg-success"><spring:message code="myReservationDetail.refund.reviewedBadge"/></span>
                            </c:if>
                        </div>
                        <div class="d-flex flex-wrap gap-2">
                            <c:url var="refundApprovalUrl" value="/my-reservations/${reservation.id}/refund-receipt/approval"/>
                            <c:if test="${not refundReceiptApproved}">
                                <form method="post" action="<c:out value='${refundApprovalUrl}'/>" class="d-inline">
                                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                                    <%@ include file="includes/fromCarHubHidden.jspf" %>
                                    <input type="hidden" name="approved" value="true"/>
                                    <button type="submit" class="btn btn-primary btn-sm">
                                        <spring:message code="myReservationDetail.refund.markReviewed"/>
                                    </button>
                                </form>
                            </c:if>
                            <c:if test="${refundReceiptApproved}">
                                <form method="post" action="<c:out value='${refundApprovalUrl}'/>" class="d-inline">
                                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                                    <%@ include file="includes/fromCarHubHidden.jspf" %>
                                    <input type="hidden" name="approved" value="false"/>
                                    <button type="submit" class="btn btn-outline-secondary btn-sm">
                                        <spring:message code="myReservationDetail.refund.clearReview"/>
                                    </button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                </article>
            </c:if>

            <c:if test="${canOwnerMarkCarReturned}">
                <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold mb-2"><spring:message code="myReservationDetail.carReturned.title"/></h2>
                        <p class="text-secondary small mb-3"><spring:message code="myReservationDetail.carReturned.intro"/></p>
                        <spring:message code="myReservationDetail.carReturned.submit" var="carReturnedBtnLabel"/>
                        <spring:message code="myReservationDetail.carReturned.modal.title" var="carReturnedModalTitle"/>
                        <spring:message code="myReservationDetail.carReturned.modal.message" var="carReturnedModalMessage"/>
                        <spring:message code="myReservationDetail.carReturned.modal.confirm" var="carReturnedModalConfirm"/>
                        <spring:message code="myReservationDetail.carReturned.modal.back" var="carReturnedModalBack"/>
                        <c:url var="carReturnedUrl" value="/my-reservations/${reservation.id}/car-returned"/>
                        <ryden:confirmModal
                                id="carReturnedModal"
                                title="${carReturnedModalTitle}"
                                message="${carReturnedModalMessage}"
                                action="${carReturnedUrl}"
                                cancelLabel="${carReturnedModalBack}"
                                confirmLabel="${carReturnedModalConfirm}"
                                triggerLabel="${carReturnedBtnLabel}"
                                triggerClass="btn btn-primary"
                                confirmButtonClass="btn btn-primary">
                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                            <%@ include file="includes/fromCarHubHidden.jspf" %>
                        </ryden:confirmModal>
                    </div>
                </article>
            </c:if>
            <c:if test="${canOwnerReviewRider}">
                <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold mb-2"><spring:message code="myReservationDetail.reviewOwner.title"/></h2>
                        <p class="text-secondary small mb-3"><spring:message code="myReservationDetail.reviewOwner.intro"/></p>
                        <c:url var="ownerReviewUrl" value="/my-reservations/${reservation.id}/owner-review-rider"/>
                        <form:form modelAttribute="ownerReviewForm" action="${ownerReviewUrl}" method="post"
                                   cssClass="vstack gap-2" htmlEscape="true">
                            <%@ include file="includes/csrfHidden.jspf" %>
                            <%@ include file="includes/fromCarHubHidden.jspf" %>
                            <form:errors path="*" element="div" cssClass="alert alert-danger mb-0 py-2" delimiter=" "/>
                            <label class="form-label small"><spring:message code="myReservationDetail.review.rating"/></label>
                            <div class="ryden-star-rating" data-target="ownerReviewRating">
                                <span class="ryden-star" data-value="1">&#9733;</span>
                                <span class="ryden-star" data-value="2">&#9733;</span>
                                <span class="ryden-star" data-value="3">&#9733;</span>
                                <span class="ryden-star" data-value="4">&#9733;</span>
                                <span class="ryden-star" data-value="5">&#9733;</span>
                            </div>
                            <form:hidden path="rating" id="ownerReviewRating"/>
                            <label class="form-label small" for="ownerReviewComment"><spring:message code="myReservationDetail.review.commentOptional"/></label>
                            <form:textarea path="comment" id="ownerReviewComment" cssClass="form-control"
                                           rows="3" maxlength="${reviewCommentMaxLength}"/>
                            <div class="d-flex gap-2">
                                <button type="submit" name="reviewAction" value="SUBMIT" class="btn btn-primary btn-sm">
                                    <spring:message code="myReservationDetail.review.submit"/></button>
                                <button type="submit" name="reviewAction" value="OMIT" class="btn btn-outline-secondary btn-sm">
                                    <spring:message code="myReservationDetail.review.skip"/></button>
                            </div>
                        </form:form>
                    </div>
                </article>
            </c:if>

            <c:if test="${canRiderReviewOwner}">
                <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white" id="rider-review-owner">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold mb-2"><spring:message code="myReservationDetail.reviewRider.title"/></h2>
                        <p class="text-secondary small mb-3"><spring:message code="myReservationDetail.reviewRider.intro"/></p>
                        <c:url var="riderReviewUrl" value="/my-reservations/${reservation.id}/rider-review-owner"/>
                        <form:form modelAttribute="riderReviewForm" action="${riderReviewUrl}" method="post"
                                   cssClass="vstack gap-2" htmlEscape="true">
                            <%@ include file="includes/csrfHidden.jspf" %>
                            <%@ include file="includes/fromCarHubHidden.jspf" %>
                            <form:errors path="*" element="div" cssClass="alert alert-danger mb-0 py-2" delimiter=" "/>
                            <label class="form-label small"><spring:message code="myReservationDetail.review.rating"/></label>
                            <div class="ryden-star-rating" data-target="riderReviewRating">
                                <span class="ryden-star" data-value="1">&#9733;</span>
                                <span class="ryden-star" data-value="2">&#9733;</span>
                                <span class="ryden-star" data-value="3">&#9733;</span>
                                <span class="ryden-star" data-value="4">&#9733;</span>
                                <span class="ryden-star" data-value="5">&#9733;</span>
                            </div>
                            <form:hidden path="rating" id="riderReviewRating"/>
                            <label class="form-label small" for="riderReviewComment"><spring:message code="myReservationDetail.review.commentOptional"/></label>
                            <form:textarea path="comment" id="riderReviewComment" cssClass="form-control"
                                           rows="3" maxlength="${reviewCommentMaxLength}"/>
                            <div class="d-flex gap-2">
                                <button type="submit" name="reviewAction" value="SUBMIT" class="btn btn-primary btn-sm">
                                    <spring:message code="myReservationDetail.review.submit"/></button>
                                <button type="submit" name="reviewAction" value="OMIT" class="btn btn-outline-secondary btn-sm">
                                    <spring:message code="myReservationDetail.review.skip"/></button>
                            </div>
                        </form:form>
                    </div>
                </article>
            </c:if>

            <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                <div class="card-body p-4">
                    <h2 class="h5 fw-semibold mb-3"><spring:message code="myReservationDetail.carSummary.title"/></h2>
                    <div class="d-flex flex-column flex-md-row gap-3 align-items-start">
                        <div class="reservation-detail-car-media rounded-3 overflow-hidden border">
                            <c:choose>
                                <c:when test="${carImageId > 0}">
                                    <c:url var="carImageUrl" value="/image/${carImageId}"/>
                                    <img src="<c:out value='${carImageUrl}'/>" alt="<c:out value='${car.brand} ${car.model}'/>" class="w-100 h-100" style="object-fit:cover;">
                                </c:when>
                                <c:otherwise>
                                    <div class="w-100 h-100 d-flex align-items-center justify-content-center text-secondary bg-body-tertiary">
                                        <i class="bi bi-car-front fs-1" aria-hidden="true"></i>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                        <div>
                            <h3 class="h5 mb-1"><c:out value="${car.brand} ${car.model}"/></h3>
                            <p class="text-secondary mb-2"><c:out value="${listingTitle}"/></p>
                            <div class="d-flex flex-wrap gap-2">
                                <spring:message code="enum.car.transmission.${car.transmission.name()}" var="carTransmissionLabel"/>
                                <spring:message code="enum.car.powertrain.${car.powertrain.name()}" var="carPowertrainLabel"/>
                                <span class="badge text-bg-light border"><c:out value="${carTransmissionLabel}"/></span>
                                <span class="badge text-bg-light border"><c:out value="${carPowertrainLabel}"/></span>
                            </div>
                        </div>
                    </div>
                </div>
            </article>

            <article class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
                <div class="card-body p-4">
                    <h2 class="h5 fw-semibold mb-3"><spring:message code="myReservationDetail.details.title"/></h2>
                    <div class="row g-3">
                        <div class="col-sm-6">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationDetail.details.pickup"/></p>
                            <p class="mb-0 fw-medium"><c:out value="${pickupDateTime}"/></p>
                        </div>
                        <div class="col-sm-6">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationDetail.details.return"/></p>
                            <p class="mb-0 fw-medium"><c:out value="${returnDateTime}"/></p>
                        </div>
                        <div class="col-12">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationDetail.details.pickupReturnLocation"/></p>
                            <p class="mb-0 fw-medium"><c:out value="${reservationPickupLocationDisplay}"/></p>
                        </div>
                        <div class="col-sm-6">
                            <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationDetail.details.status"/></p>
                            <p class="mb-0 fw-medium"><spring:message code="enum.reservation.status.${statusKey}"/></p>
                        </div>
                    </div>
                </div>
            </article>
        </div>

        <div class="col-lg-4">
            <article class="card border-0 shadow-sm rounded-4 reservation-detail-sticky bg-white">
                <div class="card-body p-4">
                    <div class="reservation-price-compact mb-3">
                        <span class="reservation-card__meta-label mb-0"><spring:message code="myReservationDetail.totalPrice"/></span>
                        <span class="h2 fw-bold text-primary mb-0"><c:out value="${totalPrice}"/></span>
                    </div>

                    <c:if test="${chatAvailable}">
                        <spring:message code="myReservationDetail.chat.open" var="chatOpenLabel"/>
                        <c:url var="chatUrl" value="/my-reservations/${reservation.id}/chat">
                            <c:param name="role" value="${reservationRole}"/>
                            <c:if test="${not empty reservationDetailOwnerCarHubId}">
                                <c:param name="fromCar" value="${reservationDetailOwnerCarHubId}"/>
                            </c:if>
                        </c:url>
                        <a href="<c:out value='${chatUrl}'/>" class="btn btn-primary w-100 mb-3">
                            <c:out value="${chatOpenLabel}"/>
                        </a>
                    </c:if>

                    <c:url var="counterpartyProfileUrl" value="/my-reservations/${reservation.id}/counterparty-profile">
                        <c:param name="role"><c:out value="${reservationRole}"/></c:param>
                        <c:if test="${not empty reservationDetailOwnerCarHubId}">
                            <c:param name="fromCar" value="${reservationDetailOwnerCarHubId}"/>
                        </c:if>
                    </c:url>
                    <spring:message code="myReservationDetail.counterparty.viewFullProfile" var="counterpartyProfileLinkAria"/>
                    <section class="card border-0 shadow-sm rounded-4 mb-3 counterparty-summary-card" aria-labelledby="counterparty-summary-heading">
                        <div class="card-body p-4">
                            <h2 id="counterparty-summary-heading" class="h6 fw-semibold mb-3">
                                <c:choose>
                                    <c:when test="${reservationRole eq 'rider'}">
                                        <spring:message code="myReservationDetail.counterparty.ownerTitle"/>
                                    </c:when>
                                    <c:otherwise>
                                        <spring:message code="myReservationDetail.counterparty.riderTitle"/>
                                    </c:otherwise>
                                </c:choose>
                            </h2>
                            <div class="counterparty-summary-card__identity d-flex align-items-center gap-2 mb-3">
                                <a href="<c:out value='${counterpartyProfileUrl}'/>"
                                   class="d-flex align-items-center gap-2 text-decoration-none text-reset flex-grow-1 min-w-0"
                                   aria-label="<c:out value='${counterpartyProfileLinkAria}'/>">
                                    <c:choose>
                                        <c:when test="${counterpartyProfileImageId != null}">
                                            <c:url var="counterpartySummaryImageUrl" value="/image/${counterpartyProfileImageId}"/>
                                            <img src="${counterpartySummaryImageUrl}"
                                                 alt=""
                                                 class="rounded-circle border flex-shrink-0 counterparty-summary-card__avatar"/>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="d-inline-flex align-items-center justify-content-center rounded-circle border bg-white text-secondary flex-shrink-0 counterparty-summary-card__avatar counterparty-summary-card__avatar--placeholder"
                                                  aria-hidden="true">
                                                <i class="bi bi-person-fill"></i>
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                    <span class="fw-semibold text-decoration-underline text-break flex-grow-1 min-w-0">
                                        <c:out value="${counterparty.forename}"/> <c:out value="${counterparty.surname}"/>
                                    </span>
                                </a>
                            </div>
                            <div class="vstack gap-3 small">
                                <div>
                                    <div class="reservation-card__meta-label mb-1">
                                        <spring:message code="profile.email"/>
                                    </div>
                                    <div class="counterparty-summary-card__email-wrap">
                                        <a href="mailto:<c:out value='${counterparty.email}'/>"
                                           class="counterparty-summary-card__email-link link-primary text-decoration-underline">
                                            <c:out value="${counterparty.email}"/>
                                        </a>
                                    </div>
                                </div>
                                <div>
                                    <div class="reservation-card__meta-label mb-1">
                                        <spring:message code="profile.phone"/>
                                    </div>
                                    <div class="text-break">
                                        <c:choose>
                                            <c:when test="${not empty counterpartyPhoneDisplay}">
                                                <a href="tel:<c:out value='${counterpartyPhoneDisplay}'/>" class="link-body text-decoration-underline">
                                                    <c:out value="${counterpartyPhoneDisplay}"/>
                                                </a>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-secondary"><spring:message code="myReservationDetail.counterparty.fieldNotProvided"/></span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>
                    <c:url var="listingUrl" value="/car-detail">
                        <c:param name="carId"><c:out value="${reservation.carId}"/></c:param>
                    </c:url>
                    <div class="d-flex flex-column gap-2">
                        <a href="<c:out value='${listingUrl}'/>" class="btn btn-outline-warm w-100">
                            <i class="bi bi-eye me-2"></i><spring:message code="myReservationDetail.actions.viewListing"/>
                        </a>
                        <c:set var="canCancel" value="${canCancelReservation}"/>
                        <c:url var="cancelUrl" value="/my-reservations/${reservation.id}/cancel"/>
                        <c:choose>
                            <c:when test="${canCancel}">
                                <spring:message code="myReservationDetail.actions.cancel" var="cancelBtnLabel"/>
                                <spring:message code="myReservationDetail.cancelModal.title" var="cancelModalTitle"/>
                                <spring:message code="myReservationDetail.cancelModal.message" var="cancelModalMessage"/>
                                <spring:message code="myReservationDetail.cancelModal.confirm" var="cancelModalConfirm"/>
                                <spring:message code="myReservationDetail.cancelModal.back" var="cancelModalBack"/>
                                <button type="button" class="btn btn-outline-danger w-100" data-modal-open="cancelReservationModal">
                                    <i class="bi bi-x-circle me-2"></i><c:out value="${cancelBtnLabel}"/>
                                </button>
                                <ryden:confirmModal
                                        id="cancelReservationModal"
                                        title="${cancelModalTitle}"
                                        message="${cancelModalMessage}"
                                        action="${cancelUrl}"
                                        cancelLabel="${cancelModalBack}"
                                        confirmLabel="${cancelModalConfirm}"
                                        variant="danger"
                                        confirmButtonClass="btn btn-danger">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                    <input type="hidden" name="role" value="${reservationRole}"/>
                                    <%@ include file="includes/fromCarHubHidden.jspf" %>
                                </ryden:confirmModal>
                            </c:when>
                            <c:otherwise>
                                <c:if test="${(statusKey eq 'pending' and reservationRole eq 'owner') or fn:startsWith(statusKey, 'cancelled') or statusKey eq 'started' or statusKey eq 'finished'}">
                                    <div class="alert alert-info mb-0" role="alert">
                                        <c:choose>
                                            <c:when test="${statusKey eq 'pending' and reservationRole eq 'owner'}">
                                                <p class="mb-0"><spring:message code="myReservationDetail.alert.pendingPaymentOwner"/></p>
                                            </c:when>
                                            <c:when test="${fn:startsWith(statusKey, 'cancelled')}">
                                                <p class="mb-0"><spring:message code="myReservationDetail.alert.cancelled"/></p>
                                            </c:when>
                                            <c:when test="${statusKey eq 'started'}">
                                                <p class="mb-0"><spring:message code="myReservationDetail.alert.inProgress"/></p>
                                            </c:when>
                                            <c:when test="${statusKey eq 'finished'}">
                                                <p class="mb-0"><spring:message code="myReservationDetail.alert.finished"/></p>
                                            </c:when>
                                        </c:choose>
                                    </div>
                                </c:if>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </article>
        </div>
    </div>
</main>
<script>
    (function () {
        var form = document.getElementById('paymentReceiptForm');
        var input = document.getElementById('paymentReceipt');
        var err = document.getElementById('paymentReceiptClientErr');
        var fileText = document.getElementById('paymentReceiptFileText');
        var submitBtn = document.getElementById('paymentReceiptSubmit');
        if (!form || !input) return;
        var defaultHint = fileText ? (fileText.textContent || '').trim() : '';
        function maxBytes() {
            var raw = input.getAttribute('data-upload-max-image-bytes');
            var n = raw ? parseInt(raw, 10) : NaN;
            return isNaN(n) ? 0 : n;
        }
        function showErr(msg) {
            if (!err) return;
            err.textContent = msg || '';
            err.classList.toggle('d-none', !msg);
        }
        function validateFile(file) {
            if (!file) return '';
            var t = (file.type || '').toLowerCase();
            var okType = t.indexOf('image/') === 0 || t === 'application/pdf';
            if (!okType) {
                return input.getAttribute('data-invalid-file-msg') || '';
            }
            var mb = maxBytes();
            if (mb > 0 && file.size > mb) {
                return input.getAttribute('data-upload-receipt-too-large') || '';
            }
            return '';
        }
        input.addEventListener('change', function () {
            showErr('');
            var f = input.files && input.files[0];
            if (f) {
                if (fileText) { fileText.textContent = f.name; fileText.classList.remove('text-muted'); }
                if (submitBtn) { submitBtn.disabled = false; }
            } else {
                if (fileText) { fileText.textContent = defaultHint; fileText.classList.add('text-muted'); }
                if (submitBtn) { submitBtn.disabled = true; }
            }
        });
        form.addEventListener('submit', function (e) {
            showErr('');
            var f = input.files && input.files[0];
            var msg = validateFile(f);
            if (msg) {
                e.preventDefault();
                showErr(msg);
            }
        });
    })();
    (function () {
        var form = document.getElementById('refundReceiptForm');
        var input = document.getElementById('refundReceipt');
        var err = document.getElementById('refundReceiptClientErr');
        var fileText = document.getElementById('refundReceiptFileText');
        var submitBtn = document.getElementById('refundReceiptSubmit');
        if (!form || !input) return;
        var defaultHint = fileText ? (fileText.textContent || '').trim() : '';
        function maxBytes() {
            var raw = input.getAttribute('data-upload-max-image-bytes');
            var n = raw ? parseInt(raw, 10) : NaN;
            return isNaN(n) ? 0 : n;
        }
        function showErr(msg) {
            if (!err) return;
            err.textContent = msg || '';
            err.classList.toggle('d-none', !msg);
        }
        function validateFile(file) {
            if (!file) return '';
            var t = (file.type || '').toLowerCase();
            var okType = t.indexOf('image/') === 0 || t === 'application/pdf';
            if (!okType) {
                return input.getAttribute('data-invalid-file-msg') || '';
            }
            var mb = maxBytes();
            if (mb > 0 && file.size > mb) {
                return input.getAttribute('data-upload-receipt-too-large') || '';
            }
            return '';
        }
        input.addEventListener('change', function () {
            showErr('');
            var f = input.files && input.files[0];
            if (f) {
                if (fileText) { fileText.textContent = f.name; fileText.classList.remove('text-muted'); }
                if (submitBtn) { submitBtn.disabled = false; }
            } else {
                if (fileText) { fileText.textContent = defaultHint; fileText.classList.add('text-muted'); }
                if (submitBtn) { submitBtn.disabled = true; }
            }
        });
        form.addEventListener('submit', function (e) {
            showErr('');
            var f = input.files && input.files[0];
            var msg = validateFile(f);
            if (msg) {
                e.preventDefault();
                showErr(msg);
            }
        });
    })();
</script>

<%@include file="footer.jsp"%>
</body>
</html>


