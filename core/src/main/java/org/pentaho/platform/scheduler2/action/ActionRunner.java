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


package org.pentaho.platform.scheduler2.action;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.action.ActionInvocationException;
import org.pentaho.platform.api.action.IAction;
import org.pentaho.platform.api.action.IPostProcessingAction;
import org.pentaho.platform.api.action.IStreamingAction;
import org.pentaho.platform.api.action.IVarArgsAction;
import org.pentaho.platform.api.repository.IContentItem;
import org.pentaho.platform.api.repository2.unified.ISourcesStreamEvents;
import org.pentaho.platform.api.repository2.unified.IStreamListener;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.IBackgroundExecutionStreamProvider;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.engine.core.output.FileContentItem;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.solution.ActionSequenceCompatibilityFormatter;
import org.pentaho.platform.scheduler2.ISchedulerOutputPathResolver;
import org.pentaho.platform.scheduler2.messsages.Messages;
import org.pentaho.platform.util.ActionUtil;
import org.pentaho.platform.util.beans.ActionHarness;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.platform.workitem.WorkItemLifecycleEventUtil;
import org.pentaho.platform.workitem.WorkItemLifecyclePhase;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class ActionRunner implements IActionRunner {

  private static final Log logger = LogFactory.getLog( ActionRunner.class );

  protected Map<String, Object> params;
  protected IAction actionBean;
  protected IBackgroundExecutionStreamProvider streamProvider;
  protected String actionUser;

  protected String outputFilePath = null;
  protected boolean streamComplete = false;
  protected Object lock = new Object();

  public static final String KEY_USE_JCR = "useJcr"; // TODO move to more common place
  public static final String KEY_JCR_OUTPUT_PATH = "jcrOutputPath"; // TODO move to more common place

  public ActionRunner() {
    // empty constructor
  }

    public ActionRunner( final IAction actionBean, final String actionUser, final Map<String, Object> params, final
    IBackgroundExecutionStreamProvider streamProvider ) {
    this.actionBean = actionBean;
    this.actionUser = actionUser;
    this.params = params;
    this.streamProvider = streamProvider;
  }

  @Override
  public void setAction( IAction action ) {
    this.actionBean = action;
  }

  @Override
  public void setActionUser( String actionUser ) {
    this.actionUser = actionUser;
  }

  @Override
  public void setParams( Map<String, Object> params ) {
     this.params = params;
  }

  @Override
  public void setStreamProvider( IBackgroundExecutionStreamProvider streamProvider ) {
    this.streamProvider = streamProvider;
  }

  public Boolean call() throws ActionInvocationException {
    final String workItemName = ActionUtil.extractName( params );
    try {
      final ExecutionResult result = callImpl();
      if ( result.isSuccess() ) {
        WorkItemLifecycleEventUtil.publish( workItemName, params, WorkItemLifecyclePhase.SUCCEEDED );
      } else {
        WorkItemLifecycleEventUtil.publish( workItemName, params, WorkItemLifecyclePhase.FAILED );
      }
      return result.updateRequired();
    } catch ( final Throwable t ) {
      // ensure that the main thread isn't blocked on lock
      synchronized ( lock ) {
        lock.notifyAll();
      }
      WorkItemLifecycleEventUtil.publish( workItemName, params, WorkItemLifecyclePhase.FAILED, t.toString() );
      // We should not distinguish between checked and unchecked exceptions here. All job execution failures
      // should result in a rethrow of the exception
      throw new ActionInvocationException( Messages.getInstance().getActionFailedToExecute( actionBean //$NON-NLS-1$
        .getClass().getName() ), t );
    }
  }

  private ExecutionResult callImpl() throws Exception {
    boolean executionStatus = true;

    final Object locale = params.get( LocaleHelper.USER_LOCALE_PARAM );
    if ( locale instanceof Locale ) {
      LocaleHelper.setThreadLocaleOverride( (Locale) locale );
    } else {
      LocaleHelper.setThreadLocaleOverride( new Locale( (String) locale ) );
    }
    // sync job params to the action bean
    ActionHarness actionHarness = new ActionHarness( actionBean );

    final Map<String, Object> actionParams = new HashMap<>();
    actionParams.putAll( params );
    if ( streamProvider != null ) {
      actionParams.put( "inputStream", streamProvider.getInputStream() );
    }
    actionHarness.setValues( actionParams, new ActionSequenceCompatibilityFormatter() );

    if ( actionBean instanceof IVarArgsAction ) {
      actionParams.remove( "inputStream" );
      actionParams.remove( "outputStream" );
      ( (IVarArgsAction) actionBean ).setVarArgs( actionParams );
    }

    boolean waitForFileCreated = false;
    OutputStream stream = null;

    if ( streamProvider != null ) {
      actionParams.remove( "inputStream" );
      if ( actionBean instanceof IStreamingAction ) {
        streamProvider.setStreamingAction( (IStreamingAction) actionBean );
      }

      // BISERVER-9414 - validate that output path still exist
      String outputPath = resolveOutputFilePath();

      if ( outputPath == null ) {
        return new ExecutionResult( true, false );
      }

      addJcrParams( actionParams, outputPath );

      if ( !outputPath.equals( streamProvider.getOutputPath() ) ) {
        streamProvider.setOutputFilePath( outputPath ); // set fallback path
        // Job output path requires update. The update triggers a new job that will fulfill the execution.
        return new ExecutionResult( true, true );
      }

      stream = streamProvider.getOutputStream();
      if ( stream instanceof ISourcesStreamEvents ) {
        ( (ISourcesStreamEvents) stream ).addListener( new IStreamListener() {
          public void fileCreated( final String filePath ) {
            synchronized ( lock ) {
              outputFilePath = filePath;
              lock.notifyAll();
            }
          }

          @Override
          public void streamComplete() {
            synchronized ( lock ) {
              lock.notifyAll();
              streamComplete = true;
            }
          }
        } );
        waitForFileCreated = true;
      }
      actionParams.put( "outputStream", stream );
      actionHarness.setValues( actionParams );
    }

    actionBean.execute();
    executionStatus = actionBean.isExecutionSuccessful();
    if ( stream != null ) {
      IOUtils.closeQuietly( stream );
    }

    if ( waitForFileCreated ) {
      synchronized ( lock ) {
        while ( outputFilePath == null && !streamComplete ) {
          lock.wait( 1000 );
        }
      }
      sendEmail( actionParams );
      deleteFileIfEmpty();
    }
    if ( actionBean instanceof IPostProcessingAction ) {
      closeContentOutputStreams( (IPostProcessingAction) actionBean );
      markContentAsGenerated( (IPostProcessingAction) actionBean );
    }

    // Create the ExecutionResult to return the status and whether the update is required or not
    return new ExecutionResult( false, executionStatus );
  }

  /**
   * Get full path parent directory of a given full path filename.
   * @param path
   * @return
   */
  protected String getParentDirectory( String path ) {
    return FilenameUtils.getFullPathNoEndSeparator( path );
  }

  /**
   * Add JCR related key/values.
   * @param actionParams
   * @param outputPath
   */
  protected void addJcrParams( Map<String, Object> actionParams, String outputPath ) {
    actionParams.put( KEY_USE_JCR, actionParams.getOrDefault( KEY_USE_JCR, Boolean.TRUE ) );
    actionParams.put( KEY_JCR_OUTPUT_PATH, actionParams.getOrDefault( KEY_JCR_OUTPUT_PATH,
      getParentDirectory( outputPath ) ) );
  }

  /**
   * Wrapper call to {@link ISchedulerOutputPathResolver#resolveOutputFilePath()}
   * @return
   */
  protected String resolveOutputFilePath() throws SchedulerException {

    ISchedulerOutputPathResolver schedulerOutputPathResolver = createSchedulerOutputPathResolver(
        streamProvider.getOutputPath() );
    return schedulerOutputPathResolver.resolveOutputFilePath();
  }

  /**
   * Instantiate {@link ISchedulerOutputPathResolver} and calls setters.
   * @param outputPathPattern
   * @return
   */
  protected ISchedulerOutputPathResolver createSchedulerOutputPathResolver( String outputPathPattern ) {
    ISchedulerOutputPathResolver schedulerOutputPathResolver = PentahoSystem.get( ISchedulerOutputPathResolver.class );
    buildSchedulerOutputPathResolver( schedulerOutputPathResolver, outputPathPattern );
    return schedulerOutputPathResolver;
  }

  /**
   * Sets various values for <code>schedulerOutputPathResolver</code>
   * @param outputPathPattern
   * @return
   */
  protected void buildSchedulerOutputPathResolver( ISchedulerOutputPathResolver schedulerOutputPathResolver,
                                                   String outputPathPattern ) {

    String outputDirectory = FilenameUtils.getFullPath( outputPathPattern );
    String filename = outputPathPattern.replace( outputDirectory, "" ); // DO Not use FilenameUtils.getName

    schedulerOutputPathResolver.setFileName( filename );
    schedulerOutputPathResolver.setDirectory( outputDirectory );
    schedulerOutputPathResolver.setActionUser( this.actionUser );
  }

  /**
   * Send email with attachment including generated output file.
   * The successfulness of email will be determined by logic in {@link ActionUtil#sendEmail(Map, Map, String)}
   *
   * @param actionParams
   * @return
   */
  protected void sendEmail( Map<String, Object> actionParams ) {
    ActionUtil.sendEmail( actionParams, params, outputFilePath );
  }

  /**
   * Deletes repository file if the file size is zero.
   */
  protected void deleteFileIfEmpty() {
    if ( outputFilePath == null ) {
      return;
    }
    IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );
    RepositoryFile file = repo.getFile( outputFilePath );
    if ( file.getFileSize().equals( 0L ) ) {
      repo.deleteFile( file.getId(), true, null );
    }
  }

  private void closeContentOutputStreams( IPostProcessingAction actionBean ) {
    for ( IContentItem contentItem : actionBean.getActionOutputContents() ) {
      contentItem.closeOutputStream();
    }
  }

  private void markContentAsGenerated( IPostProcessingAction actionBean ) {
    IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );
    String lineageId = (String) params.get( ActionUtil.QUARTZ_LINEAGE_ID );
    for ( IContentItem contentItem : actionBean.getActionOutputContents() ) {
      RepositoryFile sourceFile = getRepositoryFileSafe( repo, contentItem.getPath() );
      // add metadata if we have access and we have file
      if ( sourceFile != null ) {
        Map<String, Serializable> metadata = repo.getFileMetadata( sourceFile.getId() );
        metadata.put( ActionUtil.QUARTZ_LINEAGE_ID, lineageId );
        repo.setFileMetadata( sourceFile.getId(), metadata );
      } else {
        String fileName = getFSFileNameSafe( contentItem );
        logger.warn( Messages.getInstance().getSkipRemovingOutputFile( fileName ) );
      }
    }
  }

  private RepositoryFile getRepositoryFileSafe( IUnifiedRepository repo, String path ) {
    try {
      return repo.getFile( path );
    } catch ( Exception e ) {
      logger.debug( Messages.getInstance().getCannotGetRepoFile( path, e.getMessage() ) );
      return null;
    }
  }

  private String getFSFileNameSafe( IContentItem contentItem ) {
    if ( contentItem instanceof FileContentItem ) {
      return ( (FileContentItem) contentItem ).getFile().getName();
    }
    return null;
  }

  /**
   * Class to hold the result of the invoke Action
   */
  private class ExecutionResult {
    private boolean updateRequired;
    private boolean isSuccess;

    public ExecutionResult( Boolean updateRequired, Boolean isSuccess ) {
      this.updateRequired = updateRequired;
      this.isSuccess = isSuccess;
    }
    public Boolean updateRequired() {
      return updateRequired;
    }

    public Boolean isSuccess() {
      return isSuccess;
    }

  }
}
