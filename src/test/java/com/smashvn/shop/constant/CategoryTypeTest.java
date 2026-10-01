package com.smashvn.shop.constant;

import com.smashvn.shop.entity.DanhMuc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryTypeTest {

    @Test
    void categoryTypeUsesNameInsteadOfDatabaseId() {
        DanhMuc category = category(99, "Vợt cầu lông");

        assertEquals(CategoryType.VOT, CategoryType.fromDanhMuc(category));
    }

    @Test
    void bagNameContainingRacketIsNotMisclassifiedAsRacket() {
        DanhMuc category = category(1, "Túi đựng vợt cầu lông");

        assertEquals(CategoryType.BALO, CategoryType.fromDanhMuc(category));
    }

    @Test
    void normalizedVietnameseNamesRemainSupported() {
        assertEquals(CategoryType.GIAY, CategoryType.fromName("Giày cầu lông"));
        assertEquals(CategoryType.HOP_CAU, CategoryType.fromName("Hộp cầu lông"));
        assertEquals(CategoryType.TRANG_PHUC, CategoryType.fromName("Quần áo cầu lông"));
        assertEquals(CategoryType.BALO, CategoryType.fromName("Balo cầu lông"));
        assertEquals(CategoryType.CUOC, CategoryType.fromName("Dây cước"));
        assertEquals(CategoryType.QUAN_CAN, CategoryType.fromName("Quấn cán vợt"));
    }

    @Test
    void unknownBusinessNameDoesNotBorrowMeaningFromItsId() {
        assertEquals(CategoryType.OTHER, CategoryType.fromDanhMuc(category(1, "Phụ kiện khác")));
    }

    private DanhMuc category(Integer id, String name) {
        DanhMuc category = new DanhMuc();
        category.setId(id);
        category.setTenDanhMuc(name);
        return category;
    }
}
