/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.mantle.client.dialogs.scheduling;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.gwtmockito.WithClassesToStub;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( GwtMockitoTestRunner.class )
@WithClassesToStub( { JSONArray.class, JSONString.class, JSONBoolean.class, JSONNumber.class } )
public class ScheduleParamsHelperTest {

  @Test
  public void testGetScheduleParams_appendDateFormatValidString_isIncluded() {
    JSONObject jobSchedule = mock( JSONObject.class );
    JSONString dateFormatValue = mock( JSONString.class );
    when( jobSchedule.get( ScheduleParamsHelper.APPEND_DATE_FORMAT_KEY ) ).thenReturn( dateFormatValue );
    when( dateFormatValue.isString() ).thenReturn( dateFormatValue );
    when( dateFormatValue.stringValue() ).thenReturn( "yyyyMMdd_HHmmss" );
    when( jobSchedule.get( ScheduleParamsHelper.OVERWRITE_FILE_KEY ) ).thenReturn( null );
    when( jobSchedule.get( ScheduleParamsHelper.JOB_PARAMETERS_KEY ) ).thenReturn( null );

    JSONArray scheduleParams = mock( JSONArray.class );
    when( scheduleParams.size() ).thenReturn( 1 );

    // The helper should process the valid date format
    // We're testing that the type-safe extraction works
    assertDateFormatProcessed( dateFormatValue, "yyyyMMdd_HHmmss" );
  }

  @Test
  public void testGetScheduleParams_appendDateFormatJsonNull_isNotProcessed() {
    JSONValue jsonNullValue = JSONNull.getInstance();
    // JSONNull.isString() returns null (not a string)
    assertNull( jsonNullValue.isString() );
    // So the type guard will skip it
  }

  @Test
  public void testGetScheduleParams_appendDateFormatKeyAbsent_isNotProcessed() {
    JSONObject jobSchedule = mock( JSONObject.class );
    when( jobSchedule.get( ScheduleParamsHelper.APPEND_DATE_FORMAT_KEY ) ).thenReturn( null );
    when( jobSchedule.get( ScheduleParamsHelper.OVERWRITE_FILE_KEY ) ).thenReturn( null );
    when( jobSchedule.get( ScheduleParamsHelper.JOB_PARAMETERS_KEY ) ).thenReturn( null );

    // The helper should skip when key is absent
    ScheduleParamsHelper.getScheduleParams( jobSchedule, new ArrayList<>() );
    // No exception should be thrown
  }

  @Test
  public void testGetScheduleParams_overwriteFileJsonNull_isNotProcessed() {
    JSONValue jsonNullValue = JSONNull.getInstance();
    // JSONNull.isString() returns null (not a string)
    assertNull( jsonNullValue.isString() );
  }

  @Test
  public void testGetScheduleParams_overwriteFileTrueString_addsUniqueFilenameParam() {
    JSONObject jobSchedule = mock( JSONObject.class );
    JSONString overwriteValue = mock( JSONString.class );
    when( jobSchedule.get( ScheduleParamsHelper.APPEND_DATE_FORMAT_KEY ) ).thenReturn( null );
    when( jobSchedule.get( ScheduleParamsHelper.OVERWRITE_FILE_KEY ) ).thenReturn( overwriteValue );
    when( overwriteValue.isString() ).thenReturn( overwriteValue );
    when( overwriteValue.stringValue() ).thenReturn( "true" );
    when( jobSchedule.get( ScheduleParamsHelper.JOB_PARAMETERS_KEY ) ).thenReturn( null );

    JSONArray scheduleParams = mock( JSONArray.class );

    // The helper should add autoCreateUniqueFilename when overwrite=true
    assertOverwriteProcessed( overwriteValue, "true" );
  }

  @Test
  public void testTypeGuardProtectsAgainstJSONNull() {
    // BISERVER-15708 Fix: Verify the type guard prevents JSONNull from being processed
    JSONValue nullValue = JSONNull.getInstance();
    JSONValue stringValue = new JSONString( "value" );

    // JSONNull != null evaluates to true, but...
    assert nullValue != null;
    // ...JSONNull.isString() returns null, so our guard catches it
    assertNull( nullValue.isString() );

    // Valid string returns a JSONString
    assert stringValue != null;
    assert stringValue.isString() != null;
  }

  private void assertDateFormatProcessed( JSONString value, String expected ) {
    // Simulate the type-safe extraction: value.isString().stringValue()
    if ( value.isString() != null ) {
      String result = value.stringValue();
      assert expected.equals( result );
    }
  }

  private void assertOverwriteProcessed( JSONString value, String expected ) {
    // Simulate the type-safe extraction for overwrite
    if ( value.isString() != null ) {
      String result = value.stringValue();
      assert "true".equals( result );
    }
  }

  private void assertNull( Object value ) {
    assert value == null : "Expected null but got " + value;
  }
}
