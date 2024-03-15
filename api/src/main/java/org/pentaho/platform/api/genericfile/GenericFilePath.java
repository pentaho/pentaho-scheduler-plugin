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

/**
 * This class represents a <i>Generic File</i> path.
 * <p>
 * Generic file path instances are immutable.
 * <p>
 * To create a generic file path instance from a path string, call one of {@link #parse(String)} or
 * {@link #parseRequired(String)}.
 */
public class GenericFilePath {
  /**
   * The path separator character, {@code /}.
   */
  public static final String PATH_SEPARATOR = "/";

  private static final Pattern PATH_WITH_SCHEME_PATTERN = Pattern.compile( "^(\\w+://)(.*)$" );

  private static final Pattern PATH_SEPARATOR_SPLIT_PATTERN = Pattern.compile( "\\s*" + PATH_SEPARATOR + "\\s*" );

  private static final String SCHEME_SUFFIX = "://";

  private static final String[] EMPTY_ARRAY = new String[ 0 ];

  @NonNull
  private final String path;

  @NonNull
  private final List<String> segments;

  private GenericFilePath( @NonNull List<String> segments ) {
    assert !segments.isEmpty();

    this.segments = Collections.unmodifiableList( segments );

    // Rebuild the path to ensure it´s normalized.
    this.path = getFirstSegment() + String.join( PATH_SEPARATOR, getRestSegments() );
  }

  /**
   * Gets the path's first segment, also called the <i>provider root</i> segment.
   * <p>
   * The first segment is that which identifies the path's provider.
   * <p>
   * The first segment of the special <i>Repository</i> provider is the {@link #PATH_SEPARATOR}.
   * While, other providers have a first segment composed of a scheme/protocol, followed by the {@code ://} suffix.
   * <p>
   * Use {@link #hasScheme()} to determine if a provider has a scheme, and {@link #getScheme()} to extract it from
   * the first segment.
   *
   * @return The first segment.
   */
  @NonNull
  public String getFirstSegment() {
    return segments.get( 0 );
  }

  /**
   * Gets the path's <i>rest</i> segments.
   * <p>
   * The rest segments are simply the ones following the {@link #getFirstSegment() first segment}.
   *
   * @return The list of rest segments, possibly empty.
   */
  @NonNull
  public List<String> getRestSegments() {
    return segments.subList( 1, segments.size() );
  }

  /**
   * Gets a value that indicates if the path has a scheme.
   * <p>
   * For more information, see {@link #getFirstSegment()}.
   *
   * @return {@code true}, if the path has a scheme; {@code false}, otherwise.
   * @see #getScheme()
   */
  public boolean hasScheme() {
    return !PATH_SEPARATOR.equals( getFirstSegment() );
  }

  /**
   * Gets the path's scheme.
   * <p>
   * For more information, see {@link #getFirstSegment()}.
   *
   * @return The path's scheme if it has one; {@code null}, if not.
   * @see #hasScheme()
   */
  @Nullable
  public String getScheme() {
    String providerRoot = getFirstSegment();
    return hasScheme()
      ? providerRoot.substring( 0, providerRoot.length() - SCHEME_SUFFIX.length() )
      : null;
  }

  /**
   * Gets the path's segments.
   *
   * @return An immutable list of path segments.
   * @see #getFirstSegment()
   * @see #getRestSegments()
   */
  @NonNull
  public List<String> getSegments() {
    return segments;
  }

  /**
   * Gets the path's normalized string representation.
   *
   * @return The string representation.
   */
  @Override
  public String toString() {
    return path;
  }

  /**
   * Gets the parent generic path instance.
   * <p>
   * The parent path of <i>provider root paths</i> is {@code null}.
   *
   * @return The parent generic path instance, if any; {@link null}, if none.
   */
  @Nullable
  public GenericFilePath getParent() {
    if ( segments.size() == 1 ) {
      return null;
    }

    return new GenericFilePath( segments.subList( 0, segments.size() - 1 ) );
  }

  /**
   * Parses path given its string representation, while ensuring a path can be returned.
   * <p>
   * If a {@code null} or blank string is specified, then an exception is thrown.
   * <p>
   * Otherwise, this method delegates to {@link #parse(String)}.
   *
   * @param path The path string to parse.
   * @return The generic path instance.
   * @throws InvalidPathException If the path is {@code null}, blank, or otherwise invalid. Specifically, if the path's
   *                              root segment is not either a {@link #PATH_SEPARATOR} or a scheme followed by the
   *                              {@code ://} suffix.
   */
  @NonNull
  public static GenericFilePath parseRequired( @Nullable String path ) throws InvalidPathException {
    GenericFilePath genericPath = parse( path );
    if ( genericPath == null ) {
      throw new InvalidPathException( "Path is empty." );
    }

    return genericPath;
  }

  /**
   * Parses a given string representation.
   * <p>
   * If a {@code null} or blank string is specified, then {@code null} is returned.
   * <p>
   * The path is otherwise parsed and normalized.
   * The first segment is identified, and other segments are space-trimmed and removed if empty.
   * <p>
   * Segments equal to {@code .} or {@code ..} are not currently being validated or normalized.
   * <p>
   * If the path ends with a {@link #PATH_SEPARATOR} character, it is ignored.
   *
   * @param path The path string to parse.
   * @return The generic path instance, or {@code null}.
   * @throws InvalidPathException If the path is invalid. Specifically, if the path's root segment is not either
   *                              a {@link #PATH_SEPARATOR} or a scheme followed by the {@code ://} suffix.
   */
  @Nullable
  public static GenericFilePath parse( @Nullable String path ) throws InvalidPathException {
    if ( path == null ) {
      return null;
    }

    String restPath = path.trim();
    if ( restPath.isEmpty() ) {
      return null;
    }

    String root;
    if ( restPath.startsWith( PATH_SEPARATOR ) ) {
      root = PATH_SEPARATOR;
      restPath = restPath.substring( PATH_SEPARATOR.length() );
      // `restPath` may have spaces at left.
    } else {
      Matcher matcher = PATH_WITH_SCHEME_PATTERN.matcher( restPath );
      if ( !matcher.matches() ) {
        throw new InvalidPathException();
      }

      root = matcher.group( 1 );
      restPath = matcher.group( 2 );
      // `restPath` may have spaces at left.
    }

    if ( restPath.endsWith( PATH_SEPARATOR ) ) {
      restPath = restPath.substring( 0, restPath.length() - 1 );
      // `restPath` may have spaces at right.
    }

    String[] restSegments = splitPath( restPath );

    List<String> segments = new ArrayList<>( 1 + restSegments.length );
    segments.add( root );
    for ( String segment : restSegments ) {
      if ( !segment.isEmpty() ) {
        segments.add( segment );
      }
    }

    return new GenericFilePath( segments );
  }

  @NonNull
  private static String[] splitPath( @NonNull String path ) {
    String pathTrimmed = path.trim();
    return pathTrimmed.isEmpty()
      ? EMPTY_ARRAY
      : PATH_SEPARATOR_SPLIT_PATTERN.split( pathTrimmed );
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

  /**
   * Checks if this path equals, or is an ancestor of, another one.
   *
   * @param other The path to check against.
   * @return {@code true}, if the given path is contained in this one; {@code false}, otherwise.
   */
  public boolean contains( @NonNull GenericFilePath other ) {
    Objects.requireNonNull( other );

    List<String> excess = other.relativeSegments( this );

    // May be empty.
    return excess != null;
  }

  /**
   * Gets the segments of this path relative to a given base path.
   * <p>
   * When {@code base} is {@code null}, then all segments of this path are returned.
   * <p>
   * When this path is not contained in the given base path, {@code null} is returned.
   * Otherwise, a list is returned with the segments of this path not contained in the given base path.
   *
   * @param base The base path.
   * @return A possibly empty segment list, if this path is contained in the given base path; {@code null}, otherwise.
   */
  @Nullable
  public List<String> relativeSegments( @Nullable GenericFilePath base ) {
    if ( base == null ) {
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

  /**
   * Builds a child path of this one, given the child segment.
   *
   * @param segment The child segment.
   * @return The child generic path instance.
   * @throws IllegalArgumentException If the given segment is empty, after normalization.
   */
  @NonNull
  public GenericFilePath child( @NonNull String segment ) throws InvalidPathException {
    Objects.requireNonNull( segment );

    String normalizedSegment = segment.trim();
    if ( normalizedSegment.isEmpty() ) {
      throw new IllegalArgumentException( "Path is empty." );
    }

    if ( normalizedSegment.contains( PATH_SEPARATOR ) ) {
      throw new InvalidPathException();
    }

    List<String> childSegments = new ArrayList<>( segments );
    childSegments.add( normalizedSegment );

    return new GenericFilePath( childSegments );
  }
}
