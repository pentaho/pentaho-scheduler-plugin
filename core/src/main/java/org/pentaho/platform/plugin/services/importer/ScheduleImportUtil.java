package org.pentaho.platform.plugin.services.importer;

import org.apache.commons.collections.CollectionUtils;
import org.pentaho.platform.api.importexport.IImportHelper;
import org.pentaho.platform.api.importexport.ImportException;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.IJob;
import org.pentaho.platform.api.scheduler2.IJobRequest;
import org.pentaho.platform.api.scheduler2.IJobScheduleParam;
import org.pentaho.platform.api.scheduler2.IJobScheduleRequest;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.ISchedulerResource;
import org.pentaho.platform.api.scheduler2.JobState;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.importexport.ImportSession;
import org.pentaho.platform.plugin.services.messages.Messages;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleImportUtil implements IImportHelper {
  private static final String RESERVEDMAPKEY_LINEAGE_ID = "lineage-id";
  private static final String SCHEDULE_IMPORT_UTIL_NAME ="schedule-import-util";

  public ScheduleImportUtil() {
    super();
  }

  public void registerAsHelper() {
    PentahoSystem.get( SolutionImportHandler.class, "solutionImportHandler", null ).addImportHelper( this );
  }

  @Override public void doImport( Object importArg ) throws ImportException {
    SolutionImportHandler solutionImportHandler = (SolutionImportHandler) importArg;

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
      for ( IJobScheduleRequest jobScheduleRequest : scheduleList ) {
        if ( solutionImportHandler.isPerformingRestore() ) {
          solutionImportHandler.getLogger().debug( "Restoring schedule name [ " + jobScheduleRequest.getJobName() + "] inputFile [ " + jobScheduleRequest.getInputFile() + " ] outputFile [ " + jobScheduleRequest.getOutputFile() + "]" );
        }
        boolean jobExists = false;

        List<IJob> jobs = schedulerResource.getJobsList();
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

            if ( solutionImportHandler.overwriteFile && jobExists ) {
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
              solutionImportHandler.getLogger().error( Messages.getInstance().getString( "SolutionImportHandler.ERROR_IMPORTING_SCHEDULE", jobScheduleRequest.getJobName(), response.getEntity() != null
                ? response.getEntity().toString() : "" ) );
            }
          } catch ( Exception e ) {
            // there is a scenario where if the file scheduled has a space in the file name, that it won't work. the
            // di server

            // replaces spaces with underscores and the export mechanism can't determine if it needs this to happen
            // or not
            // so, if we failed to import and there is a space in the path, try again but this time with replacing
            // the space(s)
            if ( jobScheduleRequest.getInputFile().contains( " " ) || jobScheduleRequest.getOutputFile()
              .contains( " " ) ) {
              solutionImportHandler.getLogger().debug( Messages.getInstance()
                .getString( "SolutionImportHandler.SchedulesWithSpaces", jobScheduleRequest.getInputFile() ) );
              File inFile = new File( jobScheduleRequest.getInputFile() );
              File outFile = new File( jobScheduleRequest.getOutputFile() );
              String inputFileName = inFile.getParent() + RepositoryFile.SEPARATOR
                + inFile.getName().replace( " ", "_" );
              String outputFileName = outFile.getParent() + RepositoryFile.SEPARATOR
                + outFile.getName().replace( " ", "_" );
              jobScheduleRequest.setInputFile( inputFileName );
              jobScheduleRequest.setOutputFile( outputFileName );
              try {
                if ( !File.separator.equals( RepositoryFile.SEPARATOR ) ) {
                  // on windows systems, the backslashes will result in the file not being found in the repository
                  jobScheduleRequest.setInputFile( inputFileName.replace( File.separator, RepositoryFile.SEPARATOR ) );
                  jobScheduleRequest
                    .setOutputFile( outputFileName.replace( File.separator, RepositoryFile.SEPARATOR ) );
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
  }

  public Response createSchedulerJob( ISchedulerResource scheduler, IJobScheduleRequest jobScheduleRequest )
    throws IOException {
    Response rs = scheduler != null ? (Response) scheduler.createJob( jobScheduleRequest ) : null;
    if ( jobScheduleRequest.getJobState() != JobState.NORMAL ) {
      IJobRequest jobRequest = PentahoSystem.get( IScheduler.class, "IScheduler2", null ).createJobRequest();
      jobRequest.setJobId( rs.getEntity().toString() );
      scheduler.pauseJob( jobRequest );
    }
    return rs;
  }

  @Override public String getName() {
    return SCHEDULE_IMPORT_UTIL_NAME;
  }
}
