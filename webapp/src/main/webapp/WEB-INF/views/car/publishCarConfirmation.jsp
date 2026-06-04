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
  <spring:message code="publishCar.confirmation.title" var="publishConfirmLabel"/>
  <ryden:breadcrumbTrail currentLabel="${publishConfirmLabel}"/>
  <div class="row justify-content-center">
    <div class="col-md-9 col-lg-7">
      <div class="card border-0 shadow-sm rounded-4 bg-white">
        <div class="card-body p-4 p-md-5 text-center">
          <div class="mb-3">
            <i class="bi bi-check-circle-fill text-success" style="font-size:2.5rem;" aria-hidden="true"></i>
          </div>
          <h1 class="h3 fw-bold mb-2"><spring:message code="publishCar.confirmation.title"/></h1>
          <p class="mb-1 ryden-text-break">
            <spring:message code="publishCar.confirmation.greeting.before"/>
            <c:out value="${currentUser.forename}"/><spring:message code="publishCar.confirmation.greeting.after"/>
          </p>
          <p class="mb-1 ryden-text-break">
            <spring:message code="publishCar.confirmation.message.before"/> <strong><c:out value="${car.brand}"/> <c:out value="${car.model}"/></strong> <spring:message code="publishCar.confirmation.message.after"/>
          </p>
          <p class="text-secondary mb-4"><spring:message code="publishCar.confirmation.subtitle"/></p>

          <div class="card mb-4 bg-cream border-0 text-start">
            <div class="card-body">
              <p class="mb-2 ryden-text-break"><strong><spring:message code="publishCar.confirmation.brand"/></strong> <c:out value="${car.brand}"/></p>
              <p class="mb-2 ryden-text-break"><strong><spring:message code="publishCar.confirmation.model"/></strong> <c:out value="${car.model}"/></p>
              <c:if test="${car.year.present}">
                <p class="mb-2"><strong><spring:message code="publishCar.confirmation.year"/></strong> <c:out value="${car.year.get()}"/></p>
              </c:if>
              <p class="mb-2 ryden-text-break"><strong><spring:message code="publishCar.confirmation.plate"/></strong> <c:out value="${car.plate}"/></p>
              <spring:message code="enum.car.type.${car.type.name()}" var="confirmCarTypeLabel"/>
              <spring:message code="enum.car.powertrain.${car.powertrain.name()}" var="confirmPowertrainLabel"/>
              <spring:message code="enum.car.transmission.${car.transmission.name()}" var="confirmTransmissionLabel"/>
              <p class="mb-2"><strong><spring:message code="publishCar.confirmation.type"/></strong> <c:out value="${confirmCarTypeLabel}"/></p>
              <p class="mb-2"><strong><spring:message code="publishCar.confirmation.powertrain"/></strong> <c:out value="${confirmPowertrainLabel}"/></p>
              <p class="mb-0"><strong><spring:message code="publishCar.confirmation.transmission"/></strong> <c:out value="${confirmTransmissionLabel}"/></p>
            </div>
          </div>

          <c:if test="${newCatalogEntry}">
            <div class="alert alert-info d-flex align-items-start gap-2 text-start mb-4" role="alert">
              <i class="bi bi-info-circle-fill flex-shrink-0 mt-1" aria-hidden="true"></i>
              <span><spring:message code="publishCar.confirmation.newCatalogEntry" arguments="${car.brand},${car.model}"/></span>
            </div>
          </c:if>

          <div class="d-grid gap-2 d-sm-flex justify-content-sm-center align-items-sm-center">
            <a href="<c:url value='/my-cars/car/${car.id}/create'/>" class="btn btn-primary">
              <i class="bi bi-plus-lg me-1" aria-hidden="true"></i>
              <spring:message code="publishCar.confirmation.addCarAvailabilityCta"/>
            </a>
            <a href="<c:url value='/my-cars'/>" class="btn btn-outline-primary">
              <i class="bi bi-car-front me-1" aria-hidden="true"></i>
              <spring:message code="publishCar.confirmation.myCarsCta"/>
            </a>
            <a href="<c:url value='/search'/>" class="btn btn-outline-secondary">
              <spring:message code="common.backToSearch"/>
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
