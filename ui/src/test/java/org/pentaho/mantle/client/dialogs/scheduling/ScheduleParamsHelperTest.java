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
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.gwtmockito.WithClassesToStub;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( GwtMockitoTestRunner.class )
@WithClassesToStub( { JSONArray.class, JSONString.class } )
public class ScheduleParamsHelperTest {

  /**
   * Sentinel exception used to short-circuit the real {@link ScheduleParamsHelper#getScheduleParams}
   * call immediately after the type-safe extraction, before {@code buildScheduleParam} performs raw
   * GWT {@code JavaScriptObject} operations that cannot run under GwtMockito.
   */
  private static final class ExtractionReached extends RuntimeException {
  }

  /**
   * When a valid date format string is present, the helper must extract it through the type-safe
   * {@code isString().stringValue()} path (the BISERVER-15708 fix) rather than the old
   * {@code toString() + substring()} approach.
   */
  @Test
  public void testGetScheduleParams_appendDateFormatValidString_isExtractedViaStringValue() {
    JSONObject jobSchedule = mock( JSONObject.class );
    JSONString dateFormatValue = mock( JSONString.class );
    when( jobSchedule.get( ScheduleParamsHelper.APPEND_DATE_FORMAT_KEY ) ).thenReturn( dateFormatValue );
    when( dateFormatValue.isString() ).thenReturn( dateFormatValue );
    // Short-circuit right after the type-safe extraction, before the un-mockable JSO build step.
    when( dateFormatValue.stringValue() ).thenThrow( new ExtractionReached() );
    when( jobSchedule.get( ScheduleParamsHelper.OVERWRITE_FILE_KEY ) ).thenReturn( null );
    when( jobSchedule.get( ScheduleParamsHelper.JOB_PARAMETERS_KEY ) ).thenReturn( null );

    try {
      ScheduleParamsHelper.getScheduleParams( jobSchedule, new ArrayList<>() );
      fail( "Expected the type-safe extraction path (stringValue()) to be reached" );
    } catch ( ExtractionReached expected ) {
      // The type-safe extraction path was taken.
    }

    verify( dateFormatValue, atLeastOnce() ).isString();
    verify( dateFormatValue ).stringValue();
  }

  /**
   * Primary regression test for BISERVER-15708: a non-string value (e.g. {@code JSONNull}) must be
   * skipped by the {@code isString() != null} guard, so the broken {@code toString()}/{@code substring()}
   * path is never taken.
   */
  @Test
  public void testGetScheduleParams_appendDateFormatNonString_isSkipped() {
    JSONObject jobSchedule = mock( JSONObject.class );
    JSONValue nonStringValue = mock( JSONValue.class );
    when( jobSchedule.get( ScheduleParamsHelper.APPEND_DATE_FORMAT_KEY ) ).thenReturn( nonStringValue );
    // A JSONNull / non-string value returns null from isString().
    when( nonStringValue.isString() ).thenReturn( null );
    when( jobSchedule.get( ScheduleParamsHelper.OVERWRITE_FILE_KEY ) ).thenReturn( null );
    when( jobSchedule.get( ScheduleParamsHelper.JOB_PARAMETERS_KEY ) ).thenReturn( null );

    ScheduleParamsHelper.getScheduleParams( jobSchedule, new ArrayList<>() );

    // The guard must be consulted; because isString() is null the broken extraction path is skipped.
    verify( nonStringValue ).isString();
  }

  /**
   * When the {@code appendDateFormat} key is absent, the helper must complete without attempting any
   * extraction.
   */
  @Test
  public void testGetScheduleParams_appendDateFormatKeyAbsent_isSkipped() {
    JSONObject jobSchedule = mock( JSONObject.class );
    when( jobSchedule.get( ScheduleParamsHelper.APPEND_DATE_FORMAT_KEY ) ).thenReturn( null );
    when( jobSchedule.get( ScheduleParamsHelper.OVERWRITE_FILE_KEY ) ).thenReturn( null );
    when( jobSchedule.get( ScheduleParamsHelper.JOB_PARAMETERS_KEY ) ).thenReturn( null );

    ScheduleParamsHelper.getScheduleParams( jobSchedule, new ArrayList<>() );

    verify( jobSchedule ).get( ScheduleParamsHelper.APPEND_DATE_FORMAT_KEY );
  }

  /**
   * A non-string {@code overwriteFile} value must be skipped by the same type-safe guard.
   */
  @Test
  public void testGetScheduleParams_overwriteFileNonString_isSkipped() {
    JSONObject jobSchedule = mock( JSONObject.class );
    JSONValue nonStringValue = mock( JSONValue.class );
    when( jobSchedule.get( ScheduleParamsHelper.APPEND_DATE_FORMAT_KEY ) ).thenReturn( null );
    when( jobSchedule.get( ScheduleParamsHelper.OVERWRITE_FILE_KEY ) ).thenReturn( nonStringValue );
    when( nonStringValue.isString() ).thenReturn( null );
    when( jobSchedule.get( ScheduleParamsHelper.JOB_PARAMETERS_KEY ) ).thenReturn( null );

    ScheduleParamsHelper.getScheduleParams( jobSchedule, new ArrayList<>() );

    verify( nonStringValue ).isString();
  }

  /**
   * When {@code overwriteFile} is the string {@code "true"}, the helper extracts it via the type-safe
   * {@code stringValue()} path so it can add the {@code autoCreateUniqueFilename} parameter.
   */
  @Test
  public void testGetScheduleParams_overwriteFileTrueString_isExtractedViaStringValue() {
    JSONObject jobSchedule = mock( JSONObject.class );
    JSONString overwriteValue = mock( JSONString.class );
    when( jobSchedule.get( ScheduleParamsHelper.APPEND_DATE_FORMAT_KEY ) ).thenReturn( null );
    when( jobSchedule.get( ScheduleParamsHelper.OVERWRITE_FILE_KEY ) ).thenReturn( overwriteValue );
    when( overwriteValue.isString() ).thenReturn( overwriteValue );
    // Short-circuit right after the type-safe extraction, before the un-mockable JSO build step.
    when( overwriteValue.stringValue() ).thenThrow( new ExtractionReached() );
    when( jobSchedule.get( ScheduleParamsHelper.JOB_PARAMETERS_KEY ) ).thenReturn( null );

    try {
      ScheduleParamsHelper.getScheduleParams( jobSchedule, new ArrayList<>() );
      fail( "Expected the type-safe extraction path (stringValue()) to be reached" );
    } catch ( ExtractionReached expected ) {
      // The type-safe extraction path was taken.
    }

    verify( overwriteValue, atLeastOnce() ).isString();
    verify( overwriteValue ).stringValue();
  }
}
