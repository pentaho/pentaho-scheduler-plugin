/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2023 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.platform.web.http.api.resources;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.model.IGenericTree;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;



@Path( "/api/generic-files" )
public class GenericFileResource {

    private final IGenericFileService genericFileService;

    public GenericFileResource( IGenericFileService genericFileService ) {
        this.genericFileService = genericFileService;
    }

    @GET
    @Path( "/tree" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response loadFolderTree( @QueryParam("depth") Integer depth ) {
        IGenericTree tree = genericFileService.loadFoldersOnly( depth );
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
            genericFileService.clearCache();;
            return Response.status(Response.Status.OK).build();
        } catch ( Exception e ) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    @GET
    @Path ( "/validate" )
    @Produces ( MediaType.TEXT_PLAIN )
    @StatusCodes ( {
            @ResponseCode ( code = 200, condition = "Successfully returns a boolean value, either true or false" ) } )
    public String validate( @QueryParam("path") String path ) {
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
