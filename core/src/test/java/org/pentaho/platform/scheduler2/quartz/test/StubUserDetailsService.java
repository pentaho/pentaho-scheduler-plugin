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

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Arrays;

public class StubUserDetailsService implements UserDetailsService {

  @Override
  public UserDetails loadUserByUsername( String username ) throws UsernameNotFoundException, DataAccessException {
    GrantedAuthority[] auths = new GrantedAuthority[2];
    auths[0] = new SimpleGrantedAuthority( "Authenticated" );
    auths[1] = new SimpleGrantedAuthority( "Administrator" );

    UserDetails user = new User( "admin", "password", true, true, true, true, Arrays.asList( auths ) );

    return user;
  }

}
