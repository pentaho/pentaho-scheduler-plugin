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

import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.gwtmockito.WithClassesToStub;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( GwtMockitoTestRunner.class )
@WithClassesToStub( Frame.class )
public class ScheduleParamsWizardPanelTest {
  private ScheduleParamsWizardPanel scheduleParamsWizardPanel;

  @Before
  public void setUp() throws Exception {
    scheduleParamsWizardPanel = mock( ScheduleParamsWizardPanel.class );
  }

  @Test
  public void testSetRadioParameterValue() throws Exception {
    doCallRealMethod().when( scheduleParamsWizardPanel ).setRadioParameterValue( anyString() );

    scheduleParamsWizardPanel.setRadioParameterValue( "?no_radio_param_test=true" );
    verify( scheduleParamsWizardPanel, never() ).setRadioButton( anyString() );

    final String value = "value";
    scheduleParamsWizardPanel.setRadioParameterValue( "?REPORT_FORMAT_TYPE=" + value );
    verify( scheduleParamsWizardPanel ).setRadioButton( value );
  }

  @Test
  public void testSchedulerParamsCompleteCallback() throws Exception {
    doCallRealMethod().when( scheduleParamsWizardPanel ).schedulerParamsCompleteCallback( anyBoolean() );

    scheduleParamsWizardPanel.parametersFrame = mock( Frame.class );

    final String url = "url";
    when( scheduleParamsWizardPanel.parametersFrame.getUrl() ).thenReturn( url );
    verifyComplete( url, true );
    final String url1 = "url1";
    when( scheduleParamsWizardPanel.parametersFrame.getUrl() ).thenReturn( url1 );
    verifyComplete( url1, false );
  }

  @Test
  public void testSetParametersUrl() throws Exception {
    doCallRealMethod().when( scheduleParamsWizardPanel ).setParametersUrl( anyString() );

    final Frame frame = mock( Frame.class );
    scheduleParamsWizardPanel.parametersFrame = frame;
    scheduleParamsWizardPanel.scheduleParameterPanel = mock( SimplePanel.class );
    scheduleParamsWizardPanel.setParametersUrl( null );
    verify( scheduleParamsWizardPanel.scheduleParameterPanel ).remove( frame );
    assertNull( scheduleParamsWizardPanel.parametersFrame );

    reset( scheduleParamsWizardPanel.scheduleParameterPanel );
    scheduleParamsWizardPanel.parametersFrame = null;
    final String url = "url";
    scheduleParamsWizardPanel.setParametersUrl( url );
    verify( scheduleParamsWizardPanel.scheduleParameterPanel ).add( any( Frame.class ) );
    verify( scheduleParamsWizardPanel ).setRadioParameterValue( anyString() );

    reset( scheduleParamsWizardPanel.scheduleParameterPanel, frame );
    scheduleParamsWizardPanel.parametersFrame = frame;
    when( frame.getUrl() ).thenReturn( url );
    scheduleParamsWizardPanel.setParametersUrl( url );
    verify( scheduleParamsWizardPanel.scheduleParameterPanel, never() ).add( any( Frame.class ) );
    verify( frame, never() ).setUrl( anyString() );

    final String newUrl = "new_url";
    scheduleParamsWizardPanel.setParametersUrl( newUrl );
    verify( scheduleParamsWizardPanel.scheduleParameterPanel, never() ).add( any( Frame.class ) );
    verify( frame ).setUrl( newUrl );
  }

  private void verifyComplete( String url, boolean complete ) {
    scheduleParamsWizardPanel.schedulerParamsCompleteCallback( complete );
    assertEquals( complete, scheduleParamsWizardPanel.parametersComplete );
    verify( scheduleParamsWizardPanel ).setCanContinue( complete );
    verify( scheduleParamsWizardPanel ).setCanFinish( complete );
    verify( scheduleParamsWizardPanel ).setRadioParameterValue( url );
  }
}
