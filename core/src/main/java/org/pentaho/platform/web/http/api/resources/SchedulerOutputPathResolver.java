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


package org.pentaho.platform.web.http.api.resources;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.genericfile.GenericFilePermission;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.usersettings.IUserSettingService;
import org.pentaho.platform.api.usersettings.pojo.IUserSetting;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.repository.RepositoryFilenameUtils;
import org.pentaho.platform.repository2.ClientRepositoryPaths;
import org.pentaho.platform.scheduler2.messsages.Messages;
import org.pentaho.platform.web.http.api.resources.services.SchedulerService;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * @author Rowell Belen
 */
public class SchedulerOutputPathResolver {

  private static final String DEFAULT_SETTING_KEY = "default-scheduler-output-path";
  private static final String FALLBACK_SETTING_KEY = "settings/scheduler-fallback";

  private static final Log logger = LogFactory.getLog( SchedulerOutputPathResolver.class );

  private static final List<GenericFilePermission> permissions = new ArrayList<>();

  static {
    // initialize permissions
    permissions.add( GenericFilePermission.READ );
    permissions.add( GenericFilePermission.WRITE );
  }

  private IGenericFileService genericFileService;
  private IPentahoSession pentahoSession = PentahoSessionHolder.getSession();

  private IUserSettingService settingsService;
  private JobScheduleRequest scheduleRequest;

  private String scheduleOwner;

  public SchedulerOutputPathResolver( JobScheduleRequest scheduleRequest ) {
    this.scheduleRequest = scheduleRequest;
    scheduleOwner = getScheduleOwnerFromRequest();
  }

  private IUserSettingService getSettingsService() {
    if ( settingsService == null ) {
      settingsService = PentahoSystem.get( IUserSettingService.class, pentahoSession );
    }
    return settingsService;
  }

  @NonNull
  private IGenericFileService getGenericFileService() {
    if ( genericFileService == null ) {
      genericFileService = PentahoSystem.get( IGenericFileService.class, pentahoSession );
    }

    return genericFileService;
  }

  @VisibleForTesting
  void setGenericFileService( @Nullable IGenericFileService genericFileService ) {
    this.genericFileService = genericFileService;
  }

  public IPentahoSession getSession() {
    return pentahoSession;
  }

  public void setSession( IPentahoSession session ) {
    this.pentahoSession = Objects.requireNonNull( session );
  }

  public String getJobName() {
    return scheduleRequest.getJobName();
  }

  private String getScheduleOwner() {
    return scheduleOwner;
  }

  /**
   * Gets the job schedule owner using the job parameter `ActionAdapterQuartzJob-ActionUser`.
   *
   * @return A trimmed non-empty string containing the value of the job parameter or null.
   */
  private String getScheduleOwnerFromRequest() {
    String userName = scheduleRequest.getJobParameters().stream()
      .filter( jobParameter -> StringUtils.equals( ( jobParameter ).getName(), IScheduler.RESERVEDMAPKEY_ACTIONUSER ) )
      .findFirst()
      .map( jobParameter -> jobParameter.getValue() != null ? ( (String) jobParameter.getValue() ).trim() : null )
      .orElse( null );

    // Schedule Owner should have already been validated with a fallback to administrator
    // if there is a problem with the schedule owner at this point, it is a bigger issue.
    if ( userName == null || userName.isEmpty() ) {
      throw new IllegalArgumentException( Messages.getString( "EnterpriseSchedulerService.InvalidUserName" ) );
    }

    return userName;
  }

  public String resolveOutputFilePath() throws SchedulerException {

    try {
      return SecurityHelper.getInstance().runAsUser( getScheduleOwner(), this::resolveOutputFilePathCore );
    } catch ( Exception e ) {
      logger.error( e.getMessage(), e );
      throw new SchedulerException( e );
    }
  }

  String resolveOutputFilePathCore() throws SchedulerException {
    String fileNamePattern = "/" + getOutputFileBaseName() + ".*";

    String outputFolderPath = scheduleRequest.getOutputFile();
    if ( outputFolderPath != null && outputFolderPath.endsWith( fileNamePattern ) ) {
      // we are creating a schedule with a completed path already, strip off the pattern and validate the folder is
      // valid
      outputFolderPath = outputFolderPath.substring( 0, outputFolderPath.indexOf( fileNamePattern ) );
    }

    if ( isValidOutputPath( outputFolderPath, false ) ) {
      return outputFolderPath + fileNamePattern; // return if valid
    } else if ( !SchedulerService.isFallbackEnabled() ) {  // If fallback is not enabled, throw an exception
      throw new SchedulerException( Messages.getInstance()
              .getString( "QuartzScheduler.ERROR_0016_UNAVAILABLE_OUTPUT_LOCATION", getScheduleOwner() ) );
    }

    // output path invalid, proceed to fallback
    logger.warn( Messages.getInstance()
      .getString( "QuartzScheduler.ERROR_0011_UNAVAILABLE_OUTPUT_LOCATION_ERROR", outputFolderPath, getJobName(),
        getScheduleOwner() ) );

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
          getLogJobName(),
          getScheduleOwner() ) );

        return fallbackPath + fileNamePattern; // return the first valid path
      }
    }

    // Should not really happen, but if it does...
    logger.error( Messages.getInstance().getString(
      "QuartzScheduler.ERROR_0015_NO_AVAILABLE_OUTPUT_LOCATION_FALLBACK",
      getLogJobName(),
      getScheduleOwner() ) );

    return null;
  }

  protected String getOutputFileBaseName() {
    // Use job name as file name if exists.
    String outputFileBaseName = getJobName();
    if ( StringUtils.isEmpty( outputFileBaseName ) ) {
      outputFileBaseName = RepositoryFilenameUtils.getBaseName( scheduleRequest.getInputFile() );
    }

    return outputFileBaseName;
  }

  protected String getLogJobName() {
    return getOutputFileBaseName();
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
        logger.warn( Messages.getInstance().getString( msgId, outputPath, getLogJobName(), getScheduleOwner() ) );
      }

      return result;
    } catch ( OperationFailedException e ) {
      String msgId = isFallback
        ? "QuartzScheduler.ERROR_0013_UNAVAILABLE_OUTPUT_LOCATION_FALLBACK_ERROR"
        : "QuartzScheduler.ERROR_0011_UNAVAILABLE_OUTPUT_LOCATION_ERROR";
      logger.warn( Messages.getInstance().getString( msgId, outputPath, getLogJobName(), getScheduleOwner() ), e );
      return false;
    }
  }

  protected boolean doesFolderExist( @NonNull String path ) throws OperationFailedException {
    return getGenericFileService().doesFolderExist( path );
  }

  protected boolean isPermitted( String path ) throws OperationFailedException {
    return getGenericFileService().hasAccess( path, EnumSet.copyOf( permissions ) );
  }

  protected String getUserSettingOutputPath() {
    try {
      IUserSetting userSetting = getSettingsService().getUserSetting( DEFAULT_SETTING_KEY, null );
      if ( userSetting != null && StringUtils.isNotBlank( userSetting.getSettingValue() ) ) {
        return userSetting.getSettingValue();
      }
    } catch ( Exception e ) {
      logger.warn( e.getMessage(), e );
    }
    return null;
  }

  protected String getSystemSettingOutputPath() {
    try {
      return PentahoSystem.getSystemSettings().getSystemSetting( DEFAULT_SETTING_KEY, null );
    } catch ( Exception e ) {
      logger.warn( e.getMessage(), e );
    }
    return null;
  }

  protected String getUserHomeDirectoryPath() {
    try {
      return ClientRepositoryPaths.getUserHomeFolderPath( getScheduleOwner() );
    } catch ( Exception e ) {
      logger.warn( e.getMessage(), e );
    }
    return null;
  }
}
