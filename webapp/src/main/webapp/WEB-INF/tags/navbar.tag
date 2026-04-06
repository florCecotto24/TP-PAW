<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<nav class="navbar navbar-expand shadow-sm fixed-top mb-0">
    <div class="container-fluid">
        <a class="navbar-brand ms-3 fw-semibold" href="${pageContext.request.contextPath}/">
            <img src="${pageContext.request.contextPath}/assets/images/Ryden_logo.ico" alt="Logo" width="30" height="24" class="d-inline-block align-text-top">
            <spring:message code="app.title"/></a>
        <div class="d-flex flex-row justify-content-end align-items-center">
            <ul class="navbar-nav nav-pills align-items-center mb-0">
                <li class="nav-item my-nav-item">
                    <a class="nav-link d-flex align-items-center ${activeTab == 'search' ? 'active' : ''}" aria-current="page" href="${pageContext.request.contextPath}/search"><spring:message code="navbar.explore"/></a>
                </li>
                <li class="nav-item px-1">
                    <a class="nav-link d-flex align-items-center ${activeTab == 'publish-car' ? 'active' : ''}" href="${pageContext.request.contextPath}/publish-car"><spring:message code="navbar.publish"/></a>
                </li>
            </ul>
        </div>
    </div>
</nav>