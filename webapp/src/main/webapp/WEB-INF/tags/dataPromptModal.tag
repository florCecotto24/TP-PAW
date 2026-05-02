<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%--
  Reusable "prompt for data" modal: intro (optional body or plain bodyText), one text field, error line, cancel/confirm.
  Composes with ryden:modal. Example (CBU before publish):
    <ryden:dataPromptModal id="publishMissingCbuModal" title="..." openButtonId="rydenOpen..." fieldId="..." ...>
        <p class="mb-3 text-secondary">...</p>
    </ryden:dataPromptModal>
--%>
<%@ attribute name="id" required="true" rtexprvalue="true" description="Id of the modal overlay; used for data-modal-open on the hidden trigger" %>
<%@ attribute name="title" required="true" rtexprvalue="true" %>
<%@ attribute name="fieldId" required="true" rtexprvalue="true" description="id (and for=) of the main input" %>
<%@ attribute name="fieldLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="cancelLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="confirmLabel" required="true" rtexprvalue="true" %>
<%@ attribute name="errorId" required="true" rtexprvalue="true" description="id for the error paragraph (hidden until shown by script)" %>
<%@ attribute name="confirmId" required="false" rtexprvalue="true" description="Primary action button id for script hooks" %>
<%@ attribute name="openButtonId" required="false" rtexprvalue="true" description="Hidden trigger button id" %>
<%@ attribute name="includeOpenTrigger" required="false" type="java.lang.Boolean" description="Render the hidden data-modal-open trigger" %>
<%@ attribute name="inputType" required="false" rtexprvalue="true" %>
<%@ attribute name="maxlength" required="false" rtexprvalue="true" %>
<%@ attribute name="inputmode" required="false" rtexprvalue="true" %>
<%@ attribute name="inputPattern" required="false" rtexprvalue="true" %>
<%@ attribute name="autocomplete" required="false" rtexprvalue="true" %>
<%@ attribute name="formControlClass" required="false" rtexprvalue="true" description="Input cssClass; default form-control" %>
<%@ attribute name="size" required="false" rtexprvalue="true" %>
<%@ attribute name="variant" required="false" rtexprvalue="true" %>
<%@ attribute name="showCloseButton" required="false" type="java.lang.Boolean" description="Top-right X; default true" %>
<%@ attribute name="confirmButtonClass" required="false" rtexprvalue="true" %>
<%@ attribute name="cancelButtonClass" required="false" rtexprvalue="true" %>
<%@ attribute name="digitsOnly" required="false" type="java.lang.Boolean"
              description="When true, sets data-ryden-digits-only for components.js (digits + maxlength cap)" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>

<c:set var="icType" value="${not empty inputType ? inputType : 'text'}" />
<c:set var="fcClass" value="${not empty formControlClass ? formControlClass : 'form-control'}" />
<c:set var="confirmCls" value="${not empty confirmButtonClass ? confirmButtonClass : 'btn btn-primary'}" />
<c:set var="cancelCls" value="${not empty cancelButtonClass ? cancelButtonClass : 'btn btn-secondary'}" />
<c:set var="modalClosable" value="${showCloseButton ne null ? showCloseButton : true}" />
<c:set var="modalSize" value="${not empty size ? size : 'md'}" />
<c:set var="modalVar" value="${not empty variant ? variant : 'default'}" />
<c:set var="doOpenTrigger" value="${includeOpenTrigger ne null and includeOpenTrigger and not empty openButtonId}" />
<c:set var="doDigitsOnly" value="${digitsOnly ne null and digitsOnly}" />

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
    <label for="${fieldId}" class="form-label"><c:out value="${fieldLabel}"/></label>
    <c:set var="acValue" value="${not empty autocomplete ? autocomplete : 'off'}" />
    <c:choose>
        <c:when test="${doDigitsOnly}">
            <c:choose>
                <c:when test="${not empty maxlength and not empty inputmode and not empty inputPattern}">
                    <input type="${icType}" class="${fcClass}" id="${fieldId}" name="${fieldId}" maxlength="${maxlength}"
                           inputmode="${inputmode}" pattern="${inputPattern}" autocomplete="${acValue}"
                           data-ryden-digits-only="true"/>
                </c:when>
                <c:when test="${not empty maxlength and not empty inputmode}">
                    <input type="${icType}" class="${fcClass}" id="${fieldId}" name="${fieldId}" maxlength="${maxlength}"
                           inputmode="${inputmode}" autocomplete="${acValue}" data-ryden-digits-only="true"/>
                </c:when>
                <c:when test="${not empty maxlength}">
                    <input type="${icType}" class="${fcClass}" id="${fieldId}" name="${fieldId}" maxlength="${maxlength}"
                           autocomplete="${acValue}" data-ryden-digits-only="true"/>
                </c:when>
                <c:otherwise>
                    <input type="${icType}" class="${fcClass}" id="${fieldId}" name="${fieldId}" autocomplete="${acValue}"
                           data-ryden-digits-only="true"/>
                </c:otherwise>
            </c:choose>
        </c:when>
        <c:otherwise>
            <c:choose>
                <c:when test="${not empty maxlength and not empty inputmode and not empty inputPattern}">
                    <input type="${icType}" class="${fcClass}" id="${fieldId}" name="${fieldId}" maxlength="${maxlength}"
                           inputmode="${inputmode}" pattern="${inputPattern}" autocomplete="${acValue}"/>
                </c:when>
                <c:when test="${not empty maxlength and not empty inputmode}">
                    <input type="${icType}" class="${fcClass}" id="${fieldId}" name="${fieldId}" maxlength="${maxlength}"
                           inputmode="${inputmode}" autocomplete="${acValue}"/>
                </c:when>
                <c:when test="${not empty maxlength}">
                    <input type="${icType}" class="${fcClass}" id="${fieldId}" name="${fieldId}" maxlength="${maxlength}"
                           autocomplete="${acValue}"/>
                </c:when>
                <c:otherwise>
                    <input type="${icType}" class="${fcClass}" id="${fieldId}" name="${fieldId}" autocomplete="${acValue}"/>
                </c:otherwise>
            </c:choose>
        </c:otherwise>
    </c:choose>
    <p id="${errorId}" class="text-danger small mt-2 mb-0 d-none" role="alert"></p>
    <div class="d-flex justify-content-end gap-2 mt-4">
        <button type="button" class="${cancelCls}" data-modal-close="${id}"><c:out value="${cancelLabel}"/></button>
        <button type="button" class="${confirmCls}" <c:if test="${not empty confirmId}">id="${confirmId}"</c:if>><c:out value="${confirmLabel}"/></button>
    </div>
</ryden:modal>
