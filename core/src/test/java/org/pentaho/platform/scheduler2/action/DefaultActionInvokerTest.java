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

package org.pentaho.platform.scheduler2.action;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.action.ActionInvocationException;
import org.pentaho.platform.api.scheduler2.IBackgroundExecutionStreamProvider;
import org.pentaho.platform.scheduler2.quartz.QuartzScheduler;
import org.pentaho.platform.util.bean.TestAction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@RunWith( MockitoJUnitRunner.class )
public class DefaultActionInvokerTest
{
  @Test
  public void testGetStreamProvider() throws Exception {
    final DefaultActionInvoker ai = new DefaultActionInvoker();
    final Map<String, Serializable> params = new HashMap<>();

    Assert.assertNull( ai.getStreamProvider( params ) );

    params.put( "foo", "bar" );
    Assert.assertNull( ai.getStreamProvider( params ) );

    params.put( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER, null );
    Assert.assertNull( ai.getStreamProvider( params ) );

    params.put( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER, 1 );
    Assert.assertNull( ai.getStreamProvider( params ) );

    params.put( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER, true );
    Assert.assertNull( ai.getStreamProvider( params ) );

    params.put( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER, "streamProviderFoo" );
    Assert.assertNull( ai.getStreamProvider( params ) );

    params.put( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER, Mockito.mock( IBackgroundExecutionStreamProvider.class ) );
    Assert.assertNotNull( ai.getStreamProvider( params ) );
  }

  @Test
  public void testValidate() throws Exception {
    final DefaultActionInvoker ai = new DefaultActionInvoker();
    ai.validate( new TestAction(), "user", new HashMap() );
  }

  @Test( expected = ActionInvocationException.class )
  public void testValidateNullAction() throws Exception {
    final DefaultActionInvoker ai = new DefaultActionInvoker();
    ai.validate( null, "user", new HashMap() );
  }

  @Test( expected = ActionInvocationException.class )
  public void testValidateNullParams() throws Exception {
    final DefaultActionInvoker ai = new DefaultActionInvoker();
    ai.validate( new TestAction(), "user", null );
  }
}
