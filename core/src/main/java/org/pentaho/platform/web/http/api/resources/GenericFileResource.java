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
 * Copyright (c) 2023 - 2024 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.web.http.api.resources;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.pentaho.platform.api.genericfile.GenericFilePath;
import org.pentaho.platform.api.genericfile.GetTreeOptions;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.AccessControlException;
import org.pentaho.platform.api.genericfile.exception.InvalidOperationException;
import org.pentaho.platform.api.genericfile.exception.InvalidPathException;
import org.pentaho.platform.api.genericfile.exception.NotFoundException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFileContentWrapper;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;
import org.pentaho.platform.web.servlet.HttpMimeTypeListener;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import static org.apache.commons.io.IOUtils.copy;

@Path( "/scheduler-plugin/api/generic-files" )
public class GenericFileResource {

  private final IGenericFileService genericFileService;

  public GenericFileResource( @NonNull IGenericFileService genericFileService ) {
    Objects.requireNonNull( genericFileService );
    this.genericFileService = genericFileService;
  }

  @GET
  @Path( "/tree" )
  @Produces( { MediaType.APPLICATION_JSON } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Operation successful" ),
    @ResponseCode( code = 400, condition = "Filter is invalid" ),
    @ResponseCode( code = 401, condition = "Authentication required" ),
    @ResponseCode( code = 403, condition = "Access forbidden to operation" ),
    @ResponseCode( code = 500, condition = "Operation failed" )
  } )
  public IGenericFileTree getFileTree( @QueryParam( "depth" ) Integer maxDepth,
                                       @QueryParam( "expandedPath" ) String expandedPath,
                                       @QueryParam( "filter" ) String filterString,
                                       @QueryParam( "showHidden" ) boolean showHiddenFiles ) {
    try {
      return getTreeWithOptions( null, maxDepth, expandedPath, filterString, showHiddenFiles );
    } catch ( IllegalArgumentException e ) {
      throw new WebApplicationException( e, Response.Status.BAD_REQUEST );
    } catch ( AccessControlException e ) {
      throw new WebApplicationException( e, Response.Status.FORBIDDEN );
    } catch ( OperationFailedException e ) {
      throw new WebApplicationException( e, Response.Status.INTERNAL_SERVER_ERROR );
    }
  }

  @GET
  @Path( "/{path : .+}/tree" )
  @Produces( { MediaType.APPLICATION_JSON } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Operation successful" ),
    @ResponseCode( code = 400, condition = "Base path, expanded path, and/or filter are invalid" ),
    @ResponseCode( code = 401, condition = "Authentication required" ),
    @ResponseCode( code = 403, condition = "Access forbidden to operation" ),
    @ResponseCode(
      code = 404,
      condition = "Base path does not exist, is not a folder or user has no read access to it" ),
    @ResponseCode( code = 500, condition = "Operation failed" )
  } )
  public IGenericFileTree getFileSubtree( @NonNull @PathParam( "path" ) String basePath,
                                          @QueryParam( "depth" ) Integer maxDepth,
                                          @QueryParam( "expandedPath" ) String expandedPath,
                                          @QueryParam( "filter" ) String filterString,
                                          @QueryParam( "showHidden" ) boolean showHiddenFiles ) {
    try {
      return getTreeWithOptions( basePath, maxDepth, expandedPath, filterString, showHiddenFiles );
    } catch ( AccessControlException e ) {
      throw new WebApplicationException( e, Response.Status.FORBIDDEN );
    } catch ( InvalidPathException | IllegalArgumentException e ) {
      throw new WebApplicationException( e, Response.Status.BAD_REQUEST );
    } catch ( NotFoundException e ) {
      throw new WebApplicationException( e, Response.Status.NOT_FOUND );
    } catch ( OperationFailedException e ) {
      throw new WebApplicationException( e, Response.Status.INTERNAL_SERVER_ERROR );
    }
  }

  private IGenericFileTree getTreeWithOptions( @Nullable String basePath,
                                               @NonNull Integer maxDepth,
                                               @NonNull String expandedPath,
                                               @NonNull String filterString,
                                               @Nullable boolean showHiddenFiles )
    throws OperationFailedException, IllegalArgumentException {
    GetTreeOptions options = new GetTreeOptions();

    if ( basePath != null ) {
      options.setBasePath( decodePath( basePath ) );
    }
    options.setMaxDepth( maxDepth );
    options.setFilter( filterString );
    options.setShowHiddenFiles( showHiddenFiles );

    // Path in query parameter is not specially encoded.
    options.setExpandedPath( expandedPath );

    return genericFileService.getTree( options );
  }

  @DELETE
  @Path( "/tree/cache" )
  @StatusCodes( {
    @ResponseCode( code = 204, condition = "Cache was cleared successfully" ),
    @ResponseCode( code = 401, condition = "Authentication required" ),
    @ResponseCode( code = 403, condition = "Access forbidden" ),
    @ResponseCode( code = 500, condition = "Operation failed" )
  } )
  public void clearCache() {
    try {
      genericFileService.clearTreeCache();
    } catch ( AccessControlException e ) {
      throw new WebApplicationException( e, Response.Status.FORBIDDEN );
    } catch ( OperationFailedException e ) {
      throw new WebApplicationException( e, Response.Status.INTERNAL_SERVER_ERROR );
    }
  }

  @HEAD
  @Path( "/folders/{path : .+}" )
  @StatusCodes( {
    @ResponseCode( code = 204, condition = "Folder exists" ),
    @ResponseCode( code = 400, condition = "Folder path is invalid" ),
    @ResponseCode( code = 401, condition = "Authentication required" ),
    @ResponseCode( code = 403, condition = "Access forbidden to operation" ),
    @ResponseCode( code = 404, condition = "File does not exist, is not a folder or user has no read access to it" ),
    @ResponseCode( code = 500, condition = "Operation failed" ) } )
  public void doesFolderExist( @NonNull @PathParam( "path" ) String path ) {
    try {
      if ( !genericFileService.doesFolderExist( decodePath( path ) ) ) {
        throw new WebApplicationException( Response.Status.NOT_FOUND );
      }
    } catch ( AccessControlException e ) {
      throw new WebApplicationException( e, Response.Status.FORBIDDEN );
    } catch ( InvalidPathException e ) {
      throw new WebApplicationException( e, Response.Status.BAD_REQUEST );
    } catch ( OperationFailedException e ) {
      throw new WebApplicationException( e, Response.Status.INTERNAL_SERVER_ERROR );
    }
  }

  @POST
  @Path( "/folders/{path : .+}" )
  @StatusCodes( {
    @ResponseCode( code = 201, condition = "Folder creation succeeded" ),
    @ResponseCode( code = 400, condition = "Folder path is invalid" ),
    @ResponseCode( code = 401, condition = "Authentication required" ),
    @ResponseCode( code = 403, condition = "Access forbidden" ),
    @ResponseCode( code = 409, condition = "Folder already exists" ),
    @ResponseCode( code = 500, condition = "Folder creation failed" )
  } )
  public Response createFolder( @NonNull @PathParam( "path" ) String path ) {

    try {
      if ( !genericFileService.createFolder( decodePath( path ) ) ) {
        throw new WebApplicationException( Response.Status.CONFLICT );
      }

      return Response.status( Response.Status.CREATED ).build();

    } catch ( InvalidPathException | InvalidOperationException e ) {
      throw new WebApplicationException( e, Response.Status.BAD_REQUEST );
    } catch ( AccessControlException e ) {
      throw new WebApplicationException( e, Response.Status.FORBIDDEN );
    } catch ( OperationFailedException e ) {
      throw new WebApplicationException( e, Response.Status.INTERNAL_SERVER_ERROR );
    }
  }

  @GET
  @Path( "{path : .+}/content" )
  @Produces( { MediaType.APPLICATION_JSON } )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Operation successful" ),
    @ResponseCode( code = 400, condition = "File path is invalid, or is a folder" ),
    @ResponseCode( code = 401, condition = "Authentication required" ),
    @ResponseCode( code = 403, condition = "Access forbidden" ),
    @ResponseCode( code = 404, condition = "File does not exist or user has no read access to it" ),
    @ResponseCode( code = 500, condition = "Operation failed" )
  } )
  public Response getFileContent( @NonNull @PathParam( "path" ) String filePath ) {
    try {
      GenericFilePath genericFilePath = GenericFilePath.parseRequired( decodePath( filePath ) );
      IGenericFileContentWrapper contentWrapper = genericFileService.getFileContentWrapper( genericFilePath );

      return buildOkResponse( getStreamingOutput( contentWrapper.getInputStream() ), contentWrapper.getFileName(),
        contentWrapper.getMimeType() );

    } catch ( NotFoundException e ) {
      throw new WebApplicationException( e, Response.Status.NOT_FOUND );
    } catch ( InvalidPathException | InvalidOperationException e ) {
      throw new WebApplicationException( e, Response.Status.BAD_REQUEST );
    } catch ( AccessControlException e ) {
      throw new WebApplicationException( e, Response.Status.FORBIDDEN );
    } catch ( OperationFailedException e ) {
      throw new WebApplicationException( e, Response.Status.INTERNAL_SERVER_ERROR );
    }
  }

  @NonNull
  public String decodePath( @NonNull String path ) {
    return path.replace( ":", "/" )
      .replace( "~", ":" )
      .replace( "\t", "~" );
  }

  protected Response buildOkResponse( StreamingOutput streamingOutput, String fileName, String mimeType ) {
    Response.ResponseBuilder builder;

    MediaType mediaType;
    try {
      mediaType = MediaType.valueOf( mimeType );
    } catch ( IllegalArgumentException e ) {
      //Downloadable type
      mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
    }

    builder = Response.ok( streamingOutput, mediaType );

    return builder.header( "Content-Disposition", HttpMimeTypeListener.buildContentDispositionValue( fileName, false ) )
      .build();
  }

  public StreamingOutput getStreamingOutput( final InputStream is ) {
    return new StreamingOutput() {
      @Override
      public void write( OutputStream output ) throws IOException {
        copy( is, output );
      }
    };
  }
}
