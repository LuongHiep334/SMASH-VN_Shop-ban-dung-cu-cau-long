package com.smashvn.shop.service.user;

import com.smashvn.shop.dto.user.UserAddressDto;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.repository.SoDiaChiRepository;
import com.smashvn.shop.service.api.GhnService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAddressServiceTest {

    @Test
    void themDiaChiMoi_persistsTheExactGhnAdministrativeMapping() {
        SoDiaChiRepository repository = mock(SoDiaChiRepository.class);
        GhnService ghnService = mock(GhnService.class);
        UserAddressService service = new UserAddressService(repository, ghnService);
        KhachHang customer = customer(7);
        UserAddressDto dto = validAddress();

        when(repository.countByKhachHang_IdAndDiaChiMacDinhTrue(7)).thenReturn(0L);
        when(ghnService.validateSelectedAddress(201, 1442, "20101"))
                .thenReturn(validatedAddress());

        service.themDiaChiMoi(customer, dto);

        ArgumentCaptor<SoDiaChi> addressCaptor = ArgumentCaptor.forClass(SoDiaChi.class);
        verify(repository).save(addressCaptor.capture());
        SoDiaChi saved = addressCaptor.getValue();
        assertEquals(201, saved.getProvinceId());
        assertEquals(1442, saved.getDistrictId());
        assertEquals("20101", saved.getWardCode());
        assertEquals("Quận Ba Đình", saved.getQuanHuyen());
        assertEquals("Phường Phúc Xá", saved.getPhuongXa());
        assertEquals("Hà Nội", saved.getTinhThanh());
        assertEquals("Số 10 Kim Mã", saved.getDiaChiCuThe());
        assertEquals(true, saved.isDefaultShipping());
    }

    @Test
    void themDiaChiMoi_rejectsAddressWithoutGhnAdministrativeMapping() {
        SoDiaChiRepository repository = mock(SoDiaChiRepository.class);
        GhnService ghnService = mock(GhnService.class);
        UserAddressService service = new UserAddressService(repository, ghnService);
        UserAddressDto dto = validAddress();
        dto.setGhnProvinceId(null);
        dto.setGhnDistrictId(null);
        dto.setGhnWardCode(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.themDiaChiMoi(customer(7), dto));

        assertEquals("Vui lòng chọn đầy đủ Tỉnh/Thành phố, Quận/Huyện và Phường/Xã.",
                exception.getMessage());
        verify(repository, never()).save(any(SoDiaChi.class));
        verify(ghnService, never()).validateSelectedAddress(any(), any(), any());
    }

    @Test
    void xoaDiaChi_rejectsTheOnlyDefaultAddress() {
        SoDiaChiRepository repository = mock(SoDiaChiRepository.class);
        GhnService ghnService = mock(GhnService.class);
        UserAddressService service = new UserAddressService(repository, ghnService);
        KhachHang customer = customer(7);
        SoDiaChi address = new SoDiaChi();
        address.setId(11);
        address.setKhachHang(customer);
        address.setDefaultShipping(true);

        when(repository.findById(11)).thenReturn(Optional.of(address));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.xoaDiaChi(11, 7));

        assertEquals("Không thể xóa địa chỉ mặc định. Vui lòng chọn địa chỉ khác làm mặc định trước!",
                exception.getMessage());
        verify(repository, never()).delete(any(SoDiaChi.class));
    }

    @Test
    void xoaDiaChi_deletesOwnedNonDefaultAddress() {
        SoDiaChiRepository repository = mock(SoDiaChiRepository.class);
        GhnService ghnService = mock(GhnService.class);
        UserAddressService service = new UserAddressService(repository, ghnService);
        KhachHang customer = customer(7);
        SoDiaChi address = new SoDiaChi();
        address.setId(12);
        address.setKhachHang(customer);
        address.setDefaultShipping(false);

        when(repository.findById(12)).thenReturn(Optional.of(address));

        service.xoaDiaChi(12, 7);

        verify(repository).delete(address);
    }

    private KhachHang customer(Integer id) {
        KhachHang customer = new KhachHang();
        customer.setId(id);
        return customer;
    }

    private UserAddressDto validAddress() {
        return UserAddressDto.builder()
                .hoNguoiNhan("Nguyễn")
                .tenNguoiNhan("An")
                .sdtNguoiNhan("0912345678")
                .diaChiCuThe("Số 10 Kim Mã")
                .tinhThanh("Tên tỉnh giả từ trình duyệt")
                .quocGia("Quốc gia giả")
                .ghnProvinceId(201)
                .ghnDistrictId(1442)
                .ghnWardCode("20101")
                .quanHuyen("Tên huyện giả từ trình duyệt")
                .phuongXa("Tên xã giả từ trình duyệt")
                .build();
    }

    private GhnService.GhnAddressDetails validatedAddress() {
        return new GhnService.GhnAddressDetails(
                201, "Hà Nội", 1442, "Quận Ba Đình", "20101", "Phường Phúc Xá");
    }
}
