<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="admin.cars.title"/></title>
    <%@include file="../header.jsp" %>
</head>
<body>
<ryden:navbar/>
<div class="container py-5 mt-5">
    <h1 class="h2 fw-bold mb-4"><spring:message code="admin.cars.title"/></h1>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success rounded-3"><c:out value="${successMessage}"/></div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger rounded-3"><c:out value="${errorMessage}"/></div>
    </c:if>

    <spring:message code="admin.cars.adminOwned" var="adminOwnedLabel"/>
    <spring:message code="admin.cars.pauseModal.title" var="pauseModalTitle"/>
    <spring:message code="admin.cars.pauseModal.confirm" var="pauseModalConfirm"/>
    <spring:message code="admin.cars.pauseModal.back" var="pauseModalBack"/>
    <spring:message code="admin.cars.resumeModal.title" var="resumeModalTitle"/>
    <spring:message code="admin.cars.resumeModal.confirm" var="resumeModalConfirm"/>
    <spring:message code="admin.cars.resumeModal.back" var="resumeModalBack"/>

    <c:choose>
        <c:when test="${empty cars.content}">
            <p class="text-secondary"><spring:message code="admin.cars.emptyAll"/></p>
        </c:when>
        <c:otherwise>
            <div class="card border-0 shadow-sm rounded-4" style="overflow: hidden;">
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-primary">
                                <tr>
                                    <th><spring:message code="admin.cars.table.plate"/></th>
                                    <th><spring:message code="admin.cars.table.car"/></th>
                                    <th><spring:message code="admin.cars.owner"/></th>
                                    <th><spring:message code="admin.cars.status"/></th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="car" items="${cars.content}">
                                    <spring:message code="enum.car.status.${car.status.name()}" var="carStatusLabel"/>
                                    <tr>
                                        <td><c:out value="${car.plate}"/></td>
                                        <td><c:out value="${car.brand} ${car.model}"/></td>
                                        <td><c:out value="${car.owner.forename} ${car.owner.surname}"/></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${car.status.name() == 'ADMIN_PAUSED'}">
                                                    <span class="badge text-bg-warning"><c:out value="${carStatusLabel}"/></span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge text-bg-secondary"><c:out value="${carStatusLabel}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-end">
                                            <a href="${pageContext.request.contextPath}/cars/${car.id}"
                                               class="btn btn-sm btn-outline-secondary rounded-3 me-1">
                                                <spring:message code="admin.cars.viewCar"/>
                                            </a>
                                            <c:choose>
                                                <c:when test="${car.owner.admin}">
                                                    <span class="text-secondary small"><c:out value="${adminOwnedLabel}"/></span>
                                                </c:when>
                                                <c:when test="${car.status.name() == 'ADMIN_PAUSED'}">
                                                    <button type="button"
                                                            class="btn btn-sm btn-outline-success rounded-3"
                                                            data-modal-open="resumeCarModal-${car.id}">
                                                        <spring:message code="admin.cars.resume"/>
                                                    </button>
                                                    <spring:message code="admin.cars.resumeModal.message" arguments="${car.plate}" var="resumeModalMessage"/>
                                                    <ryden:modal
                                                            id="resumeCarModal-${car.id}"
                                                            title="${resumeModalTitle}"
                                                            message="${resumeModalMessage}"
                                                            variant="warning">
                                                        <form method="post" action="${pageContext.request.contextPath}/admin/cars/${car.id}/resume">
                                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                                            <input type="hidden" name="page" value="${cars.currentPage}"/>
                                                            <div class="d-flex justify-content-end gap-2 mt-3">
                                                                <button type="button" class="btn btn-secondary" data-modal-close="resumeCarModal-${car.id}">
                                                                    <c:out value="${resumeModalBack}"/>
                                                                </button>
                                                                <button type="submit" class="btn btn-warning">
                                                                    <c:out value="${resumeModalConfirm}"/>
                                                                </button>
                                                            </div>
                                                        </form>
                                                    </ryden:modal>
                                                </c:when>
                                                <c:otherwise>
                                                    <button type="button"
                                                            class="btn btn-sm btn-outline-danger rounded-3"
                                                            data-modal-open="pauseCarModal-${car.id}">
                                                        <spring:message code="admin.cars.pause"/>
                                                    </button>
                                                    <spring:message code="admin.cars.pauseModal.message" arguments="${car.plate}" var="pauseModalMessage"/>
                                                    <ryden:modal
                                                            id="pauseCarModal-${car.id}"
                                                            title="${pauseModalTitle}"
                                                            message="${pauseModalMessage}"
                                                            variant="danger">
                                                        <form method="post" action="${pageContext.request.contextPath}/admin/cars/${car.id}/pause">
                                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                                            <input type="hidden" name="page" value="${cars.currentPage}"/>
                                                            <div class="d-flex justify-content-end gap-2 mt-3">
                                                                <button type="button" class="btn btn-secondary" data-modal-close="pauseCarModal-${car.id}">
                                                                    <c:out value="${pauseModalBack}"/>
                                                                </button>
                                                                <button type="submit" class="btn btn-danger">
                                                                    <c:out value="${pauseModalConfirm}"/>
                                                                </button>
                                                            </div>
                                                        </form>
                                                    </ryden:modal>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <c:if test="${cars.totalPages > 1}">
                <nav class="mt-4">
                    <ul class="pagination justify-content-center">
                        <c:if test="${cars.hasPrevious}">
                            <li class="page-item">
                                <a class="page-link" href="${pageContext.request.contextPath}/admin/cars?page=${cars.currentPage - 1}">&laquo;</a>
                            </li>
                        </c:if>
                        <c:if test="${cars.hasNext}">
                            <li class="page-item">
                                <a class="page-link" href="${pageContext.request.contextPath}/admin/cars?page=${cars.currentPage + 1}">&raquo;</a>
                            </li>
                        </c:if>
                    </ul>
                </nav>
            </c:if>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="../footer.jsp" %>
</body>
</html>
