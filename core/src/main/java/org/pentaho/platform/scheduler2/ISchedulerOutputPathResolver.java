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

