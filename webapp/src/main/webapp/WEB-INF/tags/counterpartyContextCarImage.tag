<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ attribute name="imageUrl" required="false" type="java.lang.String" %>
<%@ attribute name="altText" required="false" type="java.lang.String" %>

<div class="counterparty-context-image">
    <c:choose>
        <c:when test="${not empty imageUrl}">
            <img src="${imageUrl}" alt="${altText}" class="counterparty-context-image__img"/>
        </c:when>
        <c:otherwise>
            <div class="counterparty-context-image__placeholder">
                <i class="bi bi-car-front" aria-hidden="true"></i>
            </div>
        </c:otherwise>
    </c:choose>
</div>

