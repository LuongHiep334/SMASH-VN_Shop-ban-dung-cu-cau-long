package com.smashvn.shop.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.EditLog;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.EditLogRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final EditLogRepository editLogRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    @Transactional
    public void log(Integer idTaiKhoan, String tenBang, Long idBanGhi, String hanhDong,
            String giaTriCu, String giaTriMoi, String diaChiIp, String ghiChu, String vaiTroThucHien) {
        EditLog log = new EditLog();
        if (idTaiKhoan != null) {
            TaiKhoan tk = taiKhoanRepository.findById(idTaiKhoan).orElse(null);
            log.setTaiKhoan(tk);
        }
        log.setTenBang(tenBang);
        log.setIdBanGhi(idBanGhi != null ? idBanGhi.intValue() : 0);

        String cleanHanhDong = "UPDATE";
        if (hanhDong != null) {
            String upper = hanhDong.toUpperCase().trim();
            if (upper.contains("INSERT") || upper.contains("CREATE") || upper.contains("ADD")) {
                cleanHanhDong = "INSERT";
            } else if (upper.contains("DELETE") || upper.contains("REMOVE") || upper.contains("CANCEL")) {
                cleanHanhDong = "DELETE";
            } else {
                cleanHanhDong = "UPDATE";
            }
        }
        if (hanhDong != null && !hanhDong.equalsIgnoreCase(cleanHanhDong)) {
            ghiChu = "[" + hanhDong + "] " + (ghiChu != null ? ghiChu : "");
        }

        log.setHanhDong(cleanHanhDong);
        log.setGiaTriCu(giaTriCu);
        log.setGiaTriMoi(giaTriMoi);
        log.setThoiGian(LocalDateTime.now());
        log.setDiaChiIp(diaChiIp);
        log.setGhiChu(ghiChu);
        log.setVaiTroThucHien(vaiTroThucHien);

        editLogRepository.save(log);
    }

}
