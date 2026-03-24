<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="icon" required="true" type="java.lang.String" %>
<%@ attribute name="label" required="true" type="java.lang.String" %>

<div class="spec-card border rounded-4 p-3 text-center h-100 d-flex flex-column align-items-center justify-content-center gap-2">
    <i class="bi bi-${icon} fs-3 text-primary" aria-hidden="true"></i>
    <span class="small fw-medium text-dark">${label}</span>
</div>
