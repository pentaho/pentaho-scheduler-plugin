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

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.gwt.widgets.client.dialogs.PromptDialogBox;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyChar;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

@RunWith( GwtMockitoTestRunner.class )
public class ScheduleRecurrenceDialogTest {
  private ScheduleRecurrenceDialog scheduleRecurrenceDialog;

  @Before
  public void setUp() throws Exception {
    scheduleRecurrenceDialog = mock( ScheduleRecurrenceDialog.class );
  }

  @Test
  public void testOnKeyDownPreview() throws Exception {
    doCallRealMethod().when( scheduleRecurrenceDialog ).onKeyDownPreview( anyChar(), anyInt() );

    assertTrue( scheduleRecurrenceDialog.onKeyDownPreview( (char) KeyCodes.KEY_ENTER, -1 ) );
    verify( scheduleRecurrenceDialog, never() ).hide();

    assertTrue( scheduleRecurrenceDialog.onKeyDownPreview( (char) KeyCodes.KEY_ESCAPE, -1 ) );
    verify( scheduleRecurrenceDialog ).hide();
  }

  @Test
  public void testAddCustomPanel() throws Exception {
    doCallRealMethod().when( scheduleRecurrenceDialog ).addCustomPanel( any( Widget.class ), any(
        DockPanel.DockLayoutConstant.class ) );

    scheduleRecurrenceDialog.scheduleEditorWizardPanel = mock( ScheduleEditorWizardPanel.class );

    final DockPanel.DockLayoutConstant position = mock( DockPanel.DockLayoutConstant.class );
    final Widget widget = mock( Widget.class );
    scheduleRecurrenceDialog.addCustomPanel( widget, position );
    verify( scheduleRecurrenceDialog.scheduleEditorWizardPanel ).add( widget, position );
  }

  @Test
  public void testBackClicked() throws Exception {
    doCallRealMethod().when( scheduleRecurrenceDialog ).backClicked();

    doCallRealMethod().when( scheduleRecurrenceDialog ).setParentDialog( any( PromptDialogBox.class ) );

    final PromptDialogBox parentDialog = mock( PromptDialogBox.class );
    scheduleRecurrenceDialog.setParentDialog( parentDialog );
    scheduleRecurrenceDialog.backClicked();
    verify( scheduleRecurrenceDialog ).hide();
    verify( parentDialog ).center();
  }
}
