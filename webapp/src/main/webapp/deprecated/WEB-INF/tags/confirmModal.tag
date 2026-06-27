<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%--
  Reusable POST confirmation modal: message, optional body for hidden fields, cancel/submit.
  Composes with ryden:modal. Omit triggerLabel when the page provides its own data-modal-open button.
--%>
<%@ attribute name="id" required="true" rtexprvalue="true" %>
<%@ attribute name="title" required="true" rtexprvalue="true" %>
<%@ attribute name="message" required="true" rtexprvalue="true" %>
<%@ attribute name="action" required="true" rtexprvalue="true" %>
<%@ attribute name="cancelLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="confirmLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="triggerLabel" required="false" rtexprvalue="true" %>
<%@ attribute name="triggerClass" required="false" rtexprvalue="true" %>
<%@ attribute name="variant" required="false" rtexprvalue="true" %>
<%@ attribute name="confirmButtonClass" required="false" rtexprvalue="true" %>
<%@ attribute name="cancelButtonClass" required="false" rtexprvalue="true" %>
<%@ attribute name="size" required="false" rtexprvalue="true" %>
<%@ attribute name="method" required="false" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>

<c:set var="formMethod" value="${not empty method ? method : 'post'}" />
<c:set var="confirmCls" value="${not empty confirmButtonClass ? confirmButtonClass : 'btn btn-primary'}" />
<c:set var="cancelCls" value="${not empty cancelButtonClass ? cancelButtonClass : 'btn btn-secondary'}" />
<c:set var="modalVar" value="${not empty variant ? variant : 'default'}" />
<c:set var="modalSize" value="${not empty size ? size : 'md'}" />
<c:set var="triggerCls" value="${not empty triggerClass ? triggerClass : 'btn btn-primary'}" />

<c:if test="${not empty triggerLabel}">
    <button type="button" class="${triggerCls}" data-modal-open="${id}">
        <c:out value="${triggerLabel}"/>
    </button>
</c:if>

<ryden:modal
        id="${id}"
        title="${title}"
        message="${message}"
        size="${modalSize}"
        variant="${modalVar}"
        showFooter="false">
    <form method="${formMethod}" action="<c:out value='${action}'/>">
        <jsp:doBody />
        <div class="d-flex justify-content-end gap-2 mt-3">
            <button type="button" class="${cancelCls}" data-modal-close="${id}">
                <c:out value="${cancelLabel}"/>
            </button>
            <button type="submit" class="${confirmCls}">
                <c:out value="${confirmLabel}"/>
            </button>
        </div>
    </form>
</ryden:modal>
