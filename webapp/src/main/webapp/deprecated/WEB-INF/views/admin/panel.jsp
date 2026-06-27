<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="${pageContext.response.locale.language}">
<head>
    <title><spring:message code="admin.panel.title"/></title>
    <%@include file="../header.jsp" %>
    <style>
        .admin-panel-header {
            background: var(--color-surface);
            border-bottom: 1px solid var(--color-border);
            padding: 2rem 0 1.75rem;
        }
        .admin-panel-header__icon {
            width: 52px;
            height: 52px;
            border-radius: var(--radius-card);
            background: var(--color-primary-soft);
            color: var(--color-primary);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.5rem;
            flex-shrink: 0;
        }
        .admin-nav-card {
            transition: box-shadow var(--transition-base), transform var(--transition-base);
            cursor: pointer;
        }
        .admin-nav-card:hover {
            box-shadow: var(--shadow-soft) !important;
            transform: translateY(-2px);
        }
        .admin-nav-card__icon-wrap {
            width: 48px;
            height: 48px;
            border-radius: 12px;
            background: var(--color-primary-soft);
            color: var(--color-primary);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.25rem;
            flex-shrink: 0;
        }
        .admin-nav-card__arrow {
            opacity: 0;
            transition: opacity var(--transition-base), transform var(--transition-base);
        }
        .admin-nav-card:hover .admin-nav-card__arrow {
            opacity: 1;
            transform: translateX(3px);
        }
        .admin-action-btn {
            display: inline-flex;
            align-items: center;
            gap: 0.6rem;
            padding: 0.6rem 1.25rem;
            border-radius: 10px;
            border: 1.5px solid var(--color-primary);
            background: var(--color-primary-soft);
            color: var(--color-primary);
            font-size: 0.88rem;
            font-weight: 600;
            text-decoration: none;
            transition: background var(--transition-base), box-shadow var(--transition-base);
            cursor: pointer;
        }
        .admin-action-btn:hover {
            background: var(--color-primary);
            color: #fff;
            box-shadow: var(--shadow-soft);
        }
        .admin-action-btn__icon {
            font-size: 1rem;
        }
        .section-label {
            font-size: 0.7rem;
            font-weight: 700;
            letter-spacing: 0.1em;
            text-transform: uppercase;
            color: var(--color-text-tertiary);
            margin-bottom: 1rem;
        }
    </style>
</head>
<body>
<ryden:navbar/>

<div class="admin-panel-header mt-5">
    <div class="container">
        <div>
            <div>
                <h1 class="h3 fw-bold mb-0"><spring:message code="admin.panel.title"/></h1>
                <p class="text-muted mb-0 mt-1" style="font-size: 0.9rem;"><spring:message code="admin.panel.subtitle"/></p>
            </div>
        </div>
    </div>
</div>

<div class="container py-5">

    <p class="section-label"><spring:message code="admin.panel.title"/></p>
    <div class="row g-4 mb-5">

        <div class="col-md-6">
            <a href="${pageContext.request.contextPath}/admin/users" class="text-decoration-none">
                <div class="card bg-white border-0 shadow-sm rounded-4 admin-nav-card h-100">
                    <div class="card-body p-4">
                        <div class="d-flex align-items-center gap-3">
                            <div class="admin-nav-card__icon-wrap">
                                <i class="bi bi-people-fill"></i>
                            </div>
                            <div class="flex-grow-1">
                                <h2 class="h6 fw-semibold mb-1 text-body"><spring:message code="admin.panel.users"/></h2>
                                <p class="text-muted mb-0" style="font-size: 0.82rem;"><spring:message code="admin.panel.users.desc"/></p>
                            </div>
                            <i class="bi bi-chevron-right text-muted admin-nav-card__arrow"></i>
                        </div>
                    </div>
                </div>
            </a>
        </div>

        <div class="col-md-6">
            <a href="${pageContext.request.contextPath}/admin/cars" class="text-decoration-none">
                <div class="card bg-white border-0 shadow-sm rounded-4 admin-nav-card h-100">
                    <div class="card-body p-4">
                        <div class="d-flex align-items-center gap-3">
                            <div class="admin-nav-card__icon-wrap">
                                <i class="bi bi-car-front-fill"></i>
                            </div>
                            <div class="flex-grow-1">
                                <h2 class="h6 fw-semibold mb-1 text-body"><spring:message code="admin.panel.cars"/></h2>
                                <p class="text-muted mb-0" style="font-size: 0.82rem;"><spring:message code="admin.panel.cars.desc"/></p>
                            </div>
                            <i class="bi bi-chevron-right text-muted admin-nav-card__arrow"></i>
                        </div>
                    </div>
                </div>
            </a>
        </div>

        <div class="col-md-6">
            <a href="${pageContext.request.contextPath}/admin/catalog" class="text-decoration-none">
                <div class="card bg-white border-0 shadow-sm rounded-4 admin-nav-card h-100">
                    <div class="card-body p-4">
                        <div class="d-flex align-items-center gap-3">
                            <div class="admin-nav-card__icon-wrap">
                                <i class="bi bi-collection-fill"></i>
                            </div>
                            <div class="flex-grow-1">
                                <h2 class="h6 fw-semibold mb-1 text-body"><spring:message code="admin.panel.catalog"/></h2>
                                <p class="text-muted mb-0" style="font-size: 0.82rem;"><spring:message code="admin.panel.catalog.desc"/></p>
                            </div>
                            <i class="bi bi-chevron-right text-muted admin-nav-card__arrow"></i>
                        </div>
                    </div>
                </div>
            </a>
        </div>

        <div class="col-md-6">
            <a href="${pageContext.request.contextPath}/admin/reservations" class="text-decoration-none">
                <div class="card bg-white border-0 shadow-sm rounded-4 admin-nav-card h-100">
                    <div class="card-body p-4">
                        <div class="d-flex align-items-center gap-3">
                            <div class="admin-nav-card__icon-wrap">
                                <i class="bi bi-calendar-check-fill"></i>
                            </div>
                            <div class="flex-grow-1">
                                <h2 class="h6 fw-semibold mb-1 text-body"><spring:message code="admin.panel.reservations"/></h2>
                                <p class="text-muted mb-0" style="font-size: 0.82rem;"><spring:message code="admin.panel.reservations.desc"/></p>
                            </div>
                            <i class="bi bi-chevron-right text-muted admin-nav-card__arrow"></i>
                        </div>
                    </div>
                </div>
            </a>
        </div>

    </div>

    <p class="section-label"><spring:message code="admin.panel.quickActions"/></p>
    <div class="d-flex flex-wrap gap-2">
        <a href="${pageContext.request.contextPath}/admin/users/create" class="admin-action-btn">
            <i class="bi bi-person-plus-fill admin-action-btn__icon"></i>
            <spring:message code="admin.panel.createAdmin"/>
        </a>
    </div>

</div>

<%@ include file="../footer.jsp" %>
</body>
</html>
