<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="admin.catalog.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container py-5 mt-5">
    <spring:message code="admin.panel.title" var="adminPanelLabel"/>
    <spring:message code="admin.catalog.title" var="adminCatalogLabel"/>
    <ryden:breadcrumbTrail
            homeLabel="${adminPanelLabel}"
            homeHref="${pageContext.request.contextPath}/admin"
            currentLabel="${adminCatalogLabel}"/>
    <h1 class="h2 fw-bold mb-4"><spring:message code="admin.catalog.title"/></h1>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success rounded-3"><c:out value="${successMessage}"/></div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger rounded-3"><c:out value="${errorMessage}"/></div>
    </c:if>

    <h2 class="h4 fw-semibold mb-3"><spring:message code="admin.catalog.entries"/></h2>
    <c:choose>
        <c:when test="${empty pendingModels}">
            <p class="text-secondary mb-4"><spring:message code="admin.catalog.entries.empty"/></p>
        </c:when>
        <c:otherwise>
            <div class="card border-0 shadow-sm rounded-4 mb-4" style="overflow: hidden;">
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-primary">
                                <tr>
                                    <th><spring:message code="admin.catalog.brandName"/></th>
                                    <th><spring:message code="admin.catalog.modelName"/></th>
                                    <th><spring:message code="admin.catalog.modelType"/></th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="model" items="${pendingModels}">
                                    <tr>
                                        <td><c:out value="${model.brand.name}"/></td>
                                        <td><c:out value="${model.name}"/></td>
                                        <td>
                                            <spring:message code="enum.car.type.${model.type.name()}" var="modelTypeLabel"/>
                                            <c:out value="${modelTypeLabel}"/>
                                        </td>
                                        <td class="text-end">
                                            <form:form action="${pageContext.request.contextPath}/admin/catalog/entries/${model.id}/validate" method="post" modelAttribute="adminActionForm" cssClass="d-inline">
                                                <button type="submit" class="btn btn-sm btn-outline-success rounded-3 me-1">
                                                    <spring:message code="admin.catalog.validate"/>
                                                </button>
                                            </form:form>
                                            <form:form action="${pageContext.request.contextPath}/admin/catalog/entries/${model.id}/reject" method="post" modelAttribute="adminActionForm" cssClass="d-inline">
                                                <button type="submit" class="btn btn-sm btn-outline-danger rounded-3">
                                                    <spring:message code="admin.catalog.reject"/>
                                                </button>
                                            </form:form>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="../footer.jsp" %>
</body>
</html>
