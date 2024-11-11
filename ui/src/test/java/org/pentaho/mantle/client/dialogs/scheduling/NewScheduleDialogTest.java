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

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pentaho.gwt.widgets.client.wizards.AbstractWizardDialog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings( "deprecation" )
@RunWith( GwtMockitoTestRunner.class )
public class NewScheduleDialogTest {
  @Mock
  private NewScheduleDialog dialog;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks( this );
    TextBox textBoxMock = mock( TextBox.class );
    ListBox listBoxMock = mock( ListBox.class );
    CheckBox checkBoxMock = mock( CheckBox.class );
    CaptionPanel captionPanelMock = mock( CaptionPanel.class );
    Label labelMock = mock( Label.class );

    // schedule name ui
    when( dialog.getScheduleNameTextBox() ).thenReturn( textBoxMock );
    when( dialog.getTimestampListBox() ).thenReturn( listBoxMock );
    when( dialog.getAppendTimeCheckbox() ).thenReturn( checkBoxMock );
    when( dialog.getPreviewCaptionPanel() ).thenReturn( captionPanelMock );
    when( dialog.getScheduleNamePreviewLabel() ).thenReturn( labelMock );

    TextBox textBoxMock2 = mock( TextBox.class );
    CheckBox checkBoxMock2 = mock( CheckBox.class );
    Button buttonMock = mock( Button.class );
    // schedule location ui
    when( dialog.getScheduleLocationTextBox() ).thenReturn( textBoxMock2 );
    when( dialog.getSelectLocationButton() ).thenReturn( buttonMock );
    when( dialog.getOverrideExistingCheckbox() ).thenReturn( checkBoxMock2 );
  }

  @Test
  public void testCreateScheduleNameUI() {
    doCallRealMethod().when( dialog ).createScheduleNameUI();
    dialog.createScheduleNameUI();

    verify( dialog.getScheduleNameTextBox() ).addStyleName( ScheduleEditor.SCHEDULE_INPUT );
    verify( dialog.getScheduleNameTextBox() ).addKeyUpHandler( any( KeyUpHandler.class ) );
    verify( dialog.getScheduleNameTextBox() ).addChangeHandler( any( ChangeHandler.class ) );

    verify( dialog.getTimestampListBox(), atLeastOnce() ).addItem( anyString() );
    verify( dialog.getTimestampListBox() ).addStyleName( "schedule-timestamp-listbox" );
    verify( dialog.getTimestampListBox() ).addChangeHandler( any( ChangeHandler.class ) );

    verify( dialog.getAppendTimeCheckbox() ).setText( "appendTimeToName" );
    verify( dialog.getAppendTimeCheckbox() ).addClickHandler( any( ClickHandler.class ) );

    verify( dialog.getPreviewCaptionPanel() ).setStyleName( "schedule-caption-panel" );
    verify( dialog.getPreviewCaptionPanel() ).setVisible( false );

    verify( dialog.getScheduleNamePreviewLabel() ).addStyleName( "schedule-name-preview" );
  }

  @Test
  public void testCreateScheduleLocationUI() {
    doCallRealMethod().when( dialog ).createScheduleLocationUI();
    dialog.createScheduleLocationUI();

    verify( dialog.getScheduleLocationTextBox() ).addStyleName( ScheduleEditor.SCHEDULE_INPUT );
    verify( dialog.getScheduleLocationTextBox() ).setEnabled( false );
    verify( dialog.getScheduleLocationTextBox() ).addChangeHandler( any( ChangeHandler.class ) );

    verify( dialog.getSelectLocationButton() ).setStyleName( AbstractWizardDialog.PENTAHO_BUTTON );
    verify( dialog.getSelectLocationButton() ).addStyleName( ScheduleEditor.SCHEDULE_BUTTON );
    verify( dialog.getSelectLocationButton() ).addClickHandler( any( ClickHandler.class ) );
  }

  @Test
  public void testCanSubmit() {
    doCallRealMethod().when( dialog ).canSubmit();
    when( dialog.getScheduleLocation() ).thenReturn( "location" );
    when( dialog.getScheduleName() ).thenReturn( "name" );

    assertTrue( dialog.canSubmit() );
  }

  @Test
  public void testCanSubmit_invalidLocation() {
    doCallRealMethod().when( dialog ).canSubmit();
    when( dialog.getScheduleLocation() ).thenReturn( "" );
    when( dialog.getScheduleName() ).thenReturn( "name" );

    assertFalse( dialog.canSubmit() );
  }

  @Test
  public void testCanSubmit_invalidScheduleName() {
    doCallRealMethod().when( dialog ).canSubmit();
    when( dialog.getScheduleLocation() ).thenReturn( "location" );
    when( dialog.getScheduleName() ).thenReturn( "" );

    assertFalse( dialog.canSubmit() );
  }
}
