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

import com.google.gwt.user.client.ui.TextBox;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.gwt.widgets.client.controls.DateRangeEditor;
import org.pentaho.gwt.widgets.client.controls.ErrorLabel;
import org.pentaho.gwt.widgets.client.utils.TimeUtil;
import static junit.framework.TestCase.assertEquals;

import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith( GwtMockitoTestRunner.class )
public class CronEditorTest {
  private CronEditor cronEditor;

  @Before
  public void setUp() throws Exception {
    cronEditor = mock( CronEditor.class );
  }

  @Test
  public void testReset() throws Exception {
    doCallRealMethod().when( cronEditor ).reset( any( Date.class ) );

    cronEditor.cronTb = mock( TextBox.class );
    cronEditor.dateRangeEditor = mock( DateRangeEditor.class );

    final Date date = new Date();
    cronEditor.reset( date );
    verify( cronEditor.cronTb ).setText( "" );
    verify( cronEditor.dateRangeEditor ).reset( date );
  }

  @Test
  public void testGetCronString() throws Exception {
    doCallRealMethod().when( cronEditor ).getCronString();
    cronEditor.cronTb = mock( TextBox.class );
    final String test = "test";
    when( cronEditor.cronTb.getText() ).thenReturn( test );

    assertEquals( test, cronEditor.getCronString() );
  }

  @Test
  public void testGetStartDate() throws Exception {
    doCallRealMethod().when( cronEditor ).getStartDate();
    cronEditor.dateRangeEditor = mock( DateRangeEditor.class );
    final Date date = new Date();
    when( cronEditor.dateRangeEditor.getStartDate() ).thenReturn( date );

    assertEquals( date, cronEditor.getStartDate() );
  }

  @Test
  public void testSetStartDate() throws Exception {
    doCallRealMethod().when( cronEditor ).setStartDate( any( Date.class ) );
    cronEditor.dateRangeEditor = mock( DateRangeEditor.class );

    final Date date = new Date();
    cronEditor.setStartDate( date );
    verify( cronEditor.dateRangeEditor ).setStartDate( date );
  }

  @Test
  public void testGetEndDate() throws Exception {
    doCallRealMethod().when( cronEditor ).getEndDate();
    cronEditor.dateRangeEditor = mock( DateRangeEditor.class );
    final Date date = new Date();
    when( cronEditor.dateRangeEditor.getEndDate() ).thenReturn( date );

    assertEquals( date, cronEditor.getEndDate() );
  }

  @Test
  public void testSetEndDate() throws Exception {
    doCallRealMethod().when( cronEditor ).setEndDate( any( Date.class ) );
    cronEditor.dateRangeEditor = mock( DateRangeEditor.class );

    final Date date = new Date();
    cronEditor.setEndDate( date );
    verify( cronEditor.dateRangeEditor ).setEndDate( date );
  }

  @Test
  public void testSetNoEndDate() throws Exception {
    doCallRealMethod().when( cronEditor ).setNoEndDate();
    cronEditor.dateRangeEditor = mock( DateRangeEditor.class );

    cronEditor.setNoEndDate();
    verify( cronEditor.dateRangeEditor ).setNoEndDate();
  }

  @Test
  public void testIsEndBy() throws Exception {
    doCallRealMethod().when( cronEditor ).isEndBy();
    cronEditor.dateRangeEditor = mock( DateRangeEditor.class );

    final boolean bool = true;
    when( cronEditor.dateRangeEditor.isEndBy() ).thenReturn( bool );
    assertEquals( bool, cronEditor.isEndBy() );
  }

  @Test
  public void testSetEndBy() throws Exception {
    doCallRealMethod().when( cronEditor ).setEndBy();
    cronEditor.dateRangeEditor = mock( DateRangeEditor.class );

    cronEditor.setEndBy();
    verify( cronEditor.dateRangeEditor ).setEndBy();
  }

  @Test
  public void testIsNoEndDate() throws Exception {
    doCallRealMethod().when( cronEditor ).isNoEndDate();
    cronEditor.dateRangeEditor = mock( DateRangeEditor.class );

    final boolean bool = true;
    when( cronEditor.dateRangeEditor.isNoEndDate() ).thenReturn( bool );
    assertEquals( bool, cronEditor.isNoEndDate() );
  }

  @Test
  public void testGetStartTime() throws Exception {
    doCallRealMethod().when( cronEditor ).getStartTime();

    assertEquals( "00:00:00", cronEditor.getStartTime() );
  }

  @Test
  public void testSetCronError() throws Exception {
    doCallRealMethod().when( cronEditor ).setCronError( anyString() );
    cronEditor.cronLabel = mock( ErrorLabel.class );

    final String test = "test";
    cronEditor.setCronError( test );
    verify( cronEditor.cronLabel ).setErrorMsg( test );
  }
}
