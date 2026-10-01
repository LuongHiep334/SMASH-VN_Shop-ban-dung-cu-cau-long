(function () {
    'use strict';
    function init() {
        document.querySelectorAll('[data-support-nav]').forEach(function (nav) {
            if (nav.dataset.supportReady) return;
            nav.dataset.supportReady = 'true';
            var button = nav.querySelector('.support-nav-trigger');
            var menu = nav.querySelector('.support-nav-menu');
            var links = Array.from(menu.querySelectorAll('a'));
            function setOpen(open) {
                button.setAttribute('aria-expanded', String(open));
                menu.hidden = !open;
            }
            button.addEventListener('click', function () { setOpen(menu.hidden); });
            nav.addEventListener('pointerenter', function (event) {
                if (event.pointerType === 'mouse' && window.matchMedia('(hover: hover)').matches) setOpen(true);
            });
            nav.addEventListener('pointerleave', function (event) {
                if (event.pointerType === 'mouse' && !nav.contains(document.activeElement)) setOpen(false);
            });
            nav.addEventListener('focusout', function (event) {
                if (!nav.contains(event.relatedTarget)) setOpen(false);
            });
            nav.addEventListener('keydown', function (event) {
                if (event.key === 'Escape') {
                    event.preventDefault();
                    event.stopPropagation();
                    setOpen(false);
                    button.focus();
                } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                    event.preventDefault();
                    setOpen(true);
                    var index = links.indexOf(document.activeElement);
                    var next = event.key === 'ArrowDown' ? (index + 1) % links.length : (index <= 0 ? links.length - 1 : index - 1);
                    links[next].focus();
                }
            });
            document.addEventListener('click', function (event) { if (!nav.contains(event.target)) setOpen(false); });
            window.addEventListener('resize', function () { setOpen(false); });
        });
    }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
    else init();
})();
