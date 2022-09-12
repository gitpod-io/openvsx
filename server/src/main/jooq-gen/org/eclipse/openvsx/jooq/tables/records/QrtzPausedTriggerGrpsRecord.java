/*
 * This file is generated by jOOQ.
 */
package org.eclipse.openvsx.jooq.tables.records;


import org.eclipse.openvsx.jooq.tables.QrtzPausedTriggerGrps;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class QrtzPausedTriggerGrpsRecord extends UpdatableRecordImpl<QrtzPausedTriggerGrpsRecord> implements Record2<String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.qrtz_paused_trigger_grps.sched_name</code>.
     */
    public void setSchedName(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.qrtz_paused_trigger_grps.sched_name</code>.
     */
    public String getSchedName() {
        return (String) get(0);
    }

    /**
     * Setter for <code>public.qrtz_paused_trigger_grps.trigger_group</code>.
     */
    public void setTriggerGroup(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.qrtz_paused_trigger_grps.trigger_group</code>.
     */
    public String getTriggerGroup() {
        return (String) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<String, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<String, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<String, String> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return QrtzPausedTriggerGrps.QRTZ_PAUSED_TRIGGER_GRPS.SCHED_NAME;
    }

    @Override
    public Field<String> field2() {
        return QrtzPausedTriggerGrps.QRTZ_PAUSED_TRIGGER_GRPS.TRIGGER_GROUP;
    }

    @Override
    public String component1() {
        return getSchedName();
    }

    @Override
    public String component2() {
        return getTriggerGroup();
    }

    @Override
    public String value1() {
        return getSchedName();
    }

    @Override
    public String value2() {
        return getTriggerGroup();
    }

    @Override
    public QrtzPausedTriggerGrpsRecord value1(String value) {
        setSchedName(value);
        return this;
    }

    @Override
    public QrtzPausedTriggerGrpsRecord value2(String value) {
        setTriggerGroup(value);
        return this;
    }

    @Override
    public QrtzPausedTriggerGrpsRecord values(String value1, String value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached QrtzPausedTriggerGrpsRecord
     */
    public QrtzPausedTriggerGrpsRecord() {
        super(QrtzPausedTriggerGrps.QRTZ_PAUSED_TRIGGER_GRPS);
    }

    /**
     * Create a detached, initialised QrtzPausedTriggerGrpsRecord
     */
    public QrtzPausedTriggerGrpsRecord(String schedName, String triggerGroup) {
        super(QrtzPausedTriggerGrps.QRTZ_PAUSED_TRIGGER_GRPS);

        setSchedName(schedName);
        setTriggerGroup(triggerGroup);
    }
}
