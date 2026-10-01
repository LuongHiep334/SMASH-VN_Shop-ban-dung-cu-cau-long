/*
 * Guest checkout creates GUEST_ACTIVATION tokens and the password recovery
 * flow creates FORGOT_PASSWORD tokens. Older database scripts constrained
 * dbo.MaKhoiPhuc.loai_xac_nhan to EMAIL/SMS, which rolled the whole guest
 * registration transaction back before the guest session could be created.
 */
IF OBJECT_ID(N'dbo.MaKhoiPhuc', N'U') IS NOT NULL
BEGIN
    DECLARE @constraintName sysname;
    DECLARE @dropConstraintSql nvarchar(1000);

    DECLARE token_type_constraints CURSOR LOCAL FAST_FORWARD FOR
        SELECT cc.name
        FROM sys.check_constraints cc
        WHERE cc.parent_object_id = OBJECT_ID(N'dbo.MaKhoiPhuc')
          AND cc.definition LIKE N'%loai_xac_nhan%';

    OPEN token_type_constraints;
    FETCH NEXT FROM token_type_constraints INTO @constraintName;

    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @dropConstraintSql = N'ALTER TABLE dbo.MaKhoiPhuc DROP CONSTRAINT '
            + QUOTENAME(@constraintName) + N';';
        EXEC sys.sp_executesql @dropConstraintSql;
        FETCH NEXT FROM token_type_constraints INTO @constraintName;
    END;

    CLOSE token_type_constraints;
    DEALLOCATE token_type_constraints;

    ALTER TABLE dbo.MaKhoiPhuc WITH CHECK
        ADD CONSTRAINT CK_MaKhoiPhuc_LoaiXacNhan
        CHECK (loai_xac_nhan IN ('EMAIL', 'SMS', 'GUEST_ACTIVATION', 'FORGOT_PASSWORD'));

    ALTER TABLE dbo.MaKhoiPhuc
        CHECK CONSTRAINT CK_MaKhoiPhuc_LoaiXacNhan;
END;
