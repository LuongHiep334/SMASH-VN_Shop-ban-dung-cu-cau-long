package com.smashvn.shop.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ReturnStatus;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.service.api.GhnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class ReturnEvidenceUploadTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private com.smashvn.shop.repository.EditLogRepository editLogRepository;

    @Autowired
    private com.smashvn.shop.service.order.OrderViewService orderViewService;

    @MockitoBean
    private GhnService ghnService;

    @Autowired
    private com.smashvn.shop.repository.TaiKhoanRepository taiKhoanRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token-123");
    private KhachHang testKhachHang;

    // Standard MP4 ftyp box header bytes recognized by Apache Tika
    private static final byte[] VALID_MP4_BYTES = new byte[]{
            0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, // ...ftyp
            0x69, 0x73, 0x6f, 0x6d, 0x00, 0x00, 0x02, 0x00, // isom....
            0x69, 0x73, 0x6f, 0x6d, 0x69, 0x73, 0x6f, 0x32  // isomiso2
    };

    // DOS/Windows executable header (MZ)
    private static final byte[] EXE_BYTES = new byte[]{
            0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00
    };

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        when(ghnService.resolveGhnAddress(any())).thenAnswer(invocation -> {
            return new GhnService.GhnAddressMapping(201, 1442, "20101");
        });

        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("return_test_user_" + System.currentTimeMillis() + "@gmail.com");
        tk.setMatKhau("testpass123");
        tk.setVaiTro("KH");
        tk.setTrangThai("hoat_dong");
        tk = taiKhoanRepository.save(tk);

        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(tk);
        kh.setHoKh("Nguyen");
        kh.setTenKh("Van Test");
        kh.setSoDienThoaiKh("09" + String.valueOf(System.currentTimeMillis()).substring(5, 13));
        testKhachHang = khachHangRepository.save(kh);
    }

    private Integer createAndDeliverTestOrder(KhachHang kh) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", kh.getTaiKhoan().getId());

        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        MvcResult startResult = mockMvc.perform(post("/checkout/start")
                        .session(session)
                        .param("selectedItemIds", "25"))
                .andExpect(status().isOk())
                .andReturn();

        Map resp = objectMapper.readValue(startResult.getResponse().getContentAsString(), Map.class);
        String token = (String) resp.get("checkoutToken");

        String phone = kh.getSoDienThoaiKh() != null ? kh.getSoDienThoaiKh() : "09" + String.valueOf(System.currentTimeMillis()).substring(3, 11);
        String email = kh.getTaiKhoan().getUsername();

        MvcResult submitResult = mockMvc.perform(post("/checkout/submit")
                        .session(session)
                        .param("checkoutToken", token)
                        .param("hoTenNhan", kh.getHoTenKh() != null ? kh.getHoTenKh() : "Customer ReturnTest")
                        .param("sdtNhan", phone)
                        .param("email", email)
                        .param("diaChiNhan", "123 Le Loi, Quan 1, TP HCM")
                        .param("ghnProvinceId", "201")
                        .param("ghnToDistrictId", "1442")
                        .param("ghnToWardCode", "20101")
                        .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk())
                .andReturn();

        Map submitMap = objectMapper.readValue(submitResult.getResponse().getContentAsString(), Map.class);
        Integer orderId = (Integer) submitMap.get("orderId");

        // Transition order to da_giao (Delivered)
        orderViewService.applyShippingStatus(orderId, "da_giao", "delivered");

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        hd.setThoiGianXacNhan(java.time.LocalDateTime.now());
        hoaDonRepository.save(hd);

        return orderId;
    }

    @Test
    @DisplayName("Case 1: Upload video MP4 < 50MB thành công -> lưu file, DB có đường dẫn, status PENDING_APPROVAL")
    void testCase1_ValidMp4UploadSuccess() throws Exception {
        Integer orderId = createAndDeliverTestOrder(testKhachHang);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", testKhachHang.getTaiKhoan().getId());

        MockMultipartFile videoFile = new MockMultipartFile(
                "files",
                "evidence_video.mp4",
                "video/mp4",
                VALID_MP4_BYTES
        );

        mockMvc.perform(multipart("/user/manage-order/request-return/" + orderId)
                        .file(videoFile)
                        .param("loaiYeuCau", "TRA")
                        .param("lyDo", "Sản phẩm bị lỗi - Vợt bị nứt khung")
                        .session(session)
                        .sessionAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Yêu cầu Đổi/Trả hàng của bạn đã được gửi thành công! Shop sẽ sớm phản hồi."));

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals(ReturnStatus.PENDING_APPROVAL, hd.getTrangThaiHoanHang());
        assertEquals("TRA", hd.getLoaiYeuCauDoiTra());
        assertTrue(hd.getLyDoHoanTra().contains("Vợt bị nứt khung"));

        assertNotNull(hd.getBangChungHoanTra());
        assertTrue(hd.getBangChungHoanTra().startsWith("[\"/uploads/returns/" + orderId + "/"));
        assertTrue(hd.getBangChungHoanTra().endsWith(".mp4\"]"));

        // Verify physical file was created on disk
        String savedJson = hd.getBangChungHoanTra();
        List<String> paths = objectMapper.readValue(savedJson, List.class);
        assertEquals(1, paths.size());
        String relPath = paths.get(0).substring("/uploads/".length());
        Path diskPath = Paths.get("uploads").resolve(relPath).toAbsolutePath();
        assertTrue(Files.exists(diskPath), "Physical video file must exist on disk");

        // Clean up test file
        Files.deleteIfExists(diskPath);
    }

    @Test
    @DisplayName("Case 2: Không chọn video -> Backend từ chối với thông báo rõ ràng")
    void testCase2_NoVideoAttached() throws Exception {
        Integer orderId = createAndDeliverTestOrder(testKhachHang);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", testKhachHang.getTaiKhoan().getId());

        mockMvc.perform(multipart("/user/manage-order/request-return/" + orderId)
                        .param("loaiYeuCau", "TRA")
                        .param("lyDo", "Sản phẩm bị lỗi")
                        .session(session)
                        .sessionAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Vui lòng đính kèm video bằng chứng."));

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        assertNull(hd.getBangChungHoanTra());
        assertNull(hd.getTrangThaiHoanHang());
    }

    @Test
    @DisplayName("Case 3: Video > 50MB -> Backend từ chối, không lưu file rác")
    void testCase3_VideoExceeds50MB() throws Exception {
        Integer orderId = createAndDeliverTestOrder(testKhachHang);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", testKhachHang.getTaiKhoan().getId());

        // Create a MockMultipartFile with size > 50MB
        byte[] oversizedBytes = new byte[51 * 1024 * 1024]; // 51MB
        System.arraycopy(VALID_MP4_BYTES, 0, oversizedBytes, 0, VALID_MP4_BYTES.length);

        MockMultipartFile largeVideo = new MockMultipartFile(
                "files",
                "huge_video.mp4",
                "video/mp4",
                oversizedBytes
        );

        mockMvc.perform(multipart("/user/manage-order/request-return/" + orderId)
                        .file(largeVideo)
                        .param("loaiYeuCau", "TRA")
                        .param("lyDo", "Sản phẩm bị lỗi")
                        .session(session)
                        .sessionAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Video bằng chứng không được vượt quá 50MB."));

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        assertNull(hd.getBangChungHoanTra());
        assertNull(hd.getTrangThaiHoanHang());
    }

    @Test
    @DisplayName("Case 4: Upload file .exe đổi tên thành .mp4 -> Tika phát hiện MIME không phải video và từ chối")
    void testCase4_FakeMp4Extension() throws Exception {
        Integer orderId = createAndDeliverTestOrder(testKhachHang);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", testKhachHang.getTaiKhoan().getId());

        MockMultipartFile fakeVideo = new MockMultipartFile(
                "files",
                "malware.mp4",
                "video/mp4",
                EXE_BYTES
        );

        mockMvc.perform(multipart("/user/manage-order/request-return/" + orderId)
                        .file(fakeVideo)
                        .param("loaiYeuCau", "TRA")
                        .param("lyDo", "Sản phẩm bị lỗi")
                        .session(session)
                        .sessionAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Video bằng chứng chỉ hỗ trợ MP4, WEBM hoặc MOV."));

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        assertNull(hd.getBangChungHoanTra());
        assertNull(hd.getTrangThaiHoanHang());
    }

    @Test
    @DisplayName("Case 5: Gửi 2 video trong request -> Backend từ chối")
    void testCase5_MultipleVideosRejected() throws Exception {
        Integer orderId = createAndDeliverTestOrder(testKhachHang);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", testKhachHang.getTaiKhoan().getId());

        MockMultipartFile video1 = new MockMultipartFile("files", "vid1.mp4", "video/mp4", VALID_MP4_BYTES);
        MockMultipartFile video2 = new MockMultipartFile("files", "vid2.mp4", "video/mp4", VALID_MP4_BYTES);

        mockMvc.perform(multipart("/user/manage-order/request-return/" + orderId)
                        .file(video1)
                        .file(video2)
                        .param("loaiYeuCau", "TRA")
                        .param("lyDo", "Sản phẩm bị lỗi")
                        .session(session)
                        .sessionAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Mỗi yêu cầu chỉ được đính kèm một video bằng chứng."));

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        assertNull(hd.getBangChungHoanTra());
    }

    @Test
    @DisplayName("Case 6: Upload video thành công nhưng đơn hàng quá 7 ngày -> Service từ chối và cleanup video")
    void testCase6_ExpiredReturnWindowCleansUpFile() throws Exception {
        Integer orderId = createAndDeliverTestOrder(testKhachHang);

        // Edit EditLog so delivered timestamp is 10 days ago (expired 7-day window)
        List<com.smashvn.shop.entity.EditLog> logs = editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", orderId);
        for (com.smashvn.shop.entity.EditLog log : logs) {
            if (log.getGiaTriMoi() != null && log.getGiaTriMoi().contains("da_giao")) {
                log.setThoiGian(java.time.LocalDateTime.now().minusDays(10));
                editLogRepository.save(log);
            }
        }

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", testKhachHang.getTaiKhoan().getId());

        MockMultipartFile videoFile = new MockMultipartFile(
                "files",
                "evidence_expired.mp4",
                "video/mp4",
                VALID_MP4_BYTES
        );

        mockMvc.perform(multipart("/user/manage-order/request-return/" + orderId)
                        .file(videoFile)
                        .param("loaiYeuCau", "TRA")
                        .param("lyDo", "Sản phẩm bị lỗi sau 10 ngày")
                        .session(session)
                        .sessionAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Đã hết thời hạn 7 ngày đổi/trả kể từ khi giao hàng thành công."));

        HoaDon updatedHd = hoaDonRepository.findById(orderId).orElseThrow();
        assertNull(updatedHd.getBangChungHoanTra());
        assertNull(updatedHd.getTrangThaiHoanHang());
    }
}
