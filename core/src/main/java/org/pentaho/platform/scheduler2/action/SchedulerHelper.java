package org.pentaho.platform.scheduler2.action;

import org.pentaho.platform.engine.core.system.PentahoSystem;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class SchedulerHelper {
    Properties scheduleProperties;
    public SchedulerHelper(){
        if ( scheduleProperties == null ) {
            try {
                scheduleProperties = findPropertiesInClasspath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private Properties findPropertiesInClasspath() throws IOException {
        String schedulePropPath =
                PentahoSystem.getApplicationContext()
                        .getSolutionPath( "system/scheduler-plugin/resources/schedule.properties" ).replace( '\\', '/' );
        File f = new File( schedulePropPath );
        if ( f.exists() ) {
            InputStream iStream = new BufferedInputStream( new FileInputStream( f ) );
            if ( iStream != null ) {
                try {
                    Properties props = new Properties();
                    props.load( iStream );
                    return props;
                } finally {
                    try {
                        iStream.close();
                    } catch ( IOException ignored ) {
                        boolean ignore = true; // close quietly
                    }
                }
            }
            return null; // Couldn't find properties file.
        }
        return null;
    }

    public String getHideInternalVarible() {
        String hideInternalVariable = scheduleProperties.getProperty( "PENTAHO_SCHEDULER_HIDE_INTERNAL_VARIABLES" );
        return hideInternalVariable;
    }
}
