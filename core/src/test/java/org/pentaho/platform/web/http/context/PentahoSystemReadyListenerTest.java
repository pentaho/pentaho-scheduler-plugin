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


package org.pentaho.platform.web.http.context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPlatformPlugin;
import org.pentaho.platform.api.engine.IPlatformReadyListener;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.api.engine.IPluginProvider;
import org.pentaho.platform.api.engine.PluginLifecycleException;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import jakarta.servlet.ServletContextEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

/**
 * Created by rfellows on 10/28/15.
 */
@RunWith( MockitoJUnitRunner.class )
public class PentahoSystemReadyListenerTest {

  PentahoSystemReadyListener systemReadyListener;

  @Mock ServletContextEvent contextEvent;
  @Mock IPluginManager pluginManager;
  @Mock IPluginProvider pluginProvider;
  @Mock IPlatformPlugin pluginA;
  @Mock IPlatformPlugin pluginB;
  @Mock ClassLoader classLoader;

  List<IPlatformPlugin> pluginList;

  @Before
  public void setUp() throws Exception {
    systemReadyListener = new PentahoSystemReadyListener();
    PentahoSystem.registerObject( pluginManager );
    PentahoSystem.registerObject( pluginProvider );
    pluginList = new ArrayList<>();
    pluginList.add( pluginA );
    pluginList.add( pluginB );

  }

  @After
  public void tearDown() throws Exception {
    PentahoSystem.clearObjectFactory();
  }

  @Test
  public void testContextInitialized() throws Exception {
    when( pluginProvider.getPlugins( nullable( IPentahoSession.class ) ) ).thenReturn( pluginList );

    List<String> classNames = new ArrayList<>();
    classNames.add( TestReadyListener.class.getName() );

    when( pluginA.getLifecycleListenerClassnames() ).thenReturn( classNames );
    when( pluginA.getId() ).thenReturn( "PluginA" );

    classNames = new ArrayList<>();
    classNames.add( "" );
    when( pluginB.getLifecycleListenerClassnames() ).thenReturn( classNames );

    when( pluginManager.getClassLoader( "PluginA" ) ).thenReturn( classLoader );
    Class clazz = TestReadyListener.class;
    when( classLoader.loadClass( TestReadyListener.class.getName() ) ).thenReturn( clazz );

    systemReadyListener.contextInitialized( contextEvent );
    assertTrue( TestReadyListener.readyCalled );
  }

  @Test
  public void testContextDestroyed() throws Exception {
    // no-op, calling for code coverage
    systemReadyListener.contextDestroyed( contextEvent );
  }

  static class TestReadyListener implements IPlatformReadyListener {
    static boolean readyCalled = false;

    public TestReadyListener() {
      readyCalled = false;
    }

    @Override public void ready() throws PluginLifecycleException {
      // to verify this is called in the new instance created within the listener, we are going to
      // set a static property that we can check. we don't have the luxury of getting at the actual instance.
      readyCalled = true;
    }
  }

}
