/**
 * SMASH-VN Admin Layout Interaction & Helper Engine
 */

document.addEventListener('DOMContentLoaded', function () {
    // 1. Sidebar Mobile Toggle & Desktop Collapse Handling
    const sidebar = document.getElementById('adminSidebar');
    const toggleBtn = document.getElementById('adminSidebarToggle') || document.getElementById('sidebarToggle');
    const closeBtn = document.getElementById('adminSidebarClose');
    const backdrop = document.getElementById('adminSidebarBackdrop');
    const sidebarNav = sidebar ? sidebar.querySelector('.admin-sidebar-nav') : null;
    const submenuToggles = document.querySelectorAll('.admin-nav-link[data-submenu-target]');
    const allNavLinks = document.querySelectorAll('.admin-sidebar .admin-nav-link');
    let lastFocusedBeforeMobileOpen = null;
    let previousMobileMode = window.innerWidth < 992;
    let collapsedFlyoutCloseTimer = null;
    let collapsedFlyoutPinned = null;

    function isMobile() {
        return window.innerWidth < 992;
    }

    function isMobileSidebarOpen() {
        return !!(sidebar && (sidebar.classList.contains('show') || sidebar.classList.contains('sidebar-open')));
    }

    function closeCollapsedFlyouts(exceptSubmenu) {
        if (collapsedFlyoutCloseTimer) {
            window.clearTimeout(collapsedFlyoutCloseTimer);
            collapsedFlyoutCloseTimer = null;
        }
        document.querySelectorAll('.admin-nav-submenu.show-flyout').forEach(function (submenu) {
            if (submenu === exceptSubmenu) return;
            submenu.classList.remove('show-flyout');
            submenu.style.removeProperty('top');
            if (collapsedFlyoutPinned === submenu) collapsedFlyoutPinned = null;
            const parentToggle = document.querySelector(`.admin-nav-link[data-submenu-target="${submenu.id}"]`);
            if (parentToggle) parentToggle.setAttribute('aria-expanded', 'false');
        });
        if (!exceptSubmenu) collapsedFlyoutPinned = null;
    }

    function openCollapsedFlyout(parentToggle, focusFirstItem = false, pinFlyout = false) {
        if (isMobile() || !document.body.classList.contains('sidebar-collapsed')) return false;

        const targetId = parentToggle.getAttribute('data-submenu-target');
        const targetSubmenu = targetId ? document.getElementById(targetId) : null;
        if (!targetSubmenu) return false;

        closeCollapsedFlyouts(targetSubmenu);
        hideSidebarTooltip();

        const rect = parentToggle.getBoundingClientRect();
        targetSubmenu.dataset.parentLabel = parentToggle.dataset.sidebarGroupLabel || parentToggle.dataset.sidebarLabel || '';
        targetSubmenu.style.top = `${Math.max(8, rect.top)}px`;
        targetSubmenu.classList.add('show-flyout');
        parentToggle.setAttribute('aria-expanded', 'true');
        if (pinFlyout) collapsedFlyoutPinned = targetSubmenu;

        window.requestAnimationFrame(function () {
            const flyoutRect = targetSubmenu.getBoundingClientRect();
            if (flyoutRect.bottom > window.innerHeight - 8) {
                targetSubmenu.style.top = `${Math.max(8, window.innerHeight - flyoutRect.height - 8)}px`;
            }

            if (focusFirstItem) {
                const firstChildLink = targetSubmenu.querySelector('.admin-nav-link');
                if (firstChildLink) firstChildLink.focus();
            }
        });
        return true;
    }

    function scheduleCollapsedFlyoutClose() {
        if (collapsedFlyoutCloseTimer) window.clearTimeout(collapsedFlyoutCloseTimer);
        collapsedFlyoutCloseTimer = window.setTimeout(function () {
            const shouldStayOpen = Array.from(document.querySelectorAll('.admin-nav-submenu.show-flyout')).some(function (submenu) {
                const navItem = submenu.closest('.admin-nav-item');
                return submenu.matches(':hover') ||
                    (navItem && navItem.matches(':hover')) ||
                    (navItem && navItem.contains(document.activeElement));
            });
            if (shouldStayOpen) {
                collapsedFlyoutCloseTimer = null;
                return;
            }
            closeCollapsedFlyouts();
        }, 180);
    }

    function updateToggleState() {
        if (!toggleBtn) return;

        const mobile = isMobile();
        const expanded = mobile ? isMobileSidebarOpen() : !document.body.classList.contains('sidebar-collapsed');
        const icon = toggleBtn.querySelector('i');
        let label;

        if (mobile) {
            label = expanded ? 'Đóng menu quản trị' : 'Mở menu quản trị';
            if (icon) icon.className = 'fas fa-bars';
        } else {
            label = expanded ? 'Thu gọn menu quản trị' : 'Mở rộng menu quản trị';
            if (icon) icon.className = expanded ? 'fas fa-chevron-left' : 'fas fa-chevron-right';
        }

        toggleBtn.setAttribute('aria-expanded', expanded ? 'true' : 'false');
        toggleBtn.setAttribute('aria-label', label);
        toggleBtn.setAttribute('title', label);

        if (sidebar) {
            sidebar.setAttribute('aria-hidden', mobile && !expanded ? 'true' : 'false');
            if (mobile && !expanded) {
                sidebar.setAttribute('inert', '');
            } else {
                sidebar.removeAttribute('inert');
            }
        }

        if (!mobile && document.body.classList.contains('sidebar-collapsed')) {
            submenuToggles.forEach(function (submenuToggle) {
                const target = document.getElementById(submenuToggle.getAttribute('data-submenu-target'));
                submenuToggle.setAttribute('aria-expanded', target && target.classList.contains('show-flyout') ? 'true' : 'false');
            });
        }
    }

    // Give icon-only links an accessible name and a label for the collapsed tooltip.
    allNavLinks.forEach(function (link) {
        const textNode = link.querySelector('.admin-nav-link-content > span');
        const label = textNode ? textNode.textContent.trim() : '';
        if (!label) return;
        link.setAttribute('aria-label', label);
        link.dataset.sidebarLabel = label;
        link.dataset.sidebarGroupLabel = label;
    });

    let sidebarTooltip = null;

    function showSidebarTooltip(link) {
        if (isMobile() || !document.body.classList.contains('sidebar-collapsed')) return;
        if (link.closest('.admin-nav-submenu.show-flyout')) return;
        const submenuTarget = link.getAttribute('data-submenu-target');
        if (submenuTarget && document.getElementById(submenuTarget)?.classList.contains('show-flyout')) return;
        const label = link.dataset.sidebarLabel;
        if (!label) return;

        if (!sidebarTooltip) {
            sidebarTooltip = document.createElement('div');
            sidebarTooltip.className = 'admin-sidebar-tooltip';
            sidebarTooltip.setAttribute('role', 'tooltip');
            document.body.appendChild(sidebarTooltip);
        }

        const rect = link.getBoundingClientRect();
        sidebarTooltip.textContent = label;
        sidebarTooltip.style.left = `${rect.right + 10}px`;
        sidebarTooltip.style.top = `${rect.top + (rect.height / 2)}px`;
        sidebarTooltip.classList.add('show');
    }

    function hideSidebarTooltip() {
        if (sidebarTooltip) sidebarTooltip.classList.remove('show');
    }

    allNavLinks.forEach(function (link) {
        link.addEventListener('mouseenter', function () { showSidebarTooltip(link); });
        link.addEventListener('mouseleave', hideSidebarTooltip);
        link.addEventListener('focus', function () { showSidebarTooltip(link); });
        link.addEventListener('blur', hideSidebarTooltip);
    });

    // Restore desktop collapsed state from localStorage if on desktop.
    if (!isMobile()) {
        let shouldCollapse = false;
        try {
            shouldCollapse = localStorage.getItem('adminSidebarCollapsed') === 'true';
        } catch (error) {
            shouldCollapse = false;
        }
        document.body.classList.toggle('sidebar-collapsed', shouldCollapse);
    } else {
        document.body.classList.remove('sidebar-collapsed');
    }
    document.documentElement.classList.remove('sidebar-collapsed-preload');

    function openMobileSidebar() {
        if (!isMobile()) return;
        lastFocusedBeforeMobileOpen = document.activeElement;
        if (sidebar) {
            sidebar.classList.add('show');
            sidebar.classList.add('sidebar-open');
        }
        if (backdrop) backdrop.classList.add('show');
        document.body.classList.add('sidebar-open');
        document.body.style.overflow = 'hidden';
        updateToggleState();
        window.setTimeout(function () {
            if (closeBtn) closeBtn.focus();
        }, 0);
    }

    function closeMobileSidebar(restoreFocus = true) {
        if (sidebar) {
            sidebar.classList.remove('show');
            sidebar.classList.remove('sidebar-open');
        }
        if (backdrop) backdrop.classList.remove('show');
        document.body.classList.remove('sidebar-open');
        document.body.style.overflow = '';
        updateToggleState();

        if (restoreFocus && lastFocusedBeforeMobileOpen && typeof lastFocusedBeforeMobileOpen.focus === 'function') {
            lastFocusedBeforeMobileOpen.focus();
        }
        lastFocusedBeforeMobileOpen = null;
    }

    function toggleDesktopSidebar() {
        closeCollapsedFlyouts();
        hideSidebarTooltip();
        document.body.classList.toggle('sidebar-collapsed');
        const isCollapsed = document.body.classList.contains('sidebar-collapsed');
        localStorage.setItem('adminSidebarCollapsed', isCollapsed ? 'true' : 'false');
        updateToggleState();
    }

    if (toggleBtn) {
        toggleBtn.addEventListener('click', function (e) {
            e.preventDefault();
            if (isMobile()) {
                if (sidebar && (sidebar.classList.contains('show') || sidebar.classList.contains('sidebar-open'))) {
                    closeMobileSidebar();
                } else {
                    openMobileSidebar();
                }
            } else {
                toggleDesktopSidebar();
            }
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', function (e) {
            e.preventDefault();
            closeMobileSidebar();
        });
    }

    if (backdrop) {
        backdrop.addEventListener('click', function () {
            closeMobileSidebar();
        });
    }

    window.addEventListener('resize', function () {
        const currentMobileMode = isMobile();
        if (currentMobileMode !== previousMobileMode) {
            if (currentMobileMode) {
                document.body.classList.remove('sidebar-collapsed');
                closeCollapsedFlyouts();
                hideSidebarTooltip();
            } else {
                closeMobileSidebar(false);
                document.body.classList.toggle('sidebar-collapsed', localStorage.getItem('adminSidebarCollapsed') === 'true');
            }
            previousMobileMode = currentMobileMode;
        }
        updateToggleState();
    });

    // Auto-close mobile sidebar only when navigating to a destination.
    const navLinks = document.querySelectorAll('.admin-sidebar .admin-nav-link:not([data-submenu-target]):not([data-bs-toggle])');
    navLinks.forEach(function (link) {
        link.addEventListener('click', function () {
            if (isMobile()) {
                closeMobileSidebar();
            }
        });
    });

    // 2. Submenu Collapse Handler for "Bình luận & đánh giá"
    submenuToggles.forEach(function (toggle) {
        const navItem = toggle.closest('.admin-nav-item');
        const controlledSubmenu = document.getElementById(toggle.getAttribute('data-submenu-target'));
        if (navItem) {
            navItem.addEventListener('mouseenter', function () {
                openCollapsedFlyout(toggle);
            });
            navItem.addEventListener('mouseleave', scheduleCollapsedFlyoutClose);
        }
        if (controlledSubmenu) {
            controlledSubmenu.addEventListener('mouseenter', function () {
                if (collapsedFlyoutCloseTimer) {
                    window.clearTimeout(collapsedFlyoutCloseTimer);
                    collapsedFlyoutCloseTimer = null;
                }
            });

            controlledSubmenu.addEventListener('keydown', function (e) {
                if (isMobile() || !document.body.classList.contains('sidebar-collapsed')) return;
                const childLinks = Array.from(controlledSubmenu.querySelectorAll('.admin-nav-link'));
                const currentIndex = childLinks.indexOf(document.activeElement);

                if (e.key === 'Escape' || e.key === 'ArrowLeft') {
                    e.preventDefault();
                    closeCollapsedFlyouts();
                    toggle.focus();
                } else if (e.key === 'ArrowDown' && childLinks.length > 0) {
                    e.preventDefault();
                    childLinks[(currentIndex + 1 + childLinks.length) % childLinks.length].focus();
                } else if (e.key === 'ArrowUp' && childLinks.length > 0) {
                    e.preventDefault();
                    childLinks[(currentIndex - 1 + childLinks.length) % childLinks.length].focus();
                }
            });
        }

        toggle.addEventListener('keydown', function (e) {
            if (isMobile() || !document.body.classList.contains('sidebar-collapsed')) return;

            if (e.key === 'ArrowRight' || e.key === 'ArrowDown' || e.key === ' ') {
                e.preventDefault();
                openCollapsedFlyout(this, true, true);
            } else if (e.key === 'ArrowLeft' || e.key === 'Escape') {
                e.preventDefault();
                closeCollapsedFlyouts();
                this.focus();
            }
        });

        toggle.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();

            const targetId = this.getAttribute('data-submenu-target');
            const targetSubmenu = document.getElementById(targetId);

            if (!isMobile() && document.body.classList.contains('sidebar-collapsed')) {
                const isOpen = targetSubmenu && targetSubmenu.classList.contains('show-flyout');
                if (isOpen && collapsedFlyoutPinned === targetSubmenu) {
                    closeCollapsedFlyouts();
                    this.focus();
                    return;
                }
                openCollapsedFlyout(this, false, true);
                return;
            }

            // Normal toggle behavior (when desktop is expanded or on mobile)
            if (targetSubmenu) {
                const isExpanded = this.getAttribute('aria-expanded') === 'true' || targetSubmenu.classList.contains('show');
                if (isExpanded) {
                    targetSubmenu.classList.remove('show');
                    this.setAttribute('aria-expanded', 'false');
                } else {
                    targetSubmenu.classList.add('show');
                    this.setAttribute('aria-expanded', 'true');
                }
            }
        });
    });

    // Auto-expand submenu & highlight parent if active sub-item inside
    const activeSubmenuLink = document.querySelector('.admin-nav-submenu .admin-nav-link.active');
    if (activeSubmenuLink) {
        const parentSubmenu = activeSubmenuLink.closest('.admin-nav-submenu');
        if (parentSubmenu) {
            parentSubmenu.classList.add('show');
            const toggleSubmenuBtn = document.querySelector(`.admin-nav-link[data-submenu-target="${parentSubmenu.id}"]`);
            if (toggleSubmenuBtn) {
                toggleSubmenuBtn.classList.add('active');
                toggleSubmenuBtn.setAttribute('aria-expanded', 'true');
                const activeChildLabelNode = activeSubmenuLink.querySelector('.admin-nav-link-content > span');
                const activeChildLabel = activeChildLabelNode ? activeChildLabelNode.textContent.trim() : '';
                const parentLabel = toggleSubmenuBtn.dataset.sidebarGroupLabel || toggleSubmenuBtn.dataset.sidebarLabel || '';
                toggleSubmenuBtn.closest('.admin-nav-item')?.classList.add('has-active-child');
                if (activeChildLabel) {
                    toggleSubmenuBtn.dataset.activeChildLabel = activeChildLabel;
                    toggleSubmenuBtn.dataset.sidebarLabel = `${parentLabel} — đang chọn ${activeChildLabel}`;
                    toggleSubmenuBtn.setAttribute('aria-label', `${parentLabel}, tab đang chọn: ${activeChildLabel}`);
                }
            }
        }
    }

    document.addEventListener('click', function (e) {
        if (!e.target.closest('.admin-nav-item')) closeCollapsedFlyouts();
    });

    // Persist the sidebar scroll position and keep the current destination visible.
    if (sidebarNav) {
        const savedScrollTop = Number(localStorage.getItem('adminSidebarScrollTop'));
        if (Number.isFinite(savedScrollTop) && savedScrollTop > 0) {
            sidebarNav.scrollTop = savedScrollTop;
        }

        sidebarNav.addEventListener('scroll', function () {
            localStorage.setItem('adminSidebarScrollTop', String(sidebarNav.scrollTop));
            closeCollapsedFlyouts();
            hideSidebarTooltip();
        }, { passive: true });

        window.requestAnimationFrame(function () {
            const activeLink = sidebarNav.querySelector('.admin-nav-link.active');
            if (!activeLink) return;
            const navRect = sidebarNav.getBoundingClientRect();
            const linkRect = activeLink.getBoundingClientRect();
            if (linkRect.top < navRect.top || linkRect.bottom > navRect.bottom) {
                sidebarNav.scrollTop += linkRect.top - navRect.top - (navRect.height / 2) + (linkRect.height / 2);
            }
        });
    }

    // Keyboard support for the mobile off-canvas menu and collapsed flyouts.
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            if (isMobile() && isMobileSidebarOpen()) {
                e.preventDefault();
                closeMobileSidebar();
                return;
            }
            closeCollapsedFlyouts();
        }

        if (e.key !== 'Tab' || !isMobile() || !isMobileSidebarOpen() || !sidebar) return;
        const focusable = Array.from(sidebar.querySelectorAll('a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])'))
            .filter(function (element) { return element.offsetParent !== null; });
        if (focusable.length === 0) return;

        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (e.shiftKey && document.activeElement === first) {
            e.preventDefault();
            last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
            e.preventDefault();
            first.focus();
        }
    });

    updateToggleState();

    // 3. Product Search Handler
    const globalSearch = document.getElementById("adminGlobalSearch");
    const searchBar = document.getElementById('adminProductSearch');
    const searchToggle = document.getElementById('adminSearchToggle');
    const searchMobileClose = document.getElementById('adminSearchMobileClose');
    const searchClear = document.getElementById('adminSearchClear');
    const localProductSearch = window.getAdminLocalProductSearch();

    function updateSearchClearButton() {
        if (searchClear && globalSearch) searchClear.hidden = globalSearch.value.trim().length === 0;
    }

    function openMobileSearch() {
        if (!searchBar || !searchToggle) return;
        searchBar.classList.add('mobile-search-open');
        searchToggle.setAttribute('aria-expanded', 'true');
        if (globalSearch) globalSearch.focus();
    }

    function closeMobileSearch() {
        if (!searchBar || !searchToggle) return;
        searchBar.classList.remove('mobile-search-open');
        searchToggle.setAttribute('aria-expanded', 'false');
        if (window.innerWidth <= 576) searchToggle.focus();
    }

    if (globalSearch) {
        if (localProductSearch && localProductSearch !== globalSearch) {
            localProductSearch.addEventListener('input', function () {
                window.syncAdminGlobalProductSearch(localProductSearch.value);
                window.updateAdminProductSearchUrl(localProductSearch.value);
            });
        }

        const urlParams = new URLSearchParams(window.location.search);
        const urlQuery = urlParams.get('q') || urlParams.get('productSearch');
        if (urlQuery !== null) {
            globalSearch.value = urlQuery;
            window.requestAnimationFrame(function () {
                window.applyAdminProductQuery(urlQuery);
            });
        }

        globalSearch.addEventListener('input', updateSearchClearButton);
        globalSearch.addEventListener("keydown", function (e) {
            if (e.key === "Enter") {
                e.preventDefault();
                executeSearch();
            }
        });

        const searchBtn = document.querySelector(".admin-search-bar .search-btn");
        if (searchBtn) {
            searchBtn.addEventListener("click", executeSearch);
        }

        updateSearchClearButton();
    }

    if (searchClear) {
        searchClear.addEventListener('click', function () {
            globalSearch.value = '';
            updateSearchClearButton();
            if (!window.applyAdminProductQuery('')) window.updateAdminProductSearchUrl('');
            globalSearch.focus();
        });
    }

    if (searchToggle) searchToggle.addEventListener('click', openMobileSearch);
    if (searchMobileClose) searchMobileClose.addEventListener('click', closeMobileSearch);

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && searchBar && searchBar.classList.contains('mobile-search-open')) {
            e.preventDefault();
            closeMobileSearch();
            return;
        }

        if (e.key === 'Tab' && window.innerWidth <= 576 && searchBar && searchBar.classList.contains('mobile-search-open')) {
            const focusable = Array.from(searchBar.querySelectorAll('input, button:not([disabled])'))
                .filter(function (element) { return !element.hidden && element.offsetParent !== null; });
            if (focusable.length === 0) return;
            const first = focusable[0];
            const last = focusable[focusable.length - 1];
            if (e.shiftKey && document.activeElement === first) {
                e.preventDefault();
                last.focus();
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
            }
        }
    });

    window.addEventListener('resize', function () {
        if (window.innerWidth > 576 && searchBar) {
            searchBar.classList.remove('mobile-search-open');
            if (searchToggle) searchToggle.setAttribute('aria-expanded', 'false');
        }
    });

    // 4. Scroll Top Button Handler
    const scrollTopBtn = document.getElementById("topScroll");
    if (scrollTopBtn) {
        window.addEventListener("scroll", function () {
            if (window.scrollY > 300) {
                scrollTopBtn.classList.add("show");
            } else {
                scrollTopBtn.classList.remove("show");
            }
        });

        scrollTopBtn.addEventListener("click", function () {
            window.scrollTo({ top: 0, behavior: "smooth" });
        });
    }

    // 5. Relocate Footer helper if placeholder exists
    const footerPlaceholder = document.querySelector(".admin-footer-placeholder");
    const mainContent = document.querySelector(".admin-content");
    if (footerPlaceholder && mainContent) {
        mainContent.appendChild(footerPlaceholder);
        footerPlaceholder.style.display = "block";
    }
});

// Product search helpers shared by the topbar and product/POS screens.
window.getAdminLocalProductSearch = function () {
    const localSearch = document.getElementById('productSearchInput');
    if (localSearch) return localSearch;
    return document.getElementById('posSearchInput') || document.querySelector("input[placeholder*='Tìm kiếm sản phẩm']");
};

window.syncAdminGlobalProductSearch = function (query) {
    const globalSearch = document.getElementById('adminGlobalSearch');
    if (!globalSearch) return false;

    const nextValue = query || '';
    if (globalSearch.value !== nextValue) globalSearch.value = nextValue;
    globalSearch.dispatchEvent(new Event('input', { bubbles: true }));
    return true;
};

window.updateAdminProductSearchUrl = function (query) {
    if (!window.history || typeof window.history.replaceState !== 'function') return;

    const normalizedQuery = (query || '').trim();
    const url = new URL(window.location.href);
    if (normalizedQuery) {
        url.searchParams.set('q', normalizedQuery);
    } else {
        url.searchParams.delete('q');
    }
    url.searchParams.delete('productSearch');

    const nextRelativeUrl = `${url.pathname}${url.search}${url.hash}`;
    const currentRelativeUrl = `${window.location.pathname}${window.location.search}${window.location.hash}`;
    if (nextRelativeUrl !== currentRelativeUrl) {
        window.history.replaceState(window.history.state, '', nextRelativeUrl);
    }
};

window.applyAdminProductQuery = function (query) {
    const localSearch = window.getAdminLocalProductSearch();
    if (!localSearch) return false;
    const normalizedQuery = (query || '').trim();
    localSearch.value = normalizedQuery;
    localSearch.dispatchEvent(new Event('input', { bubbles: true }));
    window.updateAdminProductSearchUrl(normalizedQuery);
    return true;
};

window.executeSearch = function () {
    const globalSearch = document.getElementById("adminGlobalSearch");
    if (!globalSearch) return;
    const query = globalSearch.value.trim();

    if (window.applyAdminProductQuery(query)) return;
    if (!query) return;

    window.location.href = "/admin/san-pham?q=" + encodeURIComponent(query);
};

// Global form confirmation helper
window.confirmSubmitForm = function (formElement, message) {
    if (formElement.dataset.confirmed === "true") {
        return true;
    }
    const msgLower = (message || '').toLowerCase();
    const isDelete = msgLower.includes('xóa') || msgLower.includes('khóa') || msgLower.includes('tạm dừng') || msgLower.includes('ngừng') || msgLower.includes('ẩn');

    window.SmashNotify.confirm({
        title: isDelete ? 'Xác nhận thực hiện' : 'Xác nhận cập nhật',
        message: message || 'Bạn có chắc chắn muốn thực hiện thao tác này?',
        danger: isDelete
    }).then((confirmed) => {
            if (confirmed) {
                formElement.dataset.confirmed = "true";
                const submitBtn = formElement.querySelector("button[type='submit']");
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span> Đang xử lý...`;
                }
                formElement.submit();
            }
    });
    return false;
};

// Global action-link confirmation helper
window.confirmActionLink = function (event, linkElement, message) {
    if (event) event.preventDefault();
    const href = linkElement.getAttribute('href');
    const msgLower = (message || '').toLowerCase();
    const isDelete = msgLower.includes('xóa') || msgLower.includes('khóa') || msgLower.includes('tạm dừng') || msgLower.includes('ngừng') || msgLower.includes('ẩn');

    window.SmashNotify.confirm({
        title: 'Xác nhận thực hiện',
        message: message || 'Bạn có chắc chắn muốn thực hiện thao tác này?',
        danger: isDelete
    }).then((confirmed) => {
            if (confirmed) {
                window.location.href = href;
            }
    });
    return false;
};
