<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ tag language="java" pageEncoding="utf-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="label" required="true" %>
<%@ attribute name="options" required="false" type="java.util.List" %>

<div class="dropdown-center mx-2">
    <button class="btn btn-light border dropdown-toggle rounded-4" type="button" data-bs-toggle="dropdown" aria-expanded="false">
        <c:out value="${label}"/>
    </button>
    <ul class="dropdown-menu">
        <c:if test="${empty options}">
            <li><span class="dropdown-item-text text-muted"><spring:message code="filterButton.noOptions"/></span></li>
        </c:if>
        <c:forEach items="${options}" var="option">
            <li><button class="dropdown-item" type="button"><c:out value="${option}"/></button></li>
        </c:forEach>
    </ul>
</div>