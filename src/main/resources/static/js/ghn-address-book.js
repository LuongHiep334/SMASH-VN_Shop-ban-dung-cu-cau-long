(function () {
    'use strict';

    const FRIENDLY_FALLBACK = 'Không thể xác định chính xác khu vực. Vui lòng chọn địa chỉ bên dưới.';
    let initializationPromise = Promise.resolve(false);
    let districtLoadVersion = 0;
    let wardLoadVersion = 0;
    let resolveRequestVersion = 0;
    let manualSelectionVersion = 0;
    let applyResolvedAddressHandler = null;

    function resetSelect(select, placeholder) {
        select.innerHTML = '';
        const option = document.createElement('option');
        option.value = '';
        option.textContent = placeholder;
        select.appendChild(option);
        select.disabled = true;
    }

    function addOptions(select, items, valueKey, labelKey) {
        (items || []).slice().sort(function (left, right) {
            return String(left[labelKey] || '').localeCompare(String(right[labelKey] || ''), 'vi');
        }).forEach(function (item) {
            const option = document.createElement('option');
            option.value = item[valueKey];
            option.textContent = item[labelKey];
            select.appendChild(option);
        });
    }

    function hasOption(select, value) {
        if (value === null || value === undefined || value === '') return false;
        return Array.prototype.some.call(select.options, function (option) {
            return option.value === String(value);
        });
    }

    async function getDeliveryAreas(url) {
        const controller = new AbortController();
        const timeout = window.setTimeout(function () { controller.abort(); }, 10000);
        try {
            const response = await fetch(url, {
                headers: { 'Accept': 'application/json' },
                signal: controller.signal
            });
            const payload = await response.json().catch(function () { return {}; });
            if (!response.ok || payload.status !== 'ok' || !Array.isArray(payload.data) || payload.data.length === 0) {
                throw new Error(payload.message || 'Không thể tải danh sách khu vực giao hàng. Vui lòng thử lại.');
            }
            return payload.data;
        } catch (error) {
            if (error && error.name === 'AbortError') {
                throw new Error('Kết nối đang chậm. Vui lòng thử lại.');
            }
            throw error;
        } finally {
            window.clearTimeout(timeout);
        }
    }

    function initAddressBook() {
        const province = document.getElementById('ghnProvinceId');
        const district = document.getElementById('ghnDistrictId');
        const ward = document.getElementById('ghnWardCode');
        if (!province || !district || !ward) return Promise.resolve(false);

        const form = province.closest('form');
        const feedback = document.getElementById('address-area-guidance');
        const initialProvinceId = province.dataset.selectedValue || province.value;
        const initialDistrictId = district.dataset.selectedValue || district.value;
        const initialWardCode = ward.dataset.selectedValue || ward.value;

        function showFeedback(message, isError) {
            if (!feedback) return;
            feedback.textContent = message;
            feedback.style.background = isError ? '#fff0f0' : '#eef6ff';
            feedback.style.color = isError ? '#a12424' : '#1f4f7a';
        }

        function setSelectError(select, message) {
            const errorElement = document.getElementById('error-' + select.id);
            if (errorElement) {
                errorElement.textContent = message || '';
                errorElement.style.display = message ? 'block' : 'none';
            }
            select.style.borderColor = message ? 'red' : '';
        }

        async function loadDistricts(provinceId, selectedDistrictId) {
            console.info('[AddressApply] LOAD districts start', {
                provinceId: provinceId,
                selectedDistrictId: selectedDistrictId
            });
            const requestVersion = ++districtLoadVersion;
            ++wardLoadVersion;
            resetSelect(district, '-- Chọn quận/huyện --');
            resetSelect(ward, '-- Chọn phường/xã --');
            if (!provinceId) return false;

            const districts = await getDeliveryAreas(
                    '/api/ghn/districts?provinceId=' + encodeURIComponent(provinceId));
            if (requestVersion !== districtLoadVersion) return false;
            addOptions(district, districts, 'DistrictID', 'DistrictName');
            district.disabled = false;
            if (selectedDistrictId && hasOption(district, selectedDistrictId)) {
                district.value = String(selectedDistrictId);
            }
            console.info('[AddressApply] LOAD districts completed', {
                optionsLength: district.options.length,
                selectedValue: district.value,
                hasRequestedOption: hasOption(district, selectedDistrictId)
            });
            return true;
        }

        async function loadWards(districtId, selectedWardCode) {
            console.info('[AddressApply] LOAD wards start', {
                districtId: districtId,
                selectedWardCode: selectedWardCode
            });
            const requestVersion = ++wardLoadVersion;
            resetSelect(ward, '-- Chọn phường/xã --');
            if (!districtId) return false;

            const wards = await getDeliveryAreas(
                    '/api/ghn/wards?districtId=' + encodeURIComponent(districtId));
            if (requestVersion !== wardLoadVersion) return false;
            addOptions(ward, wards, 'WardCode', 'WardName');
            ward.disabled = false;
            if (selectedWardCode && hasOption(ward, selectedWardCode)) {
                ward.value = String(selectedWardCode);
            }
            console.info('[AddressApply] LOAD wards completed', {
                optionsLength: ward.options.length,
                selectedValue: ward.value,
                hasRequestedOption: hasOption(ward, selectedWardCode)
            });
            return true;
        }

        async function applyResolvedAddress(result) {
            console.info('[AddressApply] BEFORE apply', {
                resolutionLevel: result && result.resolutionLevel,
                provinceId: result && result.provinceId,
                districtId: result && result.districtId,
                wardCode: result && result.wardCode,
                provinceOptionsLength: province.options.length,
                districtOptionsLength: district.options.length,
                wardOptionsLength: ward.options.length,
                hasProvinceOption: Boolean(result && hasOption(province, result.provinceId))
            });
            if (!result || !result.provinceId || !hasOption(province, result.provinceId)) {
                showFeedback((result && result.message) || FRIENDLY_FALLBACK, true);
                return 'NONE';
            }

            console.info('[AddressApply] APPLY province', { value: String(result.provinceId) });
            province.value = String(result.provinceId);
            setSelectError(province, '');
            try {
                await loadDistricts(result.provinceId, result.districtId);
            } catch (error) {
                showFeedback('Đã xác định Tỉnh/Thành phố. Vui lòng chọn tiếp khi danh sách khu vực tải lại.', false);
                return 'PROVINCE';
            }

            if (!result.districtId || district.value !== String(result.districtId)) {
                showFeedback(result.message || 'Đã xác định Tỉnh/Thành phố. Vui lòng chọn Quận/Huyện và Phường/Xã.', false);
                return 'PROVINCE';
            }
            setSelectError(district, '');
            console.info('[AddressApply] APPLY district', { value: String(result.districtId) });
            try {
                await loadWards(result.districtId, result.wardCode);
            } catch (error) {
                showFeedback('Đã xác định Tỉnh/Thành phố và Quận/Huyện. Vui lòng chọn Phường/Xã khi danh sách tải lại.', false);
                return 'DISTRICT';
            }

            if (result.wardCode && ward.value === String(result.wardCode)) {
                console.info('[AddressApply] APPLY ward', { value: String(result.wardCode) });
                setSelectError(ward, '');
                showFeedback(result.message || 'Đã tự động điền khu vực giao hàng.', false);
                return 'WARD';
            }
            showFeedback(result.message || 'Đã xác định Tỉnh/Thành phố và Quận/Huyện. Vui lòng chọn Phường/Xã.', false);
            return 'DISTRICT';
        }

        province.addEventListener('change', async function (event) {
            manualSelectionVersion++;
            console.info('[AddressApply] manual selection revision changed', {
                select: 'province',
                revision: manualSelectionVersion,
                isTrusted: event.isTrusted
            });
            setSelectError(province, province.value ? '' : 'Vui lòng chọn Tỉnh/Thành phố.');
            try {
                await loadDistricts(province.value, null);
                showFeedback(province.value
                        ? 'Tiếp tục chọn Quận/Huyện và Phường/Xã.'
                        : 'Chọn khu vực giao hàng hoặc sử dụng vị trí hiện tại để điền nhanh địa chỉ.', false);
            } catch (error) {
                showFeedback(error.message || FRIENDLY_FALLBACK, true);
            }
        });

        district.addEventListener('change', async function (event) {
            manualSelectionVersion++;
            console.info('[AddressApply] manual selection revision changed', {
                select: 'district',
                revision: manualSelectionVersion,
                isTrusted: event.isTrusted
            });
            setSelectError(district, district.value ? '' : 'Vui lòng chọn Quận/Huyện.');
            try {
                await loadWards(district.value, null);
                showFeedback(district.value ? 'Tiếp tục chọn Phường/Xã.' : FRIENDLY_FALLBACK, false);
            } catch (error) {
                showFeedback(error.message || FRIENDLY_FALLBACK, true);
            }
        });

        ward.addEventListener('change', function (event) {
            manualSelectionVersion++;
            console.info('[AddressApply] manual selection revision changed', {
                select: 'ward',
                revision: manualSelectionVersion,
                isTrusted: event.isTrusted
            });
            setSelectError(ward, ward.value ? '' : 'Vui lòng chọn Phường/Xã.');
            if (ward.value) showFeedback('Khu vực giao hàng đã được chọn đầy đủ.', false);
        });

        [province, district, ward].forEach(function (select) {
            select.addEventListener('invalid', function (event) {
                event.preventDefault();
                const label = select === province ? 'Tỉnh/Thành phố'
                        : (select === district ? 'Quận/Huyện' : 'Phường/Xã');
                setSelectError(select, 'Vui lòng chọn ' + label + '.');
            });
        });

        if (form) {
            form.addEventListener('submit', function (event) {
                setSelectError(province, province.value ? '' : 'Vui lòng chọn Tỉnh/Thành phố.');
                setSelectError(district, district.value ? '' : 'Vui lòng chọn Quận/Huyện.');
                setSelectError(ward, ward.value ? '' : 'Vui lòng chọn Phường/Xã.');
                if (province.value && district.value && ward.value) return;
                event.preventDefault();
                showFeedback('Vui lòng chọn đầy đủ Tỉnh/Thành phố, Quận/Huyện và Phường/Xã.', true);
                (province.value ? (district.value ? ward : district) : province).focus();
            });
        }

        return getDeliveryAreas('/api/ghn/provinces').then(async function (provinces) {
            addOptions(province, provinces, 'ProvinceID', 'ProvinceName');
            if (initialProvinceId && hasOption(province, initialProvinceId)) {
                province.value = String(initialProvinceId);
                await loadDistricts(initialProvinceId, initialDistrictId);
                if (district.value) await loadWards(district.value, initialWardCode);
            }
            window.SmashAddressBook.loadDistricts = loadDistricts;
            window.SmashAddressBook.loadWards = loadWards;
            applyResolvedAddressHandler = applyResolvedAddress;
            return true;
        }).catch(function (error) {
            showFeedback(error.message || 'Không thể tải khu vực giao hàng. Vui lòng thử lại.', true);
            return false;
        });
    }

    async function ensureInitialized() {
        return Boolean(await initializationPromise);
    }

    async function resolveCoordinates(latitude, longitude) {
        const requestVersion = ++resolveRequestVersion;
        const selectionVersion = manualSelectionVersion;
        const url = '/api/address/resolve?lat=' + encodeURIComponent(latitude)
                + '&lng=' + encodeURIComponent(longitude);
        const response = await fetch(url, { headers: { 'Accept': 'application/json' } });
        const result = await response.json().catch(function () { return {}; });
        document.body.dataset.addressResolutionJson = JSON.stringify(result);
        console.info('[AddressResolve] browser response', result);
        if (!response.ok) {
            throw new Error(result.message || FRIENDLY_FALLBACK);
        }
        if (requestVersion !== resolveRequestVersion) return result;
        const initialized = await ensureInitialized();
        console.info('[AddressResolve] before apply gate', {
            requestVersion: requestVersion,
            currentRequestVersion: resolveRequestVersion,
            capturedManualSelectionVersion: selectionVersion,
            currentManualSelectionVersion: manualSelectionVersion,
            initialized: initialized,
            hasApplyHandler: Boolean(applyResolvedAddressHandler)
        });
        if (initialized && applyResolvedAddressHandler && selectionVersion === manualSelectionVersion) {
            await applyResolvedAddressHandler(result);
        }
        return result;
    }

    window.SmashAddressBook = {
        ready: ensureInitialized,
        resolveCoordinates: resolveCoordinates,
        applyResolvedAddress: async function (result) {
            const initialized = await ensureInitialized();
            if (!initialized || !applyResolvedAddressHandler) return 'NONE';
            return applyResolvedAddressHandler(result);
        }
    };

    if (document.readyState === 'loading') {
        initializationPromise = new Promise(function (resolve) {
            document.addEventListener('DOMContentLoaded', function () {
                resolve(initAddressBook());
            }, { once: true });
        }).then(function (result) { return result; });
    } else {
        initializationPromise = initAddressBook();
    }
})();
