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

import org.pentaho.platform.web.http.api.resources.utils.FileUtils;

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
   * Windows style path separator.
   */
  protected static final String WINDOWS_PATH_SEPARATOR =  "\\";

  /**
   * Separator for file paths. Assuming unix style separator due to pentaho domain.
   * The same value as {@link FileUtils#PATH_SEPARATOR}.
   */
  protected static final String PATH_SEPARATOR = FileUtils.PATH_SEPARATOR;

  /**
   * Combine <code>basePath</code> and <code>pathToAdd</code> using path separator defined by {@link #PATH_SEPARATOR}.
   * Windows path separator, defined by {@link #WINDOWS_PATH_SEPARATOR}, is not supported.
   * <p/>
   *
   * Examples:
   * <p/>
   * <pre>
   * /foo + bar                       -->   /foo/bar
   * /foo/ + bar                      -->   /foo/bar
   * /foo + /bar                      -->   /foo/bar
   * /foo/ + /bar                     -->   /foo/bar
   * /foo + bar.txt                   -->   /foo/bar.txt
   * /foo/ + bar.txt                  -->   /foo/bar.txt
   * /foo + /bar.txt                  -->   /foo/bar.txt
   * /foo/ + /bar.txt                 -->   /foo/bar.txt
   * </pre>
   * Same permutations for the following:
   * <pre>
   * scheme://foo/ + /bar.txt         -->   scheme://foo/bar.txt
   * /foo/ + some/folder/path/bar.txt --> /foo/some/folder/path/bar.txt
   * </pre>
   *
   * @param basePath
   * @param pathToAdd
   * @return if both arguments are non-null then
   * <p/> combined path consisting of: <code>basePath</code> + {@value #PATH_SEPARATOR} + <code>pathToAdd</code> <p/> , otherwise null.
   * @throws IllegalArgumentException if path contains windows separator defined by {@link #WINDOWS_PATH_SEPARATOR}
   */
  public static String concat( String basePath, String pathToAdd ) {

    if ( basePath == null || pathToAdd == null ) {
      return null;
    }

    checkPaths( basePath, pathToAdd );

    /*
     * NOTE don't alter schema syntax ie remove // in a URI.
     * Don't call any function that eventually calls org.apache.commons.io.FilenameUtils#normalize
     * Similar logic to org.pentaho.platform.web.http.api.resources.SchedulerOutputPathResolver
     */
    return getNoEndSeparator( toSb( basePath ) )
        .append( PATH_SEPARATOR )
        .append( getNoEndSeparator( toSb( pathToAdd ).reverse() ).reverse() ) // remove separator at the start of path
        .toString();
  }

  /**
   * Return only the path without the ending separator defined by {@link #PATH_SEPARATOR}
   * @param path
   * @return <code>path</code> without the end separator
   */
  protected static StringBuilder getNoEndSeparator( StringBuilder path ) {
    if ( path == null ) {
      return null;
    }

    return ( indexOfLastSeparator( path ) != -1 && path.length() - 1 == indexOfLastSeparator( path )  )
        ? path.deleteCharAt( path.length() - 1 ) // remove end separator
        : path;
  }

  /**
   * Return index of last separator defined by {@link #PATH_SEPARATOR}
   * @param fileName
   * @return 0th index of {@link #PATH_SEPARATOR}, -1 otherwise.
   */
  protected static int indexOfLastSeparator( StringBuilder fileName ) {
    if ( fileName == null ) {
      return -1;
    } else {
      return fileName.lastIndexOf( PATH_SEPARATOR );
      /*
       * NOTE: if we need to handle backward slash ie windows path separator
       * see {@link org.apache.commons.io.FilenameUtils#indexOfLastSeparator}
       */
    }
  }

  /**
   * Determines if all <code>paths</code> are valid.
   * @param paths
   * @throws IllegalArgumentException if a single path contains windows separator defined
   * by {@link #WINDOWS_PATH_SEPARATOR}
   */
  protected static void checkPaths( String... paths ) {
    for ( String path : paths ) {
      checkPath( path );
    }
  }

  /**
   * Determines if <code>path</code> is valid.
   * @param path
   * @throws IllegalArgumentException if path contains windows separator defined by {@link #WINDOWS_PATH_SEPARATOR}
   */
  protected static void checkPath( String path ) {
    if ( path.contains( WINDOWS_PATH_SEPARATOR ) ) {
      throw new IllegalArgumentException(
        "path with windows separator '" + WINDOWS_PATH_SEPARATOR + "' is not supported" );
    }
  }

  /**
   * Wrapper around instantiation of {@link StringBuilder}
   * @param str
   * @return
   */
  private static StringBuilder toSb( String str ) {
    return new StringBuilder( str );
  }
}
