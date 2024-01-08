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
 * Copyright (c) 2002-2023 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.web.http.api.resources;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.usersettings.IUserSettingService;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Created by rfellows on 9/23/15.
 */
public class SchedulerOutputPathResolverTest {

  SchedulerOutputPathResolver schedulerOutputPathResolver;

  IUserSettingService userSettingService;

  @Before
  public void setUp() throws Exception {
    userSettingService = mock( IUserSettingService.class );
    PentahoSystem.registerObject( userSettingService );
  }

  @Test
  public void testResolveOutputFilePath() throws OperationFailedException {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    Mockito.when( genericFileServiceMock.doesFolderExist( outputFolder ) ).thenReturn( true );

    schedulerOutputPathResolver = new SchedulerOutputPathResolver( scheduleRequest );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/output/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock ).doesFolderExist( outputFolder );
  }

  @Test
  public void testResolveOutputFilePath_ContainsPatternAlready() throws OperationFailedException {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output/test.*";
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    Mockito.when( genericFileServiceMock.doesFolderExist( "/home/admin/output" ) ).thenReturn( true );

    schedulerOutputPathResolver = new SchedulerOutputPathResolver( scheduleRequest );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/output/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock ).doesFolderExist( "/home/admin/output" );
  }

  @Test
  public void testResolveOutputFilePath_Fallback() throws OperationFailedException {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = null;
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    Mockito.when( genericFileServiceMock.doesFolderExist( "/home/admin/setting" ) ).thenReturn( true );

    schedulerOutputPathResolver = Mockito.spy( new SchedulerOutputPathResolver( scheduleRequest ) );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( "/system/setting" ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();
    Mockito.doReturn( "/home/admin" ).when( schedulerOutputPathResolver ).getUserHomeDirectoryPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/setting/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( null );
    Mockito.verify( genericFileServiceMock ).doesFolderExist( "/home/admin/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/system/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/home/admin" );
  }

  @Test
  public void testResolveOutputFilePath_FallbackFarther() throws OperationFailedException {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = null;
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    Mockito.when( genericFileServiceMock.doesFolderExist( "/home/admin" ) ).thenReturn( true );

    schedulerOutputPathResolver = Mockito.spy( new SchedulerOutputPathResolver( scheduleRequest ) );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );

    Mockito.doReturn( null ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( null ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();
    Mockito.doReturn( "/home/admin" ).when( schedulerOutputPathResolver ).getUserHomeDirectoryPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/test.*", outputFilePath );

    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( null );
    Mockito.verify( genericFileServiceMock ).doesFolderExist( "/home/admin" );
  }

  @After
  public void tearDown() throws Exception {
    PentahoSystem.clearObjectFactory();
  }
}
