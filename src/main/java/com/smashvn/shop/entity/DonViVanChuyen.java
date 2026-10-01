package com.smashvn.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Table(name = "DonViVanChuyen")
@Data
public class DonViVanChuyen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_don_vi", length = 50)
    private String maDonVi;

    @Column(name = "ten_don_vi", length = 100)
    private String tenDonVi;

    @Column(name = "so_hotline", length = 20)
    private String hotline;

    @Column(name = "web_url", length = 100)
    private String website;

    @Column(name = "ma_token", length = 255)
    private String token;

    @Column(name = "ma_client", length = 100)
    private String clientId;

    @Column(name = "dia_chi_kho", length = 500)
    private String diaChiKho;

    @Column(name = "phi_noi_dia")
    private java.math.BigDecimal phiLocal;

    @Column(name = "phi_toan_quoc")
    private java.math.BigDecimal phiNationwide;

    @Column(name = "phien_ban")
    private Long version;

    public static boolean isGhnCarrier(DonViVanChuyen dv) {
        if (dv == null) {
            return false;
        }
        if (dv.getMaDonVi() != null && dv.getMaDonVi().trim().equalsIgnoreCase("GHN")) {
            return true;
        }
        if (dv.getTenDonVi() != null) {
            String norm = java.text.Normalizer.normalize(dv.getTenDonVi().toLowerCase(), java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "");
            return norm.contains("ghn") || norm.contains("giao hang nhanh");
        }
        return false;
    }

    public static boolean isCounterCarrier(DonViVanChuyen dv) {
        if (dv == null) {
            return false;
        }
        if (dv.getMaDonVi() != null && dv.getMaDonVi().trim().equalsIgnoreCase("TAIQUAY")) {
            return true;
        }
        if (dv.getTenDonVi() != null) {
            String norm = java.text.Normalizer.normalize(dv.getTenDonVi().toLowerCase(), java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "");
            return norm.contains("quay") || norm.contains("cho") || norm.contains("tai quay") || norm.contains("mua tai quay");
        }
        return false;
    }
}
