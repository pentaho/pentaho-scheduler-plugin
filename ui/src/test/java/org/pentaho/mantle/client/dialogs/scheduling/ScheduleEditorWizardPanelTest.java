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

import com.google.gwt.user.client.ui.ListBox;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.gwt.widgets.client.controls.TimePicker;
import org.pentaho.mantle.client.dialogs.scheduling.validators.ScheduleEditorValidator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( GwtMockitoTestRunner.class )
public class ScheduleEditorWizardPanelTest {
  private ScheduleEditorWizardPanel scheduleEditorWizardPanel;

  @Before
  public void setUp() throws Exception {
    scheduleEditorWizardPanel = mock( ScheduleEditorWizardPanel.class );
  }

  @Test
  public void testCanContinue() throws Exception {
    doCallRealMethod().when( scheduleEditorWizardPanel ).canContinue();

    scheduleEditorWizardPanel.scheduleEditorValidator = mock( ScheduleEditorValidator.class );

    when( scheduleEditorWizardPanel.scheduleEditorValidator.isValid() ).thenReturn( true );
    assertTrue( scheduleEditorWizardPanel.canContinue() );

    when( scheduleEditorWizardPanel.scheduleEditorValidator.isValid() ).thenReturn( false );
    assertFalse( scheduleEditorWizardPanel.canContinue() );
  }

  @Test
  public void testCanFinish() throws Exception {
    doCallRealMethod().when( scheduleEditorWizardPanel ).canFinish();

    scheduleEditorWizardPanel.scheduleEditorValidator = mock( ScheduleEditorValidator.class );

    when( scheduleEditorWizardPanel.scheduleEditorValidator.isValid() ).thenReturn( true );
    assertTrue( scheduleEditorWizardPanel.canFinish() );

    when( scheduleEditorWizardPanel.scheduleEditorValidator.isValid() ).thenReturn( false );
    assertFalse( scheduleEditorWizardPanel.canFinish() );
  }

  @Test
  public void testGetBlockoutStartTime() throws Exception {
    doCallRealMethod().when( scheduleEditorWizardPanel ).getBlockoutStartTime();

    scheduleEditorWizardPanel.scheduleEditor = mock( ScheduleEditor.class );

    when( scheduleEditorWizardPanel.scheduleEditor.getStartTimePicker() ).thenReturn( null );
    assertNull( scheduleEditorWizardPanel.getBlockoutStartTime() );

    final TimePicker timePicker = mock( TimePicker.class );
    final String time = "time";
    when( timePicker.getTime() ).thenReturn( time );
    when( scheduleEditorWizardPanel.scheduleEditor.getStartTimePicker() ).thenReturn( timePicker );
    assertEquals( time, scheduleEditorWizardPanel.getBlockoutStartTime() );
  }

  @Test
  public void testGetBlockoutEndTime() throws Exception {
    doCallRealMethod().when( scheduleEditorWizardPanel ).getBlockoutEndTime();

    scheduleEditorWizardPanel.scheduleEditor = mock( ScheduleEditor.class );

    when( scheduleEditorWizardPanel.scheduleEditor.getBlockoutEndTimePicker() ).thenReturn( null );
    assertNull( scheduleEditorWizardPanel.getBlockoutEndTime() );

    final TimePicker timePicker = mock( TimePicker.class );
    final String time = "time";
    when( timePicker.getTime() ).thenReturn( time );
    when( scheduleEditorWizardPanel.scheduleEditor.getBlockoutEndTimePicker() ).thenReturn( timePicker );
    assertEquals( time, scheduleEditorWizardPanel.getBlockoutEndTime() );
  }

  @Test
  public void testGetTimeZone() throws Exception {
    doCallRealMethod().when( scheduleEditorWizardPanel ).getTimeZone();

    scheduleEditorWizardPanel.scheduleEditor = mock( ScheduleEditor.class );

    when( scheduleEditorWizardPanel.scheduleEditor.getTimeZonePicker() ).thenReturn( null );
    assertNull( scheduleEditorWizardPanel.getTimeZone() );

    final ListBox listBox = mock( ListBox.class );
    when( scheduleEditorWizardPanel.scheduleEditor.getTimeZonePicker() ).thenReturn( listBox );

    when( listBox.getSelectedIndex() ).thenReturn( -1 );
    assertNull( scheduleEditorWizardPanel.getTimeZone() );

    final int selIndex = 1;
    when( listBox.getSelectedIndex() ).thenReturn( selIndex );
    final String selTZ = "GMT";
    when( listBox.getValue( selIndex ) ).thenReturn( selTZ );
    assertEquals( selTZ, scheduleEditorWizardPanel.getTimeZone() );
  }
}
