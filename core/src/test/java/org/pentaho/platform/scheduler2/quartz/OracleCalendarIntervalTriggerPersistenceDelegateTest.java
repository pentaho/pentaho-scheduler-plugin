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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.SimplePropertiesTriggerProperties;
import org.quartz.spi.OperableTrigger;

public class OracleCalendarIntervalTriggerPersistenceDelegateTest {

  private TestableOracleCalendarIntervalTriggerPersistenceDelegate delegate;
  private Connection mockConnection;
  private PreparedStatement mockPreparedStatement;
  private ResultSet mockResultSet;
  private OperableTrigger mockTrigger;
  private JobDetail mockJobDetail;
  private TriggerKey triggerKey;

  @Before
  public void setUp() {
    delegate = new TestableOracleCalendarIntervalTriggerPersistenceDelegate();
    mockConnection = mock( Connection.class );
    mockPreparedStatement = mock( PreparedStatement.class );
    mockResultSet = mock( ResultSet.class );
    mockTrigger = mock( OperableTrigger.class );
    mockJobDetail = mock( JobDetail.class );

    triggerKey = new TriggerKey( "testTrigger", "testGroup" );
    when( mockTrigger.getKey() ).thenReturn( triggerKey );
  }

  private SimplePropertiesTriggerProperties createMockProperties() {
    SimplePropertiesTriggerProperties props = new SimplePropertiesTriggerProperties();
    props.setString1( "str1" );
    props.setString2( "str2" );
    props.setString3( "str3" );
    props.setInt1( 1 );
    props.setInt2( 2 );
    props.setLong1( 100L );
    props.setLong2( 200L );
    props.setDecimal1( new BigDecimal( "10.5" ) );
    props.setDecimal2( new BigDecimal( "20.5" ) );
    props.setBoolean1( true );
    props.setBoolean2( false );
    return props;
  }

  /**
   * Test subclass that exposes getTriggerProperties for mocking
   */
  private class TestableOracleCalendarIntervalTriggerPersistenceDelegate
    extends OracleCalendarIntervalTriggerPersistenceDelegate {
    private SimplePropertiesTriggerProperties mockProperties;

    public void setMockProperties( SimplePropertiesTriggerProperties props ) {
      this.mockProperties = props;
    }

    @Override
    protected SimplePropertiesTriggerProperties getTriggerProperties( OperableTrigger trigger ) {
      if ( mockProperties != null ) {
        return mockProperties;
      }
      return super.getTriggerProperties( trigger );
    }
  }

  @Test
  public void testInsertExtendedTriggerPropertiesSuccessfully() throws SQLException, IOException {
    delegate.setMockProperties( createMockProperties() );
    when( mockConnection.prepareStatement( anyString() ) ).thenReturn( mockPreparedStatement );
    when( mockPreparedStatement.executeUpdate() ).thenReturn( 1 );

    int result = delegate.insertExtendedTriggerProperties( mockConnection, mockTrigger, "WAITING", mockJobDetail );

    assertEquals( 1, result );
    verify( mockConnection ).prepareStatement( anyString() );
    verify( mockPreparedStatement ).setString( 1, "testTrigger" );
    verify( mockPreparedStatement ).setString( 2, "testGroup" );
    verify( mockPreparedStatement ).setString( 12, OracleCharBooleanDelegate.TRUE_VALUE );
    verify( mockPreparedStatement ).setString( 13, OracleCharBooleanDelegate.FALSE_VALUE );
    verify( mockPreparedStatement ).executeUpdate();
  }

  @Test
  public void testInsertExtendedTriggerPropertiesThrowsSQLException() throws SQLException, IOException {
    delegate.setMockProperties( createMockProperties() );
    when( mockConnection.prepareStatement( anyString() ) ).thenThrow( new SQLException( "Connection failed" ) );

    try {
      delegate.insertExtendedTriggerProperties( mockConnection, mockTrigger, "WAITING", mockJobDetail );
      fail( "Expected SQLException" );
    } catch ( SQLException e ) {
      assertEquals( "Connection failed", e.getMessage() );
    }
  }

  @Test
  public void testLoadExtendedTriggerPropertiesNoRecordFound() throws SQLException {
    when( mockConnection.prepareStatement( anyString() ) ).thenReturn( mockPreparedStatement );
    when( mockPreparedStatement.executeQuery() ).thenReturn( mockResultSet );
    when( mockResultSet.next() ).thenReturn( false );

    try {
      delegate.loadExtendedTriggerProperties( mockConnection, triggerKey );
      fail( "Expected IllegalStateException" );
    } catch ( IllegalStateException e ) {
      assertTrue( e.getMessage().contains( "No record found" ) );
    }
  }

  @Test
  public void testLoadExtendedTriggerPropertiesThrowsSQLException() throws SQLException {
    when( mockConnection.prepareStatement( anyString() ) ).thenThrow( new SQLException( "Connection failed" ) );

    try {
      delegate.loadExtendedTriggerProperties( mockConnection, triggerKey );
      fail( "Expected SQLException" );
    } catch ( SQLException e ) {
      assertEquals( "Connection failed", e.getMessage() );
    }
  }

  @Test
  public void testUpdateExtendedTriggerPropertiesSuccessfully() throws SQLException, IOException {
    delegate.setMockProperties( createMockProperties() );
    when( mockConnection.prepareStatement( anyString() ) ).thenReturn( mockPreparedStatement );
    when( mockPreparedStatement.executeUpdate() ).thenReturn( 1 );

    int result = delegate.updateExtendedTriggerProperties( mockConnection, mockTrigger, "WAITING", mockJobDetail );

    assertEquals( 1, result );
    verify( mockConnection ).prepareStatement( anyString() );
    verify( mockPreparedStatement ).setString( 10, OracleCharBooleanDelegate.TRUE_VALUE );
    verify( mockPreparedStatement ).setString( 11, OracleCharBooleanDelegate.FALSE_VALUE );
    verify( mockPreparedStatement ).setString( 12, "testTrigger" );
    verify( mockPreparedStatement ).setString( 13, "testGroup" );
    verify( mockPreparedStatement ).executeUpdate();
  }

  @Test
  public void testUpdateExtendedTriggerPropertiesThrowsSQLException() throws SQLException, IOException {
    delegate.setMockProperties( createMockProperties() );
    when( mockConnection.prepareStatement( anyString() ) ).thenThrow( new SQLException( "Connection failed" ) );

    try {
      delegate.updateExtendedTriggerProperties( mockConnection, mockTrigger, "WAITING", mockJobDetail );
      fail( "Expected SQLException" );
    } catch ( SQLException e ) {
      assertEquals( "Connection failed", e.getMessage() );
    }
  }

  @Test
  public void testLoadExtendedTriggerPropertiesSuccessfully() throws SQLException {
    when( mockConnection.prepareStatement( anyString() ) ).thenReturn( mockPreparedStatement );
    when( mockPreparedStatement.executeQuery() ).thenReturn( mockResultSet );
    when( mockResultSet.next() ).thenReturn( true );
    
    // Setup mock ResultSet to return distinct values for each property
    when( mockResultSet.getString( "STR_PROP_1" ) ).thenReturn( "value1" );
    when( mockResultSet.getString( "STR_PROP_2" ) ).thenReturn( "value2" );
    when( mockResultSet.getString( "STR_PROP_3" ) ).thenReturn( "value3" );
    when( mockResultSet.getInt( "INT_PROP_1" ) ).thenReturn( 10 );
    when( mockResultSet.getInt( "INT_PROP_2" ) ).thenReturn( 20 );
    when( mockResultSet.getLong( "LONG_PROP_1" ) ).thenReturn( 1000L );
    when( mockResultSet.getLong( "LONG_PROP_2" ) ).thenReturn( 2000L );
    when( mockResultSet.getBigDecimal( "DEC_PROP_1" ) ).thenReturn( new BigDecimal( "15.5" ) );
    when( mockResultSet.getBigDecimal( "DEC_PROP_2" ) ).thenReturn( new BigDecimal( "25.5" ) );
    when( mockResultSet.getString( "BOOL_PROP_1" ) ).thenReturn( OracleCharBooleanDelegate.TRUE_VALUE );
    when( mockResultSet.getString( "BOOL_PROP_2" ) ).thenReturn( OracleCharBooleanDelegate.FALSE_VALUE );

    // Create a testable subclass that properly handles the return type
    TestableLoadOracleCalendarIntervalTriggerPersistenceDelegate testDelegate = 
      new TestableLoadOracleCalendarIntervalTriggerPersistenceDelegate();
    Object result = testDelegate.loadExtendedTriggerProperties( mockConnection, triggerKey );

    // Verify the result is not null
    assertNotNull( result );
    
    // Verify PreparedStatement was configured correctly
    verify( mockConnection ).prepareStatement( anyString() );
    verify( mockPreparedStatement ).setString( 1, triggerKey.getName() );
    verify( mockPreparedStatement ).setString( 2, triggerKey.getGroup() );
    verify( mockPreparedStatement ).executeQuery();
    
    // Verify that the captured properties have the expected values
    assertEquals( "value1", testDelegate.capturedProperties.getString1() );
    assertEquals( "value2", testDelegate.capturedProperties.getString2() );
    assertEquals( "value3", testDelegate.capturedProperties.getString3() );
    assertEquals( 10, testDelegate.capturedProperties.getInt1() );
    assertEquals( 20, testDelegate.capturedProperties.getInt2() );
    assertEquals( 1000L, testDelegate.capturedProperties.getLong1() );
    assertEquals( 2000L, testDelegate.capturedProperties.getLong2() );
    assertEquals( new BigDecimal( "15.5" ), testDelegate.capturedProperties.getDecimal1() );
    assertEquals( new BigDecimal( "25.5" ), testDelegate.capturedProperties.getDecimal2() );
    assertEquals( true, testDelegate.capturedProperties.isBoolean1() );
    assertEquals( false, testDelegate.capturedProperties.isBoolean2() );
  }

  /**
   * Testable subclass for capturing properties during load
   */
  private static class TestableLoadOracleCalendarIntervalTriggerPersistenceDelegate
    extends OracleCalendarIntervalTriggerPersistenceDelegate {
    public SimplePropertiesTriggerProperties capturedProperties;

    @Override
    protected TriggerPropertyBundle getTriggerPropertyBundle( SimplePropertiesTriggerProperties properties ) {
      this.capturedProperties = properties;
      // Bypass super to avoid enum parsing of mock string values
      return new TriggerPropertyBundle( CalendarIntervalScheduleBuilder.calendarIntervalSchedule(), null, null );
    }
  }
}
