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

import com.google.gwt.json.client.JSONObject;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.gwt.widgets.client.dialogs.IDialogCallback;
import org.pentaho.mantle.client.workspace.JsJob;
import org.pentaho.mantle.client.workspace.JsJobTrigger;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith( GwtMockitoTestRunner.class )
public class NewBlockoutScheduleDialogTest {

  @Test
  public void testOnFinish() throws Exception {
    final NewBlockoutScheduleDialog dialog = mock( NewBlockoutScheduleDialog.class );
    doCallRealMethod().when( dialog ).onFinish();

    final JsJobTrigger jsJobTrigger = mock( JsJobTrigger.class );
    when( dialog.getJsJobTrigger() ).thenReturn( jsJobTrigger );
    final JSONObject schedule = mock( JSONObject.class );
    when( dialog.getSchedule() ).thenReturn( schedule );
    final IDialogCallback callback = mock( IDialogCallback.class );
    when( dialog.getCallback() ).thenReturn( callback );

    dialog.updateMode = false;
    assertTrue( dialog.onFinish() );
    verify( dialog ).addBlockoutPeriod( eq( schedule ), eq( jsJobTrigger ), contains( "add" ) );
    verify( callback ).okPressed();

    dialog.updateMode = true;
    dialog.editJob = mock( JsJob.class );
    when( dialog.editJob.getJobId() ).thenReturn( "jobID" );
    assertTrue( dialog.onFinish() );
    verify( dialog ).addBlockoutPeriod( eq( schedule ), eq( jsJobTrigger ), contains( "update" ) );
    verify( callback, times( 2 ) ).okPressed();
  }

  @Test
  public void testSetUpdateMode() throws Exception {
    final NewBlockoutScheduleDialog dialog = mock( NewBlockoutScheduleDialog.class );
    doCallRealMethod().when( dialog ).setUpdateMode();

    dialog.updateMode = false;
    dialog.setUpdateMode();
    assertTrue( dialog.updateMode );
    verify( dialog ).setNewSchedule( dialog.updateMode );
  }
}
