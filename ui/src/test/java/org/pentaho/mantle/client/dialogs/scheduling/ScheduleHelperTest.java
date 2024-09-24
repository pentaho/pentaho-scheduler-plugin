/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/

package org.pentaho.mantle.client.dialogs.scheduling;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.mantle.client.workspace.JsJob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( GwtMockitoTestRunner.class )
public class ScheduleHelperTest {
  public static final String JOB_ID = "JobID123";

  @Test
  public void testBuildJobRequest() throws Exception {
    JsJob job = null;
    JSONObject requestPayload = null;

    // null job so use job URL
    ScheduleHelper helper = mock( ScheduleHelper.class );
    RequestBuilder requestBuilder = ScheduleHelper.buildRequestForJob( job, requestPayload );
    assertTrue( requestBuilder.getUrl().endsWith( ScheduleHelper.JOB_SCHEDULER_URL ) );
    assertNull( requestPayload );
    assertHeaders( requestBuilder );

    // no Job ID so use job URL
    job = mock( JsJob.class );
    when( job.getJobId() ).thenReturn( null );
    requestBuilder = ScheduleHelper.buildRequestForJob( job, requestPayload );
    assertTrue( requestBuilder.getUrl().endsWith( ScheduleHelper.JOB_SCHEDULER_URL ) );
    assertNull( requestPayload );
    assertHeaders( requestBuilder );

    // Job ID so use update URL, payload is null so nothing is set
    job = mock( JsJob.class );
    when( job.getJobId() ).thenReturn( JOB_ID );
    requestBuilder = ScheduleHelper.buildRequestForJob( job, requestPayload );
    assertTrue( requestBuilder.getUrl().endsWith( ScheduleHelper.UPDATE_JOB_SCHEDULER_URL ) );
    assertNull( requestPayload );
    assertHeaders( requestBuilder );

    // Job ID so use update URL, payload has Job ID
    job = mock( JsJob.class );
    when( job.getJobId() ).thenReturn( JOB_ID );
    requestPayload = mock( JSONObject.class );
    requestBuilder = ScheduleHelper.buildRequestForJob( job, requestPayload );
    assertTrue( requestBuilder.getUrl().endsWith( ScheduleHelper.UPDATE_JOB_SCHEDULER_URL ) );
    assertNotNull( requestPayload );
    verify( requestPayload ).put( "jobId", new JSONString( JOB_ID ) );
    assertHeaders( requestBuilder );
  }

  public void assertHeaders( RequestBuilder builder ) {
    assertEquals( "01 Jan 1970 00:00:00 GMT", builder.getHeader( "If-Modified-Since" ) );
    assertEquals( "application/json", builder.getHeader( "Content-Type" ) );
  }
}
