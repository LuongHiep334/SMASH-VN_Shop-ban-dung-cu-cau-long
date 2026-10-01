package com.smashvn.shop.service.product;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P0TemplateContractTest {

    @Test
    void addProductFormSubmitsEveryDynamicVariantAttribute() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/admin/sanpham-add.html"));

        assertTrue(template.contains("dataset.attrId = att.id"));
        assertTrue(template.contains("variantAttributes"));
        assertTrue(template.contains(".attributes[${attrIndex}].attributeId"));
        assertTrue(template.contains(".attributes[${attrIndex}].value"));
    }

    @Test
    void adminProductTemplatesNoLongerBranchOnFixedCategoryIds() throws Exception {
        String addTemplate = Files.readString(Path.of("src/main/resources/templates/admin/sanpham-add.html"));
        String editTemplate = Files.readString(Path.of("src/main/resources/templates/admin/sanpham-edit.html"));
        String variantTemplate = Files.readString(Path.of("src/main/resources/templates/admin/bienthe-list.html"));

        assertFalse(addTemplate.contains("catIds"));
        assertFalse(editTemplate.contains("catIds"));
        assertFalse(editTemplate.contains("categoryIds"));
        assertFalse(variantTemplate.contains("categoryIds"));
    }
}
