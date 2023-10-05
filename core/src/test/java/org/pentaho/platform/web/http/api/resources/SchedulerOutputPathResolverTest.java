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
 * Copyright (c) 2002-2021 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.web.http.api.resources;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.usersettings.IUserSettingService;
import org.pentaho.platform.engine.core.system.PentahoSystem;

/**
 * Created by rfellows on 9/23/15.
 */
public class SchedulerOutputPathResolverTest {

  SchedulerOutputPathResolver schedulerOutputPathResolver;

  IUnifiedRepository repo;
  IUserSettingService userSettingService;

  @Before
  public void setUp() throws Exception {
    repo = Mockito.mock( IUnifiedRepository.class );
    userSettingService = Mockito.mock( IUserSettingService.class );
    PentahoSystem.registerObject( repo );
    PentahoSystem.registerObject( userSettingService );
  }

  @Test
  public void testResolveOutputFilePath() throws Exception {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    RepositoryFile repoFile = Mockito.mock( RepositoryFile.class );
    Mockito.when( repo.getFile( outputFolder ) ).thenReturn( repoFile );
    Mockito.when( repoFile.isFolder() ).thenReturn( true );

    schedulerOutputPathResolver = new SchedulerOutputPathResolver( scheduleRequest );
    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/output/test.*", outputFilePath );
    Mockito.verify( repo ).getFile( outputFolder );

  }

  @Test
  public void testResolveOutputFilePath_ContainsPatternAlready() throws Exception {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output/test.*";
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    RepositoryFile repoFile = Mockito.mock( RepositoryFile.class );
    Mockito.when( repo.getFile( ArgumentMatchers.nullable( String.class ) ) ).thenReturn( repoFile );
    Mockito.when( repoFile.isFolder() ).thenReturn( true );

    schedulerOutputPathResolver = new SchedulerOutputPathResolver( scheduleRequest );
    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/output/test.*", outputFilePath );
    Mockito.verify( repo ).getFile( "/home/admin/output" );
  }

  @Test
  public void testResolveOutputFilePath_Fallback() throws Exception {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = null;
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    RepositoryFile repoFile = Mockito.mock( RepositoryFile.class );
    Mockito.when( repo.getFile( ArgumentMatchers.nullable( String.class ) ) ).thenReturn( repoFile );
    Mockito.when( repoFile.isFolder() ).thenReturn( false );

    schedulerOutputPathResolver = Mockito.spy( new SchedulerOutputPathResolver( scheduleRequest ) );
    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( "/system/setting" ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();
    Mockito.doReturn( "/home/admin" ).when( schedulerOutputPathResolver ).getUserHomeDirectoryPath();
    Mockito.doReturn( true ).when( schedulerOutputPathResolver ).isValidOutputPath( "/home/admin/setting" );
    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/setting/test.*", outputFilePath );
  }

  @Test
  public void testResolveOutputFilePath_FallbackFarther() throws Exception {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = null;
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    RepositoryFile repoFile = Mockito.mock( RepositoryFile.class );
    Mockito.when( repo.getFile( ArgumentMatchers.nullable( String.class ) ) ).thenReturn( repoFile );
    Mockito.when( repoFile.isFolder() ).thenReturn( false );

    schedulerOutputPathResolver = Mockito.spy( new SchedulerOutputPathResolver( scheduleRequest ) );
    Mockito.doReturn( null ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( null ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();
    Mockito.doReturn( "/home/admin" ).when( schedulerOutputPathResolver ).getUserHomeDirectoryPath();
    Mockito.doReturn( true ).when( schedulerOutputPathResolver ).isValidOutputPath( "/home/admin" );
    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/test.*", outputFilePath );
  }

  @After
  public void tearDown() throws Exception {
    PentahoSystem.clearObjectFactory();
  }
}