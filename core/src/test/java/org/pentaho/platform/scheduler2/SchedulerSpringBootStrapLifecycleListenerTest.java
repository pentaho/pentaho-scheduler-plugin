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


package org.pentaho.platform.scheduler2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.PluginLifecycleException;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.objfac.StandaloneSpringPentahoObjectFactory;

import org.pentaho.test.platform.engine.core.MicroPlatform;

import static org.junit.Assert.*;

public class SchedulerSpringBootStrapLifecycleListenerTest {
    private MicroPlatform mp;
    private SchedulerSpringBootstrapLifecycleListener listener;

    @Before
    public void setup() {
        PentahoSystem.clearObjectFactory();
    }

    @After
    public void tearDown() throws Exception {
        if ( mp != null ) {
            mp.stop();
            mp = null;
        }
        listener = null;
    }

    @Test
    public void testListWithException() {
        StandaloneSession session = new StandaloneSession();
        PentahoSessionHolder.setSession( session );

        StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory();
        factory.init( "src/test/resources/plugin.spring.xml", null );
        PentahoSystem.registerObjectFactory( factory );
        listener = new SchedulerSpringBootstrapLifecycleListener();
        try {
            listener.init();
        } catch ( PluginLifecycleException ple ) {
            assertNotNull( ple );
        }
    }

    @Test
    public void testListWithNoException() {
        StandaloneSession session = new StandaloneSession();
        PentahoSessionHolder.setSession( session );

        StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory();
        factory.init( "src/test/resources/anotherplugin.spring.xml", null );
        PentahoSystem.registerObjectFactory( factory );
        listener = new SchedulerSpringBootstrapLifecycleListener();
        try {
            listener.init();
            assertTrue( true );
        } catch ( PluginLifecycleException ple ) {
            fail();
        }
    }

    @Test
    public void testListWithNothingInTheList() {
        StandaloneSession session = new StandaloneSession();
        PentahoSessionHolder.setSession( session );

        StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory();
        factory.init( "src/test/resources/nolifecyclelistenerplugin.spring.xml", null );
        PentahoSystem.registerObjectFactory( factory );
        listener = new SchedulerSpringBootstrapLifecycleListener();
        try {
            listener.init();
            assertTrue( true );
        } catch ( PluginLifecycleException ple ) {
            fail();
        }
    }

    @Test
    public void testListWithNEmptyPluginXML() {
        StandaloneSession session = new StandaloneSession();
        PentahoSessionHolder.setSession( session );

        StandaloneSpringPentahoObjectFactory factory = new StandaloneSpringPentahoObjectFactory();
        factory.init( "src/test/resources/emptyplugin.spring.xml", null );
        PentahoSystem.registerObjectFactory( factory );
        listener = new SchedulerSpringBootstrapLifecycleListener();
        try {
            listener.init();
            assertTrue( true );
        } catch ( PluginLifecycleException ple ) {
            fail();
        }
    }
}
