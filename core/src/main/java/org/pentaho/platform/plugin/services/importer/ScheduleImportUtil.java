package org.pentaho.platform.plugin.services.importer;

import org.apache.commons.collections.CollectionUtils;
import org.pentaho.platform.api.engine.ISystemConfig;
import org.pentaho.platform.api.engine.ISystemSettings;
import org.pentaho.platform.api.importexport.IImportHelper;
import org.pentaho.platform.api.importexport.ImportException;
import org.pentaho.platform.api.mt.ITenant;
import org.pentaho.platform.api.mt.ITenantManager;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobRequest;
import org.pentaho.platform.api.scheduler2.IJobScheduleParam;
import org.pentaho.platform.api.scheduler2.IJobScheduleRequest;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.ISchedulerResource;
import org.pentaho.platform.api.scheduler2.JobState;
import org.pentaho.platform.core.mt.Tenant;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.TenantUtils;
import org.pentaho.platform.plugin.services.importexport.ComponentConfig;
import org.pentaho.platform.plugin.services.importexport.ImportSession;
import org.pentaho.platform.plugin.services.importexport.exportManifest.ExportManifest;

import org.pentaho.platform.plugin.services.messages.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Import helper for schedule imports.
 * Handles restoration of schedules from backup archives.
 *
 * Key Features:
 * - Imports schedule input files (dependencies) before schedule creation
 * - Ensures schedule owner users and home folders exist before schedule import
 * - Validates and corrects schedule times during import
 * - Provides comprehensive metrics tracking for import operations
 */
public class ScheduleImportUtil implements IImportHelper {
  private static final Logger logger = LoggerFactory.getLogger( ScheduleImportUtil.class );

  private static final String SCHEDULE_IMPORT_UTIL_NAME = "schedule-import-util";
  private static final String RESERVEDMAPKEY_LINEAGE_ID = "lineage-id";

  private SolutionImportHandler solutionImportHandler;

  public ScheduleImportUtil() {
    super();
  }

  public void registerAsHelper() {
    PentahoSystem.get( SolutionImportHandler.class, "solutionImportHandler", null ).addImportHelper( this );
  }

  @Override
  public boolean shouldExecute( Object config ) {
    if ( config instanceof ComponentConfig ) {
      return ( ( ComponentConfig ) config ).isIncludeSchedules();
    }
    return false;
  }

  @Override public void doImport( Object exportArg ) throws ImportException {
    solutionImportHandler = (SolutionImportHandler) exportArg;
    ISystemSettings settings = PentahoSystem.getSystemSettings();

    List<IJobScheduleRequest> scheduleList = solutionImportHandler.getImportSession().getManifest().getScheduleList();
    if ( solutionImportHandler.isPerformingRestore() ) {
      solutionImportHandler.getLogger().info( Messages.getInstance().getString( "SolutionImportHandler.INFO_START_IMPORT_SCHEDULE" ) );
    }
    if ( CollectionUtils.isNotEmpty( scheduleList ) ) {
      if ( solutionImportHandler.isPerformingRestore() ) {
        solutionImportHandler.getLogger().info( Messages.getInstance().getString( "SolutionImportHandler.INFO_COUNT_SCHEDULUE", scheduleList.size() ) );
      }
      int successfulScheduleImportCount = 0;

      IScheduler scheduler = PentahoSystem.get( IScheduler.class, "IScheduler2", null ); //$NON-NLS-1$
      ISchedulerResource schedulerResource = scheduler.createSchedulerResource();
      if ( solutionImportHandler.isPerformingRestore() ) {
        solutionImportHandler.getLogger().debug( "Pausing the scheduler before the start of the restore process" );
      }
      schedulerResource.pause();
      if ( solutionImportHandler.isPerformingRestore() ) {
        solutionImportHandler.getLogger().debug( "Successfully paused the scheduler" );
      }
      // Read property from system/security.properties
      ISystemConfig config = PentahoSystem.get( ISystemConfig.class );
      String provider = "jackrabbit";
      if ( config != null ) {
        provider = config.getProperty( "security.provider",  "jackrabbit");
      }
      for ( IJobScheduleRequest jobScheduleRequest : scheduleList ) {
        if ( solutionImportHandler.isPerformingRestore() ) {
          solutionImportHandler.getLogger().debug( "Restoring schedule name [ " + jobScheduleRequest.getJobName() + "] inputFile [ " + jobScheduleRequest.getInputFile() + " ] outputFile [ " + jobScheduleRequest.getOutputFile() + "]" );
        }

        // PHASE 1: Import the schedule owner user if needed
        // This creates the user account and home folder before schedule creation

        String scheduleOwnerUsername = extractScheduleOwnerUsername( jobScheduleRequest );
        if ( scheduleOwnerUsername != null && !scheduleOwnerUsername.trim().isEmpty() ) {
          if ( provider.equalsIgnoreCase( "jackrabbit" ) ) {
              ExportManifest manifest = solutionImportHandler.getImportSession().getManifest();
              solutionImportHandler.importUserAndRole( scheduleOwnerUsername, manifest );
          } else {
            // System is configured with an external authentication provider. Skip user creation, but create the user's home folder
            // Ensure home folder exists even for existing users (in case it was missing)

            if ( solutionImportHandler.isPerformingRestore() ) {
              solutionImportHandler.getLogger().debug( "Skipping the exporting of schedule owner's username [ " + scheduleOwnerUsername + " ]" );
            }
            try {
              ITenant tenant = new Tenant( "/pentaho/" + TenantUtils.getDefaultTenant(), true );
              ITenantManager tenantManager = PentahoSystem.get( ITenantManager.class );
              if ( tenantManager != null ) {
                tenantManager.createUserHomeFolder( tenant, scheduleOwnerUsername );
                if ( solutionImportHandler.isPerformingRestore() ) {
                  solutionImportHandler.getLogger().debug( "Verified/created home folder for existing user [ " + scheduleOwnerUsername + " ]" );
                }
              }
            } catch ( Exception e ) {
              // Don't fail if home folder creation has issues
              if ( solutionImportHandler.isPerformingRestore() ) {
                solutionImportHandler.getLogger().debug( "Could not verify home folder for existing user [ " + scheduleOwnerUsername + " ]: " + e.getMessage() );
              }
            }
          }
        }

        // PHASE 1.5: Import schedule dependencies from backup FIRST
        String inputFilePath = jobScheduleRequest.getInputFile();
        if ( inputFilePath != null && !inputFilePath.trim().isEmpty() ) {
          // Normalize path using full URL decoding (handles + to space, %2B to +, etc.)
          String normalizedInputPath = solutionImportHandler.normalizePath( inputFilePath );

          if ( !fileExistsInRepository( normalizedInputPath ) ) {
            // File doesn't exist - import it from backup
            if ( !importScheduleDependencyFile( normalizedInputPath ) ) {
              if ( solutionImportHandler.isPerformingRestore() ) {
                solutionImportHandler.getLogger().warn( "Skipping schedule [ " + jobScheduleRequest.getJobName()
                  + " ] because required input file [ " + inputFilePath + " ] could not be imported from backup" );
              }
              continue; // Skip this schedule, the file couldn't be imported
            }
          }
        }

        // PHASE 2: Now that file is guaranteed to exist, proceed with schedule import
        boolean jobExists = false;

        List<IJob> jobs = null;
        try {
          jobs = schedulerResource.getJobsList();
        } catch ( Exception e ) {
          throw new ImportException( "Failed to get list of existing scheduler jobs: " + e.getMessage(), e );
        }

        if ( jobs != null ) {

          //paramRequest to map<String, Serializable>
          Map<String, Serializable> mapParamsRequest = new HashMap<>();
          for ( IJobScheduleParam paramRequest : jobScheduleRequest.getJobParameters() ) {
            mapParamsRequest.put( paramRequest.getName(), paramRequest.getValue() );
          }

          // We will check the existing job in the repository. If the job being imported exists, we will remove it from the repository
          for ( IJob job : jobs ) {

            if ( ( mapParamsRequest.get( RESERVEDMAPKEY_LINEAGE_ID ) != null )
              && ( mapParamsRequest.get( RESERVEDMAPKEY_LINEAGE_ID )
              .equals( job.getJobParams().get( RESERVEDMAPKEY_LINEAGE_ID ) ) ) ) {
              jobExists = true;
            }

            if ( solutionImportHandler.isOverwriteFile() && jobExists ) {
              if ( solutionImportHandler.isPerformingRestore() ) {
                solutionImportHandler.getLogger().debug( "Schedule  [ " + jobScheduleRequest.getJobName() + "] already exists and overwrite flag is set to true. Removing the job so we can add it again" );
              }
              IJobRequest jobRequest = scheduler.createJobRequest();
              jobRequest.setJobId( job.getJobId() );
              schedulerResource.removeJob( jobRequest );
              jobExists = false;
              break;
            }
          }
        }

        if ( !jobExists ) {
          try {
            Response response = createSchedulerJob( schedulerResource, jobScheduleRequest );
            if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
              if ( response.getEntity() != null ) {
                // get the schedule job id from the response and add it to the import session
                ImportSession.getSession().addImportedScheduleJobId( response.getEntity().toString() );
                if ( solutionImportHandler.isPerformingRestore() ) {
                  solutionImportHandler.getLogger().debug( "Successfully restored schedule [ " + jobScheduleRequest.getJobName() + " ] " );
                }
                successfulScheduleImportCount++;
              }
            } else {
              solutionImportHandler.getLogger().error( Messages.getInstance()
                .getString( "SolutionImportHandler.ERROR_IMPORTING_SCHEDULE", jobScheduleRequest.getJobName(), response.getEntity() != null ? response.getEntity().toString() : "" ) );
            }
          } catch ( Exception e ) {
            // there is a scenario where if the file scheduled has a space in the file name, that it won't work. the di server replaces spaces with underscores and the export mechanism can't determine if it needs this to happen or not
            // so, if we failed to import and there is a space in the path, try again but this time with replacing the space(s)
            if ( jobScheduleRequest.getInputFile().contains( " " ) || jobScheduleRequest.getOutputFile().contains( " " ) ) {
              solutionImportHandler.getLogger().debug( Messages.getInstance()
                .getString( "SolutionImportHandler.SchedulesWithSpaces", jobScheduleRequest.getInputFile() ) );
              java.io.File inFile = new java.io.File( jobScheduleRequest.getInputFile() );
              java.io.File outFile = new java.io.File( jobScheduleRequest.getOutputFile() );
              String inputFileName = inFile.getParent() + RepositoryFile.SEPARATOR + inFile.getName().replace( " ", "_" );
              String outputFileName = outFile.getParent() + RepositoryFile.SEPARATOR + outFile.getName().replace( " ", "_" );
              jobScheduleRequest.setInputFile( inputFileName );
              jobScheduleRequest.setOutputFile( outputFileName );
              try {
                if ( !java.io.File.separator.equals( RepositoryFile.SEPARATOR ) ) {
                  // on windows systems, the backslashes will result in the file not being found in the repository
                  jobScheduleRequest.setInputFile( inputFileName.replace( java.io.File.separator, RepositoryFile.SEPARATOR ) );
                  jobScheduleRequest
                    .setOutputFile( outputFileName.replace( java.io.File.separator, RepositoryFile.SEPARATOR ) );
                }
                Response response = createSchedulerJob( schedulerResource, jobScheduleRequest );
                if ( response.getStatus() == Response.Status.OK.getStatusCode() ) {
                  if ( response.getEntity() != null ) {
                    // get the schedule job id from the response and add it to the import session
                    ImportSession.getSession().addImportedScheduleJobId( response.getEntity().toString() );
                    successfulScheduleImportCount++;
                  }
                }
              } catch ( Exception ex ) {
                // log it and keep going. we shouldn't stop processing all schedules just because one fails.
                solutionImportHandler.getLogger().error( Messages.getInstance()
                  .getString( "SolutionImportHandler.ERROR_0001_ERROR_CREATING_SCHEDULE", "[ " + jobScheduleRequest.getJobName() + " ] cause [ " + ex.getMessage() + " ]" ), ex );
              }
            } else {
              // log it and keep going. we shouldn't stop processing all schedules just because one fails.
              solutionImportHandler.getLogger().error( Messages.getInstance()
                .getString( "SolutionImportHandler.ERROR_0001_ERROR_CREATING_SCHEDULE", "[ " + jobScheduleRequest.getJobName() + " ]" ) );
            }
          }
        } else {
          solutionImportHandler.getLogger().info( Messages.getInstance()
            .getString( "DefaultImportHandler.ERROR_0009_OVERWRITE_CONTENT", jobScheduleRequest.toString() ) );
        }
      }
      if ( solutionImportHandler.isPerformingRestore() ) {
        solutionImportHandler.getLogger().info( Messages.getInstance()
          .getString( "SolutionImportHandler.INFO_SUCCESSFUL_SCHEDULE_IMPORT_COUNT", successfulScheduleImportCount, scheduleList.size() ) );
      }
      schedulerResource.start();
      if ( solutionImportHandler.isPerformingRestore() ) {
        solutionImportHandler.getLogger().debug( "Successfully started the scheduler" );
      }
    }
    if ( solutionImportHandler.isPerformingRestore() ) {
      solutionImportHandler.getLogger().info( Messages.getInstance().getString( "SolutionImportHandler.INFO_END_IMPORT_SCHEDULE" ) );
    }

    if ( solutionImportHandler.isPerformingRestore() ) {
      solutionImportHandler.getLogger().info( Messages.getInstance().getString( "SolutionImportHandler.INFO_END_IMPORT_SCHEDULE" ) );
    }
  }

  /**
   * Ensures that the file referenced by a schedule input path exists in the repository.
   * Files are imported by SolutionImportHandler.importRepositoryFilesAndFolders() before
   * this helper runs (since runImportHelpers is called after repository files are imported).
   *
   * This method checks if the file exists, skipping the schedule if not found.
   *
   * @param inputFilePath the repository path of the file referenced by the schedule
   * @return true if the file exists, false otherwise
   */
  protected boolean ensureScheduleInputFileExists( String inputFilePath ) {
    if ( inputFilePath == null || inputFilePath.trim().isEmpty() ) {
      return true; // No file reference, nothing to check
    }

    // Normalize the path to use forward slashes
    String normalizedPath = inputFilePath.replace( File.separator, RepositoryFile.SEPARATOR );

    // Check if the file already exists in the repository
    org.pentaho.platform.api.repository2.unified.IUnifiedRepository repo =
      PentahoSystem.get( org.pentaho.platform.api.repository2.unified.IUnifiedRepository.class );

    if ( repo == null ) {
      logger.warn( "Unable to get repository instance to validate schedule input file" );
      return true; // Assume file exists if we can't check
    }

    RepositoryFile existingFile = repo.getFile( normalizedPath );
    if ( existingFile != null ) {
      if ( solutionImportHandler.isPerformingRestore() ) {
        solutionImportHandler.getLogger().debug( "Schedule input file [ " + normalizedPath + " ] already exists in repository" );
      }
      return true;
    }

    // File doesn't exist in repository - try to import from the backup bundle
    if ( solutionImportHandler.isPerformingRestore() ) {
      solutionImportHandler.getLogger().debug( "Schedule input file [ " + normalizedPath + " ] not found in repository, attempting to import from backup" );
    }

    try {
      // Call SolutionImportHandler to import the file from the backup bundle
      if ( solutionImportHandler.importFileFromBundle( normalizedPath ) ) {
        // CRITICAL: Verify the file actually exists after import before returning success
        RepositoryFile importedFile = repo.getFile( normalizedPath );
        if ( importedFile != null ) {
          if ( solutionImportHandler.isPerformingRestore() ) {
            solutionImportHandler.getLogger().debug( "Successfully imported and verified schedule dependency file from backup: [ " + normalizedPath + " ]" );
          }
          return true;
        } else {
          // Import method returned true but file doesn't actually exist
          logger.warn( "Schedule input file import reported success but file not found in repository: [ " + normalizedPath + " ]" );
          return false;
        }
      }
    } catch ( Exception e ) {
      logger.warn( "Error importing schedule dependency file [ " + normalizedPath + " ]: " + e.getMessage() );
    }

    // File could not be imported
    if ( solutionImportHandler.isPerformingRestore() ) {
      solutionImportHandler.getLogger().warn( "Schedule input file [ " + normalizedPath + " ] not found in backup and could not be imported" );
    }
    return false;
  }

  /**
   * Normalize a repository path for consistent comparison:
   * - URL decode special characters (%28, %29, %20, etc.)
   * - Convert backslashes to forward slashes
   * - Normalize multiple spaces to single space
   */
  protected String normalizePath( String path ) {
    if ( path == null ) {
      return null;
    }

    try {
      // Handle URL encoding: replace + with space first (common in form encoding)
      String processed = path.replace( "+", " " );

      // URL decode: convert %XX to actual characters
      String decoded = URLDecoder.decode( processed, "UTF-8" );

      // Convert backslashes to forward slashes (Windows path support)
      String normalized = decoded.replace( "\\", "/" );

      // Ensure leading slash for repository path
      if ( !normalized.startsWith( "/" ) ) {
        normalized = "/" + normalized;
      }

      // Normalize multiple slashes to single slash
      normalized = normalized.replaceAll( "/+", "/" );

      // Normalize multiple spaces to single space
      normalized = normalized.replaceAll( " +", " " );

      return normalized;
    } catch ( Exception e ) {
      solutionImportHandler.getLogger().debug( "Error normalizing path [ " + path + " ]: " + e.getMessage() );
      return path;
    }
  }

  /**
   * Check if a file exists in the repository
   */
  protected boolean fileExistsInRepository( String normalizedPath ) {
    if ( normalizedPath == null || normalizedPath.trim().isEmpty() ) {
      return true;
    }

    org.pentaho.platform.api.repository2.unified.IUnifiedRepository repo =
      PentahoSystem.get( org.pentaho.platform.api.repository2.unified.IUnifiedRepository.class );

    if ( repo == null ) {
      logger.warn( "Unable to get repository instance to validate schedule input file" );
      return false;
    }

    RepositoryFile file = repo.getFile( normalizedPath );
    return file != null;
  }

  /**
   * Import a schedule dependency file from the backup bundle
   *
   * @param normalizedPath the normalized repository path of the file to import
   * @return true if the file was successfully imported and verified to exist, false otherwise
   */
  protected boolean importScheduleDependencyFile( String normalizedPath ) {
    if ( normalizedPath == null || normalizedPath.trim().isEmpty() ) {
      return true;
    }

    if ( solutionImportHandler.isPerformingRestore() ) {
      solutionImportHandler.getLogger().debug( "Importing schedule dependency file from backup: [ " + normalizedPath + " ]" );
    }

    try {
      // Import the file from the backup bundle
      boolean imported = solutionImportHandler.importFileFromBundle( normalizedPath );

      if ( imported ) {
        // Verify the file was actually imported and is now in the repository
        if ( fileExistsInRepository( normalizedPath ) ) {
          if ( solutionImportHandler.isPerformingRestore() ) {
            solutionImportHandler.getLogger().debug( "Successfully imported and verified schedule dependency file: [ " + normalizedPath + " ]" );
          }
          return true;
        } else {
          // Import reported success but file still doesn't exist
          logger.warn( "File import reported success but file not found in repository after import: [ " + normalizedPath + " ]" );
          return false;
        }
      } else {
        logger.warn( "Failed to import schedule dependency file from backup: [ " + normalizedPath + " ]" );
        return false;
      }
    } catch ( Exception e ) {
      logger.warn( "Exception while importing schedule dependency file [ " + normalizedPath + " ]: " + e.getMessage(), e );
      return false;
    }
  }

  public Response createSchedulerJob( ISchedulerResource scheduler, IJobScheduleRequest jobScheduleRequest )
    throws IOException {
    Response rs = scheduler != null ? (Response) scheduler.createJob( jobScheduleRequest ) : null;
    if ( jobScheduleRequest.getJobState() != JobState.NORMAL ) {
      try {
        IJobRequest jobRequest = PentahoSystem.get( IScheduler.class, "IScheduler2", null ).createJobRequest();
        jobRequest.setJobId( rs.getEntity().toString() );
        scheduler.pauseJob( jobRequest );
      } catch ( Exception e ) {
        // Job was created but may reference missing files. Log warning but don't fail.
        // The job exists in the scheduler but is paused due to validation issues.
        logger.warn( "Warning: Job created but could not be paused. It may reference missing files: " + e.getMessage() );
      }
    }
    return rs;
  }

  @Override public String getName() {
    return SCHEDULE_IMPORT_UTIL_NAME;
  }

  /**
   * Extract the schedule owner username from the job schedule request.
   * The owner is stored in the job parameters using the reserved key RESERVEDMAPKEY_ACTIONUSER.
   *
   * @param jobScheduleRequest the job schedule request to extract username from
   * @return the username of the schedule owner, or null if not found
   */
  protected String extractScheduleOwnerUsername( IJobScheduleRequest jobScheduleRequest ) {
    if ( jobScheduleRequest == null || jobScheduleRequest.getJobParameters() == null ) {
      return null;
    }

    // Search for the schedule owner parameter
    for ( IJobScheduleParam param : jobScheduleRequest.getJobParameters() ) {
      if ( IScheduler.RESERVEDMAPKEY_ACTIONUSER.equals( param.getName() ) ) {
        String username = (String) param.getValue();
        return username != null ? username.trim() : null;
      }
    }

    return null;
  }

  /**
   * Import the schedule owner user if needed.
   * Gets the UserExport from the manifest and calls SolutionImportHandler to import the user.
   * This ensures the schedule owner exists before the schedule is created.
   *
   * @param scheduleOwnerUsername the username of the schedule owner to import
   */


  /**
   * Validates and corrects schedule times before import.
   * KEY FIX: Removes endTime for one-time schedules (repeatCount < 0) to prevent validation errors
   *
   * One-time schedules (RUN_ONCE, repeatCount=-1) should NOT have an endTime constraint.
   * When endTime is populated for one-time schedules, the scheduler validator fails with
   * "End time cannot be before start time" error.
   *
   * NOTE: This is a defensive correction during import. The primary filtering should happen
   * during export via ScheduleExportUtil.shouldExportSchedule() which prevents invalid schedules
   * from being exported in the first place.
   *
   * @param schedule the schedule to validate
   * @return true if valid or corrected, false if uncorrectable
   */
  protected boolean validateAndCorrectScheduleTime( IJobScheduleRequest schedule ) {
    if ( schedule == null ) {
      return false;
    }

    String scheduleName = "Schedule [" + schedule.getJobName() + "]";

    // Check simple job trigger
    if ( schedule.getSimpleJobTrigger() != null ) {
      org.pentaho.platform.api.scheduler2.ISimpleJobTrigger trigger = schedule.getSimpleJobTrigger();
      java.util.Date startTimeDate = trigger.getStartTime();
      java.util.Date endTimeDate = trigger.getEndTime();
      long duration = trigger.getDuration();
      int repeatCount = trigger.getRepeatCount();
      long repeatInterval = trigger.getRepeatInterval();

      long startTime = startTimeDate != null ? startTimeDate.getTime() : 0;
      long endTime = endTimeDate != null ? endTimeDate.getTime() : 0;

      // PRIMARY FIX: One-time schedules (repeatCount < 0) should NOT have endTime
      if ( repeatCount < 0 && endTime > 0 ) {
        if ( solutionImportHandler.isPerformingRestore() ) {
          solutionImportHandler.getLogger().warn( scheduleName +
            " is a RUN_ONCE schedule (repeatCount=" + repeatCount +
            ") but has endTime. Removing endTime for one-time execution." );
        }
        trigger.setEndTime( null );  // Remove invalid end time
        return true;
      }

      // Fix invalid repeatInterval when repeatCount >= 0
      if ( repeatInterval < 0 && repeatCount >= 0 ) {
        if ( solutionImportHandler.isPerformingRestore() ) {
          solutionImportHandler.getLogger().warn( scheduleName +
            " has invalid repeatInterval: " + repeatInterval +
            ". Setting to default (1 hour)." );
        }
        trigger.setRepeatInterval( 3600000 );  // 1 hour
      }

      // Fix invalid start time
      if ( startTime <= 0 ) {
        if ( solutionImportHandler.isPerformingRestore() ) {
          solutionImportHandler.getLogger().warn( scheduleName + " has invalid start time: " + startTime +
            ". Setting to current time." );
        }
        trigger.setStartTime( new java.util.Date() );
      }

      // Fix end time < start time (secondary check)
      if ( endTime > 0 && startTime > 0 && endTime < startTime ) {
        if ( solutionImportHandler.isPerformingRestore() ) {
          solutionImportHandler.getLogger().warn( scheduleName + " has end time before start time. " +
            "Removing end time constraint." );
        }
        trigger.setEndTime( null );
      }

      // Fix invalid duration for repeating schedules
      if ( duration <= 0 && repeatCount >= 0 ) {
        if ( solutionImportHandler.isPerformingRestore() ) {
          solutionImportHandler.getLogger().warn( scheduleName + " has invalid duration: " + duration +
            ". Setting to default (1 hour)." );
        }
        trigger.setDuration( 3600000 );  // 1 hour
      }

      // Verify final state after corrections
      java.util.Date finalStartTime = trigger.getStartTime();
      java.util.Date finalEndTime = trigger.getEndTime();
      if ( finalStartTime != null && finalEndTime != null ) {
        if ( finalEndTime.getTime() < finalStartTime.getTime() ) {
          if ( solutionImportHandler.isPerformingRestore() ) {
            solutionImportHandler.getLogger().error( scheduleName +
              " still has invalid time range after correction. Schedule will be skipped." );
          }
          return false; // Cannot fix, skip this schedule
        }
      }
    }

    // Validate cron trigger times
    if ( schedule.getCronJobTrigger() != null ) {
      org.pentaho.platform.api.scheduler2.ICronJobTrigger trigger = schedule.getCronJobTrigger();
      java.util.Date startTimeDate = trigger.getStartTime();
      java.util.Date endTimeDate = trigger.getEndTime();

      long startTime = startTimeDate != null ? startTimeDate.getTime() : 0;
      long endTime = endTimeDate != null ? endTimeDate.getTime() : 0;

      // Fix: Cron without end time (or end before start)
      if ( endTime <= 0 || (endTime > 0 && endTime < startTime) ) {
        if ( solutionImportHandler.isPerformingRestore() ) {
          solutionImportHandler.getLogger().debug( scheduleName +
            " cron schedule has no or invalid end time. Setting to far future (10 years)." );
        }

        // Set end time to 10 years in future
        long futureEndTime = startTime + (10L * 365 * 24 * 60 * 60 * 1000);
        trigger.setEndTime( new java.util.Date( futureEndTime ) );
      }
    }

    if ( solutionImportHandler.isPerformingRestore() ) {
      solutionImportHandler.getLogger().debug( scheduleName + " passed time validation." );
    }

    return true;
  }
}
