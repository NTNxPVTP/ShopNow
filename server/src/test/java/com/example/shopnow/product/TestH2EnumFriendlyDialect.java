package com.example.shopnow.product;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.EnumJdbcType;
import org.hibernate.type.descriptor.jdbc.OrdinalEnumJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

/**
 * Test-only H2 dialect that teaches Hibernate how to render
 * {@code @JdbcTypeCode(SqlTypes.NAMED_ENUM)} columns on the embedded H2
 * database used by {@link ProductRepositoryBug9ExplorationTest}.
 *
 * <p>The production entities ({@code Product}, {@code Order},
 * {@code SubOrder}, {@code Token}, {@code User}) annotate enum columns
 * with {@code @JdbcTypeCode(NAMED_ENUM)} which is a Postgres-specific
 * native enum type. The default {@link H2Dialect} has no DDL mapping for
 * code {@code 6001}, so schema generation fails with
 * {@code MappingException: Unable to determine SQL type name for column
 * 'status' ... type mapping for SqlTypes code: 6001 (NAMED_ENUM)}.
 *
 * <p>This dialect overrides {@link #contributeTypes} to register a plain
 * {@code VARCHAR(255)} DDL type for the {@code NAMED_ENUM} code, which is
 * exactly the storage Hibernate already binds at runtime when
 * {@code @Enumerated(EnumType.STRING)} is also present on the field. The
 * effect on {@code Product.status} is identical to running on Postgres
 * with the production custom enum type from the developer's point of
 * view: the column round-trips the enum name as a string, and the JPQL
 * comparison {@code p.status = ProductStatus.ACTIVE} that the BUG-9 fix
 * introduces will be lowered to a {@code WHERE status = 'ACTIVE'} predicate
 * that H2 can evaluate.
 *
 * <p>Used only by the BUG-9 exploration slice test via
 * {@code spring.jpa.properties.hibernate.dialect=...TestH2EnumFriendlyDialect}.
 */
public class TestH2EnumFriendlyDialect extends H2Dialect {

    public TestH2EnumFriendlyDialect() {
        super();
    }

    public TestH2EnumFriendlyDialect(DatabaseVersion version) {
        super(version);
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);

        DdlTypeRegistry ddlTypeRegistry =
                typeContributions.getTypeConfiguration().getDdlTypeRegistry();

        // NAMED_ENUM (6001) and NAMED_ORDINAL_ENUM (6003) are Postgres
        // native enum codes that the default H2Dialect has no mapping
        // for. Register VARCHAR fallbacks so schema generation succeeds
        // and the JPQL `p.status = ProductStatus.ACTIVE` comparison is
        // lowered to a string equality predicate.
        DdlType varcharDdlType = new DdlTypeImpl(SqlTypes.VARCHAR, "varchar(255)", this);
        ddlTypeRegistry.addDescriptor(SqlTypes.NAMED_ENUM, varcharDdlType);
        ddlTypeRegistry.addDescriptor(SqlTypes.NAMED_ORDINAL_ENUM, varcharDdlType);

        // Re-register NAMED_ENUM / NAMED_ORDINAL_ENUM in the JDBC type
        // registry to use the standard VARCHAR-backed EnumJdbcType.
        // Without this Hibernate keeps the Postgres-specific binder
        // (which tries to send raw bytes via VarbinaryJdbcType) and any
        // INSERT of an enum-typed column fails on H2 with
        // "Could not convert ProductStatus to '[B' using EnumJavaType".
        JdbcTypeRegistry jdbcTypeRegistry =
                typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
        jdbcTypeRegistry.addDescriptor(SqlTypes.NAMED_ENUM, EnumJdbcType.INSTANCE);
        jdbcTypeRegistry.addDescriptor(SqlTypes.NAMED_ORDINAL_ENUM, OrdinalEnumJdbcType.INSTANCE);
    }

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        super.contributeFunctions(functionContributions);
    }
}
