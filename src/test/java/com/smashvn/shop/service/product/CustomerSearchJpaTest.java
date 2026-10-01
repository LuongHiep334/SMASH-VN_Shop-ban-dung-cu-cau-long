package com.smashvn.shop.service.product;

import com.smashvn.shop.dto.product.CustomerPriceSummary;
import com.smashvn.shop.dto.product.ShopFilterRequest;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.specification.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "app.dotenv.enabled=false", "spring.flyway.enabled=false", "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:customer-search;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CustomerSearchJpaTest.Config.class)
class CustomerSearchJpaTest {
    @Configuration
    @EntityScan(basePackageClasses = SanPham.class)
    @EnableJpaRepositories(basePackageClasses = SanPhamRepository.class)
    static class Config { }

    @Autowired SanPhamRepository products;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void catalog() {
        // Only this in-memory test database is used, with complete foreign-key fixtures.
        assertTrue(jdbc.execute((java.sql.Connection connection) -> connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:"));
        jdbc.update("INSERT INTO TaiKhoan(id,username,trang_thai_tai_khoan,so_lan_mua_thanh_cong,vai_tro,so_lan_nhac_nho_vi_pham,ngay_tao) VALUES(999,'customer-search-fixture','ACTIVE',0,'ADMIN',0,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO NhanVien(id,id_tai_khoan,ho_ten,chuc_vu,so_dien_thoai_nv,ngay_tao) VALUES(999,999,'Search fixture','Tester','0900000000',CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO DanhMuc(id,ten_danh_muc,trang_thai) VALUES(9001,?,true)", "Vợt cầu lông");
        jdbc.update("INSERT INTO ThuongHieu(id,ten_thuong_hieu,trang_thai) VALUES(9001,'Yonex',true)");
        product(9001, "Vợt cầu lông Yonex Nanoflare 700 Pro", true);
        product(9002, "Vợt Yonex Đỏ", true);
        product(9003, "Vợt Yonex cũ", false);
        variant(9101, 9001, 100000, 0, true);
        variant(9102, 9001, 1000000, 3, true);
        variant(9103, 9001, 1000, 50, false);
        variant(9104, 9002, 850000, 2, true);
        variant(9105, 9003, 100, 2, true);
        promotion(9201, 25, true, -1, 1);
        promotion(9202, 90, true, -2, -1);
        promotion(9203, 80, false, -1, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"vot", "VỢT", "cau long", "cầu lông", "  yonex   700 ", "700 yonex", "Vo\u0323t 700"})
    void vietnameseAndWordOrderFindProductOnBothSurfaces(String query) {
        assertTrue(ids(query).contains(9001));
        assertTrue(products.findAll(CustomerAutocompleteSpecification.matching(query)).stream().anyMatch(p -> p.getId() == 9001));
        assertFalse(ids(query).contains(9003));
    }

    @Test void crossedDAndUnmatchedWords() {
        assertEquals(List.of(9002), ids("do yonex"));
        assertTrue(ids("yonex nonexistent").isEmpty());
        assertTrue(ids("SECRET-SKU-9102").isEmpty());
        assertTrue(products.findAll(CustomerAutocompleteSpecification.matching("SECRET-SKU-9102")).isEmpty());
    }

    @Test void everyWordMustMatchButNoNewRankingIsAdded() {
        var page = products.findAll(CustomerAutocompleteSpecification.matching("vot yonex"),
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id")));
        assertEquals(List.of(9002, 9001), page.map(SanPham::getId).getContent());
    }

    @Test void displayedPriceEqualsFilterAndSortPrice() {
        var request = ShopFilterRequest.builder().minPrice(new BigDecimal("740000"))
                .maxPrice(new BigDecimal("760000")).sort("price_asc").build();
        var found = products.findAll(SanPhamSpecification.filter(request));
        assertEquals(List.of(9001), found.stream().map(SanPham::getId).toList());
        var summary = CustomerPriceSummary.from(found.get(0), found.get(0).getSanPhamChiTiets());
        assertEquals(0, summary.giaSauGiam().compareTo(new BigDecimal("750000")));
        assertTrue(summary.conHang());
        assertEquals(List.of(9001, 9002), sorted("price_asc"));
        assertEquals(List.of(9002, 9001), sorted("price_desc"));
    }

    @Test void soldOutCheapVariantDoesNotMatchWhileAnotherVariantIsPurchasable() {
        var request = ShopFilterRequest.builder().maxPrice(new BigDecimal("100000")).build();
        assertTrue(products.findAll(SanPhamSpecification.filter(request)).isEmpty());
    }

    @Test void entirelySoldOutProductHasPriceButSortsAfterAvailableProducts() {
        jdbc.update("UPDATE SanPhamChiTiet SET so_luong_ton=0 WHERE id_san_pham=9001");
        assertEquals(List.of(9002, 9001), sorted("price_asc"));
        var request = ShopFilterRequest.builder().maxPrice(new BigDecimal("80000")).build();
        var found = products.findAll(SanPhamSpecification.filter(request));
        assertEquals(1, found.size());
        var summary = CustomerPriceSummary.from(found.get(0), found.get(0).getSanPhamChiTiets());
        assertFalse(summary.conHang());
        assertEquals(0, summary.giaSauGiam().compareTo(new BigDecimal("75000")));
    }

    @Test void countAndPaginationRemainValidWithTextAndPriceFilter() {
        var request = ShopFilterRequest.builder().keyword("vot").maxPrice(new BigDecimal("900000")).sort("price_asc").build();
        var page = products.findAll(SanPhamSpecification.filter(request), PageRequest.of(0, 1));
        assertEquals(2, page.getTotalElements());
        assertEquals(9001, page.getContent().get(0).getId());
    }

    private List<Integer> ids(String q) {
        return products.findAll(SanPhamSpecification.filter(ShopFilterRequest.builder().keyword(q).build()))
                .stream().map(SanPham::getId).toList();
    }
    private List<Integer> sorted(String order) {
        return products.findAll(SanPhamSpecification.filter(ShopFilterRequest.builder().sort(order).build()))
                .stream().map(SanPham::getId).toList();
    }
    private void product(int id, String name, boolean active) {
        jdbc.update("INSERT INTO SanPham(id,id_danh_muc,id_thuong_hieu,id_nhan_vien,ten_san_pham,mo_ta,trang_thai,so_luot_danh_gia,diem_trung_binh,ma_san_pham) VALUES(?,9001,9001,999,?,'Test',?,0,0,?)", id,name,active,"SP"+id);
    }
    private void variant(int id, int product, int price, int stock, boolean active) {
        jdbc.update("INSERT INTO SanPhamChiTiet(id,id_san_pham,gia_ban,so_luong_ton,so_luong_sp_loi,trang_thai,sku) VALUES(?,?,?,?,0,?,?)", id,product,price,stock,active,"SECRET-SKU-"+id);
    }
    private void promotion(int id, int percent, boolean enabled, int start, int end) {
        jdbc.update("INSERT INTO DotGiamGia(id,ten_chien_dich,ngay_bat_dau,ngay_ket_thuc,phan_tram_giam,loai_giam_gia,id_nhan_vien,trang_thai) VALUES(?,'Test',?,?,?,'Theo Phần Trăm',999,?)",id,LocalDateTime.now().plusDays(start),LocalDateTime.now().plusDays(end),percent,enabled);
        jdbc.update("INSERT INTO SanPham_DotGiamGia(id_san_pham,id_dot_giam_gia) VALUES(9001,?)",id);
    }
}
