<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%--
  Two file inputs + error line + cancel/confirm (same layout as dataPromptModal / CBU publish flow).
  Each slot can show either a file input (still needed) or an on-file confirmation when already uploaded.
--%>
<%@ attribute name="id" required="true" rtexprvalue="true" %>
<%@ attribute name="title" required="true" rtexprvalue="true" %>
<%@ attribute name="licenseInputId" required="true" rtexprvalue="true" %>
<%@ attribute name="identityInputId" required="true" rtexprvalue="true" %>
<%@ attribute name="licenseLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="identityLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="uploadedSlotMessage" required="true" rtexprvalue="true" %>
<%@ attribute name="licensePending" required="false" type="java.lang.Boolean"
              description="When false, license row shows uploaded state instead of file input" %>
<%@ attribute name="identityPending" required="false" type="java.lang.Boolean"
              description="When false, identity row shows uploaded state instead of file input" %>
<%@ attribute name="cancelLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="confirmLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="errorId" required="true" rtexprvalue="true" %>
<%@ attribute name="confirmId" required="false" rtexprvalue="true" %>
<%@ attribute name="openButtonId" required="false" rtexprvalue="true" %>
<%@ attribute name="includeOpenTrigger" required="false" type="java.lang.Boolean" %>
<%@ attribute name="showCloseButton" required="false" type="java.lang.Boolean" %>
<%@ attribute name="confirmButtonClass" required="false" rtexprvalue="true" %>
<%@ attribute name="cancelButtonClass" required="false" rtexprvalue="true" %>
<%@ attribute name="size" required="false" rtexprvalue="true" %>
<%@ attribute name="variant" required="false" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>

<c:set var="confirmCls" value="${not empty confirmButtonClass ? confirmButtonClass : 'btn btn-primary'}" />
<c:set var="cancelCls" value="${not empty cancelButtonClass ? cancelButtonClass : 'btn btn-secondary'}" />
<c:set var="modalClosable" value="${showCloseButton ne null ? showCloseButton : true}" />
<c:set var="modalSize" value="${not empty size ? size : 'md'}" />
<c:set var="modalVar" value="${not empty variant ? variant : 'default'}" />
<c:set var="doOpenTrigger" value="${includeOpenTrigger ne null and includeOpenTrigger and not empty openButtonId}" />

<c:if test="${doOpenTrigger}">
    <button type="button" id="${openButtonId}" class="d-none" data-modal-open="${id}" aria-hidden="true" tabindex="-1">open</button>
</c:if>

<ryden:modal
        id="${id}"
        title="${title}"
        size="${modalSize}"
        variant="${modalVar}"
        showFooter="false"
        closable="${modalClosable}">
    <jsp:doBody />
    <div id="${id}-license-pending" class="mb-3<c:if test="${licensePending eq false}"> d-none</c:if>">
        <label for="${licenseInputId}" class="form-label"><c:out value="${licenseLabel}"/></label>
        <input type="file" class="form-control" id="${licenseInputId}" name="${licenseInputId}"
               accept="image/*,application/pdf"/>
    </div>
    <div id="${id}-license-done" class="mb-3<c:if test="${licensePending ne false}"> d-none</c:if>">
        <p class="form-label fw-semibold mb-1"><c:out value="${licenseLabel}"/></p>
        <p class="text-success small mb-0">
            <span class="bi bi-check-circle-fill me-1" aria-hidden="true"></span>
            <c:out value="${uploadedSlotMessage}"/>
        </p>
    </div>
    <div id="${id}-identity-pending" class="mb-3<c:if test="${identityPending eq false}"> d-none</c:if>">
        <label for="${identityInputId}" class="form-label"><c:out value="${identityLabel}"/></label>
        <input type="file" class="form-control" id="${identityInputId}" name="${identityInputId}"
               accept="image/*,application/pdf"/>
    </div>
    <div id="${id}-identity-done" class="mb-2<c:if test="${identityPending ne false}"> d-none</c:if>">
        <p class="form-label fw-semibold mb-1"><c:out value="${identityLabel}"/></p>
        <p class="text-success small mb-0">
            <span class="bi bi-check-circle-fill me-1" aria-hidden="true"></span>
            <c:out value="${uploadedSlotMessage}"/>
        </p>
    </div>
    <p id="${errorId}" class="text-danger small mt-2 mb-0 d-none" role="alert"></p>
    <div class="d-flex justify-content-end gap-2 mt-4">
        <button type="button" class="${cancelCls}" data-modal-close="${id}"><c:out value="${cancelLabel}"/></button>
        <button type="button" class="${confirmCls}" <c:if test="${not empty confirmId}">id="${confirmId}"</c:if>><c:out value="${confirmLabel}"/></button>
    </div>
</ryden:modal>
