<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="text" required="false" %>
<%@ attribute name="type" required="false" %>
<%@ attribute name="size" required="false" %>
<%@ attribute name="onclick" required="false" %>
<%@ attribute name="href" required="false" %>
<%@ attribute name="disabled" required="false" type="java.lang.Boolean"%>
<%@ attribute name="id" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<c:set var="btnText" value="${not empty text ? text : 'Button'}"/>
<c:set var="btnType" value="${not empty type ? type : 'primary'}"/>
<c:set var="btnSize" value="${not empty size ? size : 'md'}"/>
<c:set var="btnDisabled" value="${disabled ne null ? disabled : false}"/>
<c:set var="btnCssClass" value="${not empty cssClass ? cssClass : ''}"/>

<c:set var="classes" value="btn btn-${btnSize} ${btnCssClass}" />

<c:choose>
    <c:when test="${not empty href and not btnDisabled}">
        <a href="${href}"
           class="${classes}"
           <c:if test="${not empty id}">id="${id}"</c:if>
           <c:if test="${not empty onclick}">onclick="${onclick}" </c:if>
        >
            <c:out value="${btnText}"/>
        </a>
    </c:when>
    <c:otherwise>
        <button type="button"
                class="${classes}"
                <c:if test="${not empty id}">id="${id}"</c:if>
                <c:if test="${not empty onclick}">onclick="${onclick}"</c:if>
                <c:if test="${btnDisabled}">disabled</c:if>
        >
            <c:out value="${btnText}"/>
        </button>
    </c:otherwise>
</c:choose>