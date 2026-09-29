/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/



package org.pentaho.platform.scheduler2.action;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pentaho.platform.scheduler2.action.ActionRunner.KEY_JCR_OUTPUT_PATH;
import static org.pentaho.platform.scheduler2.action.ActionRunner.KEY_USE_JCR;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.action.ActionInvocationException;
import org.pentaho.platform.api.action.IAction;
import org.pentaho.platform.api.action.IPostProcessingAction;
import org.pentaho.platform.api.repository.IContentItem;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.IStreamListener;
import org.pentaho.platform.api.repository2.unified.ISourcesStreamEvents;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.IBackgroundExecutionStreamProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.actions.TestVarArgsAction;
import org.pentaho.platform.scheduler2.ISchedulerOutputPathResolver;
import org.pentaho.platform.util.ActionUtil;
import org.pentaho.platform.util.bean.TestAction;
import org.pentaho.platform.util.messages.LocaleHelper;

@RunWith( MockitoJUnitRunner.class )
public class ActionRunnerTest {

  @Test
  public void testCallInvokesExecute() throws Exception {
    Map<String, Object> paramsMap = createMapWithUserLocale();
    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    ActionRunner actionRunner = new ActionRunner( actionBeanSpy, "actionUser", paramsMap, null );
    actionRunner.call();
    Mockito.verify( actionBeanSpy ).execute();

    // Verify that, by default the isExecutionSuccessful returns true
    assertTrue( actionBeanSpy.isExecutionSuccessful() );
  }


  @Test
  public void testCallWithStreamProvider() throws Exception {
    Map<String, Object> paramsMap = createMapWithUserLocale();
    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    IBackgroundExecutionStreamProvider streamProvider = Mockito.mock( IBackgroundExecutionStreamProvider.class );
    InputStream mockInputStream = Mockito.mock( InputStream.class );
    OutputStream mockOutputStream = Mockito.mock( OutputStream.class );
    String mockOutputPath = "/someUser/someOutput";
    when( streamProvider.getInputStream() ).thenReturn( mockInputStream );
    when( streamProvider.getOutputPath() ).thenReturn( mockOutputPath );
    when( streamProvider.getOutputStream() ).thenReturn( mockOutputStream );

    ActionRunner actionRunner = actionRunnerWithOutputPath( actionBeanSpy, paramsMap, streamProvider, mockOutputPath );

    assertFalse( actionRunner.call() );
    verify( actionBeanSpy ).execute();
    verify( mockOutputStream ).close();
  }

  @Test
  public void testCallWithStreamProviderAndVarargsAction() throws Exception {
    Map<String, Object> paramsMap = createMapWithUserLocale();
    TestVarArgsAction testVarArgsAction = new TestVarArgsAction();
    IBackgroundExecutionStreamProvider streamProvider = Mockito.mock( IBackgroundExecutionStreamProvider.class );
    InputStream mockInputStream = Mockito.mock( InputStream.class );
    OutputStream mockOutputStream = Mockito.mock( OutputStream.class );
    String mockOutputPath = "/someUser/someOutput";
    when( streamProvider.getInputStream() ).thenReturn( mockInputStream );
    when( streamProvider.getOutputPath() ).thenReturn( mockOutputPath );
    when( streamProvider.getOutputStream() ).thenReturn( mockOutputStream );

    ActionRunner actionRunner = actionRunnerWithOutputPath( testVarArgsAction, paramsMap, streamProvider, mockOutputPath );

    assertFalse( actionRunner.call() );
    assertThat( testVarArgsAction.isExecuteWasCalled(), is( true ) );
  }

  @Test
  public void testCallSkipsActionWhenOutputPathIsUnavailable() throws Exception {
    Map<String, Object> paramsMap = createMapWithUserLocale();
    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    IBackgroundExecutionStreamProvider streamProvider = Mockito.mock( IBackgroundExecutionStreamProvider.class );
    InputStream inputStream = mock( InputStream.class );
    when( streamProvider.getInputStream() ).thenReturn( inputStream );

    ActionRunner actionRunner = actionRunnerWithOutputPath( actionBeanSpy, paramsMap, streamProvider, null );

    try ( MockedStatic<ActionUtil> actionUtil = Mockito.mockStatic( ActionUtil.class ) ) {
      assertTrue( actionRunner.call() );
      actionUtil.verify( () -> ActionUtil.sendFailureEmail( paramsMap, null ) );
    }

    verify( actionBeanSpy, never() ).execute();
    verify( streamProvider, never() ).getOutputStream();
  }

  @Test
  public void testCallRequestsJobUpdateWhenOutputPathChanges() throws Exception {
    Map<String, Object> paramsMap = createMapWithUserLocale();
    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    IBackgroundExecutionStreamProvider streamProvider = Mockito.mock( IBackgroundExecutionStreamProvider.class );
    InputStream inputStream = mock( InputStream.class );
    when( streamProvider.getInputStream() ).thenReturn( inputStream );
    when( streamProvider.getOutputPath() ).thenReturn( "/someUser/originalOutput" );

    ActionRunner actionRunner = actionRunnerWithOutputPath( actionBeanSpy, paramsMap, streamProvider,
      "/someUser/resolvedOutput" );

    assertTrue( actionRunner.call() );
    verify( streamProvider ).setOutputFilePath( "/someUser/resolvedOutput" );
    verify( actionBeanSpy, never() ).execute();
  }

  @Test
  public void testCallClosesPostProcessingContentAndRecordsLineageMetadata() throws Exception {
    Map<String, Object> paramsMap = createMapWithUserLocale();
    paramsMap.put( ActionUtil.QUARTZ_LINEAGE_ID, "lineage-1" );
    IContentItem contentItem = Mockito.mock( IContentItem.class );
    RepositoryFile repositoryFile = Mockito.mock( RepositoryFile.class );
    IUnifiedRepository repository = Mockito.mock( IUnifiedRepository.class );
    Map<String, Serializable> metadata = new HashMap<>();
    PostProcessingTestAction action = new PostProcessingTestAction( Collections.singletonList( contentItem ) );
    when( contentItem.getPath() ).thenReturn( "/home/testUser/output.csv" );
    when( repository.getFile( "/home/testUser/output.csv" ) ).thenReturn( repositoryFile );
    when( repositoryFile.getId() ).thenReturn( "output-id" );
    when( repository.getFileMetadata( "output-id" ) ).thenReturn( metadata );

    try ( MockedStatic<PentahoSystem> pentahoSystem = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystem.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn( repository );

      assertFalse( new ActionRunner( action, "actionUser", paramsMap, null ).call() );
    }

    verify( contentItem ).closeOutputStream();
    assertEquals( "lineage-1", metadata.get( ActionUtil.QUARTZ_LINEAGE_ID ) );
    verify( repository ).setFileMetadata( "output-id", metadata );
  }

  @Test
  public void testCallHandlesStreamCreatedAndCompletedEvents() throws Exception {
    Map<String, Object> paramsMap = createMapWithUserLocale();
    String outputPath = "/home/testUser/output.csv";
    EventOutputStream eventStream = new EventOutputStream();
    IAction action = Mockito.mock( IAction.class );
    IBackgroundExecutionStreamProvider streamProvider = Mockito.mock( IBackgroundExecutionStreamProvider.class );
    InputStream inputStream = mock( InputStream.class );
    when( streamProvider.getInputStream() ).thenReturn( inputStream );
    when( streamProvider.getOutputPath() ).thenReturn( outputPath );
    when( streamProvider.getOutputStream() ).thenReturn( eventStream );
    when( action.isExecutionSuccessful() ).thenReturn( true );
    Mockito.doAnswer( invocation -> {
      eventStream.fileCreated( outputPath );
      eventStream.streamComplete();
      return null;
    } ).when( action ).execute();
    final boolean[] emailSent = new boolean[1];

    ActionRunner actionRunner = new ActionRunner( action, "actionUser", paramsMap, streamProvider ) {
      @Override
      protected String resolveOutputFilePath() {
        return outputPath;
      }

      @Override
      protected void sendEmail( Map<String, Object> actionParams ) {
        emailSent[0] = true;
      }

      @Override
      protected void deleteFileIfEmpty() {
        // Repository cleanup is covered separately; this test isolates stream synchronization.
      }
    };

    assertFalse( actionRunner.call() );

    verify( action ).execute();
    assertTrue( emailSent[0] );
    assertTrue( eventStream.closed );
  }

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void testCallThrowsException() throws Exception {
    Map<String, Object> paramsMap = createMapWithUserLocale();
    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    IBackgroundExecutionStreamProvider mockStreamProvider = Mockito.mock( IBackgroundExecutionStreamProvider.class );
    when( mockStreamProvider.getInputStream() ).thenThrow( new Exception( "something went wrong" ) );
    ActionRunner actionRunner = new ActionRunner( actionBeanSpy, "actionUser", paramsMap, mockStreamProvider );
    exception.expect( ActionInvocationException.class );
    actionRunner.call();
  }

  @Test
  public void testFailureEmailSentWhenExecutionStatusFalse() throws Exception {
    Map<String, Object> paramsMap = createMapWithUserLocale();
    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    Mockito.doReturn( false ).when( actionBeanSpy ).isExecutionSuccessful();

    ActionRunner actionRunner = new ActionRunner( actionBeanSpy, "actionUser", paramsMap, null );

    try ( MockedStatic<ActionUtil> actionUtilStatic = Mockito.mockStatic( ActionUtil.class, Mockito.CALLS_REAL_METHODS ) ) {
      actionRunner.call();

      actionUtilStatic.verify( () -> ActionUtil.sendFailureEmail( paramsMap, null ), times( 1 ) );
    }
  }

  @Test
  public void testFailureEmailSentWhenExceptionThrown() throws Exception {
    Map<String, Object> paramsMap = createMapWithUserLocale();
    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    IBackgroundExecutionStreamProvider mockStreamProvider = Mockito.mock( IBackgroundExecutionStreamProvider.class );
    Exception boom = new Exception( "something went wrong" );

    try ( MockedStatic<ActionUtil> actionUtilStatic = Mockito.mockStatic( ActionUtil.class, Mockito.CALLS_REAL_METHODS ) ) {
      when( mockStreamProvider.getInputStream() ).thenThrow( boom );

      ActionRunner actionRunner = new ActionRunner( actionBeanSpy, "actionUser", paramsMap, mockStreamProvider );
      exception.expect( ActionInvocationException.class );
      try {
        actionRunner.call();
      } finally {
        actionUtilStatic.verify( () -> ActionUtil.sendFailureEmail( paramsMap, boom ), times( 1 ) );
      }
    }
  }

  @Test
  public void testFailureEmailNotSentWhenRestartFlagPresent() throws Exception {
    Map<String, Object> paramsMap = createMapWithUserLocale();
    paramsMap.put( ActionUtil.QUARTZ_RESTART_FLAG, Boolean.TRUE );

    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    Mockito.doReturn( false ).when( actionBeanSpy ).isExecutionSuccessful();

    ActionRunner actionRunner = new ActionRunner( actionBeanSpy, "actionUser", paramsMap, null );

    try ( MockedStatic<ActionUtil> actionUtilStatic = Mockito.mockStatic( ActionUtil.class, Mockito.CALLS_REAL_METHODS ) ) {
      actionRunner.call();

      actionUtilStatic.verify( () -> ActionUtil.sendFailureEmail( paramsMap, null ), Mockito.never() );
    }
  }

  @Test
  public void testFailureEmailNotSentOnExceptionWhenRestartFlagPresent() throws Exception {
    Map<String, Object> paramsMap = createMapWithUserLocale();
    paramsMap.put( ActionUtil.QUARTZ_RESTART_FLAG, Boolean.TRUE );

    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    IBackgroundExecutionStreamProvider mockStreamProvider = Mockito.mock( IBackgroundExecutionStreamProvider.class );
    Exception boom = new Exception( "something went wrong" );

    try ( MockedStatic<ActionUtil> actionUtilStatic = Mockito.mockStatic( ActionUtil.class, Mockito.CALLS_REAL_METHODS ) ) {
      when( mockStreamProvider.getInputStream() ).thenThrow( boom );

      ActionRunner actionRunner = new ActionRunner( actionBeanSpy, "actionUser", paramsMap, mockStreamProvider );
      exception.expect( ActionInvocationException.class );
      try {
        actionRunner.call();
      } finally {
        actionUtilStatic.verify( () -> ActionUtil.sendFailureEmail( paramsMap, boom ), Mockito.never() );
      }
    }
  }

  private Map<String, Object> createMapWithUserLocale() {
    Map<String, Object> paramsMap = new HashMap<>();
    paramsMap.put( LocaleHelper.USER_LOCALE_PARAM, Locale.US );
    return paramsMap;
  }

  @Test
  public void deleteFileIfEmptyDoesNothingWithoutAnOutputFile() {
    try ( MockedStatic<PentahoSystem> pentahoSystemMockedStatic = Mockito.mockStatic( PentahoSystem.class ) ) {
      IUnifiedRepository mockRepository = Mockito.mock( IUnifiedRepository.class );
      pentahoSystemMockedStatic.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn( mockRepository );

      Map<String, Object> paramsMap = createMapWithUserLocale();
      IAction actionBeanSpy = Mockito.spy( new TestAction() );
      ActionRunner actionRunner = new ActionRunner( actionBeanSpy, "actionUser", paramsMap, null );
      actionRunner.outputFilePath = null;
      actionRunner.deleteFileIfEmpty();

      verify( mockRepository, times( 0 ) ).getFile( any() );
    }
  }

  @Test
  public void deleteFileIfEmptyDeletesAZeroSizeOutputFile() {
    IUnifiedRepository repository = Mockito.mock( IUnifiedRepository.class );
    RepositoryFile file = Mockito.mock( RepositoryFile.class );
    when( repository.getFile( "/home/testUser/empty.csv" ) ).thenReturn( file );
    when( file.getFileSize() ).thenReturn( 0L );
    when( file.getId() ).thenReturn( "empty-file-id" );
    ActionRunner actionRunner = new ActionRunner( null, null, new HashMap<>(), null );
    actionRunner.outputFilePath = "/home/testUser/empty.csv";

    try ( MockedStatic<PentahoSystem> pentahoSystem = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystem.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn( repository );

      actionRunner.deleteFileIfEmpty();
    }

    verify( repository ).deleteFile( "empty-file-id", true, null );
  }

  @Test
  public void deleteFileIfEmptyKeepsANonEmptyOutputFile() {
    IUnifiedRepository repository = Mockito.mock( IUnifiedRepository.class );
    RepositoryFile file = Mockito.mock( RepositoryFile.class );
    when( repository.getFile( "/home/testUser/output.csv" ) ).thenReturn( file );
    when( file.getFileSize() ).thenReturn( 1L );
    ActionRunner actionRunner = new ActionRunner( null, null, new HashMap<>(), null );
    actionRunner.outputFilePath = "/home/testUser/output.csv";

    try ( MockedStatic<PentahoSystem> pentahoSystem = Mockito.mockStatic( PentahoSystem.class ) ) {
      pentahoSystem.when( () -> PentahoSystem.get( IUnifiedRepository.class ) ).thenReturn( repository );

      actionRunner.deleteFileIfEmpty();
    }

    verify( repository, never() ).deleteFile( any(), Mockito.anyBoolean(), any() );
  }

  private ActionRunner actionRunnerWithOutputPath( IAction action, Map<String, Object> params,
                                                   IBackgroundExecutionStreamProvider streamProvider,
                                                   String resolvedOutputPath ) {
    return new ActionRunner( action, "actionUser", params, streamProvider ) {
      @Override
      protected String resolveOutputFilePath() {
        return resolvedOutputPath;
      }
    };
  }

  private static class PostProcessingTestAction extends TestAction implements IPostProcessingAction {
    private final List<IContentItem> outputContents;

    private PostProcessingTestAction( List<IContentItem> outputContents ) {
      this.outputContents = outputContents;
    }

    @Override
    public List<IContentItem> getActionOutputContents() {
      return outputContents;
    }
  }

  private static class EventOutputStream extends OutputStream implements ISourcesStreamEvents {
    private IStreamListener listener;
    private boolean closed;

    @Override
    public void addListener( IStreamListener listener ) {
      this.listener = listener;
    }

    @Override
    public void write( int value ) {
      // The test exercises stream events, not byte persistence.
    }

    @Override
    public void close() {
      closed = true;
    }

    private void fileCreated( String path ) {
      listener.fileCreated( path );
    }

    private void streamComplete() {
      listener.streamComplete();
    }
  }

  @Test
  public void testBuildSchedulerOutputPathResolver() {
    String testActionUser = "RandomActionUser";
    ActionRunner actionRunner = new ActionRunner( null, testActionUser, new HashMap<>(), null );
    ISchedulerOutputPathResolver schedulerOutputPathResolver = Mockito.mock( ISchedulerOutputPathResolver.class );
    String testFilename = "myImportantJOb.*";
    String testDirectory = "/home/janeDoe/somePath/some_directory/";
    String outputPathPattern = testDirectory + testFilename;

    // Execute
    actionRunner.buildSchedulerOutputPathResolver( schedulerOutputPathResolver, outputPathPattern );

    verify( schedulerOutputPathResolver ).setFileName( testFilename );
    verify( schedulerOutputPathResolver ).setDirectory( testDirectory );
    verify( schedulerOutputPathResolver ).setActionUser( testActionUser );

  }

  @Test
  public void testGetParentDirectory() {
    ActionRunner actionRunner = new ActionRunner( null, null, new HashMap<>(), null );

    assertEquals( "/home/someUser/somePath", actionRunner
      .getParentDirectory( "/home/someUser/somePath/someFile.txt" ) );

    assertEquals( "/home/someUser/somePath", actionRunner
      .getParentDirectory( "/home/someUser/somePath/anotherFile.*" ) );
  }

  @Test
  public void addJcrParamsDefaults() {
    ActionRunner actionRunner = new ActionRunner( null, null, new HashMap<>(), null );

    String directory = "/home/janeDoe/reports";
    String outPath  = directory + "/someJob.*";

    //TEST 1 - no jcr defined keys
    HashMap actionParams1 = new HashMap() {{
      put( "key1", "value1" );
      put( "key2", "value2" );
    } };

    actionRunner.addJcrParams( actionParams1, outPath );

    assertEquals( Boolean.TRUE, actionParams1.get( KEY_USE_JCR ) );
    assertEquals( directory, actionParams1.get( KEY_JCR_OUTPUT_PATH ) );

    // TEST 2 - existing values don't get override or removed

    String alternateDirectory = "/home/sally/super/secret";

    HashMap actionParams2 = new HashMap() {{
      put( "key1", "value1" );
      put( "key2", "value2" );
      put( KEY_USE_JCR, Boolean.FALSE );
      put( KEY_JCR_OUTPUT_PATH, alternateDirectory );
    } };

    actionRunner.addJcrParams( actionParams2, outPath );

    assertEquals( Boolean.FALSE, actionParams2.get( KEY_USE_JCR ) );
    assertEquals( alternateDirectory, actionParams2.get( KEY_JCR_OUTPUT_PATH ) );
  }

}
