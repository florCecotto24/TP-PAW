<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ryden" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title><spring:message code="missingCbu.title"/> — <spring:message code="app.title"/></title>
    <%@include file="../header.jsp" %>
    <style>
        .steps-container {
            display: flex;
            flex-wrap: wrap;
            align-items: stretch;
            gap: 1rem;
            margin: 2rem 0;
            justify-content: space-between;
        }

        .step-box {
            flex: 1;
            min-width: 110px;
            background: linear-gradient(135deg, var(--color-surface, #f8f9fa) 0%, #fff 100%);
            border: 2px solid var(--color-border, #e9ecef);
            border-radius: 0.5rem;
            padding: 1rem;
            text-align: center;
            position: relative;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
            transition: all 0.2s ease;
        }

        .step-number {
            font-size: 1.5rem;
            font-weight: bold;
            color: var(--color-primary, #3b7be0);
            margin-bottom: 0.5rem;
        }

        .step-text {
            font-size: 0.875rem;
            color: var(--color-text, #333);
            font-weight: 500;
            line-height: 1.4;
        }

        .step-arrow {
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.5rem;
            color: var(--color-primary, #3b7be0);
            font-weight: bold;
            flex-shrink: 0;
        }

        /* Desktop: horizontal arrow */
        @media (min-width: 768px) {
            .steps-container {
                flex-wrap: nowrap;
                align-items: center;
            }

            .step-arrow {
                width: auto;
                margin: 0 0.5rem;
            }

            .step-box {
                min-height: 120px;
            }
        }

        /* Mobile: vertical arrow */
        @media (max-width: 767.98px) {
            .steps-container {
                flex-direction: column;
            }

            .step-arrow {
                height: 2rem;
                margin: 0.5rem 0;
            }
        }
    </style>
</head>
<body>
<ryden:navbar/>

<div class="container mt-5 pt-4">
    <article class="card border-0 shadow-sm rounded-4 mb-4">
        <div class="card-body p-4">
            <spring:message code="navbar.publish" var="publishLabel"/>
            <spring:message code="missingCbu.heading" var="missingCbuLabel"/>
            <h1 class="h3 mb-3"><spring:message code="missingCbu.heading"/></h1>
            <p class="text-muted mb-4"><spring:message code="missingCbu.description"/></p>

            <div class="mb-4">
                <p class="fw-semibold mb-3"><spring:message code="missingCbu.steps"/></p>

                <div class="steps-container">
                    <div class="step-box">
                        <div class="step-number">1</div>
                        <div class="step-text"><spring:message code="missingCbu.steps.one"/></div>
                    </div>

                    <div class="step-arrow">
                        <span class="d-none d-md-inline">→</span>
                        <span class="d-md-none">↓</span>
                    </div>

                    <div class="step-box">
                        <div class="step-number">2</div>
                        <div class="step-text"><spring:message code="missingCbu.steps.two"/></div>
                    </div>

                    <div class="step-arrow">
                        <span class="d-none d-md-inline">→</span>
                        <span class="d-md-none">↓</span>
                    </div>

                    <div class="step-box">
                        <div class="step-number">3</div>
                        <div class="step-text"><spring:message code="missingCbu.steps.three"/></div>
                    </div>

                    <div class="step-arrow">
                        <span class="d-none d-md-inline">→</span>
                        <span class="d-md-none">↓</span>
                    </div>

                    <div class="step-box">
                        <div class="step-number">4</div>
                        <div class="step-text"><spring:message code="missingCbu.steps.four"/></div>
                    </div>
                </div>
            </div>

            <div class="d-flex gap-2">
                <a href="${pageContext.request.contextPath}${profileUrl}" class="btn btn-primary">
                    <spring:message code="missingCbu.button.profile"/>
                </a>
                <a href="${pageContext.request.contextPath}/" class="btn btn-outline-secondary">
                    <spring:message code="common.backToHome"/>
                </a>
            </div>
        </div>
    </article>
</div>

<%@include file="../footer.jsp" %>
</body>
</html>
