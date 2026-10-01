package com.smashvn.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SoDiaChiRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import java.util.List;

@SpringBootTest
public class DebugOrderTest {

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private SoDiaChiRepository soDiaChiRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Test
    public void debugAddresses() {
        System.out.println("=== DEBUGGING ACCOUNTS, CUSTOMERS & ADDRESSES ===");
        List<TaiKhoan> accounts = taiKhoanRepository.findAll();
        System.out.println("Total TaiKhoan: " + accounts.size());
        for (TaiKhoan tk : accounts) {
            System.out.println("TaiKhoan ID: " + tk.getId() + " | Username: " + tk.getUsername() + " | Trạng thái: " + tk.getTrangThai() + " | VaiTro: " + tk.getVaiTro() + " | Loai: " + tk.getTrangThaiTaiKhoan());
            
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
            if (kh != null) {
                System.out.println("  -> KhachHang ID: " + kh.getId() + " | Họ Tên: " + kh.getHoTenKh() + " | SĐT: " + kh.getSoDienThoaiKh());
                List<SoDiaChi> addresses = soDiaChiRepository.findByKhachHang_Id(kh.getId());
                System.out.println("     Addresses Count: " + addresses.size());
                for (SoDiaChi sdc : addresses) {
                    System.out.println("       - Address ID: " + sdc.getId() + " | Recipient: " + sdc.getHoVaTenNguoiNhan() + " | SĐT: " + sdc.getSdtNguoiNhan() + " | Chi tiết: " + sdc.getDiaChiCuThe() + " | Default: " + sdc.isDiaChiMacDinh());
                }
            } else {
                System.out.println("  -> No corresponding KhachHang record.");
            }
        }
        System.out.println("=================================================");
    }
}
