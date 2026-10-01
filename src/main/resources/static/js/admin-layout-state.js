(function () {
    try {
        const isDesktop = window.matchMedia('(min-width: 992px)').matches;
        const savedCollapsedState = localStorage.getItem('adminSidebarCollapsed') === 'true';
        if (isDesktop && savedCollapsedState) {
            document.documentElement.classList.add('sidebar-collapsed-preload');
        }
    } catch (error) {
        document.documentElement.classList.remove('sidebar-collapsed-preload');
    }
})();
