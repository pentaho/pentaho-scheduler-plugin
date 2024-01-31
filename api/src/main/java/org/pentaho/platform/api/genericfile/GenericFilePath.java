/*!
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
 * Copyright (c) 2024 Hitachi Vantara. All rights reserved.
 */
package org.pentaho.platform.api.genericfile;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenericFilePath {
  public static final String PATH_SEPARATOR = "/";

  private static final Pattern PATH_WITH_SCHEME_PATTERN = Pattern.compile( "^(\\w+://)(.*)$" );

  private static final Pattern PATH_SEPARATOR_SPLIT_PATTERN = Pattern.compile( "\\s*" + PATH_SEPARATOR + "\\s*" );

  private static final String SCHEME_SUFFIX = "://";

  public static final GenericFilePath NULL = new GenericFilePath( Collections.emptyList() );

  private static final String[] EMPTY_ARRAY = new String[ 0 ];

  @NonNull
  private final String path;

  @NonNull
  private final List<String> segments;

  private GenericFilePath( @NonNull List<String> segments ) {
    this.segments = Collections.unmodifiableList( segments );

    // Rebuild the path to ensure it´s normalized.
    this.path = getRoot() + String.join( PATH_SEPARATOR, getNonRootSegments() );
  }

  public boolean isNull() {
    return segments.isEmpty();
  }

  /**
   * Gets the path's provider root.
   *
   * @return The provider root.
   */
  @NonNull
  public String getRoot() {
    return isNull() ? "" : segments.get( 0 );
  }

  @NonNull
  public List<String> getNonRootSegments() {
    return isNull() ? Collections.emptyList() : segments.subList( 1, segments.size() );
  }

  public boolean hasScheme() {
    return !PATH_SEPARATOR.equals( getRoot() );
  }

  @Nullable
  public String getScheme() {
    String root = getRoot();
    return hasScheme()
      ? root.substring( 0, root.length() - SCHEME_SUFFIX.length() )
      : null;
  }

  @NonNull
  public List<String> getSegments() {
    return segments;
  }

  @Override
  public String toString() {
    return path;
  }

  @Nullable
  public GenericFilePath getParent() {
    if ( isNull() ) {
      return null;
    }

    return new GenericFilePath( segments.subList( 0, segments.size() - 1 ) );
  }

  @NonNull
  public static GenericFilePath parse( @Nullable String path ) throws InvalidPathException {
    if ( path == null ) {
      return NULL;
    }

    String restPath = path.trim();
    if ( restPath.isEmpty() ) {
      return NULL;
    }

    String root;
    if ( restPath.startsWith( PATH_SEPARATOR ) ) {
      root = PATH_SEPARATOR;
      restPath = restPath.substring( PATH_SEPARATOR.length() );
    } else {
      Matcher matcher = PATH_WITH_SCHEME_PATTERN.matcher( restPath );
      if ( !matcher.matches() ) {
        throw new InvalidPathException();
      }

      root = matcher.group( 1 );
      restPath = matcher.group( 2 );
    }

    if ( restPath.endsWith( PATH_SEPARATOR ) ) {
      restPath = restPath.substring( 0, restPath.length() - 1 );
    }

    String[] restSegments = splitPath( restPath );
    List<String> segments = new ArrayList<>( 1 + restSegments.length );
    segments.add( root );
    Collections.addAll( segments, restSegments );

    return new GenericFilePath( segments );
  }

  @NonNull
  private static String[] splitPath( @NonNull String path ) {
    return path.isEmpty()
      ? EMPTY_ARRAY
      : PATH_SEPARATOR_SPLIT_PATTERN.split( path );
  }

  @Override
  public boolean equals( Object other ) {
    if ( this == other ) {
      return true;
    }

    if ( !( other instanceof GenericFilePath ) ) {
      return false;
    }

    GenericFilePath that = (GenericFilePath) other;
    return Objects.equals( path, that.path );
  }

  @Override
  public int hashCode() {
    return Objects.hash( path );
  }

  public boolean contains( @NonNull GenericFilePath other ) {
    Objects.requireNonNull( other );

    List<String> excess = other.relativeSegments( this );

    // May be empty.
    return excess != null;
  }

  @Nullable
  public List<String> relativeSegments( @NonNull GenericFilePath base ) {
    Objects.requireNonNull( base );

    if ( base.isNull() ) {
      return segments;
    }

    int baseCount = base.segments.size();
    int count = segments.size();
    if ( baseCount > count ) {
      return null;
    }

    for ( int i = 0; i < baseCount; i++ ) {
      if ( !base.segments.get( i ).equals( segments.get( i ) ) ) {
        return null;
      }
    }

    return segments.subList( baseCount, count );
  }

  public GenericFilePath child( @NonNull String segment ) {
    Objects.requireNonNull( segment );

    String normalizedSegment = segment.trim();
    if ( normalizedSegment.isEmpty() ) {
      throw new IllegalArgumentException( "Path is empty." );
    }

    List<String> childSegments = new ArrayList<>( segments );
    childSegments.add( normalizedSegment );

    return new GenericFilePath( childSegments );
  }
}
