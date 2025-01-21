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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.scheduler2.quartz.EmbeddedQuartzSystemListener;

import java.util.ArrayList;
import java.util.List;

public class SchedulerSpringBootstrapLifecycleListener implements IPluginLifecycleListener {
    @Override
    public void init() throws PluginLifecycleException {
        Log logger = LogFactory.getLog( EmbeddedQuartzSystemListener.class );
        IPentahoSession session = PentahoSessionHolder.getSession();
        boolean aPluginLifeCycleListenerFailed = false;
        List<IPluginLifecycleListener> lifecycleListenerList = PentahoSystem.get( ArrayList.class, "schedulerLifecycleListenerList", session );
        if ( lifecycleListenerList != null && lifecycleListenerList.size() > 0 ) {
            for (IPluginLifecycleListener lifecycleListener : lifecycleListenerList) {
                try {
                    lifecycleListener.init();
                    if (logger.isDebugEnabled()) {
                        logger.debug(" Successfully initialized a lifecycle listener [ " + lifecycleListener.getClass() + " ]");
                    }
                } catch (PluginLifecycleException ple) {
                    logger.error(" Error initializing a plugin [ " + ple + " ]");
                    aPluginLifeCycleListenerFailed = true;
                }
            }
            if (aPluginLifeCycleListenerFailed) {
                throw new PluginLifecycleException(" One or more scheduler lifecycle listener failed ");
            }
        } else {
            logger.info(" Scheduler plugin lifecycle list is empty. Nothing to initialize");
        }
    }

    @Override
    public void loaded() throws PluginLifecycleException {

    }

    @Override
    public void unLoaded() throws PluginLifecycleException {
        Log logger = LogFactory.getLog( SchedulerSpringBootstrapLifecycleListener.class );
        IPentahoSession session = PentahoSessionHolder.getSession();
        boolean aPluginLifeCycleListenerFailed = false;
        List<IPluginLifecycleListener> lifecycleListenerList = PentahoSystem.get( ArrayList.class, "schedulerLifecycleListenerList", session );
        if ( lifecycleListenerList != null && lifecycleListenerList.size() > 0 ) {
            for (IPluginLifecycleListener lifecycleListener : lifecycleListenerList) {
                try {
                    lifecycleListener.unLoaded();
                    if (logger.isDebugEnabled()) {
                        logger.debug(" Successfully unloaded a lifecycle listener [ " + lifecycleListener.getClass() + " ]");
                    }
                } catch (PluginLifecycleException ple) {
                    logger.error(" Error unloading a plugin [ " + ple + " ]");
                    aPluginLifeCycleListenerFailed = true;
                }
            }
            if (aPluginLifeCycleListenerFailed) {
                throw new PluginLifecycleException(" One or more scheduler lifecycle listener failed to unload");
            }
        } else {
            logger.info(" Scheduler plugin lifecycle list is empty. Nothing to unload");
        }
    }
}
