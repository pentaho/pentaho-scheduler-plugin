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

import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.apache.xpath.operations.Bool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith( GwtMockitoTestRunner.class )
public class ScheduleEmailWizardPanelTest {
  private ScheduleEmailWizardPanel scheduleEmailWizardPanel;

  @Before
  public void setUp() throws Exception {
    scheduleEmailWizardPanel = mock( ScheduleEmailWizardPanel.class );
  }

  @Test
  public void testGetEmailParams() throws Exception {
    doCallRealMethod().when( scheduleEmailWizardPanel ).getEmailParams();

    scheduleEmailWizardPanel.yes = mock( RadioButton.class );
    scheduleEmailWizardPanel.toAddressTextBox = mock( TextBox.class );
    scheduleEmailWizardPanel.subjectTextBox = mock( TextBox.class );
    scheduleEmailWizardPanel.messageTextArea = mock( TextArea.class );
    scheduleEmailWizardPanel.attachmentNameTextBox = mock( TextBox.class );

    when( scheduleEmailWizardPanel.yes.getValue() ).thenReturn( false );
    assertNull( scheduleEmailWizardPanel.getEmailParams() );

    when( scheduleEmailWizardPanel.yes.getValue() ).thenReturn( true );
    assertNotNull( scheduleEmailWizardPanel.getEmailParams() );
  }

  @Test
  public void testIsValidConfig() throws Exception {
    doCallRealMethod().when( scheduleEmailWizardPanel ).isValidConfig();

    scheduleEmailWizardPanel.no = mock( RadioButton.class );

    when( scheduleEmailWizardPanel.no.getValue() ).thenReturn( true );
    assertTrue( scheduleEmailWizardPanel.isValidConfig() );

    when( scheduleEmailWizardPanel.no.getValue() ).thenReturn( false );
    scheduleEmailWizardPanel.toAddressTextBox = mock( TextBox.class );
    when( scheduleEmailWizardPanel.toAddressTextBox.getText() ).thenReturn( "" );
    assertFalse( scheduleEmailWizardPanel.isValidConfig() );

    when( scheduleEmailWizardPanel.toAddressTextBox.getText() ).thenReturn( "wrong_email" );
    assertFalse( scheduleEmailWizardPanel.isValidConfig() );

    when( scheduleEmailWizardPanel.toAddressTextBox.getText() ).thenReturn( "correct@email" );
    assertTrue( scheduleEmailWizardPanel.isValidConfig() );
  }

  @Test
  public void testPanelWidgetChanged() throws Exception {
    doCallRealMethod().when( scheduleEmailWizardPanel ).panelWidgetChanged( any( Widget.class ) );

    checkValidConfig( true );
    checkValidConfig( false );
  }

  private void checkValidConfig( boolean value ) {
    when( scheduleEmailWizardPanel.isValidConfig() ).thenReturn( value );
    scheduleEmailWizardPanel.panelWidgetChanged( mock( Widget.class ) );
    verify( scheduleEmailWizardPanel ).setCanContinue( value );
    verify( scheduleEmailWizardPanel ).setCanFinish( value );
  }

  @Test
  public void testApplyDateFormat() throws Exception {
    String[] numPatterns = {"yyyy-MM-dd", "yyyyMMdd", "yyyyMMddHHmmss", "MM-dd-yyyy", "MM-dd-yy", "dd-MM-yyyy"} ;
    String startTime = "2024-02-23T12:29:00.000-05:00";
    String[] supposedResult = {"2024-02-23", "20240223", "20240223122900", "02-23-2024", "02-23-24", "23-02-2024"};
    for (int i = 0; i < numPatterns.length; i++){
      String resultDate = scheduleEmailWizardPanel.applyDateFormat(numPatterns[i], startTime);
      if(supposedResult[i].equals(resultDate)){
        assertTrue( true );
      }
      else {
        assertFalse( false );
      }
    }
  }
}
