/*!
 * HITACHI VANTARA PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2023 Hitachi Vantara. All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Hitachi Vantara and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Hitachi Vantara and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Hitachi Vantara is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Hitachi Vantara,
 * explicitly covering such access.
 */

package org.pentaho.platform.scheduler2;

import org.pentaho.platform.api.scheduler2.SchedulerException;

/**
 * Determines if a filename and directory path can be used as an output.
 */
public interface ISchedulerOutputPathResolver {

  String getFilename();

  void setFileName( String fileName );

  String getDirectory();

  void setDirectory( String directory );

  /**
   * Executes {@link #resolveOutputFilePath()} with the given user.
   * <p/>
   * See {@link org.pentaho.platform.engine.security.SecurityHelper#runAsUser(String, java.util.concurrent.Callable)} for more information.
   * @return
   */
  String getActionUser();

  /**
   * Executes {@link #resolveOutputFilePath()} with set user.
   * <p/>
   * See {@link org.pentaho.platform.engine.security.SecurityHelper#runAsUser(String, java.util.concurrent.Callable)} for more information.
   * @param actionUser
   */
  void setActionUser( String actionUser );

  /**
   * Will translate filename and output directory path to the absolute file path.
   * The implementation should consider if the output path directory folder is "valid", folder permission
   * and other considerations.
   * Will try the combined path of the two function: {@link #getDirectory()} + {@link #getFilename()}
   *
   * @return full path including file system scheme if necessary.
   * <P />
   * Some valid examples, but not an exhaustive list:
   * <ul>
   *   <li>/home/.../file.csv</li>
   *   <li>file://.../someFile.pdf</li>
   *   <li>ftp://.../AnotherFile.txt</li>
   *   <li>http://.../oneMoreFile.zip</li>
   * </ul>
   */
  public String resolveOutputFilePath() throws SchedulerException;

}

