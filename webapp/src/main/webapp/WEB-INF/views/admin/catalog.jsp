<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="admin.catalog.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container py-5 mt-5">
    <h1 class="h2 fw-bold mb-4"><spring:message code="admin.catalog.title"/></h1>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success rounded-3"><c:out value="${successMessage}"/></div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger rounded-3"><c:out value="${errorMessage}"/></div>
    </c:if>

    <h2 class="h4 fw-semibold mb-3"><spring:message code="admin.catalog.brands"/></h2>
    <c:choose>
        <c:when test="${empty pendingBrands}">
            <p class="text-secondary mb-4">No pending brands.</p>
        </c:when>
        <c:otherwise>
            <div class="card border-0 shadow-sm rounded-4 mb-4">
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>ID</th>
                                    <th><spring:message code="admin.catalog.brandName"/></th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="brand" items="${pendingBrands}">
                                    <tr>
                                        <td><c:out value="${brand.id}"/></td>
                                        <td><c:out value="${brand.name}"/></td>
                                        <td class="text-end">
                                            <form action="${pageContext.request.contextPath}/admin/catalog/brands/${brand.id}/validate" method="post" class="d-inline">
                                                <button type="submit" class="btn btn-sm btn-outline-success rounded-3 me-1">
                                                    <spring:message code="admin.catalog.validate"/>
                                                </button>
                                            </form>
                                            <form action="${pageContext.request.contextPath}/admin/catalog/brands/${brand.id}/reject" method="post" class="d-inline">
                                                <button type="submit" class="btn btn-sm btn-outline-danger rounded-3">
                                                    <spring:message code="admin.catalog.reject"/>
                                                </button>
                                            </form>
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

    <h2 class="h4 fw-semibold mb-3"><spring:message code="admin.catalog.models"/></h2>
    <c:choose>
        <c:when test="${empty pendingModels}">
            <p class="text-secondary mb-4">No pending models.</p>
        </c:when>
        <c:otherwise>
            <div class="card border-0 shadow-sm rounded-4">
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>ID</th>
                                    <th><spring:message code="admin.catalog.brandName"/></th>
                                    <th><spring:message code="admin.catalog.modelName"/></th>
                                    <th><spring:message code="admin.catalog.modelType"/></th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="model" items="${pendingModels}">
                                    <tr>
                                        <td><c:out value="${model.id}"/></td>
                                        <td><c:out value="${model.brand.name}"/></td>
                                        <td><c:out value="${model.name}"/></td>
                                        <td><c:out value="${model.type}"/></td>
                                        <td class="text-end">
                                            <form action="${pageContext.request.contextPath}/admin/catalog/models/${model.id}/validate" method="post" class="d-inline">
                                                <button type="submit" class="btn btn-sm btn-outline-success rounded-3 me-1">
                                                    <spring:message code="admin.catalog.validate"/>
                                                </button>
                                            </form>
                                            <form action="${pageContext.request.contextPath}/admin/catalog/models/${model.id}/reject" method="post" class="d-inline">
                                                <button type="submit" class="btn btn-sm btn-outline-danger rounded-3">
                                                    <spring:message code="admin.catalog.reject"/>
                                                </button>
                                            </form>
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
