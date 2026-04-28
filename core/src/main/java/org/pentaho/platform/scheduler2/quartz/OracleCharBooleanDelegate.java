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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.quartz.impl.jdbcjobstore.CronTriggerPersistenceDelegate;
import org.quartz.impl.jdbcjobstore.SimpleTriggerPersistenceDelegate;
import org.quartz.impl.jdbcjobstore.oracle.OracleDelegate;

public class OracleCharBooleanDelegate extends OracleDelegate {

  static final String TRUE_VALUE = "1";
  static final String FALSE_VALUE = "0";

  @Override
  protected void setBoolean( PreparedStatement statement, int parameterIndex, boolean value ) throws SQLException {
    statement.setString( parameterIndex, value ? TRUE_VALUE : FALSE_VALUE );
  }

  @Override
  protected boolean getBoolean( ResultSet resultSet, String columnName ) throws SQLException {
    return readOracleBoolean( resultSet, columnName );
  }

  /**
   * Static utility method to read Oracle boolean values from ResultSet.
   * Supports multiple boolean formats: "1"/"0", "true"/"false", "t"/"f", "y"/"n"
   * with case-insensitive handling.
   *
   * @param resultSet the ResultSet to read from
   * @param columnName the column name
   * @return the boolean value
   * @throws SQLException if a database access error occurs
   */
  public static boolean readOracleBoolean( ResultSet resultSet, String columnName ) throws SQLException {
    String value = resultSet.getString( columnName );

    if ( value == null ) {
      return false;
    }

    switch ( value.trim().toLowerCase( Locale.ROOT ) ) {
      case TRUE_VALUE:
      case "true":
      case "t":
      case "y":
        return true;
      case FALSE_VALUE:
      case "false":
      case "f":
      case "n":
        return false;
      default:
        return resultSet.getBoolean( columnName );
    }
  }

  @Override
  protected void addDefaultTriggerPersistenceDelegates() {
    addTriggerPersistenceDelegate( new SimpleTriggerPersistenceDelegate() );
    addTriggerPersistenceDelegate( new CronTriggerPersistenceDelegate() );
    addTriggerPersistenceDelegate( new OracleCalendarIntervalTriggerPersistenceDelegate() );
    addTriggerPersistenceDelegate( new OracleDailyTimeIntervalTriggerPersistenceDelegate() );
  }
}