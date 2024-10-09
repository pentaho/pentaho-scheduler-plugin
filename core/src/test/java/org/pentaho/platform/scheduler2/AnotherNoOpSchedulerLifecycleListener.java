package org.pentaho.platform.scheduler2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;

public class AnotherNoOpSchedulerLifecycleListener  implements IPluginLifecycleListener {
    private Log logger = LogFactory.getLog( AnotherNoOpSchedulerLifecycleListener .class );

    @Override
    public void init() throws PluginLifecycleException {
        logger.info( "initialize called" );
    }

    @Override
    public void loaded() throws PluginLifecycleException {

    }

    @Override
    public void unLoaded() throws PluginLifecycleException {
        logger.info("unloaded  called" );
    }
}
