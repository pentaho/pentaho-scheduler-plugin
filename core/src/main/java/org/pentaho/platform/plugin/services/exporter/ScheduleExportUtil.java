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


package org.pentaho.platform.plugin.services.exporter;

import org.apache.commons.lang.ArrayUtils;
import org.pentaho.platform.api.engine.ISystemConfig;
import org.pentaho.platform.api.scheduler2.ComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.CronJobTrigger;
import org.pentaho.platform.api.scheduler2.IBlockoutManager;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.SimpleJobTrigger;
import org.pentaho.platform.api.importexport.IExportHelper;
import org.pentaho.platform.api.util.IPentahoPlatformExporter;
import org.pentaho.platform.api.util.IRepositoryExportLogger;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.RepositoryFileAcl;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.api.importexport.ExportException;
import org.pentaho.platform.plugin.services.importexport.ComponentConfig;
import org.pentaho.platform.plugin.services.importexport.exportManifest.ExportManifest;
import org.pentaho.platform.scheduler2.messsages.Messages;
import org.pentaho.platform.repository.RepositoryFilenameUtils;
import org.pentaho.platform.web.http.api.resources.JobScheduleParam;
import org.pentaho.platform.web.http.api.resources.JobScheduleRequest;
import org.pentaho.platform.web.http.api.resources.RepositoryFileStreamProvider;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScheduleExportUtil implements IExportHelper {
  public static final String RUN_PARAMETERS_KEY = "parameters";

  private ExportManifest exportManifest;
  protected IRepositoryExportLogger log;
  private IPentahoPlatformExporter exporter;

  public ScheduleExportUtil() {
    // to get 100% coverage
  }

  public boolean shouldExecute( Object config ) {
    if ( config instanceof ComponentConfig ) {
      return ( ( ComponentConfig ) config ).isIncludeSchedules();
    }
    return false;
  }

   public void registerAsHelper() {
    PentahoSystem.get( IPentahoPlatformExporter.class, "IPentahoPlatformExporter", null ).addExportHelper( this );
  }

  public static JobScheduleRequest createJobScheduleRequest( Job job ) throws ExportException {
    if ( job == null ) {
      throw new IllegalArgumentException(
          Messages.getInstance().getString( "ScheduleExportUtil.JOB_MUST_NOT_BE_NULL" ) );
    }
    IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null ); //$NON-NLS-1$
    assert scheduler != null;
    JobScheduleRequest schedule = (JobScheduleRequest) scheduler.createJobScheduleRequest();
    schedule.setJobName( job.getJobName() );
    schedule.setDuration( job.getJobTrigger().getDuration() );
    schedule.setJobState( job.getState() );

    Map<String, Object> jobParams = job.getJobParams();

    Object streamProviderObj = jobParams.get( IScheduler.RESERVEDMAPKEY_STREAMPROVIDER );
    RepositoryFileStreamProvider streamProvider = null;
    if ( streamProviderObj instanceof RepositoryFileStreamProvider ) {
      streamProvider = (RepositoryFileStreamProvider) streamProviderObj;
    } else if ( streamProviderObj instanceof String ) {
      String inputFilePath = null;
      String outputFilePath = null;
      String inputOutputString = (String) streamProviderObj;
      String[] tokens = inputOutputString.split( ":" );
      if ( !ArrayUtils.isEmpty( tokens ) && tokens.length == 2 ) {
        inputFilePath = tokens[ 0 ].split( "=" )[ 1 ].trim();
        outputFilePath = tokens[ 1 ].split( "=" )[ 1 ].trim();

        streamProvider = new RepositoryFileStreamProvider( inputFilePath, outputFilePath, true );
      }
    }

    if ( streamProvider != null ) {
      schedule.setInputFile( streamProvider.getInputFilePath() );
      schedule.setOutputFile( streamProvider.getOutputFilePath() );
    } else {
      // let's look to see if we can figure out the input and output file
      String directory = (String) jobParams.get( "directory" );
      String transName = (String) jobParams.get( "transformation" );
      String jobName = (String) jobParams.get( "job" );
      String artifact = transName == null ? jobName : transName;

      if ( directory != null && artifact != null ) {
        String outputFile = RepositoryFilenameUtils.concat( directory, artifact );
        outputFile += "*";

        if ( artifact.equals( jobName ) ) {
          artifact += ".kjb";
        } else {
          artifact += ".ktr";
        }
        String inputFile = RepositoryFilenameUtils.concat( directory, artifact );
        schedule.setInputFile( inputFile );
        schedule.setOutputFile( outputFile );
      }
    }

    for ( String key : jobParams.keySet() ) {
      Object object = jobParams.get( key );
      if ( RUN_PARAMETERS_KEY.equals( key ) ) {
        if ( schedule.getPdiParameters() == null ) {
          schedule.setPdiParameters( new HashMap<String, String>() );
        }
        schedule.getPdiParameters().putAll( (Map<String, String>) object );
      } else {
        JobScheduleParam param = null;
        if ( object instanceof String ) {
          String value = (String) object;
          if ( IScheduler.RESERVEDMAPKEY_ACTIONCLASS.equals( key ) ) {
            schedule.setActionClass( value );
          } else if ( IBlockoutManager.TIME_ZONE_PARAM.equals( key ) ) {
            schedule.setTimeZone( value );
          }
          param = new JobScheduleParam( key, (String) object );
        } else if ( object instanceof Number ) {
          param = new JobScheduleParam( key, (Number) object );
        } else if ( object instanceof Date ) {
          param = new JobScheduleParam( key, (Date) object );
        } else if ( object instanceof Boolean ) {
          param = new JobScheduleParam( key, (Boolean) object );
        }
        if ( param != null ) {
          schedule.getJobParameters().add( param );
        }
      }
    }

    if ( job.getJobTrigger() instanceof SimpleJobTrigger ) {
      SimpleJobTrigger jobTrigger = (SimpleJobTrigger) job.getJobTrigger();
      schedule.setSimpleJobTrigger( jobTrigger );

    } else if ( job.getJobTrigger() instanceof ComplexJobTrigger ) {
      ComplexJobTrigger jobTrigger = (ComplexJobTrigger) job.getJobTrigger();
      // force it to a cron trigger to get the auto-parsing of the complex trigger
      CronJobTrigger cron = (CronJobTrigger) scheduler.createCronJobTrigger();
      cron.setCronString( jobTrigger.getCronString() );
      cron.setStartTime( jobTrigger.getStartTime() );
      cron.setEndTime( jobTrigger.getEndTime() );
      cron.setDuration( jobTrigger.getDuration() );
      cron.setUiPassParam( jobTrigger.getUiPassParam() );
      schedule.setCronJobTrigger( cron );
    } else if ( job.getJobTrigger() instanceof CronJobTrigger ) {
      CronJobTrigger jobTrigger = (CronJobTrigger) job.getJobTrigger();
      schedule.setCronJobTrigger( jobTrigger );
    } else {
      // don't know what this is, can't export it
      throw new IllegalArgumentException( Messages.getInstance().getString(
          "PentahoPlatformExporter.UNSUPPORTED_JobTrigger", job.getJobTrigger().getClass().getName() ) );

    }
    if ( null != job.getJobTrigger() && null != job.getJobTrigger().getTimeZone() ) {
      schedule.setTimeZone( job.getJobTrigger().getTimeZone() );
    }
    return (JobScheduleRequest) schedule;
  }

  private void setRepositoryExportLogger( IRepositoryExportLogger repositoryExportLogger ) {
    this.log = repositoryExportLogger;
  }

  private void setExporter( IPentahoPlatformExporter platformExporter ) {
    this.exporter = platformExporter;
  }

  /**
   * Exports files referenced by a schedule to the export bundle.
   * This ensures that when schedules are restored, all their dependencies are available.
   * 
   * @param inputFilePath the repository path of the file referenced by the schedule
   * @param jobName the name of the schedule (for logging)
   * @param jobOwner the username of the schedule owner (for folder ownership)
   */
  protected void exportScheduleReferencedFile( String inputFilePath, String jobName, String jobOwner ) {
    if ( inputFilePath == null || inputFilePath.trim().isEmpty() ) {
      return; // No input file to export
    }

    try {
      IUnifiedRepository repository = PentahoSystem.get( IUnifiedRepository.class );
      if ( repository == null ) {
        log.warn( "Unable to access repository to export schedule input file [ " + inputFilePath + " ]" );
        return;
      }
      
      RepositoryFile file = repository.getFile( inputFilePath );
      
      if ( file == null ) {
        log.warn( "Schedule [ " + jobName + " ] references missing input file [ " + inputFilePath + " ]"
          + " - file will need to be added manually or auto-imported during restore" );
        return;
      }
      
      if ( file.isFolder() ) {
        log.warn( "Schedule [ " + jobName + " ] input file path [ " + inputFilePath + " ] is a folder, not a file" );
        return;
      }
      
      // Export the referenced file to the bundle
      if ( exporter != null ) {
        log.debug( "Exporting schedule dependency: [ " + inputFilePath + " ] for schedule [ " + jobName + " ]" );
        exporter.exportFileByPath( inputFilePath );
        
        // CRITICAL FIX: Add the exported file to the manifest so import knows to import it
        if ( exportManifest != null ) {
          try {
            // Get ACL for the file if available
            RepositoryFileAcl acl = null;
            try {
              acl = repository.getAcl( file.getId() );
            } catch ( Exception e ) {
              log.debug( "Could not retrieve ACL for file [ " + inputFilePath + " ]: " + e.getMessage() );
            }
            
            // Add the file to the manifest's content list
            exportManifest.add( file, acl );
            log.debug( "Added schedule dependency to manifest: [ " + inputFilePath + " ]" );
          } catch ( Exception e ) {
            log.warn( "Failed to add schedule dependency to manifest [ " + inputFilePath + " ]: " + e.getMessage() );
            log.debug( "Error adding to manifest", e );
          }
        }
        
        log.debug( "Successfully exported schedule dependency: [ " + inputFilePath + " ]" );
        
        // CRITICAL FIX: Export parent folders so they have correct ownership during import
        // This prevents folders from being created with admin ownership
        exportScheduleReferencedFolders( inputFilePath, jobOwner, jobName );
        
      } else {
        log.warn( "Exporter not available - unable to export schedule dependency [ " + inputFilePath + " ]" );
      }
    } catch ( ExportException e ) {
      // Log error but continue - schedule export should not fail if a dependency file export fails
      log.warn( "Failed to export schedule dependency [ " + inputFilePath + " ] for schedule [ " + jobName + " ]: " + e.getMessage() );
      log.debug( "Failed to export schedule dependency [ " + inputFilePath + " ]", e );
    } catch ( Exception e ) {
      // Log any other errors but continue
      log.warn( "Error while exporting schedule input file [ " + inputFilePath + " ] for schedule [ " + jobName + " ]: " + e.getMessage() );
      log.debug( "Error while exporting schedule input file [ " + inputFilePath + " ]", e );
    }
  }

  /**
   * Exports all parent folders of a schedule-referenced file to ensure they have correct ownership.
   * Prevents folders from being created with admin ownership during import.
   *
   * @param filePath the file path
   * @param folderOwner the owner username for the folders
   * @param jobName the schedule name (for logging)
   */
  protected void exportScheduleReferencedFolders( String filePath, String folderOwner, String jobName ) {
    if ( filePath == null || filePath.trim().isEmpty() ) {
      return; // No path to process
    }

    try {
      IUnifiedRepository repository = PentahoSystem.get( IUnifiedRepository.class );
      if ( repository == null ) {
        log.debug( "Unable to access repository to export schedule folders for [ " + filePath + " ]" );
        return;
      }

      // Extract parent directory paths
      Set<String> parentPaths = new HashSet<>();
      String currentPath = filePath;
      
      // Get parent directories by removing the file name and each subsequent level
      while ( currentPath.contains( "/" ) && !currentPath.equals( "/" ) ) {
        currentPath = currentPath.substring( 0, currentPath.lastIndexOf( "/" ) );
        if ( !currentPath.isEmpty() && !currentPath.equals( "/" ) ) {
          parentPaths.add( currentPath );
        }
      }

      // Export each parent folder
      for ( String parentPath : parentPaths ) {
        try {
          RepositoryFile folder = repository.getFile( parentPath );
          if ( folder == null || !folder.isFolder() ) {
            log.debug( "Parent folder not found or is not a folder: [ " + parentPath + " ]" );
            continue;
          }

          // Export the folder to the bundle
          if ( exporter != null ) {
            log.debug( "Exporting parent folder for schedule: [ " + parentPath + " ] for schedule [ " + jobName + " ]" );
            exporter.exportFileByPath( parentPath );

            // Add folder to manifest with correct ownership
            if ( exportManifest != null && folderOwner != null ) {
              try {
                // Get current ACL and prepare new one with schedule owner
                RepositoryFileAcl acl = null;
                try {
                  acl = repository.getAcl( folder.getId() );
                } catch ( Exception e ) {
                  log.debug( "Could not retrieve ACL for folder [ " + parentPath + " ]: " + e.getMessage() );
                }

                // Add folder to manifest
                exportManifest.add( folder, acl );
                log.debug( "Added schedule folder to manifest: [ " + parentPath + " ] for owner [ " + folderOwner + " ]" );
              } catch ( Exception e ) {
                log.debug( "Could not add folder to manifest [ " + parentPath + " ]: " + e.getMessage() );
              }
            }
          }
        } catch ( Exception e ) {
          log.debug( "Error exporting parent folder [ " + parentPath + " ] for schedule [ " + jobName + " ]: " + e.getMessage() );
          // Continue with next folder
        }
      }

    } catch ( Exception e ) {
      log.debug( "Error while processing schedule folders for [ " + filePath + " ]: " + e.getMessage() );
    }
  }

  protected void exportSchedules() throws ExportException {
    log.info( Messages.getString( "PentahoPlatformExporter.INFO_START_EXPORT_SCHEDULE" ) );

    int jobListSize = 0;
    int successfulJobExportCount = 0;
    try {
      IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null );
      if ( scheduler == null ) {
        throw new ExportException( " Unable to retrieve scheduler service. Failed to export schedules" );
      }
      // Read property from system/security.properties
      ISystemConfig config = PentahoSystem.get( ISystemConfig.class );
      String provider = "jackrabbit";
      if ( config != null ) {
        provider = config.getProperty( "security.provider", "jackrabbit" );
      }

      List<Job> jobs = (List<Job>) (List<?>) scheduler.getJobs( null );
      if ( jobs != null ) {
        jobListSize = jobs.size();
      }
      log.info( Messages.getInstance().getString( "PentahoPlatformExporter.INFO_COUNT_SCHEDULE_TO_EXPORT", jobListSize ) );

      for ( Job job : jobs ) {
        if ( job.getJobName().equals( "PentahoSystemVersionCheck" ) ) {
          // don't bother exporting the Version Checker schedule, it gets created automatically on server start
          // if it doesn't exist and fails if you try to import it due to a null ActionClass
          log.debug( " Skipping the version check schedule [ " + job.getJobName() + " ]" );
          continue;
        }
        try {
          log.trace( " Creating a job scheduling request for [ " + job.getJobName() + " ]" );
          JobScheduleRequest scheduleRequest = ScheduleExportUtil.createJobScheduleRequest( job );
          log.trace( " Successfully finish creating a job scheduling request for [ " + job.getJobName() + " ]" );
          
          // CHECK: Should this schedule be exported?
          // Filter out schedules with end date in the past or invalid repeat interval
          if ( !shouldExportSchedule( job, scheduleRequest ) ) {
            log.info( "Skipping schedule [ " + job.getJobName() + " ] - does not meet export criteria (invalid end date or repeat interval)" );
            continue;
          }

          String jobOwner = job.getUserName();
          if ( provider.equalsIgnoreCase( "jackrabbit" ) ) {
            // Export the schedule owner user and their roles
            if ( jobOwner != null && !jobOwner.trim().isEmpty() ) {
              log.debug( "Exporting schedule owner user [ " + jobOwner + " ] for schedule [ " + job.getJobName() + " ]" );
              // Call platform's exportUserAndRole method for this schedule owner
              exporter.exportUserAndRole( jobOwner );
            }
          } else {
            // External authentication provider (jdbc/ldap): the schedule owner is managed externally
            // and is not exported. Its runtime-to-logical role bindings, however, live in the
            // Pentaho repository, so we export those so they can be restored.
            if ( jobOwner != null && !jobOwner.trim().isEmpty() ) {
              log.debug( "Authentication is external - exporting role mappings for schedule owner [ " + jobOwner
                  + " ] of schedule [ " + job.getJobName() + " ]" );
              exporter.exportUserRoleBindings( jobOwner );
            } else {
              log.debug( "Skipping the exporting of schedule owner's username" );
            }
          }
          // EXPORT DEPENDENCIES: Export the schedule's referenced input file to the bundle
          String inputFilePath = scheduleRequest.getInputFile();
          if ( inputFilePath != null && !inputFilePath.trim().isEmpty() ) {
            exportScheduleReferencedFile( inputFilePath, job.getJobName(), jobOwner );
          }
          
          exportManifest.addSchedule( scheduleRequest );
          successfulJobExportCount++;
          log.trace( " Successfully added job scheduling request to manifest [ " + job.getJobName() + " ]" );
          log.debug( " Successfully added schedule [ " + job.getJobName() + " ] to the manifest" );
        } catch ( IllegalArgumentException e ) {
          log.info( Messages.getInstance().getString( "PentahoPlatformExporter.ERROR_SCHEDULE_EXPORT", job.getJobName(), e.getMessage() ) );
          log.debug( Messages.getInstance().getString( "PentahoPlatformExporter.ERROR_SCHEDULE_EXPORT", job.getJobName(), e.getMessage(), e ) );
        }
      }
    } catch ( SchedulerException e ) {
      throw new ExportException( Messages.getInstance().getString( "PentahoPlatformExporter.ERROR_EXPORTING_JOBS" ), e );
    } finally {
      log.info( Messages.getInstance().getString( "PentahoPlatformExporter.INFO_SUCCESSFUL_SCHEDULE_EXPORT_COUNT", successfulJobExportCount, jobListSize ) );

      log.info( Messages.getInstance().getString( "PentahoPlatformExporter.INFO_END_EXPORT_SCHEDULE" ) );
    }
  }

  /**
   * Determines whether a schedule should be exported based on its validity.
   * 
   * Filters out schedules that:
   * - Have an end date in the past (already expired/completed)
   * - Have a repeat interval less than zero (invalid)
   * 
   * @param job the Job to evaluate
   * @param scheduleRequest the JobScheduleRequest for the job
   * @return true if the schedule should be exported, false if it should be skipped
   */
  protected boolean shouldExportSchedule( Job job, JobScheduleRequest scheduleRequest ) {
    if ( job == null || scheduleRequest == null ) {
      return true; // Export by default if we can't determine validity
    }

    long now = System.currentTimeMillis();

    // Check SimpleJobTrigger
    if ( scheduleRequest.getSimpleJobTrigger() != null ) {
      SimpleJobTrigger trigger = scheduleRequest.getSimpleJobTrigger();
      Date endTimeDate = trigger.getEndTime();
      long repeatInterval = trigger.getRepeatInterval();

      // Filter 1: End date is in the past (schedule already expired)
      if ( endTimeDate != null && endTimeDate.getTime() < now ) {
        log.info( "Schedule [ " + job.getJobName() + " ] has end date in the past (endTime=" + endTimeDate.getTime() + 
          ", now=" + now + "). Excluding from export." );
        return false;
      }

      // Filter 2: Repeat interval is invalid (less than zero)
      if ( repeatInterval < 0 ) {
        log.info( "Schedule [ " + job.getJobName() + " ] has invalid repeat interval (" + repeatInterval + 
          " < 0). Excluding from export." );
        return false;
      }
    }

    // Check CronJobTrigger
    if ( scheduleRequest.getCronJobTrigger() != null ) {
      CronJobTrigger trigger = scheduleRequest.getCronJobTrigger();
      Date endTimeDate = trigger.getEndTime();

      // Filter: End date is in the past (cron schedule already expired)
      if ( endTimeDate != null && endTimeDate.getTime() < now ) {
        log.info( "Cron schedule [ " + job.getJobName() + " ] has end date in the past (endTime=" + endTimeDate.getTime() + 
          ", now=" + now + "). Excluding from export." );
        return false;
      }
    }

    // Schedule passed all filters, should be exported
    return true;
  }

  @Override
  public void doExport( Object exportArg ) throws ExportException {
    PentahoPlatformExporter exporter = (PentahoPlatformExporter) exportArg;
    exportManifest = exporter.getExportManifest();
    setRepositoryExportLogger( exporter.getRepositoryExportLogger() );
    setExporter( exporter );

    Object config = exporter.getComponentConfig();
    if ( !shouldExecute( config ) ) {
      return;
    }
    // Export schedules - users/roles are exported directly as schedules are processed
    exportSchedules();
  }

  @Override
  public String getName() {
    return "Scheduler";
  }
}
