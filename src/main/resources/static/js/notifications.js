(function (window, document) {
    'use strict';

    if (window.SmashNotify && window.SmashNotify.version) {
        return;
    }

    const TYPE_CONFIG = {
        success: { title: 'Thành công', icon: 'fa-check-circle', duration: 3500 },
        info: { title: 'Thông báo', icon: 'fa-info-circle', duration: 4000 },
        warning: { title: 'Cảnh báo', icon: 'fa-exclamation-triangle', duration: 6000 },
        error: { title: 'Lỗi', icon: 'fa-times-circle', duration: 6000 }
    };

    function normalizeType(type) {
        const normalized = String(type || 'info').toLowerCase();
        if (normalized === 'danger') return 'error';
        return TYPE_CONFIG[normalized] ? normalized : 'info';
    }

    function injectStyles() {
        if (document.getElementById('smash-notification-styles')) return;

        const style = document.createElement('style');
        style.id = 'smash-notification-styles';
        style.textContent = `
            #smash-notification-region {
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 2147483000;
                display: flex;
                flex-direction: column;
                gap: 10px;
                width: min(380px, calc(100vw - 40px));
                pointer-events: none;
            }
            body.admin-body #smash-notification-region { top: 80px; }
            .smash-toast {
                position: relative;
                display: grid;
                grid-template-columns: 22px minmax(0, 1fr) 28px;
                gap: 11px;
                align-items: start;
                padding: 14px 12px 14px 16px;
                overflow: hidden;
                color: #1e293b;
                background: #ffffff;
                border: 1px solid #e5e7eb;
                border-left: 5px solid #ff4500;
                border-radius: 9px;
                box-shadow: 0 12px 30px rgba(15, 23, 42, 0.16);
                opacity: 0;
                transform: translateX(115%);
                transition: opacity .28s ease, transform .32s cubic-bezier(.2,.8,.2,1);
                pointer-events: auto;
            }
            .smash-toast.is-visible { opacity: 1; transform: translateX(0); }
            .smash-toast.is-leaving { opacity: 0; transform: translateX(115%); }
            .smash-toast--success { border-left-color: #16a34a; }
            .smash-toast--warning { border-left-color: #f59e0b; }
            .smash-toast--error { border-left-color: #dc2626; }
            .smash-toast__icon { margin-top: 2px; color: #ff4500; font-size: 19px; text-align: center; }
            .smash-toast--success .smash-toast__icon { color: #16a34a; }
            .smash-toast--warning .smash-toast__icon { color: #d97706; }
            .smash-toast--error .smash-toast__icon { color: #dc2626; }
            .smash-toast__title { margin: 0 0 3px; color: #15171c; font-size: 13px; font-weight: 700; line-height: 1.35; }
            .smash-toast__message { margin: 0; color: #596170; font-size: 12.5px; line-height: 1.55; overflow-wrap: anywhere; }
            .smash-toast__close {
                width: 28px;
                height: 28px;
                padding: 0;
                color: #94a3b8;
                background: transparent;
                border: 0;
                border-radius: 5px;
                cursor: pointer;
            }
            .smash-toast__close:hover, .smash-toast__close:focus { color: #334155; background: #f1f5f9; outline: none; }
            .smash-toast__progress { position: absolute; right: 0; bottom: 0; left: 0; height: 3px; background: rgba(255, 69, 0, .18); transform-origin: left; }
            .smash-toast--success .smash-toast__progress { background: rgba(22, 163, 74, .2); }
            .smash-toast--warning .smash-toast__progress { background: rgba(245, 158, 11, .24); }
            .smash-toast--error .smash-toast__progress { background: rgba(220, 38, 38, .2); }
            .smash-confirm-fallback {
                position: fixed;
                inset: 0;
                z-index: 2147483001;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 20px;
                background: rgba(15, 23, 42, .58);
            }
            .smash-confirm-fallback__dialog {
                width: min(430px, 100%);
                padding: 28px;
                text-align: center;
                background: #fff;
                border-radius: 12px;
                box-shadow: 0 24px 70px rgba(0, 0, 0, .25);
            }
            .smash-confirm-fallback__icon { margin-bottom: 12px; color: #f59e0b; font-size: 42px; }
            .smash-confirm-fallback__title { margin: 0 0 8px; color: #15171c; font-size: 20px; font-weight: 700; }
            .smash-confirm-fallback__message { margin: 0 0 22px; color: #64748b; font-size: 14px; line-height: 1.6; }
            .smash-confirm-fallback__actions { display: flex; justify-content: center; gap: 10px; }
            .smash-confirm-fallback__button { min-width: 105px; padding: 11px 18px; border: 0; border-radius: 6px; font-weight: 700; cursor: pointer; }
            .smash-confirm-fallback__button--cancel { color: #475569; background: #e2e8f0; }
            .smash-confirm-fallback__button--confirm { color: #fff; background: #ff4500; }
            .smash-confirm-fallback__button--danger { background: #dc2626; }
            @media (max-width: 575.98px) {
                #smash-notification-region,
                body.admin-body #smash-notification-region {
                    top: 10px;
                    right: 10px;
                    left: 10px;
                    width: auto;
                }
                .smash-toast { transform: translateY(-120%); }
                .smash-toast.is-visible { transform: translateY(0); }
                .smash-toast.is-leaving { transform: translateY(-120%); }
            }
            @media (prefers-reduced-motion: reduce) {
                .smash-toast { transition: none; }
                .smash-toast__progress { display: none; }
            }
        `;
        document.head.appendChild(style);
    }

    function ensureRegion() {
        injectStyles();
        let region = document.getElementById('smash-notification-region');
        if (!region) {
            region = document.createElement('div');
            region.id = 'smash-notification-region';
            region.setAttribute('role', 'region');
            region.setAttribute('aria-label', 'Thông báo hệ thống');
            region.setAttribute('aria-live', 'polite');
            region.setAttribute('aria-relevant', 'additions');
            document.body.appendChild(region);
        }
        return region;
    }

    function dismiss(toast) {
        if (!toast || toast.dataset.dismissing === 'true') return;
        toast.dataset.dismissing = 'true';
        toast.classList.remove('is-visible');
        toast.classList.add('is-leaving');
        window.setTimeout(function () {
            if (toast.parentNode) toast.parentNode.removeChild(toast);
        }, 340);
    }

    function notify(options) {
        if (typeof options === 'string') options = { message: options };
        options = options || {};

        const type = normalizeType(options.type);
        const config = TYPE_CONFIG[type];
        const title = String(options.title || config.title);
        const message = String(options.message || '').trim();
        if (!message) return null;

        const duration = Number.isFinite(Number(options.duration))
            ? Math.max(0, Number(options.duration))
            : config.duration;
        const region = ensureRegion();

        const activeToasts = region.querySelectorAll('.smash-toast:not(.is-leaving)');
        if (activeToasts.length >= 3) dismiss(activeToasts[0]);

        const toast = document.createElement('div');
        toast.className = 'smash-toast smash-toast--' + type;
        toast.setAttribute('role', type === 'error' || type === 'warning' ? 'alert' : 'status');

        const icon = document.createElement('i');
        icon.className = 'smash-toast__icon fas ' + config.icon;
        icon.setAttribute('aria-hidden', 'true');

        const content = document.createElement('div');
        const titleElement = document.createElement('p');
        titleElement.className = 'smash-toast__title';
        titleElement.textContent = title;
        const messageElement = document.createElement('p');
        messageElement.className = 'smash-toast__message';
        messageElement.textContent = message;
        content.append(titleElement, messageElement);

        const close = document.createElement('button');
        close.type = 'button';
        close.className = 'smash-toast__close';
        close.setAttribute('aria-label', 'Đóng thông báo');
        close.innerHTML = '<i class="fas fa-times" aria-hidden="true"></i>';
        close.addEventListener('click', function () { dismiss(toast); });

        toast.append(icon, content, close);
        if (duration > 0) {
            const progress = document.createElement('span');
            progress.className = 'smash-toast__progress';
            progress.style.animation = 'smash-toast-progress ' + duration + 'ms linear forwards';
            toast.appendChild(progress);
        }

        if (!document.getElementById('smash-notification-progress-keyframes')) {
            const keyframes = document.createElement('style');
            keyframes.id = 'smash-notification-progress-keyframes';
            keyframes.textContent = '@keyframes smash-toast-progress { from { transform: scaleX(1); } to { transform: scaleX(0); } }';
            document.head.appendChild(keyframes);
        }

        region.appendChild(toast);
        window.requestAnimationFrame(function () { toast.classList.add('is-visible'); });
        if (duration > 0) window.setTimeout(function () { dismiss(toast); }, duration);
        return toast;
    }

    function legacyShowToast(first, second, third, fourth) {
        if (first && typeof first === 'object') return notify(first);
        if (third !== undefined) {
            return notify({ title: first, message: second, type: third, duration: fourth });
        }
        return notify({ message: first, type: second });
    }

    function fallbackConfirm(options) {
        injectStyles();
        return new Promise(function (resolve) {
            const overlay = document.createElement('div');
            overlay.className = 'smash-confirm-fallback';
            overlay.setAttribute('role', 'dialog');
            overlay.setAttribute('aria-modal', 'true');

            const dialog = document.createElement('div');
            dialog.className = 'smash-confirm-fallback__dialog';
            const icon = document.createElement('div');
            icon.className = 'smash-confirm-fallback__icon fas fa-exclamation-circle';
            icon.setAttribute('aria-hidden', 'true');
            const title = document.createElement('h2');
            title.className = 'smash-confirm-fallback__title';
            title.textContent = options.title;
            const message = document.createElement('p');
            message.className = 'smash-confirm-fallback__message';
            message.textContent = options.message;
            const actions = document.createElement('div');
            actions.className = 'smash-confirm-fallback__actions';
            const cancel = document.createElement('button');
            cancel.type = 'button';
            cancel.className = 'smash-confirm-fallback__button smash-confirm-fallback__button--cancel';
            cancel.textContent = options.cancelText;
            const confirmButton = document.createElement('button');
            confirmButton.type = 'button';
            confirmButton.className = 'smash-confirm-fallback__button smash-confirm-fallback__button--confirm' + (options.danger ? ' smash-confirm-fallback__button--danger' : '');
            confirmButton.textContent = options.confirmText;

            function finish(result) {
                document.removeEventListener('keydown', onKeydown);
                overlay.remove();
                resolve(result);
            }
            function onKeydown(event) {
                if (event.key === 'Escape') finish(false);
            }
            cancel.addEventListener('click', function () { finish(false); });
            confirmButton.addEventListener('click', function () { finish(true); });
            overlay.addEventListener('click', function (event) {
                if (event.target === overlay) finish(false);
            });
            document.addEventListener('keydown', onKeydown);

            actions.append(cancel, confirmButton);
            dialog.append(icon, title, message, actions);
            overlay.appendChild(dialog);
            document.body.appendChild(overlay);
            confirmButton.focus();
        });
    }

    function confirmAction(options) {
        if (typeof options === 'string') options = { message: options };
        options = options || {};
        const settings = {
            title: String(options.title || 'Xác nhận thao tác?'),
            message: String(options.message || 'Bạn có chắc chắn muốn thực hiện thao tác này?'),
            confirmText: String(options.confirmText || 'Đồng ý'),
            cancelText: String(options.cancelText || 'Hủy'),
            danger: options.danger === true,
            html: options.html || null,
            note: options.note !== undefined ? options.note : 'Vui lòng kiểm tra kỹ trước khi xác nhận.'
        };

        if (window.Swal && typeof window.Swal.fire === 'function') {
            let htmlContent = settings.html;
            if (!htmlContent) {
                let noteHtml = settings.note ? `
                    <div style="margin-top: 14px; padding: 10px 14px; background-color: #fff7ed; border: 1px solid #fed7aa; border-radius: 8px; font-size: 13px; color: #c2410c; display: flex; align-items: center; justify-content: center; gap: 6px; line-height: 1.4;">
                        <i class="fas fa-info-circle" style="color: #f97316;"></i>
                        <span>${settings.note}</span>
                    </div>` : '';
                htmlContent = `
                    <div style="font-size: 14px; line-height: 1.6; color: #334155; padding: 4px 8px;">
                        <p style="margin-bottom: 0; font-size: 15px; font-weight: 600; color: #1e293b;">${settings.message}</p>
                        ${noteHtml}
                    </div>`;
            }

            return window.Swal.fire({
                title: settings.title,
                html: htmlContent,
                icon: settings.danger ? 'warning' : 'question',
                showCancelButton: true,
                confirmButtonColor: settings.danger ? '#dc2626' : '#f4511e',
                cancelButtonColor: '#64748b',
                confirmButtonText: settings.confirmText,
                cancelButtonText: settings.cancelText,
                reverseButtons: false,
                focusCancel: settings.danger
            }).then(function (result) { return result.isConfirmed === true; });
        }
        return fallbackConfirm(settings);
    }

    function showServerNotification() {
        const source = document.querySelector('[data-smash-server-notification]');
        if (!source) return;
        const message = String(source.getAttribute('data-message') || '').trim();
        if (!message) return;

        notify({
            type: source.getAttribute('data-type') || 'info',
            title: source.getAttribute('data-title') || undefined,
            message: message
        });

        document.querySelectorAll('.alert').forEach(function (alertElement) {
            if (alertElement.closest('#smash-notification-region')) return;
            const clone = alertElement.cloneNode(true);
            clone.querySelectorAll('button, i').forEach(function (element) { element.remove(); });
            const alertText = String(clone.textContent || '').replace(/\s+/g, ' ').trim();
            if (alertText && (alertText.includes(message) || message.includes(alertText))) alertElement.remove();
        });
    }

    window.SmashNotify = {
        version: '1.0.0',
        notify: notify,
        success: function (message, title) { return notify({ type: 'success', title: title, message: message }); },
        info: function (message, title) { return notify({ type: 'info', title: title, message: message }); },
        warning: function (message, title) { return notify({ type: 'warning', title: title, message: message }); },
        error: function (message, title) { return notify({ type: 'error', title: title, message: message }); },
        confirm: confirmAction,
        dismiss: dismiss
    };
    window.showToast = legacyShowToast;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', showServerNotification, { once: true });
    } else {
        showServerNotification();
    }
})(window, document);
