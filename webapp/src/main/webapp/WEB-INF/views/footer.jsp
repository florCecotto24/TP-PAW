<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<footer class="py-3 my-4">
    <ul class="nav justify-content-center border-bottom pb-3 mb-3">
        <li class="nav-item">
            <a href="${pageContext.request.contextPath}/" class="nav-link px-2 text-body-secondary">
                <spring:message code="footer.nav.home"/>
            </a>
        </li>
        <li class="nav-item">
            <a href="${pageContext.request.contextPath}/search" class="nav-link px-2 text-body-secondary">
                <spring:message code="navbar.explore"/>
            </a>
        </li>
        <li class="nav-item">
            <a href="${pageContext.request.contextPath}/publish-car" class="nav-link px-2 text-body-secondary">
                <spring:message code="navbar.publish"/>
            </a>
        </li>
    </ul>
    <p class="text-center text-body-secondary">
        <spring:message code="footer.copyright" arguments="2026,Ryden"/>
    </p>
</footer>

<%@ include file="includes/footerScripts.jspf" %>
