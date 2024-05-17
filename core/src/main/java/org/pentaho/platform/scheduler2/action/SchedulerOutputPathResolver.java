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
 * Copyright (c) 2002-2024 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.scheduler2.action;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.genericfile.GenericFilePermission;
import org.pentaho.platform.api.genericfile.IGenericFileService;
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Rowell Belen
 */
public class SchedulerOutputPathResolver implements ISchedulerOutputPathResolver {

  final String DEFAULT_SETTING_KEY = "default-scheduler-output-path";
  public static final String SCHEDULER_ACTION_NAME = "org.pentaho.scheduler.manage";

  private static final Log logger = LogFactory.getLog( SchedulerOutputPathResolver.class );
  private static final List<GenericFilePermission> permissions = new ArrayList<GenericFilePermission>();

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

    final String fileNamePattern = getFilename();
    final String outputFilePath = getDirectory();

    boolean scheduleAllowed = isScheduleAllowed();
    if ( !scheduleAllowed ) {
      throw new SchedulerException( Messages.getInstance().getString(
        "QuartzScheduler.ERROR_0009_SCHEDULING_IS_NOT_ALLOWED_AFTER_CHANGE", getJobName(), this.actionUser ) );
    }

    // Enclose validation logic in the context of the job creator's session, not the current session
    final Callable<String> callable = new Callable<String>() {
      @Override
      public String call() throws Exception {

        if ( StringUtils.isNotBlank( outputFilePath ) && isValidOutputPath( outputFilePath )
            && isPermitted( outputFilePath ) ) {
          return concat( outputFilePath, fileNamePattern ); // return if valid
        }

        // evaluate fallback output paths
        String[] fallBackPaths = new String[] { getUserSettingOutputPath(), // user setting
          getSystemSettingOutputPath(), // system setting
          getUserHomeDirectoryPath() // home directory
        };

        for ( String path : fallBackPaths ) {
          if ( StringUtils.isNotBlank( path ) && isValidOutputPath( path ) ) {
            return concat( path, fileNamePattern ); // return the first valid path
          }
        }

        return null; // it should never reach here because the user directory is the ultimate fallback
      }
    };

    return runAsUser( callable );
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

  protected boolean isValidOutputPath( @NonNull String path ) {
    try {
      return getGenericFileService().doesFolderExist( path );
    } catch ( Exception e ) {
      logger.warn( e.getMessage(), e );
    }
    return false;
  }

  /**
   * Extracts job name from {@link #getFilename()}, by removing some of the path text.
   * @return
   */
  protected String getJobName() {
    return StringUtils.isNotBlank( getFilename() )
      ? FilenameUtils.getPathNoEndSeparator( getFilename() ) // remove "/" + <jobName> + ".*" file pattern text
      : "<?>";
  }

  private String getUserSettingOutputPath() {
    try {
      IUserSetting userSetting = getUserSettingService().getUserSetting( DEFAULT_SETTING_KEY, null );
      if ( userSetting != null && StringUtils.isNotBlank( userSetting.getSettingValue() ) ) {
        return userSetting.getSettingValue();
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

  protected boolean isPermitted( final String path ) {
    try {
      return getGenericFileService().hasAccess( path, EnumSet.copyOf( permissions ) );
    } catch ( Exception e ) {
      logger.warn( e.getMessage(), e );
    }
    return false;
  }
}
