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


package org.pentaho.platform.scheduler2.quartz.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.pentaho.platform.api.engine.IUserRoleListService;
import org.pentaho.platform.api.mt.ITenant;

@SuppressWarnings( { "nls", "unchecked" } )
public class StubUserRoleListService implements IUserRoleListService {

  public List getAllRoles() {
    // TODO Auto-generated method stub
    return null;
  }

  public List getAllUsers() {
    // TODO Auto-generated method stub
    return null;
  }

  public List getUsersInRole( String role ) {
    // TODO Auto-generated method stub
    return null;
  }

  public List getRolesForUser( String userName ) {
    return Arrays.asList( "FL_GATOR", "FS_SEMINOLE" );
  }

  @Override
  public List<String> getAllRoles( ITenant tenant ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getAllUsers( ITenant tenant ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getUsersInRole( ITenant tenant, String role ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getRolesForUser( ITenant tenant, String username ) {
    List roles = new ArrayList<String>();
    roles.add( "Admin" );
    roles.add( "Authenticated" );
    return roles;
  }

  @Override
  public List<String> getSystemRoles() {
    // TODO Auto-generated method stub
    return null;
  }

}
