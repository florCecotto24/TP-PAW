<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="name" required="true" type="java.lang.String" %>
<%@ attribute name="type" required="false" type="java.lang.String" %>
<%@ attribute name="label" required="false" type="java.lang.String" %>
<%@ attribute name="placeholder" required="false" type="java.lang.String" %>
<%@ attribute name="value" required="false" type="java.lang.String" %>
<%@ attribute name="required" required="false" type="java.lang.Boolean" %>
<%@ attribute name="disabled" required="false" type="java.lang.Boolean" %>
<%@ attribute name="readonly" required="false" type="java.lang.Boolean" %>
<%@ attribute name="id" required="false" type="java.lang.String" %>
<%@ attribute name="cssClass" required="false" type="java.lang.String" %>

<c:if test="${empty type}">
    <c:set var="type" value="text" />
</c:if>

<c:if test="${empty id}">
    <c:set var="id" value="${name}" />
</c:if>

<div class="mb-3">
    <c:if test="${not empty label}">
        <label for="${id}" class="form-label custom-label">
            ${label}
            <c:if test="${required}">
                <span class="text-danger">*</span>
            </c:if>
        </label>
    </c:if>

    <input
        type="${type}"
        name="${name}"
        id="${id}"
        class="form-control custom-input ${cssClass}"
        <c:if test="${not empty placeholder}">placeholder="${placeholder}"</c:if>
        <c:if test="${not empty value}">value="${value}"</c:if>
        <c:if test="${required}">required</c:if>
        <c:if test="${disabled}">disabled</c:if>
        <c:if test="${readonly}">readonly</c:if>
    />
</div>
