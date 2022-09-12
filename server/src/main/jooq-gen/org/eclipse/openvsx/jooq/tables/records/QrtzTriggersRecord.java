/*
 * This file is generated by jOOQ.
 */
package org.eclipse.openvsx.jooq.tables.records;


import org.eclipse.openvsx.jooq.tables.QrtzTriggers;
import org.jooq.Field;
import org.jooq.Record16;
import org.jooq.Record3;
import org.jooq.Row16;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class QrtzTriggersRecord extends UpdatableRecordImpl<QrtzTriggersRecord> implements Record16<String, String, String, String, String, String, Long, Long, Integer, String, String, Long, Long, String, Short, byte[]> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.qrtz_triggers.sched_name</code>.
     */
    public void setSchedName(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.sched_name</code>.
     */
    public String getSchedName() {
        return (String) get(0);
    }

    /**
     * Setter for <code>public.qrtz_triggers.trigger_name</code>.
     */
    public void setTriggerName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.trigger_name</code>.
     */
    public String getTriggerName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.qrtz_triggers.trigger_group</code>.
     */
    public void setTriggerGroup(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.trigger_group</code>.
     */
    public String getTriggerGroup() {
        return (String) get(2);
    }

    /**
     * Setter for <code>public.qrtz_triggers.job_name</code>.
     */
    public void setJobName(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.job_name</code>.
     */
    public String getJobName() {
        return (String) get(3);
    }

    /**
     * Setter for <code>public.qrtz_triggers.job_group</code>.
     */
    public void setJobGroup(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.job_group</code>.
     */
    public String getJobGroup() {
        return (String) get(4);
    }

    /**
     * Setter for <code>public.qrtz_triggers.description</code>.
     */
    public void setDescription(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.description</code>.
     */
    public String getDescription() {
        return (String) get(5);
    }

    /**
     * Setter for <code>public.qrtz_triggers.next_fire_time</code>.
     */
    public void setNextFireTime(Long value) {
        set(6, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.next_fire_time</code>.
     */
    public Long getNextFireTime() {
        return (Long) get(6);
    }

    /**
     * Setter for <code>public.qrtz_triggers.prev_fire_time</code>.
     */
    public void setPrevFireTime(Long value) {
        set(7, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.prev_fire_time</code>.
     */
    public Long getPrevFireTime() {
        return (Long) get(7);
    }

    /**
     * Setter for <code>public.qrtz_triggers.priority</code>.
     */
    public void setPriority(Integer value) {
        set(8, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.priority</code>.
     */
    public Integer getPriority() {
        return (Integer) get(8);
    }

    /**
     * Setter for <code>public.qrtz_triggers.trigger_state</code>.
     */
    public void setTriggerState(String value) {
        set(9, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.trigger_state</code>.
     */
    public String getTriggerState() {
        return (String) get(9);
    }

    /**
     * Setter for <code>public.qrtz_triggers.trigger_type</code>.
     */
    public void setTriggerType(String value) {
        set(10, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.trigger_type</code>.
     */
    public String getTriggerType() {
        return (String) get(10);
    }

    /**
     * Setter for <code>public.qrtz_triggers.start_time</code>.
     */
    public void setStartTime(Long value) {
        set(11, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.start_time</code>.
     */
    public Long getStartTime() {
        return (Long) get(11);
    }

    /**
     * Setter for <code>public.qrtz_triggers.end_time</code>.
     */
    public void setEndTime(Long value) {
        set(12, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.end_time</code>.
     */
    public Long getEndTime() {
        return (Long) get(12);
    }

    /**
     * Setter for <code>public.qrtz_triggers.calendar_name</code>.
     */
    public void setCalendarName(String value) {
        set(13, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.calendar_name</code>.
     */
    public String getCalendarName() {
        return (String) get(13);
    }

    /**
     * Setter for <code>public.qrtz_triggers.misfire_instr</code>.
     */
    public void setMisfireInstr(Short value) {
        set(14, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.misfire_instr</code>.
     */
    public Short getMisfireInstr() {
        return (Short) get(14);
    }

    /**
     * Setter for <code>public.qrtz_triggers.job_data</code>.
     */
    public void setJobData(byte[] value) {
        set(15, value);
    }

    /**
     * Getter for <code>public.qrtz_triggers.job_data</code>.
     */
    public byte[] getJobData() {
        return (byte[]) get(15);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record3<String, String, String> key() {
        return (Record3) super.key();
    }

    // -------------------------------------------------------------------------
    // Record16 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row16<String, String, String, String, String, String, Long, Long, Integer, String, String, Long, Long, String, Short, byte[]> fieldsRow() {
        return (Row16) super.fieldsRow();
    }

    @Override
    public Row16<String, String, String, String, String, String, Long, Long, Integer, String, String, Long, Long, String, Short, byte[]> valuesRow() {
        return (Row16) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return QrtzTriggers.QRTZ_TRIGGERS.SCHED_NAME;
    }

    @Override
    public Field<String> field2() {
        return QrtzTriggers.QRTZ_TRIGGERS.TRIGGER_NAME;
    }

    @Override
    public Field<String> field3() {
        return QrtzTriggers.QRTZ_TRIGGERS.TRIGGER_GROUP;
    }

    @Override
    public Field<String> field4() {
        return QrtzTriggers.QRTZ_TRIGGERS.JOB_NAME;
    }

    @Override
    public Field<String> field5() {
        return QrtzTriggers.QRTZ_TRIGGERS.JOB_GROUP;
    }

    @Override
    public Field<String> field6() {
        return QrtzTriggers.QRTZ_TRIGGERS.DESCRIPTION;
    }

    @Override
    public Field<Long> field7() {
        return QrtzTriggers.QRTZ_TRIGGERS.NEXT_FIRE_TIME;
    }

    @Override
    public Field<Long> field8() {
        return QrtzTriggers.QRTZ_TRIGGERS.PREV_FIRE_TIME;
    }

    @Override
    public Field<Integer> field9() {
        return QrtzTriggers.QRTZ_TRIGGERS.PRIORITY;
    }

    @Override
    public Field<String> field10() {
        return QrtzTriggers.QRTZ_TRIGGERS.TRIGGER_STATE;
    }

    @Override
    public Field<String> field11() {
        return QrtzTriggers.QRTZ_TRIGGERS.TRIGGER_TYPE;
    }

    @Override
    public Field<Long> field12() {
        return QrtzTriggers.QRTZ_TRIGGERS.START_TIME;
    }

    @Override
    public Field<Long> field13() {
        return QrtzTriggers.QRTZ_TRIGGERS.END_TIME;
    }

    @Override
    public Field<String> field14() {
        return QrtzTriggers.QRTZ_TRIGGERS.CALENDAR_NAME;
    }

    @Override
    public Field<Short> field15() {
        return QrtzTriggers.QRTZ_TRIGGERS.MISFIRE_INSTR;
    }

    @Override
    public Field<byte[]> field16() {
        return QrtzTriggers.QRTZ_TRIGGERS.JOB_DATA;
    }

    @Override
    public String component1() {
        return getSchedName();
    }

    @Override
    public String component2() {
        return getTriggerName();
    }

    @Override
    public String component3() {
        return getTriggerGroup();
    }

    @Override
    public String component4() {
        return getJobName();
    }

    @Override
    public String component5() {
        return getJobGroup();
    }

    @Override
    public String component6() {
        return getDescription();
    }

    @Override
    public Long component7() {
        return getNextFireTime();
    }

    @Override
    public Long component8() {
        return getPrevFireTime();
    }

    @Override
    public Integer component9() {
        return getPriority();
    }

    @Override
    public String component10() {
        return getTriggerState();
    }

    @Override
    public String component11() {
        return getTriggerType();
    }

    @Override
    public Long component12() {
        return getStartTime();
    }

    @Override
    public Long component13() {
        return getEndTime();
    }

    @Override
    public String component14() {
        return getCalendarName();
    }

    @Override
    public Short component15() {
        return getMisfireInstr();
    }

    @Override
    public byte[] component16() {
        return getJobData();
    }

    @Override
    public String value1() {
        return getSchedName();
    }

    @Override
    public String value2() {
        return getTriggerName();
    }

    @Override
    public String value3() {
        return getTriggerGroup();
    }

    @Override
    public String value4() {
        return getJobName();
    }

    @Override
    public String value5() {
        return getJobGroup();
    }

    @Override
    public String value6() {
        return getDescription();
    }

    @Override
    public Long value7() {
        return getNextFireTime();
    }

    @Override
    public Long value8() {
        return getPrevFireTime();
    }

    @Override
    public Integer value9() {
        return getPriority();
    }

    @Override
    public String value10() {
        return getTriggerState();
    }

    @Override
    public String value11() {
        return getTriggerType();
    }

    @Override
    public Long value12() {
        return getStartTime();
    }

    @Override
    public Long value13() {
        return getEndTime();
    }

    @Override
    public String value14() {
        return getCalendarName();
    }

    @Override
    public Short value15() {
        return getMisfireInstr();
    }

    @Override
    public byte[] value16() {
        return getJobData();
    }

    @Override
    public QrtzTriggersRecord value1(String value) {
        setSchedName(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value2(String value) {
        setTriggerName(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value3(String value) {
        setTriggerGroup(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value4(String value) {
        setJobName(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value5(String value) {
        setJobGroup(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value6(String value) {
        setDescription(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value7(Long value) {
        setNextFireTime(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value8(Long value) {
        setPrevFireTime(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value9(Integer value) {
        setPriority(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value10(String value) {
        setTriggerState(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value11(String value) {
        setTriggerType(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value12(Long value) {
        setStartTime(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value13(Long value) {
        setEndTime(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value14(String value) {
        setCalendarName(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value15(Short value) {
        setMisfireInstr(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord value16(byte[] value) {
        setJobData(value);
        return this;
    }

    @Override
    public QrtzTriggersRecord values(String value1, String value2, String value3, String value4, String value5, String value6, Long value7, Long value8, Integer value9, String value10, String value11, Long value12, Long value13, String value14, Short value15, byte[] value16) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        value14(value14);
        value15(value15);
        value16(value16);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached QrtzTriggersRecord
     */
    public QrtzTriggersRecord() {
        super(QrtzTriggers.QRTZ_TRIGGERS);
    }

    /**
     * Create a detached, initialised QrtzTriggersRecord
     */
    public QrtzTriggersRecord(String schedName, String triggerName, String triggerGroup, String jobName, String jobGroup, String description, Long nextFireTime, Long prevFireTime, Integer priority, String triggerState, String triggerType, Long startTime, Long endTime, String calendarName, Short misfireInstr, byte[] jobData) {
        super(QrtzTriggers.QRTZ_TRIGGERS);

        setSchedName(schedName);
        setTriggerName(triggerName);
        setTriggerGroup(triggerGroup);
        setJobName(jobName);
        setJobGroup(jobGroup);
        setDescription(description);
        setNextFireTime(nextFireTime);
        setPrevFireTime(prevFireTime);
        setPriority(priority);
        setTriggerState(triggerState);
        setTriggerType(triggerType);
        setStartTime(startTime);
        setEndTime(endTime);
        setCalendarName(calendarName);
        setMisfireInstr(misfireInstr);
        setJobData(jobData);
    }
}
