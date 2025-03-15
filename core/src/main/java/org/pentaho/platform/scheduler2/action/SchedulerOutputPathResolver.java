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

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.genericfile.GenericFilePermission;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.repository.IClientRepositoryPathsStrategy;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.usersettings.IUserSettingService;
import org.pentaho.platform.api.usersettings.pojo.IUserSetting;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.scheduler2.ISchedulerOutputPathResolver;
import org.pentaho.platform.scheduler2.messsages.Messages;
import org.pentaho.platform.web.http.api.resources.services.SchedulerService;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Rowell Belen
 */
public class SchedulerOutputPathResolver implements ISchedulerOutputPathResolver {

  private static final String WILDCARD_EXTENSION = ".*";
  private static final String DEFAULT_SETTING_KEY = "default-scheduler-output-path";
  public static final String SCHEDULER_ACTION_NAME = "org.pentaho.scheduler.manage";
  private static final Log logger = LogFactory.getLog( SchedulerOutputPathResolver.class );
  private static final List<GenericFilePermission> permissions = new ArrayList<>();

  private String filename;
  private String directory;
  private String actionUser;

  private IGenericFileService genericFileService;
  @NonNull
  private IGenericFileService getGenericFileService() {
    if ( genericFileService == null ) {
      genericFileService = PentahoSystem.get( IGenericFileService.class );
    }

    return genericFileService;
  }

  @VisibleForTesting
  void setGenericFileService( @Nullable IGenericFileService genericFileService ) {
    this.genericFileService = genericFileService;
  }

  static {
    // initialize permissions
    permissions.add( GenericFilePermission.READ );
    permissions.add( GenericFilePermission.WRITE );
  }

  @Override
  public String getFilename() {
    return filename;
  }

  @Override
  public void setFileName( String fileName ) {
    this.filename = fileName;
  }

  @Override
  public String getDirectory() {
    return directory;
  }

  @Override
  public void setDirectory( String directory ) {
    this.directory = directory;
  }

  @Override
  public String getActionUser() {
    return actionUser;
  }

  @Override
  public void setActionUser( String actionUser ) {
    this.actionUser = actionUser;
  }

  @Override
  public String resolveOutputFilePath() throws SchedulerException {

    // IMPROVEMENT: This does not belong here. Move to the caller.
    boolean scheduleAllowed = isScheduleAllowed();
    if ( !scheduleAllowed ) {
      throw new SchedulerException( Messages.getInstance().getString(
        "QuartzScheduler.ERROR_0009_SCHEDULING_IS_NOT_ALLOWED_AFTER_CHANGE", getJobName(), getActionUser() ) );
    }

    // Enclose validation logic in the context of the job creator's session, not the current session
    return runAsUser( this::resolveOutputFilePathCore );
  }

  private String resolveOutputFilePathCore() throws SchedulerException {
    String outputFilePath = getDirectory();
    String fileNamePattern = getFilename();

    if ( isValidOutputPath( outputFilePath, false ) ) {
      return concat( outputFilePath, fileNamePattern ); // return if valid
    } else if ( !SchedulerService.isFallbackEnabled() ) { // If fallback is not enabled, throw an exception
      throw new SchedulerException( Messages.getInstance()
                .getString( "QuartzScheduler.ERROR_0016_UNAVAILABLE_OUTPUT_LOCATION", actionUser ) );
    }

    // evaluate fallback output paths
    String[] fallbackPaths = new String[] {
      getUserSettingOutputPath(), // user setting
      getSystemSettingOutputPath(), // system setting
      getUserHomeDirectoryPath() // home directory
    };

    for ( String fallbackPath : fallbackPaths ) {
      if ( isValidOutputPath( fallbackPath, true ) ) {
        // This is a warning so that it pairs with the messages which are real warnings emitted from doesFolderExist
        // and isPermitted. This is actually a resolution message for the other warnings.
        logger.warn( Messages.getInstance().getString(
          "QuartzScheduler.ERROR_0014_FOUND_AVAILABLE_OUTPUT_LOCATION_FALLBACK",
          fallbackPath,
          getJobName(),
          getActionUser() ) );

        return concat( fallbackPath, fileNamePattern );
      }
    }

    // Should not really happen, but if it does...
    logger.error( Messages.getInstance().getString(
      "QuartzScheduler.ERROR_0015_NO_AVAILABLE_OUTPUT_LOCATION_FALLBACK",
      getJobName(),
      getActionUser() ) );

    return null;
  }

  /**
   * Combine <code>directory</code> and <code>filename</code>
   *
   * @param directory
   * @param filename
   * @return
   */
  public String concat( String directory, String filename ) {
    return SchedulerFilenameUtils.concat( directory, filename );
  }

  private String runAsUser( Callable<String> callable ) {
    try {
      if ( callable != null ) {
        return SecurityHelper.getInstance().runAsUser( this.actionUser, callable );
      }
    } catch ( Exception e ) {
      logger.error( e.getMessage(), e );
    }

    return null;
  }

  protected boolean isValidOutputPath( final String outputPath, boolean isFallback ) {
    if ( StringUtils.isBlank( outputPath ) ) {
      return false;
    }

    try {
      boolean result = doesFolderExist( outputPath ) && isPermitted( outputPath );
      if ( !result ) {
        String msgId = isFallback
          ? "QuartzScheduler.ERROR_0012_UNAVAILABLE_OUTPUT_LOCATION_FALLBACK"
          : "QuartzScheduler.ERROR_0010_UNAVAILABLE_OUTPUT_LOCATION";
        logger.warn( Messages.getInstance().getString( msgId, outputPath, getJobName(), getActionUser() ) );
      }

      return result;
    } catch ( OperationFailedException e ) {
      String msgId = isFallback
        ? "QuartzScheduler.ERROR_0013_UNAVAILABLE_OUTPUT_LOCATION_FALLBACK_ERROR"
        : "QuartzScheduler.ERROR_0011_UNAVAILABLE_OUTPUT_LOCATION_ERROR";
      logger.warn( Messages.getInstance().getString( msgId, outputPath, getJobName(), getActionUser() ), e );
      return false;
    }
  }

  protected boolean doesFolderExist( @NonNull String path ) throws OperationFailedException {
    return getGenericFileService().doesFolderExist( path );
  }

  protected boolean isPermitted( String path ) throws OperationFailedException {
    return getGenericFileService().hasAccess( path, EnumSet.copyOf( permissions ) );
  }

  /**
   * Extracts job name from {@link #getFilename()}, by removing some of the path text.
   * @return
   */
  protected String getJobName() {
    return StringUtils.isNotBlank( getFilename() )
      ? stripWildcardExtension( getFilename() )
      : "<?>";
  }

  private static String stripWildcardExtension( String fileName ) {
    if ( fileName != null && fileName.endsWith( WILDCARD_EXTENSION ) ) {
      return fileName.substring( 0, fileName.length() - WILDCARD_EXTENSION.length() );
    }

    return fileName;
  }

  private String getUserSettingOutputPath() {
    try {
      // TODO: This is currently returning null. May be due to the service not being registered using
      //  `<pen:publish as-type="INTERFACES" />`, even though that isn't impeding the counterpart
      //  `resources/SchedulerOutputPathResolver` from resolving it. Tried publishing, but then Spring would fail due to
      //  there not being a current Spring Session context when this code runs (when a schedule is run).
      IUserSettingService userSettingService = getUserSettingService();
      if ( userSettingService != null ) {
        IUserSetting userSetting = userSettingService.getUserSetting( DEFAULT_SETTING_KEY, null );
        if ( userSetting != null && StringUtils.isNotBlank( userSetting.getSettingValue() ) ) {
          return userSetting.getSettingValue();
        }
      }
    } catch ( Exception e ) {
      logger.warn( e.getMessage(), e );
    }

    return null;
  }

  private String getSystemSettingOutputPath() {
    try {
      return PentahoSystem.getSystemSettings().getSystemSetting( DEFAULT_SETTING_KEY, null );
    } catch ( Exception e ) {
      logger.warn( e.getMessage(), e );
    }
    return null;
  }

  private String getUserHomeDirectoryPath() {
    try {
      IClientRepositoryPathsStrategy pathsStrategy =
          PentahoSystem.get( IClientRepositoryPathsStrategy.class, getScheduleCreatorSession() );
      return pathsStrategy.getUserHomeFolderPath( getScheduleCreatorSession().getName() );
    } catch ( Exception e ) {
      logger.warn( e.getMessage(), e );
    }
    return null;
  }

  private IPentahoSession getScheduleCreatorSession() {
    return PentahoSessionHolder.getSession();
  }

  private IUnifiedRepository getRepository() {
    return PentahoSystem.get( IUnifiedRepository.class, getScheduleCreatorSession() );
  }

  private IUserSettingService getUserSettingService() {
    return PentahoSystem.get( IUserSettingService.class, getScheduleCreatorSession() );
  }

  private IAuthorizationPolicy getAuthorizationPolicy() {
    return PentahoSystem.get( IAuthorizationPolicy.class, getScheduleCreatorSession() );
  }

  private boolean isScheduleAllowed() {
    return getAuthorizationPolicy().isAllowed( SCHEDULER_ACTION_NAME );
  }
}
