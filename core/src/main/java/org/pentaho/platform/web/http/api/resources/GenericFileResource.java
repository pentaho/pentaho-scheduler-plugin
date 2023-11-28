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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
  @Path( "/folderTree" )
  @Produces( { MediaType.APPLICATION_JSON } )
  public Response loadFolderTree( @QueryParam( "depth" ) Integer depth ) {
    IGenericFileTree tree = genericFileService.getFolders( depth );
    return Response.ok( tree ).build();
  }

  @GET
  @Path( "/clearCache" )
  @Produces ( MediaType.APPLICATION_JSON )
  @StatusCodes( {
    @ResponseCode( code = 200, condition = "Successfully returns success" ),
    @ResponseCode( code = 500, condition = "Internal Server Error" )
  } )
  public Response clearCache(  ) {
    try {
      genericFileService.clearCache();
      return Response.status( Response.Status.OK ).build();
    } catch ( Exception e ) {
      return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).build();
    }
  }

  @GET
  @Path ( "/validate" )
  @Produces ( MediaType.TEXT_PLAIN )
  @StatusCodes ( {
    @ResponseCode ( code = 200, condition = "Successfully returns a boolean value, either true or false" ) } )
  public String validate( @QueryParam( "path" ) String path ) {
    try {
      return genericFileService.validate( path ) ? "true" : "false";
    } catch ( Exception e ) {
      return "false";
    }
  }

  @PUT
  @Path ( "/create" )
  @Consumes ( { MediaType.WILDCARD } )
  @Produces ( MediaType.TEXT_PLAIN )
  @StatusCodes ( {
    @ResponseCode ( code = 200, condition = "Successfully returns a boolean value, either true or false" ) } )
  public String create( String path ) {
    try {
      return genericFileService.add( path ) ? "true" : "false";
    } catch ( Exception e ) {
      return "false";
    }
  }
}
