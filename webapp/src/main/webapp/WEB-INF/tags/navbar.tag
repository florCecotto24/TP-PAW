<%@ tag language="java" pageEncoding="UTF-8" %>

<nav class="navbar navbar-expand shadow-sm fixed-top">
    <div class="container-fluid">
        <a class="navbar-brand ms-3 fw-semibold" href="${pageContext.request.contextPath}/">
            <img src="${pageContext.request.contextPath}/assets/images/Ryden_logo.avif" alt="Logo" width="30" height="24" class="d-inline-block align-text-top">
            Ryden</a>
        <div class="d-flex flex-row justify-content-end align-items-center">
            <ul class="navbar-nav nav-pills align-items-center mb-0">
                <li class="nav-item my-nav-item">
                    <a class="nav-link d-flex align-items-center" aria-current="page" href="${pageContext.request.contextPath}/search">Explore</a>
                </li>
                <li class="nav-item px-1">
                    <a class="nav-link d-flex align-items-center">Publish</a>
                </li>
                <li class="nav-item px-1">
                    <a class="nav-link d-flex align-items-center">Reservations</a>
                </li>
                <li class="nav-item px-1">
                    <a class="nav-link d-flex align-items-center"><i class="bi bi-chat-left-fill"></i></a>
                </li>
                <li class="nav-item px-1">
                    <a class="nav-link d-flex align-items-center"><i class="bi bi-bell-fill"></i></a>
                </li>
            </ul>
            <div class="dropdown-center text-end ms-2 me-3 py-1">
                <button
                        class="my-dropdown-button btn btn-light border dropdown-toggle"
                        data-bs-toggle="dropdown"
                        type="button"
                >
                    <img
                            src="https://github.com/mdo.png"
                            alt="mdo"
                            width="32"
                            height="32"
                            class="rounded-circle"
                    />
                </button>
                <ul class="dropdown-menu dropdown-menu-end text-small">
                    <li class="dropdown-header"><h6>Juan Pérez</h6>juan@gmail.com</li>
                    <li><hr class="dropdown-divider"/></li>
                    <li><a class="dropdown-item" href="#"><i class="bi bi-person me-2"></i>My profile</a></li>
                    <li><a class="dropdown-item" href="#"><i class="bi bi-calendar me-2"></i>My reservations</a></li>
                    <li><a class="dropdown-item" href="#"><i class="bi bi-car-front-fill me-2"></i>My cars</a></li>
                    <li><a class="dropdown-item" href="#"><i class="bi bi-gear me-2"></i>Settings</a></li>
                    <li><hr class="dropdown-divider" /></li>
                    <li><a class="dropdown-item text-danger" href="#"><i class="bi bi-box-arrow-right me-2"></i>Sign out</a></li>
                </ul>
            </div>
        </div>
    </div>
</nav>