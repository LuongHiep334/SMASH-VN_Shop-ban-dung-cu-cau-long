package com.smashvn.shop.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.DotGiamGia;

/**
 * DAO (Data Access Object) cho entity {@link DotGiamGia} – Đợt giảm giá.
 *
 * <p>Kế thừa {@link JpaRepository} nên đã sẵn có các thao tác CRUD cơ bản:</p>
 * <ul>
 *   <li>{@code findAll()}         – lấy tất cả đợt giảm giá.</li>
 *   <li>{@code findById(id)}      – tìm theo ID, trả về {@code Optional}.</li>
 *   <li>{@code save(entity)}      – lưu mới hoặc cập nhật.</li>
 *   <li>{@code deleteById(id)}    – xóa cứng (không dùng, hệ thống dùng soft-delete).</li>
 * </ul>
 *
 * <p>Nếu cần thêm query tùy chỉnh (ví dụ: tìm theo trạng thái, theo sản phẩm…),
 * hãy khai báo thêm method tại đây theo chuẩn Spring Data JPA.</p>
 */
public interface DotGiamGiaDAO extends JpaRepository<DotGiamGia, Integer> {

}
