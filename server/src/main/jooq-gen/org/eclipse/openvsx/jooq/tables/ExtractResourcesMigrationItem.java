/*
 * This file is generated by jOOQ.
 */
package org.eclipse.openvsx.jooq.tables;


import java.util.Arrays;
import java.util.List;

import org.eclipse.openvsx.jooq.Keys;
import org.eclipse.openvsx.jooq.Public;
import org.eclipse.openvsx.jooq.tables.records.ExtractResourcesMigrationItemRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ExtractResourcesMigrationItem extends TableImpl<ExtractResourcesMigrationItemRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.extract_resources_migration_item</code>
     */
    public static final ExtractResourcesMigrationItem EXTRACT_RESOURCES_MIGRATION_ITEM = new ExtractResourcesMigrationItem();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ExtractResourcesMigrationItemRecord> getRecordType() {
        return ExtractResourcesMigrationItemRecord.class;
    }

    /**
     * The column <code>public.extract_resources_migration_item.id</code>.
     */
    public final TableField<ExtractResourcesMigrationItemRecord, Long> ID = createField(DSL.name("id"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.extract_resources_migration_item.extension_id</code>.
     */
    public final TableField<ExtractResourcesMigrationItemRecord, Long> EXTENSION_ID = createField(DSL.name("extension_id"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.extract_resources_migration_item.migration_scheduled</code>.
     */
    public final TableField<ExtractResourcesMigrationItemRecord, Boolean> MIGRATION_SCHEDULED = createField(DSL.name("migration_scheduled"), SQLDataType.BOOLEAN.nullable(false), this, "");

    private ExtractResourcesMigrationItem(Name alias, Table<ExtractResourcesMigrationItemRecord> aliased) {
        this(alias, aliased, null);
    }

    private ExtractResourcesMigrationItem(Name alias, Table<ExtractResourcesMigrationItemRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.extract_resources_migration_item</code> table reference
     */
    public ExtractResourcesMigrationItem(String alias) {
        this(DSL.name(alias), EXTRACT_RESOURCES_MIGRATION_ITEM);
    }

    /**
     * Create an aliased <code>public.extract_resources_migration_item</code> table reference
     */
    public ExtractResourcesMigrationItem(Name alias) {
        this(alias, EXTRACT_RESOURCES_MIGRATION_ITEM);
    }

    /**
     * Create a <code>public.extract_resources_migration_item</code> table reference
     */
    public ExtractResourcesMigrationItem() {
        this(DSL.name("extract_resources_migration_item"), null);
    }

    public <O extends Record> ExtractResourcesMigrationItem(Table<O> child, ForeignKey<O, ExtractResourcesMigrationItemRecord> key) {
        super(child, key, EXTRACT_RESOURCES_MIGRATION_ITEM);
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public UniqueKey<ExtractResourcesMigrationItemRecord> getPrimaryKey() {
        return Keys.EXTRACT_RESOURCES_MIGRATION_ITEM_PKEY;
    }

    @Override
    public List<UniqueKey<ExtractResourcesMigrationItemRecord>> getKeys() {
        return Arrays.<UniqueKey<ExtractResourcesMigrationItemRecord>>asList(Keys.EXTRACT_RESOURCES_MIGRATION_ITEM_PKEY, Keys.UNIQUE_EXTENSION_ID);
    }

    @Override
    public ExtractResourcesMigrationItem as(String alias) {
        return new ExtractResourcesMigrationItem(DSL.name(alias), this);
    }

    @Override
    public ExtractResourcesMigrationItem as(Name alias) {
        return new ExtractResourcesMigrationItem(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public ExtractResourcesMigrationItem rename(String name) {
        return new ExtractResourcesMigrationItem(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ExtractResourcesMigrationItem rename(Name name) {
        return new ExtractResourcesMigrationItem(name, null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Long, Long, Boolean> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}