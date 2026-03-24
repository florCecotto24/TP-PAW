<%@ tag language="java" pageEncoding="UTF-8" %>

<div class="container mb-4">
    <div class="d-flex align-items-center bg-white rounded-4 px-3 py-2 shadow gap-2">

        <form class="form-floating flex-grow-1">
            <input type="text" class="form-control border-0 shadow-none" aria-label="Where" id="where">
            <label for="where">Where</label>
        </form>

        <div class="vr flex-shrink-0"></div>

        <form class="form-floating flex-grow-1">
            <input type="date" class="form-control border-0 shadow-none" aria-label="From" placeholder="From" id="from_date">
            <label for="from_date">From</label>
        </form>

        <div class="vr flex-shrink-0"></div>

        <form class="form-floating flex-grow-1">
            <input type="date" class="form-control border-0 shadow-none" aria-label="Until" placeholder="Until" id="until_date">
            <label for="until_date">Until</label>
        </form>

        <button class="btn btn-primary rounded-3 ms-3 p-2 flex-shrink-0">
            <i class="bi bi-search fs-5 search-btn"></i>
        </button>

    </div>
</div>