/*!
 * HITACHI VANTARA PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2025 Hitachi Vantara. All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Hitachi Vantara and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Hitachi Vantara and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Hitachi Vantara is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Hitachi Vantara,
 * explicitly covering such access.
 */

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
