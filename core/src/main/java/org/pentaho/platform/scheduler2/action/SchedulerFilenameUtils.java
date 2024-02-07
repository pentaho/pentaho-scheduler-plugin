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
 * Copyright (c) 2024 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.scheduler2.action;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

/**
 * General filename and filepath manipulation utilities. Primarily to support Pentaho Repository and URI file paths.
 * Similar logic to {@link org.pentaho.platform.repository.RepositoryFilenameUtils}
 * and {@link org.apache.commons.io.FilenameUtils }
 *
 */
public class SchedulerFilenameUtils {

  /**
   * Hiding default constructor this is a utility class.
   */
  private SchedulerFilenameUtils() {
    // EMPTY ON PURPOSE
  }

  /**
   * Separator for file paths. Assuming unix style separator due pentaho domain.
   */
  protected static final String PATH_SEPARATOR = "/";

  /**
   * Combine <code>basePath</code> and <code>pathToAdd</code> using path separator defined by {@link #PATH_SEPARATOR}.
   * <p/>
   *
   * Examples:
   * <p/>
   * <pre>
   * /foo + bar                  -->   /foo/bar
   * /foo/ + bar                 -->   /foo/bar
   * /foo + /bar                 -->   /foo/bar
   * /foo/ + /bar                -->   /foo/bar
   * /foo + bar.txt              -->   /foo/bar.txt
   * /foo/ + bar.txt             -->   /foo/bar.txt
   * /foo + /bar.txt             -->   /foo/bar.txt
   * /foo/ + /bar.txt            -->   /foo/bar.txt
   * scheme://foo + bar          -->   scheme://foo/bar
   * scheme://foo/ + bar         -->   scheme://foo/bar
   * scheme://foo + /bar         -->   scheme://foo/bar
   * scheme://foo/ + /bar        -->   scheme://foo/bar
   * scheme://foo + bar.txt      -->   scheme://foo/bar.txt
   * scheme://foo/ + bar.txt     -->   scheme://foo/bar.txt
   * scheme://foo + /bar.txt     -->   scheme://foo/bar.txt
   * scheme://foo/ + /bar.txt    -->   scheme://foo/bar.txt
   * </pre>
   *
   * @param basePath
   * @param pathToAdd
   * @return combined path consisting of: <code>basePath</code> + {@value #PATH_SEPARATOR} + <code>pathToAdd</code>
   */
  public static String concat( String basePath, String pathToAdd ) {

    if ( basePath == null || pathToAdd == null ) {
      return null;
    }
    /*
     * NOTE don't alter schema syntax ie remove // in a URI.
     * Don't call any function that eventually calls org.apache.commons.io.FilenameUtils#normalize
     * Similar logic to org.pentaho.platform.web.http.api.resources.SchedulerOutputPathResolver
     */
    return getNoEndSeparator( basePath )  + PATH_SEPARATOR + FilenameUtils.getName( pathToAdd );
  }

  /**
   * Return only the path without the ending separator defined by {@link #PATH_SEPARATOR}
   * @param path
   * @return <code>path</code> without the end separator
   */
  protected static String getNoEndSeparator( String path ) {
    return  ( StringUtils.isNotBlank( path )
        && path.length() - 1 == FilenameUtils.indexOfLastSeparator( path ) )
      ? path.substring( 0, path.length() - 1 ) // remove end separator
      : path;
  }
}
