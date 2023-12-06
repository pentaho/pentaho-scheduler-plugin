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
 * Copyright (c) 2023 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.web.http.api.resources;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Objects;

@Path( "/scheduler-plugin/api/generic-files" )
public class GenericFileResource {

  private final IGenericFileService genericFileService;

  public GenericFileResource( @NonNull IGenericFileService genericFileService ) {
    Objects.requireNonNull( genericFileService );
    this.genericFileService = genericFileService;
  }

  @GET
  @Path( "/folders/tree" )
  @Produces( { MediaType.APPLICATION_JSON } )
  public Response loadFolderTree( @QueryParam( "depth" ) Integer depth ) {
    IGenericFileTree tree = genericFileService.getFolders( depth );
    return Response.ok( tree ).build();
  }

  @DELETE
  @Path( "/folders/tree/cache" )
  @StatusCodes( {
    @ResponseCode( code = 204, condition = "Cache was cleared successfully" ),
    @ResponseCode( code = 401, condition = "Authentication required" ),
    @ResponseCode( code = 403, condition = "Access forbidden" ),
    @ResponseCode( code = 500, condition = "Internal Server Error" )
  } )
  public Response clearCache() {
    try {
      genericFileService.clearFolderCache();
      return Response.status( Response.Status.NO_CONTENT ).build();
    } catch ( Exception e ) {
      return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).build();
    }
  }

  @GET
  @Path( "/validate" )
  @Produces( MediaType.TEXT_PLAIN )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully returns a boolean value, either true or false" ) } )
  public String validate( @QueryParam( "path" ) String path ) {
    try {
      return genericFileService.validate( decodePath( path ) ) ? "true" : "false";
    } catch ( Exception e ) {
      return "false";
    }
  }

  @POST
  @Path( "/folders/{path : .+}" )
  @StatusCodes( {
    @ResponseCode( code = 201, condition = "Folder creation succeeded." ),
    @ResponseCode( code = 401, condition = "Authentication required" ),
    @ResponseCode( code = 403, condition = "Access forbidden" ),
    @ResponseCode( code = 409, condition = "Folder creation failed; folder already exists" ),
    @ResponseCode( code = 500, condition = "Folder creation failed" )
  } )
  public Response createFolder( @PathParam( "path" ) String path ) {

    // TODO: actually implement the error codes

    try {
      if ( genericFileService.createFolder( decodePath( path ) ) ) {
        return Response.status( Response.Status.CREATED ).build();
      }

      throw new WebApplicationException( Response.Status.INTERNAL_SERVER_ERROR );
    } catch ( Exception e ) {
      throw new WebApplicationException( e, Response.Status.INTERNAL_SERVER_ERROR );
    }
  }

  public String decodePath( String path ) {
    return path.replace( ":", "/" ).replace( "~", ":" );
  }
}
