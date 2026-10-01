(function () {
    'use strict';
    var input = document.getElementById('main-search');
    var dropdown = document.getElementById('search-autocomplete');
    var status = document.getElementById('search-status');
    if (!input || !dropdown || !status) return;
    var form = input.closest('form');
    var timer, controller, revision = 0, active = -1, composing = false, popularCache, viewportFrame;

    function fitDropdown() {
        if (input.getAttribute('aria-expanded') !== 'true') return;
        var viewport = window.visualViewport;
        var top = viewport ? viewport.offsetTop : 0;
        var bottom = top + (viewport ? viewport.height : window.innerHeight);
        var anchor = input.getBoundingClientRect();
        if (anchor.bottom <= top || anchor.top >= bottom) { close(); return; }
        var available = Math.max(0, Math.floor(bottom - dropdown.getBoundingClientRect().top - 12));
        dropdown.style.setProperty('--search-dropdown-max-height', available + 'px');
    }

    function scheduleFit() {
        cancelAnimationFrame(viewportFrame);
        viewportFrame = requestAnimationFrame(fitDropdown);
    }

    function invalidate() {
        clearTimeout(timer);
        revision++;
        if (controller) controller.abort();
        controller = null;
    }

    function clearActive() {
        active = -1;
        input.removeAttribute('aria-activedescendant');
        dropdown.querySelectorAll('[role="option"]').forEach(function (option) {
            option.classList.remove('is-active');
            option.setAttribute('aria-selected', 'false');
        });
    }

    function close() {
        invalidate();
        clearActive();
        dropdown.style.display = 'none';
        input.setAttribute('aria-expanded', 'false');
        input.setAttribute('aria-busy', 'false');
        status.textContent = '';
    }

    function show(html, announcement) {
        clearActive();
        dropdown.innerHTML = html;
        dropdown.querySelectorAll('a').forEach(function (option, index) {
            option.id = 'search-option-' + index;
            option.setAttribute('role', 'option');
            option.setAttribute('aria-selected', 'false');
            option.tabIndex = -1;
        });
        dropdown.style.display = 'block';
        dropdown.scrollTop = 0;
        input.setAttribute('aria-expanded', 'true');
        status.textContent = announcement;
        fitDropdown();
    }

    function formatVND(num) {
        return num == null ? '' : new Intl.NumberFormat('vi-VN').format(num) + ' ₫';
    }

    function highlightMatch(text, query) {
        if (!query) return text;
        var escaped = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        return text.replace(new RegExp('(' + escaped + ')', 'gi'), '<mark>$1</mark>');
    }

    function productHtml(item, query) {
        var price = item.giaSauGiam != null ? item.giaSauGiam : item.giaBan;
        return '<a href="/san-pham/' + item.id + '" class="autocomplete-item">' +
            '<div class="autocomplete-info">' +
            '<div class="autocomplete-name">' + highlightMatch(item.tenSanPham, query) + '</div>' +
            '<div class="autocomplete-brand">' + item.thuongHieu + ' · ' + item.danhMuc + '</div>' +
            '<div class="autocomplete-price">' + (item.nhieuMucGia ? 'Từ ' : '') + formatVND(price) +
            (price != null && item.giamGia > 0 ? ' <span class="autocomplete-discount">-' + item.giamGia + '%</span>' : '') + '</div>' +
            (item.conHang === false ? '<div class="autocomplete-stock">Tạm hết hàng</div>' : '') +
            '</div></a>';
    }

    function popularHtml(data) {
        var html = '<div class="popular-section" role="presentation">' +
            '<div class="popular-title"><i class="fas fa-fire" aria-hidden="true"></i> Từ khóa phổ biến</div><div class="popular-tags">';
        (data.danhMuc || []).forEach(function (item) {
            html += '<a href="/shop?categoryId=' + item.id + '" class="popular-tag popular-tag--category">' + item.ten + '</a>';
        });
        (data.thuongHieu || []).forEach(function (item) {
            html += '<a href="/shop?brandId=' + item.id + '" class="popular-tag popular-tag--brand">' + item.ten + '</a>';
        });
        html += '</div></div>';
        if (data.sanPhamNoiBat && data.sanPhamNoiBat.length) {
            html += '<div class="popular-section" role="presentation"><div class="popular-title">' +
                '<i class="fas fa-star" aria-hidden="true"></i> Sản phẩm được tìm nhiều</div>';
            data.sanPhamNoiBat.forEach(function (item) { html += productHtml(item, ''); });
            html += '</div>';
        }
        return html;
    }

    function load(query, ticket) {
        function current() {
            return revision === ticket && input.value.trim() === query && form.contains(document.activeElement);
        }
        if (!current()) return;
        if (!query && popularCache) {
            show(popularHtml(popularCache), 'Gợi ý danh mục, thương hiệu và sản phẩm.');
            return;
        }
        controller = new AbortController();
        input.setAttribute('aria-busy', 'true');
        show('<div class="autocomplete-empty" role="presentation">Đang tìm sản phẩm…</div>', 'Đang tìm sản phẩm.');
        var url = query ? '/api/search/products?q=' + encodeURIComponent(query) : '/api/search/popular';
        fetch(url, { signal: controller.signal })
            .then(function (response) {
                if (!response.ok) throw new Error('Search HTTP ' + response.status);
                return response.json();
            })
            .then(function (data) {
                if (!current()) return;
                input.setAttribute('aria-busy', 'false');
                if (!query) {
                    popularCache = data;
                    show(popularHtml(data), 'Gợi ý danh mục, thương hiệu và sản phẩm.');
                } else if (!data.length) {
                    show('<div class="autocomplete-empty" role="presentation">Không tìm thấy sản phẩm nào cho "' + query + '"</div>', 'Không tìm thấy sản phẩm.');
                } else {
                    var html = data.map(function (item) { return productHtml(item, query); }).join('');
                    html += '<a href="/shop?q=' + encodeURIComponent(query) + '" class="autocomplete-viewall">' +
                        'Xem tất cả kết quả cho "' + query + '" →</a>';
                    show(html, 'Có ' + data.length + ' sản phẩm gợi ý. Dùng phím mũi tên để chọn.');
                }
            })
            .catch(function (error) {
                if (!current() || error.name === 'AbortError') return;
                input.setAttribute('aria-busy', 'false');
                show('<div class="autocomplete-empty" role="presentation">Không thể tải gợi ý. ' +
                    '<button type="button" class="search-retry" data-search-retry>Thử lại</button></div>',
                    'Không thể tải gợi ý. Bạn có thể thử lại hoặc nhấn Enter để tìm kiếm.');
            });
    }

    function schedule(immediate) {
        invalidate();
        clearActive();
        input.setAttribute('aria-busy', 'false');
        var query = input.value.trim();
        if (composing || (query.length > 0 && query.length < 2)) { close(); return; }
        dropdown.style.display = 'none';
        input.setAttribute('aria-expanded', 'false');
        status.textContent = '';
        var ticket = revision;
        timer = setTimeout(function () { load(query, ticket); }, immediate ? 0 : 300);
    }

    input.addEventListener('input', function () { if (!composing) schedule(false); });
    input.addEventListener('focus', function () { schedule(true); });
    input.addEventListener('compositionstart', function () { composing = true; close(); });
    input.addEventListener('compositionend', function () { composing = false; schedule(false); });
    input.addEventListener('keydown', function (event) {
        if (composing || event.isComposing) return;
        if (event.key === 'Escape') { event.preventDefault(); close(); return; }
        if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault();
            if (input.getAttribute('aria-expanded') !== 'true') { schedule(true); return; }
            var options = dropdown.querySelectorAll('[role="option"]');
            if (!options.length) return;
            var next = active < 0 ? (event.key === 'ArrowDown' ? 0 : options.length - 1)
                : (active + (event.key === 'ArrowDown' ? 1 : -1) + options.length) % options.length;
            clearActive();
            active = next;
            options[active].classList.add('is-active');
            options[active].setAttribute('aria-selected', 'true');
            input.setAttribute('aria-activedescendant', options[active].id);
            options[active].scrollIntoView({ block: 'nearest' });
        } else if (event.key === 'Enter' && active >= 0) {
            var selected = dropdown.querySelectorAll('[role="option"]')[active];
            if (selected) { event.preventDefault(); selected.click(); close(); }
        }
    });
    dropdown.addEventListener('click', function (event) {
        if (event.target.closest('[data-search-retry]')) {
            input.focus();
            schedule(true);
        }
    });
    document.addEventListener('click', function (event) { if (!form.contains(event.target)) close(); });
    form.addEventListener('focusout', function (event) { if (!form.contains(event.relatedTarget)) close(); });
    form.addEventListener('submit', close);
    window.addEventListener('resize', scheduleFit, { passive: true });
    window.addEventListener('scroll', scheduleFit, { passive: true });
    if (window.visualViewport) {
        window.visualViewport.addEventListener('resize', scheduleFit, { passive: true });
        window.visualViewport.addEventListener('scroll', scheduleFit, { passive: true });
    }
})();
