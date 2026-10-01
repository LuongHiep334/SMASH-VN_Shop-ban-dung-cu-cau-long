(function () {
    'use strict';

    function initAddressValidation() {
        const form = document.querySelector('form.dash-address-manipulation');
        if (!form) return;

        const fields = [
            {
                input: document.getElementById('address-fname'),
                error: document.getElementById('error-fname'),
                validate: function (value) {
                    if (!value) return 'Họ người nhận không được để trống.';
                    return value.length <= 50 ? '' : 'Họ người nhận không được vượt quá 50 ký tự.';
                }
            },
            {
                input: document.getElementById('address-lname'),
                error: document.getElementById('error-lname'),
                validate: function (value) {
                    if (!value) return 'Tên người nhận không được để trống.';
                    return value.length <= 50 ? '' : 'Tên người nhận không được vượt quá 50 ký tự.';
                }
            },
            {
                input: document.getElementById('address-phone'),
                error: document.getElementById('error-phone'),
                validate: function (value) {
                    if (!value) return 'Số điện thoại không được để trống.';
                    return /^(0|\+84)[0-9]{9}$/.test(value) ? '' : 'Số điện thoại không đúng định dạng.';
                }
            },
            {
                input: document.getElementById('address-street'),
                error: document.getElementById('error-street'),
                validate: function (value) {
                    if (!value) return 'Địa chỉ cụ thể không được để trống.';
                    return value.length >= 5 && value.length <= 255
                            ? '' : 'Địa chỉ cụ thể phải từ 5 đến 255 ký tự.';
                }
            }
        ].filter(function (field) { return field.input && field.error; });

        function validateField(field, showFeedback) {
            const message = field.validate(field.input.value.trim());
            if (showFeedback) {
                field.error.textContent = message;
                field.error.style.display = message ? 'block' : 'none';
                field.input.style.borderColor = message ? 'red' : '';
            }
            return !message;
        }

        fields.forEach(function (field) {
            field.input.addEventListener('input', function () {
                if (field.input.dataset.touched === 'true') validateField(field, true);
            });
            field.input.addEventListener('blur', function () {
                field.input.dataset.touched = 'true';
                validateField(field, true);
            });
            field.input.addEventListener('change', function () {
                field.input.value = field.input.value.trim();
            });
            field.input.addEventListener('invalid', function (event) {
                event.preventDefault();
                field.input.dataset.touched = 'true';
                validateField(field, true);
            });
        });

        form.addEventListener('submit', function (event) {
            let valid = true;
            fields.forEach(function (field) {
                field.input.dataset.touched = 'true';
                valid = validateField(field, true) && valid;
            });
            if (!valid) {
                event.preventDefault();
                const firstInvalid = fields.find(function (field) { return !validateField(field, false); });
                if (firstInvalid) firstInvalid.input.focus();
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAddressValidation);
    } else {
        initAddressValidation();
    }
})();
