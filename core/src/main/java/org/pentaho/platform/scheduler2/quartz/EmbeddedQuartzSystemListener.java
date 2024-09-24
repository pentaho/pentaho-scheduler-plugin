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

package org.pentaho.platform.scheduler2.quartz;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.data.DBDatasourceServiceException;
import org.pentaho.platform.api.data.IDBDatasourceService;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.ObjectFactoryException;
import org.pentaho.platform.api.engine.PluginLifecycleException;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.PentahoSystemPublisher;
import org.pentaho.platform.engine.services.connection.datasource.dbcp.JndiDatasourceService;
import org.pentaho.platform.scheduler2.messsages.Messages;
import org.quartz.SchedulerException;

public class EmbeddedQuartzSystemListener implements IPluginLifecycleListener {

  /*
   * This is re-use by Copy and Paste to avoid a dependency on the bi-platform-scheduler project (which will eventually
   * be phased out).
   *
   * The only difference between this and the other class is that this system listener will initialize the quartz
   * database if it hasn't been created yet.
   */

  private static final String DEFAULT_QUARTZ_PROPERTIES_FILE = "scheduler-plugin/quartz/quartz.properties"; //$NON-NLS-1$

  Properties quartzProperties;

  String quartzPropertiesFile = DEFAULT_QUARTZ_PROPERTIES_FILE;

  private Log logger;

  private static boolean useNewDatasourceService = false;

  public synchronized void setUseNewDatasourceService( boolean useNewService ) {
    //
    // The platform should not be calling this method. But, in case someone really
    // really wants to use the new datasource service features to talk to
    // a core service like Quartz, this is now toggle-able.
    //
    useNewDatasourceService = useNewService;
  }
  public EmbeddedQuartzSystemListener() {
    logger = LogFactory.getLog( EmbeddedQuartzSystemListener.class );
  }

  protected Log getLogger() {
    return logger;
  }
  public boolean startup( final IPentahoSession session ) {
    boolean result = true;
    Properties quartzProps = null;
    if ( quartzPropertiesFile != null ) {
      quartzProps = PentahoSystem.getSystemSettings().getSystemSettingsProperties( quartzPropertiesFile );
    } else if ( quartzProperties != null ) {
      quartzProps = quartzProperties;
    }
    try {
      if ( quartzProps == null ) {
        quartzProps = findPropertiesInClasspath();
      }
      if ( quartzProps == null ) {
        result = false;
      } else {
        String dsName = quartzProps.getProperty( "org.quartz.dataSource.myDS.jndiURL" ); //$NON-NLS-1$
        if ( dsName != null ) {
          IDBDatasourceService datasourceService = getQuartzDatasourceService( session );
          String boundDsName = datasourceService.getDSBoundName( dsName );

          if ( boundDsName != null ) {
            quartzProps.setProperty( "org.quartz.dataSource.myDS.jndiURL", boundDsName ); //$NON-NLS-1$
          }

          DataSource ds = datasourceService.getDataSource( dsName );
          result = verifyQuartzIsConfigured( ds );
        }
        QuartzScheduler scheduler = (QuartzScheduler) PentahoSystem.get( IScheduler.class, "IScheduler2", null ); //$NON-NLS-1$
        if ( logger.isDebugEnabled() ) {
          logger.debug( "Quartz configured with properties" ); //$NON-NLS-1$
          quartzProps.store( System.out, "debugging" ); //$NON-NLS-1$
        }
        scheduler.setQuartzSchedulerFactory( new org.quartz.impl.StdSchedulerFactory( quartzProps ) );
        waitForSystemToStart( scheduler );
      }
    } catch ( IOException ex ) {
      result = false;
      logger.error( Messages.getInstance().getErrorString(
          "EmbeddedQuartzSystemListener.ERROR_0004_LOAD_PROPERTIES_FROM_CLASSPATH" ), ex ); //$NON-NLS-1$
    } catch ( ObjectFactoryException objface ) {
      logger
          .error(
              Messages
                  .getInstance()
                  .getErrorString(
                      "EmbeddedQuartzSystemListener.ERROR_0005_UNABLE_TO_INSTANTIATE_OBJECT", EmbeddedQuartzSystemListener.class.getName() ), objface ); //$NON-NLS-1$
      result = false;
    } catch ( DBDatasourceServiceException dse ) {
      logger
          .error(
              Messages
                  .getInstance()
                  .getErrorString(
                      "EmbeddedQuartzSystemListener.ERROR_0006_UNABLE_TO_GET_DATASOURCE", EmbeddedQuartzSystemListener.class.getName() ), dse ); //$NON-NLS-1$
      result = false;
    } catch ( SQLException sqle ) {
      logger.error( "EmbeddedQuartzSystemListener.ERROR_0007_SQLERROR", sqle ); //$NON-NLS-1$
      result = false;
    } catch ( SchedulerException e ) {
      logger
          .error(
              Messages
                  .getInstance()
                  .getErrorString(
                      "EmbeddedQuartzSystemListener.ERROR_0001_Scheduler_Not_Initialized", EmbeddedQuartzSystemListener.class.getName() ), e ); //$NON-NLS-1$
      result = false;
    } catch ( org.pentaho.platform.api.scheduler2.SchedulerException e ) {
      logger
          .error(
              Messages
                  .getInstance()
                  .getErrorString(
                      "EmbeddedQuartzSystemListener.ERROR_0001_Scheduler_Not_Initialized", EmbeddedQuartzSystemListener.class.getName() ), e ); //$NON-NLS-1$
      result = false;
    }
    return result;
  }

  protected void waitForSystemToStart( QuartzScheduler scheduler ) throws org.pentaho.platform.api.scheduler2.SchedulerException, SchedulerException {
    // This line MAY be redundant.  Quartz docs say the scheduler is always created in the paused mode.
    if ( logger.isDebugEnabled() ) {
      logger.debug( "About to pause the scheduler" );
    }
    scheduler.pause();
    if ( logger.isDebugEnabled() ) {
      logger.debug( "scheduler is now paused" );
    }
    // Tell the publisher that we want to listen for "START_UP_TOPIC" and when it fires that the system has started
    // then call the systemStartupCallback method.
    PentahoSystemPublisher.getInstance().subscribe( PentahoSystemPublisher.START_UP_TOPIC, this::systemStartupCallback );

    if ( logger.isDebugEnabled() ) {
      logger.debug( scheduler.getQuartzScheduler().getSchedulerName() );
    }
  }
  /**
   * Starts the QuartzScheduler. Introduced to be overriden in EE implementation.
   * @param quartzScheduler
   * @throws org.pentaho.platform.api.scheduler2.SchedulerException
   */
  protected void startScheduler( QuartzScheduler quartzScheduler ) throws org.pentaho.platform.api.scheduler2.SchedulerException {
    quartzScheduler.start();
  }

  // This method should be called by the publisher when the system has started
  private void systemStartupCallback( boolean systemHasStarted ) {
    QuartzScheduler scheduler = (QuartzScheduler) PentahoSystem.get( IScheduler.class, "IScheduler2", null ); //$NON-NLS-1$
    try {
      if ( systemHasStarted ) {
        if ( logger.isDebugEnabled() ) {
          logger.debug( "System has properly initialized. Starting the scheduler now" );
        }
        startScheduler( scheduler );
        if ( logger.isDebugEnabled() ) {
          logger.debug( "System has properly initialized. Finished starting the scheduler" );
        }
      } else {
        if ( logger.isDebugEnabled() ) {
          logger.debug( "System was not properly initialized. Scheduler will not start" );
        }
      }
    } catch ( org.pentaho.platform.api.scheduler2.SchedulerException e ) {
      throw new RuntimeException( e );
    }
  }

  protected boolean verifyQuartzIsConfigured( DataSource ds ) throws SQLException {
    boolean quartzIsConfigured = false;
    Connection conn = ds.getConnection();

    try {
      DatabaseMetaData databaseMetaData = conn.getMetaData();
      String tableNamePattern;
      if ( databaseMetaData.storesLowerCaseIdentifiers() ) {
        tableNamePattern = "%qrtz%";
      } else {
        tableNamePattern = "%QRTZ%";
      }
      ResultSet rs = conn.getMetaData().getTables( null, null, tableNamePattern, null );
      try {
        quartzIsConfigured = rs.next();
      } finally {
        rs.close();
      }
      if ( !quartzIsConfigured ) {
        // If we're here, then tables need creating
        String quartzInitializationScriptPath =
            PentahoSystem.getApplicationContext()
                .getSolutionPath( "system/quartz/h2-quartz-schema-updated.sql" ).replace( '\\', '/' );
        File f = new File( quartzInitializationScriptPath );
        if ( f.exists() ) {
          try ( Statement stmt = conn.createStatement() ) {
            // We know now that there's an initialization script
            stmt.executeUpdate( "RUNSCRIPT FROM '" + quartzInitializationScriptPath + "'" );
            // Tables should now exist.
            quartzIsConfigured = true;
          }
        }
      }
    } finally {
      conn.close();
    }
    return quartzIsConfigured;
  }

  private IDBDatasourceService getQuartzDatasourceService( IPentahoSession session ) throws ObjectFactoryException {
    //
    // Our new datasource stuff is provided for running queries and acquiring data. It is
    // NOT there for the inner workings of the platform. So, the Quartz datasource should ALWAYS
    // be provided by JNDI. However, the class could be twiddled so that it will use the factory.
    //
    // And, since the default shipping condition should be to NOT use the factory (and force JNDI),
    // I've reversed the logic in the class to have the negative condition first (the default execution
    // path).
    //
    // Marc - BISERVER-2004
    //
    if ( !useNewDatasourceService ) {
      return new JndiDatasourceService();
    } else {
      IDBDatasourceService datasourceService =
          PentahoSystem.getObjectFactory().get( IDBDatasourceService.class, session );
      return datasourceService;
    }
  }

  private Properties findPropertiesInClasspath() throws IOException {
    // Do my best to find the properties file...
    File propFile = new File( "quartz.properties" ); //$NON-NLS-1$
    if ( !propFile.canRead() ) {
      InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream( "quartz.properties" ); //$NON-NLS-1$
      if ( in != null ) {
        try {
          Properties props = new Properties();
          props.load( in );
          return props;
        } finally {
          in.close();
        }
      }
      return null; // Couldn't find properties file.
    } else {
      InputStream iStream = new BufferedInputStream( new FileInputStream( propFile ) );
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
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.core.system.IPentahoSystemListener#shutdown()
   */
  public void shutdown() {
    try {
      QuartzScheduler scheduler = (QuartzScheduler) PentahoSystem.get( IScheduler.class, "IScheduler2", null ); //$NON-NLS-1$
      scheduler.getQuartzScheduler().shutdown();
    } catch ( SchedulerException e ) {
      e.printStackTrace();
    }
  }

  public Properties getQuartzProperties() {
    return quartzProperties;
  }

  public void setQuartzProperties( Properties quartzProperties ) {
    this.quartzProperties = quartzProperties;
    if ( quartzProperties != null ) {
      quartzPropertiesFile = null;
    }
  }

  public String getQuartzPropertiesFile() {
    return quartzPropertiesFile;
  }

  public void setQuartzPropertiesFile( String quartzPropertiesFile ) {
    this.quartzPropertiesFile = quartzPropertiesFile;
    if ( quartzPropertiesFile != null ) {
      quartzProperties = null;
    }
  }

  @Override public void init() throws PluginLifecycleException {
    logger.info("***************************************************************");
    logger.info("EmbeddedQuartzSystemListener initialized.");
    logger.info("***************************************************************");

    startup( PentahoSessionHolder.getSession() );
  }

  @Override public void loaded() throws PluginLifecycleException {

  }

  @Override public void unLoaded() throws PluginLifecycleException {
    shutdown();
  }
}
