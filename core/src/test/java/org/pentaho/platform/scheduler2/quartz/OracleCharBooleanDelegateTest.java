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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.Test;

public class OracleCharBooleanDelegateTest {

  private static final String COLUMN_NAME = "IS_DURABLE";

  private final OracleCharBooleanDelegate delegate = new OracleCharBooleanDelegate();

  @Test
  public void testSetBooleanWritesSingleCharacterTrue() throws Exception {
    PreparedStatement statement = mock( PreparedStatement.class );

    delegate.setBoolean( statement, 2, true );

    verify( statement ).setString( 2, OracleCharBooleanDelegate.TRUE_VALUE );
  }

  @Test
  public void testSetBooleanWritesSingleCharacterFalse() throws Exception {
    PreparedStatement statement = mock( PreparedStatement.class );

    delegate.setBoolean( statement, 3, false );

    verify( statement ).setString( 3, OracleCharBooleanDelegate.FALSE_VALUE );
  }

  @Test
  public void testGetBooleanSupportsSingleCharacterValues() throws Exception {
    ResultSet resultSet = mock( ResultSet.class );

    when( resultSet.getString( COLUMN_NAME ) ).thenReturn( "1", "0" );

    assertTrue( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertFalse( delegate.getBoolean( resultSet, COLUMN_NAME ) );
  }

  @Test
  public void testGetBooleanSupportsTextValues() throws Exception {
    ResultSet resultSet = mock( ResultSet.class );

    when( resultSet.getString( COLUMN_NAME ) ).thenReturn( "true", "false", "Y", "N" );

    assertTrue( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertFalse( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertTrue( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertFalse( delegate.getBoolean( resultSet, COLUMN_NAME ) );
  }

  @Test
  public void testGetBooleanFallsBackToJdbcParsingForUnexpectedValues() throws Exception {
    ResultSet resultSet = mock( ResultSet.class );

    when( resultSet.getString( COLUMN_NAME ) ).thenReturn( "unexpected" );
    when( resultSet.getBoolean( COLUMN_NAME ) ).thenReturn( true );

    assertTrue( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    verify( resultSet ).getBoolean( COLUMN_NAME );
  }

  @Test
  public void testGetBooleanHandlesNullValue() throws Exception {
    ResultSet resultSet = mock( ResultSet.class );

    when( resultSet.getString( COLUMN_NAME ) ).thenReturn( null );

    assertFalse( delegate.getBoolean( resultSet, COLUMN_NAME ) );
  }

  @Test
  public void testGetBooleanHandlesWhitespace() throws Exception {
    ResultSet resultSet = mock( ResultSet.class );

    when( resultSet.getString( COLUMN_NAME ) ).thenReturn( "  1  ", "  0  ", "  true  ", "  false  " );

    assertTrue( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertFalse( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertTrue( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertFalse( delegate.getBoolean( resultSet, COLUMN_NAME ) );
  }

  @Test
  public void testGetBooleanHandlesMixedCase() throws Exception {
    ResultSet resultSet = mock( ResultSet.class );

    when( resultSet.getString( COLUMN_NAME ) ).thenReturn( "TRUE", "FALSE", "T", "F", "Y", "N" );

    assertTrue( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertFalse( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertTrue( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertFalse( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertTrue( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertFalse( delegate.getBoolean( resultSet, COLUMN_NAME ) );
  }

  @Test
  public void testSetBooleanWithVariousParameterIndices() throws Exception {
    PreparedStatement statement = mock( PreparedStatement.class );

    delegate.setBoolean( statement, 1, true );
    delegate.setBoolean( statement, 5, false );
    delegate.setBoolean( statement, 10, true );

    verify( statement ).setString( 1, OracleCharBooleanDelegate.TRUE_VALUE );
    verify( statement ).setString( 5, OracleCharBooleanDelegate.FALSE_VALUE );
    verify( statement ).setString( 10, OracleCharBooleanDelegate.TRUE_VALUE );
  }

  @Test
  public void testGetBooleanWithLowercaseValues() throws Exception {
    ResultSet resultSet = mock( ResultSet.class );

    when( resultSet.getString( COLUMN_NAME ) ).thenReturn( "y", "n", "t", "f" );

    assertTrue( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertFalse( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertTrue( delegate.getBoolean( resultSet, COLUMN_NAME ) );
    assertFalse( delegate.getBoolean( resultSet, COLUMN_NAME ) );
  }

  @Test
  public void testGetBooleanWithEmptyString() throws Exception {
    ResultSet resultSet = mock( ResultSet.class );

    when( resultSet.getString( COLUMN_NAME ) ).thenReturn( "" );
    when( resultSet.getBoolean( COLUMN_NAME ) ).thenReturn( false );

    assertFalse( delegate.getBoolean( resultSet, COLUMN_NAME ) );
  }

  @Test
  public void testSetBooleanMultipleValues() throws Exception {
    PreparedStatement statement = mock( PreparedStatement.class );

    delegate.setBoolean( statement, 1, true );
    delegate.setBoolean( statement, 2, true );
    delegate.setBoolean( statement, 3, false );
    delegate.setBoolean( statement, 4, false );

    verify( statement ).setString( 1, OracleCharBooleanDelegate.TRUE_VALUE );
    verify( statement ).setString( 2, OracleCharBooleanDelegate.TRUE_VALUE );
    verify( statement ).setString( 3, OracleCharBooleanDelegate.FALSE_VALUE );
    verify( statement ).setString( 4, OracleCharBooleanDelegate.FALSE_VALUE );
  }
}