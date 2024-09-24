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

package org.pentaho.platform.web.http.api.resources;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pentaho.platform.api.engine.IPentahoSession;
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

    IPentahoSession sessionMock = mock( IPentahoSession.class );
    Mockito.when( sessionMock.getName() ).thenReturn( "admin" );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, outputFolder, true, true );

    schedulerOutputPathResolver = new SchedulerOutputPathResolver( scheduleRequest );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );
    schedulerOutputPathResolver.setSession( sessionMock );

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/output/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock ).doesFolderExist( outputFolder );
  }

  @Test
  public void testResolveOutputFilePath_withJobName() throws OperationFailedException {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";
    String jobName = "test";
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );
    scheduleRequest.setJobName( jobName );

    IPentahoSession sessionMock = mock( IPentahoSession.class );
    Mockito.when( sessionMock.getName() ).thenReturn( "admin" );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, outputFolder, true, true );

    schedulerOutputPathResolver = new SchedulerOutputPathResolver( scheduleRequest );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );
    schedulerOutputPathResolver.setSession( sessionMock );

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

    IPentahoSession sessionMock = mock( IPentahoSession.class );
    Mockito.when( sessionMock.getName() ).thenReturn( "admin" );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", true, true );

    schedulerOutputPathResolver = new SchedulerOutputPathResolver( scheduleRequest );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );
    schedulerOutputPathResolver.setSession( sessionMock );

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/output/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock ).doesFolderExist( "/home/admin/output" );
  }

  @Test
  public void testResolveOutputFilePath_whenNoOutputFileThenFallsBack() throws OperationFailedException {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = null;
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", true, true );

    IPentahoSession sessionMock = mock( IPentahoSession.class );
    Mockito.when( sessionMock.getName() ).thenReturn( "admin" );

    schedulerOutputPathResolver = Mockito.spy( new SchedulerOutputPathResolver( scheduleRequest ) );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );
    schedulerOutputPathResolver.setSession( sessionMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( "/system/setting" ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();
    Mockito.doReturn( "/home/admin" ).when( schedulerOutputPathResolver ).getUserHomeDirectoryPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/setting/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( (String) null );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/system/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/home/admin" );
  }

  @Test
  public void testResolveOutputFilePath_whenOutputFolderDoesNotExistThenFallsBack() throws OperationFailedException {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", false, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", true, true );

    IPentahoSession sessionMock = mock( IPentahoSession.class );
    Mockito.when( sessionMock.getName() ).thenReturn( "admin" );

    schedulerOutputPathResolver = Mockito.spy( new SchedulerOutputPathResolver( scheduleRequest ) );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );
    schedulerOutputPathResolver.setSession( sessionMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( "/system/setting" ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();
    Mockito.doReturn( "/home/admin" ).when( schedulerOutputPathResolver ).getUserHomeDirectoryPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/setting/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/output" );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/system/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/home/admin" );
  }

  @Test
  public void testResolveOutputFilePath_whenOutputFolderHasNoAccessThenFallsBack() throws OperationFailedException {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", true, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", true, true );

    IPentahoSession sessionMock = mock( IPentahoSession.class );
    Mockito.when( sessionMock.getName() ).thenReturn( "admin" );

    schedulerOutputPathResolver = Mockito.spy( new SchedulerOutputPathResolver( scheduleRequest ) );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );
    schedulerOutputPathResolver.setSession( sessionMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( "/system/setting" ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();
    Mockito.doReturn( "/home/admin" ).when( schedulerOutputPathResolver ).getUserHomeDirectoryPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/setting/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/output" );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).hasAccess( Mockito.eq( "/home/admin/output" ), Mockito.any() );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/system/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/home/admin" );
  }

  @Test
  public void testResolveOutputFilePath_whenGenericFileServiceThrowsCheckingOutputFolderThenFallsBack()
    throws OperationFailedException {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    Mockito.when( genericFileServiceMock.doesFolderExist( "/home/admin/output" ) )
      .thenThrow( OperationFailedException.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", true, true );

    IPentahoSession sessionMock = mock( IPentahoSession.class );
    Mockito.when( sessionMock.getName() ).thenReturn( "admin" );

    schedulerOutputPathResolver = Mockito.spy( new SchedulerOutputPathResolver( scheduleRequest ) );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );
    schedulerOutputPathResolver.setSession( sessionMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( "/system/setting" ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();
    Mockito.doReturn( "/home/admin" ).when( schedulerOutputPathResolver ).getUserHomeDirectoryPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/setting/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/output" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).hasAccess( Mockito.eq( "/home/admin/output" ), Mockito.any() );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/system/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/home/admin" );
  }

  @Test
  public void testResolveOutputFilePath_whenFallbackDoesNotExistThenFallsBackFarther() throws OperationFailedException {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    IPentahoSession sessionMock = mock( IPentahoSession.class );
    Mockito.when( sessionMock.getName() ).thenReturn( "admin" );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", false, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin", true, true );

    schedulerOutputPathResolver = Mockito.spy( new SchedulerOutputPathResolver( scheduleRequest ) );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );
    schedulerOutputPathResolver.setSession( sessionMock );

    Mockito.doReturn( null ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( null ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();
    Mockito.doReturn( "/home/admin" ).when( schedulerOutputPathResolver ).getUserHomeDirectoryPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/test.*", outputFilePath );

    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( (String) null );
    Mockito.verify( genericFileServiceMock ).doesFolderExist( "/home/admin" );
    Mockito.verify( genericFileServiceMock ).hasAccess( Mockito.eq( "/home/admin" ), Mockito.any() );
  }

  @Test
  public void testResolveOutputFilePath_whenFallbackHasNoAccessThenFallsBackFarther() throws OperationFailedException {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    IPentahoSession sessionMock = mock( IPentahoSession.class );
    Mockito.when( sessionMock.getName() ).thenReturn( "admin" );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", false, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", true, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin", true, true );

    schedulerOutputPathResolver = Mockito.spy( new SchedulerOutputPathResolver( scheduleRequest ) );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );
    schedulerOutputPathResolver.setSession( sessionMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( null ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();
    Mockito.doReturn( "/home/admin" ).when( schedulerOutputPathResolver ).getUserHomeDirectoryPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertEquals( "/home/admin/test.*", outputFilePath );

    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( (String) null );

    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/output" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).hasAccess( Mockito.eq( "/home/admin/output" ), Mockito.any() );

    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/setting" );
    Mockito.verify( genericFileServiceMock, times( 1 ) )
      .hasAccess( Mockito.eq( "/home/admin/setting" ), Mockito.any() );

    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin" );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).hasAccess( Mockito.eq( "/home/admin" ), Mockito.any() );
  }


  @Test
  public void testResolveOutputFilePath_whenNoAvailableFolderOrFallbackReturnsNull() throws OperationFailedException {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    IPentahoSession sessionMock = mock( IPentahoSession.class );
    Mockito.when( sessionMock.getName() ).thenReturn( "admin" );

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", false, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", false, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin", false, false );

    schedulerOutputPathResolver = Mockito.spy( new SchedulerOutputPathResolver( scheduleRequest ) );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );
    schedulerOutputPathResolver.setSession( sessionMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( null ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();
    Mockito.doReturn( "/home/admin" ).when( schedulerOutputPathResolver ).getUserHomeDirectoryPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePath();

    Assert.assertNull( outputFilePath );

    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( (String) null );

    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/output" );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/setting" );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin" );
  }

  @After
  public void tearDown() throws Exception {
    PentahoSystem.clearObjectFactory();
  }

  // region Helpers
  private static void mockGenericFileServiceFile( IGenericFileService genericFileServiceMock,
                                                  String folder,
                                                  boolean doesFolderExist,
                                                  boolean hasAccess )
    throws OperationFailedException {
    Mockito.when( genericFileServiceMock.doesFolderExist( folder ) ).thenReturn( doesFolderExist );
    Mockito.when( genericFileServiceMock.hasAccess( Mockito.eq( folder ), Mockito.any() ) ).thenReturn( hasAccess );
  }
  // endregion
}
