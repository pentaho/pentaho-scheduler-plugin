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