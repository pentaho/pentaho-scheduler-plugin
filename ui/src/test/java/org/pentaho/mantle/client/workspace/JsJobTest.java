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


package org.pentaho.mantle.client.workspace;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( GwtMockitoTestRunner.class )
public class JsJobTest {

  private JsJob jsJob;

  @Before
  public void setUp() throws Exception {
    jsJob = mock( JsJob.class );
  }

  @Test
  public void getInputFilePath_nullParam() {

    doCallRealMethod().when( jsJob ).getInputFilePath();

    // test JobParamValue = null
    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) ).thenReturn( null );
    String resourceName = jsJob.getInputFilePath();

    verify( jsJob, times( 1 ) ).getJobName();
    assertNull( resourceName );
  }

  @Test
  public void getInputFilePath_validParam_camelCase() {

    doCallRealMethod().when( jsJob ).getInputFilePath();

    // test JobParamValue = some_valid_value
    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /some_valid_value:outputFile = /another_valid_value" );
    String resourceName = jsJob.getInputFilePath();
    
    assertEquals( "/some_valid_value", resourceName );
  }

  @Test
  public void getInputFilePath_validParam_spaceCase() {

    doCallRealMethod().when( jsJob ).getInputFilePath();

    // test JobParamValue = some_valid_value
    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
     .thenReturn( "input file = /some_valid_value:output file=/another_valid_value" );
    String resourceName = jsJob.getInputFilePath();

    assertEquals( "/some_valid_value", resourceName );
  }

  @Test
  public void getScheduledExtn_basicPaths() {
    doCallRealMethod().when( jsJob ).getScheduledExtn();
    doCallRealMethod().when( jsJob ).getInputFilePath();

    // Test jobs scheduled from PUC
    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" ) )
      .thenReturn( "/home/admin/myTransformation.ktr" );
    assertEquals( "transformation", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" ) )
      .thenReturn( "/home/admin/myJob.kjb" );
    assertEquals( "job", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" ) )
      .thenReturn( "/home/admin/myReport.prpt" );
    assertEquals( "report", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" ) )
      .thenReturn( "/home/admin/myReport2.prpti" );
    assertEquals( "report", jsJob.getScheduledExtn() );

    // Test jobs scheduled from Kettle
    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" ) )
      .thenReturn( null );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/myTransformation.ktr:outputFile = /home/admin/myTransformation.*" );
    assertEquals( "transformation", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/myJob.kjb:outputFile = /home/admin/myJob.*" );
    assertEquals( "job", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/myReport.prpt:outputFile = /home/admin/myReport.*" );
    assertEquals( "report", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/myReport2.prpti:outputFile = /home/admin/myReport2.*" );
    assertEquals( "report", jsJob.getScheduledExtn() );
  }

  @Test
  public void getScheduledExtn_unusualPaths() {
    doCallRealMethod().when( jsJob ).getScheduledExtn();
    doCallRealMethod().when( jsJob ).getInputFilePath();

    // Test jobs scheduled from PUC
    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" ) )
      .thenReturn( "/home/admin/with.dot:colon/myTransformation.ktr" );
    assertEquals( "transformation", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" ) )
      .thenReturn( "/home/admin/with.dot:colon/myJob.kjb" );
    assertEquals( "job", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" ) )
      .thenReturn( "/home/admin/with.dot:colon/myReport.prpt" );
    assertEquals( "report", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" ) )
      .thenReturn( "/home/admin/with.dot/myReport2.prpti" );
    assertEquals( "report", jsJob.getScheduledExtn() );

    // Test jobs scheduled from Kettle
    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" ) )
      .thenReturn( null );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/with.dot:colon/myTransformation.ktr:outputFile = /home/admin/with.dot:colon/myTransformation.*" );
    assertEquals( "transformation", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/with.dot:colon/myJob.kjb:outputFile = /home/admin/with.dot:colon/myJob.*" );
    assertEquals( "job", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/with.dot:colon/myReport.prpt:outputFile = /home/admin/with.dot:colon/myReport.*" );
    assertEquals( "report", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/with.dot:colon/myReport2.prpti:outputFile = /home/admin/with.dot:colon/myReport2.*" );
    assertEquals( "report", jsJob.getScheduledExtn() );
  }

  @Test
  public void getScheduledExtn_invalidExtension() {
    doCallRealMethod().when( jsJob ).getScheduledExtn();

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" ) )
      .thenReturn( "/home/admin/wrong.txt" );
    assertEquals( "-", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/wrong.mp3:outputFile = /home/admin/wrong.*" );
    assertEquals( "-", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider-InputFile" ) )
      .thenReturn( "/home/admin/with.dot:colon/wrong.pdf" );
    assertEquals( "-", jsJob.getScheduledExtn() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/with.dot:colon/wrong.zip:outputFile = /home/admin/with.dot:colon/wrong.*" );
    assertEquals( "-", jsJob.getScheduledExtn() );
  }

  @Test
  public void getOutputPath() {
    doCallRealMethod().when( jsJob ).getOutputPath();

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/myTransformation.ktr:outputFile = /home/admin/myTransformation.*" );
    assertEquals( "/home/admin", jsJob.getOutputPath() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/with.dot:colon/myJob.kjb:outputFile = /home/admin/with.dot:colon/myJob.*" );
    assertEquals( "/home/admin/with.dot:colon", jsJob.getOutputPath() );

    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
      .thenReturn( "input file = /home/admin/with.dot:colon=equals/myJob.kjb:outputFile = /home/admin/myJob.*" );
    assertEquals( "/home/admin", jsJob.getOutputPath() );

    // ":output file=" format
    when( jsJob.getJobParamValue( "ActionAdapterQuartzJob-StreamProvider" ) )
     .thenReturn( "input file = /home/admin/myTransformation.ktr:output file=/home/admin/myTransformation.*" );
    assertEquals( "/home/admin", jsJob.getOutputPath() );
  }
}
