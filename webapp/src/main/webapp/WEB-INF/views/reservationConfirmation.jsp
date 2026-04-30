<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@include file="header.jsp"%>
    <title><spring:message code="reservationConfirmation.title"/></title>
</head>
<body class="bg-light has-fixed-navbar">
<ryden:navbar/>

<main class="container py-5 reservation-confirmation">
    <spring:message code="navbar.myReservations" var="myReservationsLabel"/>
    <spring:message code="reservationConfirmation.title" var="reservationConfirmLabel"/>
    <ryden:breadcrumbTrail homeLabel="${myReservationsLabel}" homeHref="${pageContext.request.contextPath}/my-reservations" currentLabel="${reservationConfirmLabel}"/>
    <div class="row justify-content-center">
        <div class="col-md-9 col-lg-7">
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-body p-4 p-md-5">
                    <div class="text-center mb-4">
                        <h1 class="h3 fw-bold mb-3"><spring:message code="reservationConfirmation.heading"/></h1>
                        <p class="mb-2">
                            <spring:message code="reservationConfirmation.greeting.before"/>
                            <c:out value="${name}"/> <c:out value="${surname}"/><spring:message code="reservationConfirmation.greeting.after"/>
                        </p>
                        <p class="text-secondary mb-3">
                            <spring:message code="reservationConfirmation.contactMessage.beforeCar"/>
                            <strong><c:out value="${carName}"/></strong>
                            <spring:message code="reservationConfirmation.contactMessage.afterCar"/>
                            <strong><c:out value="${email}"/></strong>
                            <spring:message code="reservationConfirmation.contactMessage.afterEmail"/>
                        </p>

                        <div class="small text-secondary mx-auto reservation-confirmation__intro" style="max-width: 32rem;">
                            <p class="mb-2"><spring:message code="reservationConfirmation.paymentProofNotice" arguments="${paymentProofUploadDeadlineHours}"/></p>
                            <p class="mb-0"><spring:message code="reservationConfirmation.viewReservation.hint"/></p>
                        </div>
                    </div>

                    <section class="reservation-confirmation__payment border rounded-3 p-3 p-md-4 bg-light-subtle mx-auto mb-4">
                        <h2 class="h6 fw-semibold mb-3 text-center"><spring:message code="myReservationDetail.payment.title"/></h2>
                        <spring:message code="validation.paymentReceipt.fileTooLarge" arguments="${uploadMaxImageMegabytes}" var="paymentReceiptTooLargeMsg"/>
                        <spring:message code="myReservationDetail.payment.invalidFile" var="paymentInvalidFileMsg"/>
                        <spring:message code="reservationConfirmation.paymentReceipt.chooseHint" var="paymentReceiptChooseHint"/>
                        <spring:message code="reservationConfirmation.paymentReceipt.uploadAria" var="paymentReceiptUploadAria"/>

                        <article class="card border-0 shadow-sm rounded-4 mb-4">
                            <div class="card-body p-4">
                                <h2 class="h5 fw-semibold mb-3"><spring:message code="myReservationDetail.paymentInfo.title"/></h2>
                                <div class="row g-3">
                                    <div class="col-sm-6">
                                        <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationDetail.paymentInfo.totalPrice"/></p>
                                        <p class="mb-0 fw-medium text-primary h5">$<c:out value="${reservationTotal}"/></p>
                                    </div>
                                    <div class="col-sm-6">
                                        <p class="reservation-card__meta-label mb-1"><spring:message code="myReservationDetail.paymentInfo.ownerCbu"/></p>
                                        <p class="mb-0 fw-medium"><c:out value="${ownerCbu}"/></p>
                                    </div>
                                </div>
                            </div>
                        </article>

                        <form id="paymentReceiptForm" method="post" enctype="multipart/form-data"
                              action="${pageContext.request.contextPath}/my-reservations/${reservationId}/payment-receipt"
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
                                           data-upload-max-image-bytes="<c:out value='${uploadMaxImageBytes}'/>"
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
                        <p class="small text-muted mt-3 mb-0 text-center"><spring:message code="myReservationDetail.payment.hint" arguments="${uploadMaxImageMegabytes}"/></p>
                    </section>

                    <div class="d-flex flex-wrap justify-content-center align-items-center gap-2">
                        <c:url var="riderReservationDetailUrl" value="/my-reservations/${reservationId}">
                            <c:param name="role" value="rider"/>
                        </c:url>
                        <a href="${riderReservationDetailUrl}" class="btn btn-sm btn-outline-primary px-3">
                            <spring:message code="reservationConfirmation.viewReservation"/>
                        </a>
                        <c:url var="listingUrl" value="/car-detail">
                            <c:param name="listingId" value="${listingId}"/>
                        </c:url>
                        <a href="<c:out value='${listingUrl}'/>" class="btn btn-sm btn-outline-secondary px-3">
                            <spring:message code="reservationConfirmation.viewListing"/>
                        </a>
                    </div>
                </div>
            </div>
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
        if (!form || !input || !fileText) return;
        var defaultHint = (fileText.textContent || '').trim();
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
                fileText.textContent = f.name;
                fileText.classList.remove('text-muted');
                if (submitBtn) { submitBtn.disabled = false; }
            } else {
                fileText.textContent = defaultHint;
                fileText.classList.add('text-muted');
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
