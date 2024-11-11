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

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( GwtMockitoTestRunner.class )
public class AdditionalDetailsPanelTest {
  private AdditionalDetailsPanel additionalDetail;

  @Before
  public void setUp() throws Exception {
    additionalDetail = mock( AdditionalDetailsPanel.class );
    additionalDetail.logLevel = mock( ListBox.class );
    additionalDetail.enableSafeMode = mock( CheckBox.class );
    additionalDetail.gatherMetrics = mock( CheckBox.class );
  }

  @Test
  @SuppressWarnings( "deprecation" )
  public void testGetEnableSafeMode() throws Exception {
    final boolean enableSafeMode = false;
    when( additionalDetail.enableSafeMode.isChecked() ).thenReturn( enableSafeMode );
    assertEquals( enableSafeMode, additionalDetail.getEnableSafeMode() );
  }

  @Test
  @SuppressWarnings( "deprecation" )
  public void testGetGatherMetrics() throws Exception {
    final boolean gatherMetrics = false;
    when( additionalDetail.gatherMetrics.isChecked() ).thenReturn( gatherMetrics );
    assertEquals( gatherMetrics, additionalDetail.getGatherMetrics() );
  }

}
