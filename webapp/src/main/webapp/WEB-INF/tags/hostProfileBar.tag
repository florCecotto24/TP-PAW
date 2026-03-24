<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ attribute name="hostName" required="true" type="java.lang.String" %>
<%@ attribute name="avatarUrl" required="true" type="java.lang.String" %>
<%@ attribute name="responseHint" required="true" type="java.lang.String" %>

<div class="host-profile-bar d-flex align-items-center justify-content-between flex-wrap gap-3 rounded-4 p-3 mt-4">
    <div class="d-flex align-items-center gap-3">
        <img src="${avatarUrl}" alt="" width="48" height="48" class="rounded-circle object-fit-cover flex-shrink-0">
        <div>
            <p class="mb-0 fw-semibold">Host: ${hostName}</p>
            <p class="mb-0 small text-secondary d-flex align-items-center gap-1">
                <i class="bi bi-lightning-charge-fill text-primary" aria-hidden="true"></i>
                ${responseHint}
            </p>
        </div>
    </div>
    <paw:button text="Contact" size="sm" type="primary" cssClass="btn-outline-secondary detail-contact-btn" id="detailContactBtn" />
</div>
