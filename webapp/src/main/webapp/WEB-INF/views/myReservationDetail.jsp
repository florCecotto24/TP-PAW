<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="myReservationDetail.pageTitle" arguments="${listing.title}"/></title>
    <%@include file="header.jsp"%>
</head>
<body class="has-fixed-navbar bg-light">
<ryden:navbar/>

<main class="container pt-5 pb-4">
    <spring:message code="navbar.myReservations" var="myReservationsLabel"/>
    <ryden:breadcrumbTrail
            homeLabel="${myReservationsLabel}"
            homeHref="${pageContext.request.contextPath}/my-reservations"
            currentLabel="${listing.title}"/>

    <c:if test="${not empty cancelReservationError}">
        <div class="alert alert-danger" role="alert"><c:out value="${cancelReservationError}"/></div>
    </c:if>
    <c:if test="${not empty paymentReceiptError}">
        <div class="alert alert-danger" role="alert"><c:out value="${paymentReceiptError}"/></div>
    </c:if>
    <c:if test="${not empty paymentApprovalMessage}">
        <div class="alert alert-success" role="alert"><c:out value="${paymentApprovalMessage}"/></div>
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
                <article class="card border-0 shadow-sm rounded-4 mb-4 border-warning">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold mb-3"><spring:message code="myReservationDetail.payment.title"/></h2>
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
                <article class="card border-0 shadow-sm rounded-4 mb-4">
                    <div class="card-body p-4 d-flex flex-wrap align-items-center justify-content-between gap-2">
                        <div>
                            <h2 class="h6 fw-semibold mb-1"><spring:message code="myReservationDetail.payment.viewReceipt"/></h2>
                            <p class="text-secondary small mb-0"><spring:message code="myReservationDetail.payment.hint" arguments="${uploadMaxPaymentReceiptMegabytes}"/></p>
                        </div>
                        <c:url var="paymentReceiptDownloadUrl" value="/my-reservations/${reservation.id}/payment-receipt/download"/>
                        <a class="btn btn-outline-primary" href="<c:out value='${paymentReceiptDownloadUrl}'/>" target="_blank" rel="noopener noreferrer">
                            <spring:message code="myReservationDetail.payment.viewReceipt"/>
                        </a>
                    </div>
                </article>
            </c:if>

            <c:if test="${reservationRole eq 'owner' and hasPaymentReceipt and statusKey eq 'accepted'}">
                <article class="card border-0 shadow-sm rounded-4 mb-4">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold mb-2"><spring:message code="myReservationDetail.payment.ownerTitle"/></h2>
                        <p class="text-secondary small mb-3"><spring:message code="myReservationDetail.payment.ownerIntro"/></p>
                        <div class="d-flex flex-wrap align-items-center gap-2 mb-3">
                            <c:url var="ownerReceiptDownloadUrl" value="/my-reservations/${reservation.id}/payment-receipt/download"/>
                            <a class="btn btn-outline-primary" href="<c:out value='${ownerReceiptDownloadUrl}'/>" target="_blank" rel="noopener noreferrer">
                                <spring:message code="myReservationDetail.payment.viewReceipt"/>
                            </a>
                            <c:if test="${paymentReceiptApproved}">
                                <span class="badge text-bg-success"><spring:message code="myReservationDetail.payment.reviewedBadge"/></span>
                            </c:if>
                        </div>
                        <div class="d-flex flex-wrap gap-2">
                            <c:url var="approvalUrl" value="/my-reservations/${reservation.id}/payment-receipt/approval"/>
                            <c:if test="${not paymentReceiptApproved}">
                                <form method="post" action="<c:out value='${approvalUrl}'/>" class="d-inline">
                                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                                    <input type="hidden" name="approved" value="true"/>
                                    <button type="submit" class="btn btn-primary btn-sm">
                                        <spring:message code="myReservationDetail.payment.markReviewed"/>
                                    </button>
                                </form>
                            </c:if>
                            <c:if test="${paymentReceiptApproved}">
                                <form method="post" action="<c:out value='${approvalUrl}'/>" class="d-inline">
                                    <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                                    <input type="hidden" name="approved" value="false"/>
                                    <button type="submit" class="btn btn-outline-secondary btn-sm">
                                        <spring:message code="myReservationDetail.payment.clearReview"/>
                                    </button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                </article>
            </c:if>

            <c:if test="${canOwnerMarkCarReturned}">
                <article class="card border-0 shadow-sm rounded-4 mb-4">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold mb-2"><spring:message code="myReservationDetail.carReturned.title"/></h2>
                        <p class="text-secondary small mb-3"><spring:message code="myReservationDetail.carReturned.intro"/></p>
                        <c:url var="carReturnedUrl" value="/my-reservations/${reservation.id}/car-returned"/>
                        <form method="post" action="<c:out value='${carReturnedUrl}'/>">
                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                            <button type="submit" class="btn btn-primary"><spring:message code="myReservationDetail.carReturned.submit"/></button>
                        </form>
                    </div>
                </article>
            </c:if>

            <c:if test="${canOwnerReviewRider}">
                <article class="card border-0 shadow-sm rounded-4 mb-4">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold mb-2"><spring:message code="myReservationDetail.reviewOwner.title"/></h2>
                        <p class="text-secondary small mb-3"><spring:message code="myReservationDetail.reviewOwner.intro"/></p>
                        <c:url var="ownerReviewUrl" value="/my-reservations/${reservation.id}/owner-review-rider"/>
                        <form id="ownerSkipForm" method="post" action="<c:out value='${ownerReviewUrl}'/>">
                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                        </form>
                        <form method="post" action="<c:out value='${ownerReviewUrl}'/>" class="vstack gap-2">
                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                            <label class="form-label small"><spring:message code="myReservationDetail.review.rating"/></label>
                            <div class="ryden-star-rating" data-target="ownerReviewRating">
                                <span class="ryden-star" data-value="1">&#9733;</span>
                                <span class="ryden-star" data-value="2">&#9733;</span>
                                <span class="ryden-star" data-value="3">&#9733;</span>
                                <span class="ryden-star" data-value="4">&#9733;</span>
                                <span class="ryden-star" data-value="5">&#9733;</span>
                            </div>
                            <input type="hidden" name="rating" id="ownerReviewRating"/>
                            <label class="form-label small" for="ownerReviewComment"><spring:message code="myReservationDetail.review.commentOptional"/></label>
                            <textarea name="comment" id="ownerReviewComment" class="form-control" rows="3" maxlength="<c:out value='${reviewCommentMaxLength}'/>"></textarea>
                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary btn-sm"><spring:message code="myReservationDetail.review.submit"/></button>
                                <button type="submit" form="ownerSkipForm" class="btn btn-outline-secondary btn-sm"><spring:message code="myReservationDetail.review.skip"/></button>
                            </div>
                        </form>
                    </div>
                </article>
            </c:if>

            <c:if test="${canRiderReviewOwner}">
                <article class="card border-0 shadow-sm rounded-4 mb-4" id="rider-review-owner">
                    <div class="card-body p-4">
                        <h2 class="h5 fw-semibold mb-2"><spring:message code="myReservationDetail.reviewRider.title"/></h2>
                        <p class="text-secondary small mb-3"><spring:message code="myReservationDetail.reviewRider.intro"/></p>
                        <c:url var="riderReviewUrl" value="/my-reservations/${reservation.id}/rider-review-owner"/>
                        <form id="riderSkipForm" method="post" action="<c:out value='${riderReviewUrl}'/>">
                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                        </form>
                        <form method="post" action="<c:out value='${riderReviewUrl}'/>" class="vstack gap-2">
                            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>"/>
                            <label class="form-label small"><spring:message code="myReservationDetail.review.rating"/></label>
                            <div class="ryden-star-rating" data-target="riderReviewRating">
                                <span class="ryden-star" data-value="1">&#9733;</span>
                                <span class="ryden-star" data-value="2">&#9733;</span>
                                <span class="ryden-star" data-value="3">&#9733;</span>
                                <span class="ryden-star" data-value="4">&#9733;</span>
                                <span class="ryden-star" data-value="5">&#9733;</span>
                            </div>
                            <input type="hidden" name="rating" id="riderReviewRating"/>
                            <label class="form-label small" for="riderReviewComment"><spring:message code="myReservationDetail.review.commentOptional"/></label>
                            <textarea name="comment" id="riderReviewComment" class="form-control" rows="3" maxlength="<c:out value='${reviewCommentMaxLength}'/>"></textarea>
                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary btn-sm"><spring:message code="myReservationDetail.review.submit"/></button>
                                <button type="submit" form="riderSkipForm" class="btn btn-outline-secondary btn-sm"><spring:message code="myReservationDetail.review.skip"/></button>
                            </div>
                        </form>
                    </div>
                </article>
            </c:if>

            <article class="card border-0 shadow-sm rounded-4 mb-4">
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
                            <p class="text-secondary mb-2"><c:out value="${listing.title}"/></p>
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

            <article class="card border-0 shadow-sm rounded-4 mb-4">
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
            <article class="card border-0 shadow-sm rounded-4 reservation-detail-sticky">
                <div class="card-body p-4">
                    <div class="reservation-price-compact mb-3">
                        <span class="reservation-card__meta-label mb-0"><spring:message code="myReservationDetail.totalPrice"/></span>
                        <span class="h2 fw-bold text-primary mb-0">$<c:out value="${totalPrice}"/></span>
                    </div>

                    <div class="d-grid gap-2">
                        <!-- <button type="button" class="btn btn-primary" disabled>
                            <spring:message code="myReservationDetail.actions.modify"/>
                        </button> -->
                        <c:set var="canCancel"
                               value="${(reservation.status.name() eq 'PENDING' || reservation.status.name() eq 'ACCEPTED') and not hasPaymentReceipt}"/>
                        <c:url var="cancelUrl" value="/my-reservations/${reservation.id}/cancel"/>
                        <c:choose>
                            <c:when test="${canCancel}">
                                <spring:message code="myReservationDetail.actions.cancel" var="cancelBtnLabel"/>
                                <spring:message code="myReservationDetail.cancelModal.title" var="cancelModalTitle"/>
                                <spring:message code="myReservationDetail.cancelModal.message" var="cancelModalMessage"/>
                                <spring:message code="myReservationDetail.cancelModal.confirm" var="cancelModalConfirm"/>
                                <spring:message code="myReservationDetail.cancelModal.back" var="cancelModalBack"/>
                                <button type="button" class="btn btn-outline-danger" data-modal-open="cancelReservationModal">
                                    <c:out value="${cancelBtnLabel}"/>
                                </button>
                                <ryden:modal
                                    id="cancelReservationModal"
                                    title="${cancelModalTitle}"
                                    message="${cancelModalMessage}"
                                    variant="danger">
                                    <form:form method="post" action="${cancelUrl}">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                        <input type="hidden" name="role" value="${reservationRole}"/>
                                        <div class="d-flex justify-content-end gap-2 mt-3">
                                            <button type="button" class="btn btn-secondary" data-modal-close="cancelReservationModal">
                                                <c:out value="${cancelModalBack}"/>
                                            </button>
                                            <button type="submit" class="btn btn-danger">
                                                <c:out value="${cancelModalConfirm}"/>
                                            </button>
                                        </div>
                                    </form:form>
                                </ryden:modal>
                            </c:when>
                            <c:otherwise>
                                <c:if test="${not (statusKey eq 'pending' and reservationRole eq 'rider')}">
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
                        <!-- <c:if test="${not empty owner.email}">
                            <spring:message code="myReservationDetail.actions.contactOwner" var="contactOwnerLabel"/>
                            <a href="mailto:<c:out value='${owner.email}'/>" class="btn btn-outline-secondary">
                                <c:out value="${contactOwnerLabel}"/>
                            </a>
                        </c:if> -->
                    </div>

                    <hr class="my-4">
                    <c:url var="counterpartyProfileUrl" value="/my-reservations/${reservation.id}/counterparty-profile">
                        <c:param name="role"><c:out value="${reservationRole}"/></c:param>
                    </c:url>
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
                                <c:choose>
                                    <c:when test="${counterpartyProfileImageId != null}">
                                        <c:url var="counterpartySummaryImageUrl" value="/image/${counterpartyProfileImageId}"/>
                                        <spring:message code="myReservationDetail.counterparty.profileImageAlt" var="counterpartySummaryImageAlt"/>
                                        <img src="${counterpartySummaryImageUrl}"
                                             alt="${counterpartySummaryImageAlt}"
                                             class="rounded-circle border flex-shrink-0 counterparty-summary-card__avatar"/>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="d-inline-flex align-items-center justify-content-center rounded-circle border bg-white text-secondary flex-shrink-0 counterparty-summary-card__avatar counterparty-summary-card__avatar--placeholder"
                                              aria-hidden="true">
                                            <i class="bi bi-person-fill"></i>
                                        </span>
                                    </c:otherwise>
                                </c:choose>
                                <p class="fw-semibold mb-0 text-break flex-grow-1 min-w-0">
                                    <c:out value="${counterparty.forename}"/> <c:out value="${counterparty.surname}"/>
                                </p>
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
                            <a href="<c:out value='${counterpartyProfileUrl}'/>" class="btn btn-outline-primary w-100 mt-3">
                                <spring:message code="myReservationDetail.counterparty.viewFullProfile"/>
                            </a>
                        </div>
                    </section>
                    <c:url var="listingUrl" value="/car-detail">
                        <c:param name="listingId"><c:out value="${listing.id}"/></c:param>
                    </c:url>
                    <a href="<c:out value='${listingUrl}'/>" class="btn btn-light border w-100">
                        <spring:message code="myReservationDetail.actions.viewListing"/>
                    </a>
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
</script>

<%@include file="footer.jsp"%>
</body>
</html>


