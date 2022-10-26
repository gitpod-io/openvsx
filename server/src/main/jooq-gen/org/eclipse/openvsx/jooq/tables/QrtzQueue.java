/*
 * This file is generated by jOOQ.
 */
package org.eclipse.openvsx.jooq.tables;


import java.util.Arrays;
import java.util.List;

import org.eclipse.openvsx.jooq.Keys;
import org.eclipse.openvsx.jooq.Public;
import org.eclipse.openvsx.jooq.tables.records.QrtzQueueRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row6;
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
public class QrtzQueue extends TableImpl<QrtzQueueRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.qrtz_queue</code>
     */
    public static final QrtzQueue QRTZ_QUEUE = new QrtzQueue();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<QrtzQueueRecord> getRecordType() {
        return QrtzQueueRecord.class;
    }

    /**
     * The column <code>public.qrtz_queue.sched_name</code>.
     */
    public final TableField<QrtzQueueRecord, String> SCHED_NAME = createField(DSL.name("sched_name"), SQLDataType.VARCHAR(120).nullable(false), this, "");

    /**
     * The column <code>public.qrtz_queue.queue_name</code>.
     */
    public final TableField<QrtzQueueRecord, String> QUEUE_NAME = createField(DSL.name("queue_name"), SQLDataType.VARCHAR(200).nullable(false), this, "");

    /**
     * The column <code>public.qrtz_queue.job_name</code>.
     */
    public final TableField<QrtzQueueRecord, String> JOB_NAME = createField(DSL.name("job_name"), SQLDataType.VARCHAR(200).nullable(false), this, "");

    /**
     * The column <code>public.qrtz_queue.job_group</code>.
     */
    public final TableField<QrtzQueueRecord, String> JOB_GROUP = createField(DSL.name("job_group"), SQLDataType.VARCHAR(200).nullable(false), this, "");

    /**
     * The column <code>public.qrtz_queue.priority</code>.
     */
    public final TableField<QrtzQueueRecord, Integer> PRIORITY = createField(DSL.name("priority"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>public.qrtz_queue.state</code>.
     */
    public final TableField<QrtzQueueRecord, String> STATE = createField(DSL.name("state"), SQLDataType.VARCHAR(16).nullable(false), this, "");

    private QrtzQueue(Name alias, Table<QrtzQueueRecord> aliased) {
        this(alias, aliased, null);
    }

    private QrtzQueue(Name alias, Table<QrtzQueueRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.qrtz_queue</code> table reference
     */
    public QrtzQueue(String alias) {
        this(DSL.name(alias), QRTZ_QUEUE);
    }

    /**
     * Create an aliased <code>public.qrtz_queue</code> table reference
     */
    public QrtzQueue(Name alias) {
        this(alias, QRTZ_QUEUE);
    }

    /**
     * Create a <code>public.qrtz_queue</code> table reference
     */
    public QrtzQueue() {
        this(DSL.name("qrtz_queue"), null);
    }

    public <O extends Record> QrtzQueue(Table<O> child, ForeignKey<O, QrtzQueueRecord> key) {
        super(child, key, QRTZ_QUEUE);
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public UniqueKey<QrtzQueueRecord> getPrimaryKey() {
        return Keys.QRTZ_QUEUE_PKEY;
    }

    @Override
    public List<UniqueKey<QrtzQueueRecord>> getKeys() {
        return Arrays.<UniqueKey<QrtzQueueRecord>>asList(Keys.QRTZ_QUEUE_PKEY);
    }

    @Override
    public QrtzQueue as(String alias) {
        return new QrtzQueue(DSL.name(alias), this);
    }

    @Override
    public QrtzQueue as(Name alias) {
        return new QrtzQueue(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public QrtzQueue rename(String name) {
        return new QrtzQueue(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public QrtzQueue rename(Name name) {
        return new QrtzQueue(name, null);
    }

    // -------------------------------------------------------------------------
    // Row6 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row6<String, String, String, String, Integer, String> fieldsRow() {
        return (Row6) super.fieldsRow();
    }
}