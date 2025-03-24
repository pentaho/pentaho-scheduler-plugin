/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.platform.web.http.api.resources;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginResourceLoader;
import org.pentaho.platform.api.genericfile.IGenericFileService;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.usersettings.IUserSettingService;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.web.http.api.resources.services.SchedulerService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Created by rfellows on 9/23/15.
 */
public class SchedulerOutputPathResolverTest {

  SchedulerOutputPathResolver schedulerOutputPathResolver;

  IUserSettingService userSettingService;

  final static String ADMIN_HOME_FOLDER = "/home/admin";

  private IPluginResourceLoader resourceLoader;
  private MockedStatic<PentahoSystem> mockedPentahoSystem;

  @Before
  public void setUp() throws Exception {
    userSettingService = mock( IUserSettingService.class );
    PentahoSystem.registerObject( userSettingService );
    mockedPentahoSystem = Mockito.mockStatic( PentahoSystem.class );
    resourceLoader = mock( IPluginResourceLoader.class );
    mockedPentahoSystem.when(() -> PentahoSystem.get(IPluginResourceLoader.class, null ) ).thenReturn( resourceLoader );
  }

  @Test
  public void testResolveOutputFilePath() throws Exception {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, outputFolder, true, true );

    schedulerOutputPathResolver = setupSchedulerOutputPathResolver( inputFile, outputFolder, genericFileServiceMock );
    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePathCore();

    Assert.assertEquals( "/home/admin/output/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock ).doesFolderExist( outputFolder );
  }

  @Test
  public void testResolveOutputFilePath_withJobName() throws Exception {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";
    String jobName = "test";

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, outputFolder, true, true );
    mockGenericFileServiceFile( genericFileServiceMock, ADMIN_HOME_FOLDER, true, true );

    schedulerOutputPathResolver =
      setupSchedulerOutputPathResolver( inputFile, outputFolder, "admin", jobName, genericFileServiceMock );
    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePathCore();

    Assert.assertEquals( "/home/admin/output/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock ).doesFolderExist( outputFolder );
  }

  @Test
  public void testResolveOutputFilePath_ContainsPatternAlready() throws Exception {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output/test.*";

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", true, true );

    schedulerOutputPathResolver = setupSchedulerOutputPathResolver( inputFile, outputFolder, genericFileServiceMock );
    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePathCore();

    Assert.assertEquals( "/home/admin/output/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock ).doesFolderExist( "/home/admin/output" );
  }

  @Test
  public void testResolveOutputFilePath_whenNoOutputFileThenFallsBack() throws Exception {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = null;

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", true, true );
    configureFallbackEnabled( true );

    schedulerOutputPathResolver = setupSchedulerOutputPathResolver( inputFile, outputFolder, genericFileServiceMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( "/system/setting" ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePathCore();

    Assert.assertEquals( "/home/admin/setting/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( (String) null );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/system/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/home/admin" );
  }

  @Test
  public void testResolveOutputFilePath_whenOutputFolderDoesNotExistThenFallsBack() throws Exception {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", false, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", true, true );
    configureFallbackEnabled( true );

    schedulerOutputPathResolver = setupSchedulerOutputPathResolver( inputFile, outputFolder, genericFileServiceMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( "/system/setting" ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePathCore();

    Assert.assertEquals( "/home/admin/setting/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/output" );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/system/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/home/admin" );
  }

  @Test
  public void testResolveOutputFilePath_whenOutputFolderHasNoAccessThenFallsBack() throws Exception {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", true, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", true, true );
    configureFallbackEnabled( true );

    schedulerOutputPathResolver = setupSchedulerOutputPathResolver( inputFile, outputFolder, genericFileServiceMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( "/system/setting" ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePathCore();

    Assert.assertEquals( "/home/admin/setting/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/output" );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).hasAccess( Mockito.eq( "/home/admin/output" ), Mockito.any() );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/system/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/home/admin" );
  }

  @Test
  public void testResolveOutputFilePath_whenGenericFileServiceThrowsCheckingOutputFolderThenFallsBack()
          throws Exception {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    Mockito.when( genericFileServiceMock.doesFolderExist( "/home/admin/output" ) )
      .thenThrow( OperationFailedException.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", true, true );
    configureFallbackEnabled( true );

    schedulerOutputPathResolver = setupSchedulerOutputPathResolver( inputFile, outputFolder, genericFileServiceMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( "/system/setting" ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePathCore();

    Assert.assertEquals( "/home/admin/setting/test.*", outputFilePath );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/output" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).hasAccess( Mockito.eq( "/home/admin/output" ), Mockito.any() );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/system/setting" );
    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( "/home/admin" );
  }

  @Test
  public void testResolveOutputFilePath_whenFallbackDoesNotExistThenFallsBackFarther() throws Exception {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", false, false );
    configureFallbackEnabled( true );

    schedulerOutputPathResolver = setupSchedulerOutputPathResolver( inputFile, outputFolder, genericFileServiceMock );

    Mockito.doReturn( null ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( null ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePathCore();

    Assert.assertEquals( "/home/admin/test.*", outputFilePath );

    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( (String) null );
    Mockito.verify( genericFileServiceMock ).doesFolderExist( "/home/admin" );
    Mockito.verify( genericFileServiceMock ).hasAccess( Mockito.eq( "/home/admin" ), Mockito.any() );
  }

  @Test
  public void testResolveOutputFilePath_whenFallbackHasNoAccessThenFallsBackFarther() throws Exception {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", false, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", true, false );
    configureFallbackEnabled( true );

    schedulerOutputPathResolver = setupSchedulerOutputPathResolver( inputFile, outputFolder, genericFileServiceMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( null ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePathCore();

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
  public void testResolveOutputFilePath_whenNoAvailableFolderOrFallbackReturnsNull() throws Exception {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", false, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", false, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin", false, false );
    configureFallbackEnabled( true );

    schedulerOutputPathResolver =
      setupSchedulerOutputPathResolver( inputFile, outputFolder, "admin", null, genericFileServiceMock );

    Mockito.doReturn( "/home/admin/setting" ).when( schedulerOutputPathResolver ).getUserSettingOutputPath();
    Mockito.doReturn( null ).when( schedulerOutputPathResolver ).getSystemSettingOutputPath();

    String outputFilePath = schedulerOutputPathResolver.resolveOutputFilePathCore();

    Assert.assertNull( outputFilePath );

    Mockito.verify( genericFileServiceMock, times( 0 ) ).doesFolderExist( (String) null );

    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/output" );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin/setting" );
    Mockito.verify( genericFileServiceMock, times( 1 ) ).doesFolderExist( "/home/admin" );
  }

  @Test
  public void testResolveOutputFilePath_whenNullScheduleOwner() throws OperationFailedException {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, outputFolder, true, true );

    assertThrows( IllegalArgumentException.class,
      () -> setupSchedulerOutputPathResolver( inputFile, outputFolder, null, null, genericFileServiceMock ) );
  }

  @Test
  public void testResolveOutputFilePath_whenInvalidScheduleOwner() throws OperationFailedException {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, outputFolder, true, true );

    assertThrows( IllegalArgumentException.class,
      () -> setupSchedulerOutputPathResolver( inputFile, outputFolder, "", null, genericFileServiceMock ) );
  }

  @Test
  public void testResolveOutputFilePath_whenFallBackIsDisabled() throws Exception {
    String inputFile = "/home/admin/test.prpt";
    String outputFolder = "/home/admin/output";

    IGenericFileService genericFileServiceMock = mock( IGenericFileService.class );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/output", false, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin/setting", false, false );
    mockGenericFileServiceFile( genericFileServiceMock, "/home/admin", false, false );

    configureFallbackEnabled( false );

    schedulerOutputPathResolver =
            setupSchedulerOutputPathResolver( inputFile, outputFolder, "admin", null, genericFileServiceMock );

    assertThrows( SchedulerException.class, () -> schedulerOutputPathResolver.resolveOutputFilePathCore() );
  }

  @After
  public void tearDown() {
    PentahoSystem.clearObjectFactory();
    mockedPentahoSystem.close();
  }

  // region Helpers
  private void configureFallbackEnabled( boolean value ) {
    when(resourceLoader.getPluginSetting( SchedulerService.class, "settings/scheduler-fallback", "false")).thenReturn(String.valueOf( value ));
  }

  private static void mockGenericFileServiceFile( IGenericFileService genericFileServiceMock,
                                                  String folder,
                                                  boolean doesFolderExist,
                                                  boolean hasAccess )
    throws OperationFailedException {
    Mockito.when( genericFileServiceMock.doesFolderExist( folder ) ).thenReturn( doesFolderExist );
    Mockito.when( genericFileServiceMock.hasAccess( Mockito.eq( folder ), Mockito.any() ) ).thenReturn( hasAccess );
  }

  private SchedulerOutputPathResolver setupSchedulerOutputPathResolver( String inputFile, String outputFolder,
                                                                        IGenericFileService genericFileServiceMock )
    throws OperationFailedException {

    // admin home folder should always exist in default cases, this is our last fallback output folder for admin user
    mockGenericFileServiceFile( genericFileServiceMock, ADMIN_HOME_FOLDER, true, true );

    return setupSchedulerOutputPathResolver( inputFile, outputFolder, "admin", null, genericFileServiceMock );
  }

  private SchedulerOutputPathResolver setupSchedulerOutputPathResolver( String inputFile, String outputFolder,
                                                                        String scheduleOwner, String jobName,
                                                                        IGenericFileService genericFileServiceMock ) {
    JobScheduleRequest scheduleRequest = new JobScheduleRequest();
    scheduleRequest.setInputFile( inputFile );
    scheduleRequest.setOutputFile( outputFolder );

    if ( jobName != null ) {
      scheduleRequest.setJobName( jobName );
    }

    scheduleRequest.getJobParameters()
      .add( new JobScheduleParam( IScheduler.RESERVEDMAPKEY_ACTIONUSER, scheduleOwner ) );

    IPentahoSession sessionMock = mock( IPentahoSession.class );
    Mockito.when( sessionMock.getName() ).thenReturn( scheduleOwner );

    schedulerOutputPathResolver = Mockito.spy( new SchedulerOutputPathResolver( scheduleRequest ) );
    schedulerOutputPathResolver.setGenericFileService( genericFileServiceMock );
    schedulerOutputPathResolver.setSession( sessionMock );

    return schedulerOutputPathResolver;
  }
  // endregion
}
