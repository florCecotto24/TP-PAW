<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ attribute name="label" required="true" type="java.lang.String" %>
<%@ attribute name="value" required="true" type="java.lang.String" %>
<%@ attribute name="iconClass" required="false" type="java.lang.String" %>

<div class="counterparty-context-row">
    <c:if test="${not empty iconClass}">
        <i class="bi ${iconClass}" aria-hidden="true"></i>
    </c:if>
    <div>
        <div class="counterparty-context-row__label"><c:out value="${label}"/></div>
        <div class="counterparty-context-row__value"><c:out value="${value}"/></div>
    </div>
</div>

