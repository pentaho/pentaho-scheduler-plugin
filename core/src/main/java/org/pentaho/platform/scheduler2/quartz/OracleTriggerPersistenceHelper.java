package org.pentaho.platform.scheduler2.quartz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.SimplePropertiesTriggerProperties;
import org.quartz.impl.jdbcjobstore.TriggerPersistenceDelegate.TriggerPropertyBundle;
import org.quartz.impl.jdbcjobstore.Util;
import org.quartz.spi.OperableTrigger;

/**
 * Shared helper for Oracle-specific trigger persistence operations.
 * Eliminates duplicate logic between {@link OracleCalendarIntervalTriggerPersistenceDelegate}
 * and {@link OracleDailyTimeIntervalTriggerPersistenceDelegate}.
 */
class OracleTriggerPersistenceHelper {

  private static final String COL_STR_PROP_1 = "STR_PROP_1";
  private static final String COL_STR_PROP_2 = "STR_PROP_2";
  private static final String COL_STR_PROP_3 = "STR_PROP_3";
  private static final String COL_INT_PROP_1 = "INT_PROP_1";
  private static final String COL_INT_PROP_2 = "INT_PROP_2";
  private static final String COL_LONG_PROP_1 = "LONG_PROP_1";
  private static final String COL_LONG_PROP_2 = "LONG_PROP_2";
  private static final String COL_DEC_PROP_1 = "DEC_PROP_1";
  private static final String COL_DEC_PROP_2 = "DEC_PROP_2";
  private static final String COL_BOOL_PROP_1 = "BOOL_PROP_1";
  private static final String COL_BOOL_PROP_2 = "BOOL_PROP_2";

  private OracleTriggerPersistenceHelper() {
    // utility class
  }

  static int insertTriggerProperties( Connection conn, OperableTrigger trigger,
                                      SimplePropertiesTriggerProperties properties,
                                      String preparedSql ) throws SQLException {
    PreparedStatement ps = null;
    try {
      ps = conn.prepareStatement( preparedSql );
      ps.setString( 1, trigger.getKey().getName() );
      ps.setString( 2, trigger.getKey().getGroup() );
      ps.setString( 3, properties.getString1() );
      ps.setString( 4, properties.getString2() );
      ps.setString( 5, properties.getString3() );
      ps.setInt( 6, properties.getInt1() );
      ps.setInt( 7, properties.getInt2() );
      ps.setLong( 8, properties.getLong1() );
      ps.setLong( 9, properties.getLong2() );
      ps.setBigDecimal( 10, properties.getDecimal1() );
      ps.setBigDecimal( 11, properties.getDecimal2() );
      ps.setString( 12, properties.isBoolean1() ? OracleCharBooleanDelegate.TRUE_VALUE : OracleCharBooleanDelegate.FALSE_VALUE );
      ps.setString( 13, properties.isBoolean2() ? OracleCharBooleanDelegate.TRUE_VALUE : OracleCharBooleanDelegate.FALSE_VALUE );
      return ps.executeUpdate();
    } finally {
      Util.closeStatement( ps );
    }
  }

  static TriggerPropertyBundle loadTriggerProperties( Connection conn, TriggerKey triggerKey,
                                                       String selectSql, String errorSql,
                                                       Function<SimplePropertiesTriggerProperties, TriggerPropertyBundle> bundleFactory )
    throws SQLException {
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      ps = conn.prepareStatement( selectSql );
      ps.setString( 1, triggerKey.getName() );
      ps.setString( 2, triggerKey.getGroup() );
      rs = ps.executeQuery();

      if ( rs.next() ) {
        SimplePropertiesTriggerProperties properties = new SimplePropertiesTriggerProperties();
        properties.setString1( rs.getString( COL_STR_PROP_1 ) );
        properties.setString2( rs.getString( COL_STR_PROP_2 ) );
        properties.setString3( rs.getString( COL_STR_PROP_3 ) );
        properties.setInt1( rs.getInt( COL_INT_PROP_1 ) );
        properties.setInt2( rs.getInt( COL_INT_PROP_2 ) );
        properties.setLong1( rs.getLong( COL_LONG_PROP_1 ) );
        properties.setLong2( rs.getLong( COL_LONG_PROP_2 ) );
        properties.setDecimal1( rs.getBigDecimal( COL_DEC_PROP_1 ) );
        properties.setDecimal2( rs.getBigDecimal( COL_DEC_PROP_2 ) );
        properties.setBoolean1( OracleCharBooleanDelegate.readOracleBoolean( rs, COL_BOOL_PROP_1 ) );
        properties.setBoolean2( OracleCharBooleanDelegate.readOracleBoolean( rs, COL_BOOL_PROP_2 ) );
        return bundleFactory.apply( properties );
      }

      throw new IllegalStateException( "No record found for selection of Trigger with key: '" + triggerKey
        + "' and statement: " + errorSql );
    } finally {
      Util.closeResultSet( rs );
      Util.closeStatement( ps );
    }
  }

  static int updateTriggerProperties( Connection conn, OperableTrigger trigger,
                                      SimplePropertiesTriggerProperties properties,
                                      String preparedSql ) throws SQLException {
    PreparedStatement ps = null;
    try {
      ps = conn.prepareStatement( preparedSql );
      ps.setString( 1, properties.getString1() );
      ps.setString( 2, properties.getString2() );
      ps.setString( 3, properties.getString3() );
      ps.setInt( 4, properties.getInt1() );
      ps.setInt( 5, properties.getInt2() );
      ps.setLong( 6, properties.getLong1() );
      ps.setLong( 7, properties.getLong2() );
      ps.setBigDecimal( 8, properties.getDecimal1() );
      ps.setBigDecimal( 9, properties.getDecimal2() );
      ps.setString( 10, properties.isBoolean1() ? OracleCharBooleanDelegate.TRUE_VALUE : OracleCharBooleanDelegate.FALSE_VALUE );
      ps.setString( 11, properties.isBoolean2() ? OracleCharBooleanDelegate.TRUE_VALUE : OracleCharBooleanDelegate.FALSE_VALUE );
      ps.setString( 12, trigger.getKey().getName() );
      ps.setString( 13, trigger.getKey().getGroup() );
      return ps.executeUpdate();
    } finally {
      Util.closeStatement( ps );
    }
  }
}
