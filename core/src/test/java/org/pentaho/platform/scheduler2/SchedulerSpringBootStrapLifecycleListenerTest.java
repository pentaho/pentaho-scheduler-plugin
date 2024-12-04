/*!
 *
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
 *
 * Copyright (c) 2024 Hitachi Vantara. All rights reserved.
 *
 */

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
