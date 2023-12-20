/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2023 Hitachi Vantara. All rights reserved.
 */

package org.pentaho.mantle.client.dialogs.scheduling;

import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickHandler;
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

    // schedule name ui
    when( dialog.getScheduleNameTextBox() ).thenReturn( mock( TextBox.class ) );
    when( dialog.getTimestampListBox() ).thenReturn( mock( ListBox.class ) );
    when( dialog.getAppendTimeCheckbox() ).thenReturn( mock( CheckBox.class ) );
    when( dialog.getPreviewCaptionPanel() ).thenReturn( mock( CaptionPanel.class ) );
    when( dialog.getScheduleNamePreviewLabel() ).thenReturn( mock( Label.class ) );

    // schedule location ui
    when( dialog.getScheduleLocationTextBox() ).thenReturn( mock( TextBox.class ) );
    when( dialog.getSelectLocationButton() ).thenReturn( mock( Button.class ) );
    when( dialog.getOverrideExistingCheckbox() ).thenReturn( mock( CheckBox.class ) );
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
