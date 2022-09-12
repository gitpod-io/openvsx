/*
 * This file is generated by jOOQ.
 */
package org.eclipse.openvsx.jooq.tables;


import java.util.Arrays;
import java.util.List;

import org.eclipse.openvsx.jooq.Indexes;
import org.eclipse.openvsx.jooq.Keys;
import org.eclipse.openvsx.jooq.Public;
import org.eclipse.openvsx.jooq.tables.records.QrtzFiredTriggersRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row13;
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
public class QrtzFiredTriggers extends TableImpl<QrtzFiredTriggersRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.qrtz_fired_triggers</code>
     */
    public static final QrtzFiredTriggers QRTZ_FIRED_TRIGGERS = new QrtzFiredTriggers();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<QrtzFiredTriggersRecord> getRecordType() {
        return QrtzFiredTriggersRecord.class;
    }

    /**
     * The column <code>public.qrtz_fired_triggers.sched_name</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, String> SCHED_NAME = createField(DSL.name("sched_name"), SQLDataType.VARCHAR(120).nullable(false), this, "");

    /**
     * The column <code>public.qrtz_fired_triggers.entry_id</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, String> ENTRY_ID = createField(DSL.name("entry_id"), SQLDataType.VARCHAR(95).nullable(false), this, "");

    /**
     * The column <code>public.qrtz_fired_triggers.trigger_name</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, String> TRIGGER_NAME = createField(DSL.name("trigger_name"), SQLDataType.VARCHAR(200).nullable(false), this, "");

    /**
     * The column <code>public.qrtz_fired_triggers.trigger_group</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, String> TRIGGER_GROUP = createField(DSL.name("trigger_group"), SQLDataType.VARCHAR(200).nullable(false), this, "");

    /**
     * The column <code>public.qrtz_fired_triggers.instance_name</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, String> INSTANCE_NAME = createField(DSL.name("instance_name"), SQLDataType.VARCHAR(200).nullable(false), this, "");

    /**
     * The column <code>public.qrtz_fired_triggers.fired_time</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, Long> FIRED_TIME = createField(DSL.name("fired_time"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.qrtz_fired_triggers.sched_time</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, Long> SCHED_TIME = createField(DSL.name("sched_time"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.qrtz_fired_triggers.priority</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, Integer> PRIORITY = createField(DSL.name("priority"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>public.qrtz_fired_triggers.state</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, String> STATE = createField(DSL.name("state"), SQLDataType.VARCHAR(16).nullable(false), this, "");

    /**
     * The column <code>public.qrtz_fired_triggers.job_name</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, String> JOB_NAME = createField(DSL.name("job_name"), SQLDataType.VARCHAR(200), this, "");

    /**
     * The column <code>public.qrtz_fired_triggers.job_group</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, String> JOB_GROUP = createField(DSL.name("job_group"), SQLDataType.VARCHAR(200), this, "");

    /**
     * The column <code>public.qrtz_fired_triggers.is_nonconcurrent</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, Boolean> IS_NONCONCURRENT = createField(DSL.name("is_nonconcurrent"), SQLDataType.BOOLEAN, this, "");

    /**
     * The column <code>public.qrtz_fired_triggers.requests_recovery</code>.
     */
    public final TableField<QrtzFiredTriggersRecord, Boolean> REQUESTS_RECOVERY = createField(DSL.name("requests_recovery"), SQLDataType.BOOLEAN, this, "");

    private QrtzFiredTriggers(Name alias, Table<QrtzFiredTriggersRecord> aliased) {
        this(alias, aliased, null);
    }

    private QrtzFiredTriggers(Name alias, Table<QrtzFiredTriggersRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.qrtz_fired_triggers</code> table reference
     */
    public QrtzFiredTriggers(String alias) {
        this(DSL.name(alias), QRTZ_FIRED_TRIGGERS);
    }

    /**
     * Create an aliased <code>public.qrtz_fired_triggers</code> table reference
     */
    public QrtzFiredTriggers(Name alias) {
        this(alias, QRTZ_FIRED_TRIGGERS);
    }

    /**
     * Create a <code>public.qrtz_fired_triggers</code> table reference
     */
    public QrtzFiredTriggers() {
        this(DSL.name("qrtz_fired_triggers"), null);
    }

    public <O extends Record> QrtzFiredTriggers(Table<O> child, ForeignKey<O, QrtzFiredTriggersRecord> key) {
        super(child, key, QRTZ_FIRED_TRIGGERS);
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.IDX_QRTZ_FT_INST_JOB_REQ_RCVRY, Indexes.IDX_QRTZ_FT_J_G, Indexes.IDX_QRTZ_FT_JG, Indexes.IDX_QRTZ_FT_T_G, Indexes.IDX_QRTZ_FT_TG, Indexes.IDX_QRTZ_FT_TRIG_INST_NAME);
    }

    @Override
    public UniqueKey<QrtzFiredTriggersRecord> getPrimaryKey() {
        return Keys.QRTZ_FIRED_TRIGGERS_PKEY;
    }

    @Override
    public List<UniqueKey<QrtzFiredTriggersRecord>> getKeys() {
        return Arrays.<UniqueKey<QrtzFiredTriggersRecord>>asList(Keys.QRTZ_FIRED_TRIGGERS_PKEY);
    }

    @Override
    public QrtzFiredTriggers as(String alias) {
        return new QrtzFiredTriggers(DSL.name(alias), this);
    }

    @Override
    public QrtzFiredTriggers as(Name alias) {
        return new QrtzFiredTriggers(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public QrtzFiredTriggers rename(String name) {
        return new QrtzFiredTriggers(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public QrtzFiredTriggers rename(Name name) {
        return new QrtzFiredTriggers(name, null);
    }

    // -------------------------------------------------------------------------
    // Row13 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row13<String, String, String, String, String, Long, Long, Integer, String, String, String, Boolean, Boolean> fieldsRow() {
        return (Row13) super.fieldsRow();
    }
}
