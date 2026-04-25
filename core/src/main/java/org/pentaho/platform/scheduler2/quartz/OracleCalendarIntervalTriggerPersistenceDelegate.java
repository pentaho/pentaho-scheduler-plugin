package org.pentaho.platform.scheduler2.quartz;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.CalendarIntervalTriggerPersistenceDelegate;
import org.quartz.impl.jdbcjobstore.Util;
import org.quartz.spi.OperableTrigger;

public class OracleCalendarIntervalTriggerPersistenceDelegate extends CalendarIntervalTriggerPersistenceDelegate {

  @Override
  public int insertExtendedTriggerProperties( Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail )
    throws SQLException, IOException {
    return OracleTriggerPersistenceHelper.insertTriggerProperties(
      conn, trigger, getTriggerProperties( trigger ),
      Util.rtp( INSERT_SIMPLE_PROPS_TRIGGER, tablePrefix, schedNameLiteral ) );
  }

  @Override
  public TriggerPropertyBundle loadExtendedTriggerProperties( Connection conn, TriggerKey triggerKey ) throws SQLException {
    return OracleTriggerPersistenceHelper.loadTriggerProperties(
      conn, triggerKey,
      Util.rtp( SELECT_SIMPLE_PROPS_TRIGGER, tablePrefix, schedNameLiteral ),
      Util.rtp( SELECT_SIMPLE_TRIGGER, tablePrefix, schedNameLiteral ),
      this::getTriggerPropertyBundle );
  }

  @Override
  public int updateExtendedTriggerProperties( Connection conn, OperableTrigger trigger, String state, JobDetail jobDetail )
    throws SQLException, IOException {
    return OracleTriggerPersistenceHelper.updateTriggerProperties(
      conn, trigger, getTriggerProperties( trigger ),
      Util.rtp( UPDATE_SIMPLE_PROPS_TRIGGER, tablePrefix, schedNameLiteral ) );
  }

}
