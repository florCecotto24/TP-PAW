<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="message" required="false" %>
<%@ attribute name="size" required="false" %>
<%@ attribute name="variant" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="triggerLabel" required="false" %>
<%@ attribute name="triggerClass" required="false" %>
<%@ attribute name="confirmLabel" required="false" %>
<%@ attribute name="cancelLabel" required="false" %>
<%@ attribute name="confirmClass" required="false" %>
<%@ attribute name="cancelClass" required="false" %>
<%@ attribute name="open" required="false" type="java.lang.Boolean" %>
<%@ attribute name="closable" required="false" type="java.lang.Boolean" %>
<%@ attribute name="showFooter" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="modalSize" value="${not empty size ? size : 'md'}" />
<c:set var="modalVariant" value="${not empty variant ? variant : 'default'}" />
<c:set var="modalCssClass" value="${not empty cssClass ? cssClass : ''}" />
<c:set var="modalTriggerClass" value="${not empty triggerClass ? triggerClass : 'btn btn-primary btn-md'}" />
<c:set var="modalConfirmClass" value="${not empty confirmClass ? confirmClass : 'btn btn-primary btn-md'}" />
<c:set var="modalCancelClass" value="${not empty cancelClass ? cancelClass : 'btn btn-secondary btn-md'}" />
<c:set var="modalOpen" value="${open ne null ? open : false}" />
<c:set var="modalClosable" value="${closable ne null ? closable : true}" />
<c:set var="modalShowFooter" value="${showFooter ne null ? showFooter : true}" />
<c:set var="modalHasFooter" value="${modalShowFooter and (not empty cancelLabel or not empty confirmLabel)}" />
<c:set var="modalClasses" value="modal-overlay modal-overlay--${modalSize} modal-overlay--${modalVariant} ${modalCssClass}${modalOpen ? ' is-open' : ''}" />

<c:if test="${not empty triggerLabel}">
    <button type="button" class="${modalTriggerClass}" data-modal-open="<c:out value='${id}'/>">
        <c:out value="${triggerLabel}" />
    </button>
</c:if>

<div id="<c:out value='${id}'/>" class="<c:out value='${modalClasses}'/>" data-modal aria-hidden="${not modalOpen}">
    <div class="modal__backdrop" data-modal-close="<c:out value='${id}'/>"></div>
    <div class="modal__dialog" role="dialog" aria-modal="true" aria-labelledby="<c:out value='${id}'/><c:out value='-title'/>" tabindex="-1">
        <div class="modal__content">
            <c:if test="${modalClosable}">
                <spring:message code="modal.closeAriaLabel" var="closeModalAria"/>
                <button type="button" class="modal__close" aria-label="${closeModalAria}" data-modal-close="<c:out value='${id}'/>">
                    <span aria-hidden="true">&times;</span>
                </button>
            </c:if>

            <div class="modal__header">
                <h2 id="<c:out value='${id}'/><c:out value='-title'/>" class="modal__title">
                    <c:out value="${title}" />
                </h2>
            </div>

            <div class="modal__body">
                <c:if test="${not empty message}">
                    <p class="modal__message">
                        <c:out value="${message}" />
                    </p>
                </c:if>
                <jsp:doBody />
            </div>

            <c:if test="${modalHasFooter}">
                <div class="modal__footer">
                    <c:if test="${not empty cancelLabel}">
                        <button type="button" class="<c:out value='${modalCancelClass}'/>" data-modal-action="cancel" data-modal-close="<c:out value='${id}'/>">
                            <c:out value="${cancelLabel}" />
                        </button>
                    </c:if>
                    <c:if test="${not empty confirmLabel}">
                        <button type="button" class="<c:out value='${modalConfirmClass}'/>" data-modal-action="confirm" data-modal-close="<c:out value='${id}'/>">
                            <c:out value="${confirmLabel}" />
                        </button>
                    </c:if>
                </div>
            </c:if>
        </div>
    </div>
</div>
