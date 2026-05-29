<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <%@include file="../header.jsp"%>
</head>
<body class="bg-light has-fixed-navbar">
<ryden:navbar/>

<main class="container py-5">
  <spring:message code="publishCar.pending.title" var="publishPendingLabel"/>
  <ryden:breadcrumbTrail currentLabel="${publishPendingLabel}"/>
  <div class="row justify-content-center">
    <div class="col-md-9 col-lg-7">
      <div class="card border-0 shadow-sm rounded-4 bg-white">
        <div class="card-body p-4 p-md-5 text-center">
          <div class="mb-3">
            <i class="bi bi-hourglass-split text-warning" style="font-size:2.5rem;" aria-hidden="true"></i>
          </div>
          <h1 class="h3 fw-bold mb-2"><spring:message code="publishCar.pending.title"/></h1>
          <p class="text-secondary mb-4"><spring:message code="publishCar.pending.subtitle"/></p>

          <div class="card mb-4 bg-cream border-0 text-start">
            <div class="card-body">
              <c:if test="${not empty pendingBrand}">
                <p class="${not empty pendingModel ? 'mb-2' : 'mb-0'}">
                  <strong><spring:message code="publishCar.pending.brand.label"/>:</strong>
                  <c:out value="${pendingBrand}"/>
                  <span class="badge bg-warning text-dark ms-2">
                    <spring:message code="publishCar.pending.brand.pendingNote"/>
                  </span>
                </p>
              </c:if>
              <c:if test="${not empty pendingModel}">
                <p class="mb-0">
                  <strong><spring:message code="publishCar.pending.model.label"/>:</strong>
                  <c:out value="${pendingModel}"/>
                  <span class="badge bg-warning text-dark ms-2">
                    <spring:message code="publishCar.pending.brand.pendingNote"/>
                  </span>
                </p>
              </c:if>
            </div>
          </div>

          <div class="d-grid gap-2 d-sm-flex justify-content-sm-center">
            <c:if test="${not empty createdCarId}">
              <c:url var="pendingCarUrl" value="/my-cars/car/${createdCarId}"/>
              <a href="<c:out value='${pendingCarUrl}'/>" class="btn btn-primary">
                <i class="bi bi-car-front me-1" aria-hidden="true"></i>
                <spring:message code="publishCar.pending.viewInMyCars"/>
              </a>
            </c:if>
            <a href="<c:url value='/home'/>" class="btn btn-outline-secondary">
              <spring:message code="publishCar.pending.backToHome"/>
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</main>

<%@include file="../footer.jsp"%>
</body>
</html>
