(function (root) {
    'use strict';

    // Status recovery only reads the original checkout; it never resubmits an order.
    root.createCheckoutRequest = function (options) {
        let busy = false;
        let checking = false;
        let attempts = 0;
        const schedule = options.schedule || function (callback) { setTimeout(callback, 2000); };

        function setBusy(value) {
            busy = value;
            options.onBusy(value);
        }

        function uncertain(message, canCheckAgain) {
            options.onUncertain(message, canCheckAgain);
        }

        function handleResult(response) {
            if (response.checkoutStatus === 'PROCESSING' || response.trangThai === 'processing') {
                uncertain(response.message, false);
                if (attempts >= 15) {
                    uncertain('Đơn hàng vẫn đang được xử lý. Bạn có thể kiểm tra lại kết quả bên dưới.', true);
                    checking = false;
                } else {
                    schedule(checkStatus);
                }
                return;
            }
            checking = false;
            options.onResult(response);
            if (response.trangThai !== 'ok') {
                setBusy(false);
            }
        }

        function checkStatus() {
            attempts++;
            Promise.resolve().then(options.getStatus).then(handleResult, function () {
                checking = false;
                uncertain('Chưa thể kiểm tra kết quả. Vui lòng kết nối lại mạng rồi chọn Kiểm tra kết quả; không cần tạo phiên đặt hàng mới.', true);
            });
        }

        function recover() {
            if (checking) return;
            setBusy(true);
            checking = true;
            attempts = 0;
            uncertain('Đang kiểm tra kết quả đặt hàng...', false);
            checkStatus();
        }

        return {
            begin: function () {
                if (busy) return false;
                setBusy(true);
                return true;
            },
            cancel: function () { setBusy(false); },
            send: function (data) {
                attempts = 0;
                Promise.resolve().then(function () { return options.post(data); }).then(function (response) {
                    checking = response.checkoutStatus === 'PROCESSING' || response.trangThai === 'processing';
                    handleResult(response);
                }, recover);
            },
            recover: recover
        };
    };
}(typeof window !== 'undefined' ? window : globalThis));
