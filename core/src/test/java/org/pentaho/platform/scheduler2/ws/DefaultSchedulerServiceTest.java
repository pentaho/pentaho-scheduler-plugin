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
 * Copyright (c) 2002-2024 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.scheduler2.ws;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.scheduler2.IJobFilter;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.security.policy.rolebased.actions.SchedulerExecuteAction;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultSchedulerServiceTest {
  private ArgumentCaptor<IJobFilter> getJobsFilterCaptor;
  private DefaultSchedulerService defaultSchedulerService;

  @BeforeEach
  void setUp() {
    defaultSchedulerService = Mockito.spy( new DefaultSchedulerService() );
    getJobsFilterCaptor = ArgumentCaptor.forClass( IJobFilter.class );
  }

  @AfterEach
  void cleanup() {
    defaultSchedulerService = null;
    getJobsFilterCaptor = null;
  }

  @Test
  void testGetJobsNonAdminUser() throws Exception {
    IPentahoSession sessionMock = mock( IPentahoSession.class );
    doReturn( sessionMock ).when( defaultSchedulerService ).getPentahoSession();

    IAuthorizationPolicy policyMock = mock( IAuthorizationPolicy.class );
    doReturn( policyMock ).when( defaultSchedulerService ).getAuthorizationPolicy();

    IScheduler iSchedulerMock = mock( IScheduler.class );
    doReturn( iSchedulerMock ).when( defaultSchedulerService ).getScheduler2();

    doReturn( "testUser1" ).when( sessionMock ).getName();
    when( policyMock.isAllowed( anyString() ) ).thenReturn( false );

    defaultSchedulerService.getJobs();
    verify( iSchedulerMock ).getJobs( getJobsFilterCaptor.capture() );
    IJobFilter filter = getJobsFilterCaptor.getValue();
    assertNotNull( filter );

    List<Job> testJobs = getJobs();
    List<Job> filteredJobs = new ArrayList<>();
    for ( Job job : testJobs ) {
      if ( filter.accept( job ) ) {
        filteredJobs.add( job );
      }
    }
    assertEquals( 1, filteredJobs.size() );
    assertEquals( "testJobName1", filteredJobs.get( 0 ).getJobName() );
    assertEquals( "testUser1", filteredJobs.get( 0 ).getUserName() );
  }

  @Test
  void testGetJobsAdminUser() throws Exception {
    IPentahoSession sessionMock = mock( IPentahoSession.class );
    doReturn( sessionMock ).when( defaultSchedulerService ).getPentahoSession();

    IAuthorizationPolicy policyMock = mock( IAuthorizationPolicy.class );
    doReturn( policyMock ).when( defaultSchedulerService ).getAuthorizationPolicy();

    IScheduler iSchedulerMock = mock( IScheduler.class );
    doReturn( iSchedulerMock ).when( defaultSchedulerService ).getScheduler2();

    doReturn( "admin" ).when( sessionMock ).getName();
    doReturn( true ).when( policyMock ).isAllowed( "org.pentaho.security.administerSecurity" );
    doReturn( false ).when( policyMock ).isAllowed( SchedulerExecuteAction.NAME );

    defaultSchedulerService.getJobs();
    verify( iSchedulerMock ).getJobs( getJobsFilterCaptor.capture() );
    IJobFilter filter = getJobsFilterCaptor.getValue();
    assertNotNull( filter );

    List<Job> testJobs = getJobs();
    List<Job> filteredJobs = new ArrayList<>();
    for ( Job job : testJobs ) {
      if ( filter.accept( job ) ) {
        filteredJobs.add( job );
        assertNotEquals( "BlockoutAction", job.getJobName() );
      }
    }
    assertEquals( 10, filteredJobs.size() );

  }

  @Test
  void testGetJobsExecuteSchedulePermission() throws SchedulerException {
    IPentahoSession sessionMock = mock( IPentahoSession.class );
    doReturn( sessionMock ).when( defaultSchedulerService ).getPentahoSession();

    IAuthorizationPolicy policyMock = mock( IAuthorizationPolicy.class );
    doReturn( policyMock ).when( defaultSchedulerService ).getAuthorizationPolicy();

    IScheduler iSchedulerMock = mock( IScheduler.class );
    doReturn( iSchedulerMock ).when( defaultSchedulerService ).getScheduler2();

    doReturn( "bob" ).when( sessionMock ).getName();
    doReturn( false ).when( policyMock ).isAllowed( "org.pentaho.security.administerSecurity" );
    doReturn( true ).when( policyMock ).isAllowed( SchedulerExecuteAction.NAME );

    defaultSchedulerService.getJobs();
    verify( iSchedulerMock ).getJobs( getJobsFilterCaptor.capture() );
    IJobFilter filter = getJobsFilterCaptor.getValue();
    assertNotNull( filter );

    List<Job> testJobs = getJobs();
    List<Job> filteredJobs = new ArrayList<>();
    for ( Job job : testJobs ) {
      if ( filter.accept( job ) ) {
        filteredJobs.add( job );
        assertNotEquals( "BlockoutAction", job.getJobName() );
      }
    }
    assertEquals( 10, filteredJobs.size() );
  }

  /**
   * Helper function that creates 11 jobs being:
   * - 10 owned by `testUser[1-10]` and with name `testJobName[1-10]`;
   * - 1 owned by `system` and with name `BlockoutAction`.
   */
  private List<Job> getJobs() {
    List<Job> jobs = new ArrayList<>();
    for ( int i = 0; i < 10; i++ ) {
      jobs.add( mockJob( "testUser" + i, "testJobName" + i ) );
    }
    jobs.add( mockJob( "system", "BlockoutAction" ) );
    return jobs;
  }

  private Job mockJob( String userName, String jobName ) {
    Job job = mock( Job.class );
    when( job.getUserName() ).thenReturn( userName );
    when( job.getJobName() ).thenReturn( jobName );
    return job;
  }
}
