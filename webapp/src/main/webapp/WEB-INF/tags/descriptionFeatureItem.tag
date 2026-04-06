<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="subtitle" required="true" type="java.lang.String" %>

<div class="d-flex gap-3 descriptionFeatureItem">
    <i class="bi bi-check-circle-fill text-primary flex-shrink-0 mt-1" aria-hidden="true"></i>
    <div>
        <p class="fw-semibold mb-1 text-dark"><c:out value="${title}"/></p>
        <p class="text-secondary small mb-0"><c:out value="${subtitle}"/></p>
    </div>
</div>
