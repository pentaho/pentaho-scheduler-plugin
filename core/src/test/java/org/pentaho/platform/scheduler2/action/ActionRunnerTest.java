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

package org.pentaho.platform.scheduler2.action;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.action.ActionInvocationException;
import org.pentaho.platform.api.action.IAction;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.ISecurityHelper;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.IBackgroundExecutionStreamProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.engine.services.actions.TestVarArgsAction;
import org.pentaho.platform.scheduler2.ISchedulerOutputPathResolver;
import org.pentaho.platform.util.bean.TestAction;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pentaho.platform.scheduler2.action.ActionRunner.KEY_JCR_OUTPUT_PATH;
import static org.pentaho.platform.scheduler2.action.ActionRunner.KEY_USE_JCR;
import static org.powermock.reflect.Whitebox.setInternalState;

@RunWith( MockitoJUnitRunner.class )
@PowerMockIgnore( "jdk.internal.reflect.*" )
@PrepareForTest( PentahoSystem.class )
public class ActionRunnerTest {

  @Test
  public void testCallInvokesExecute() throws Exception {
    Map<String, Serializable> paramsMap = createMapWithUserLocale();
    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    ActionRunner actionRunner = new ActionRunner( actionBeanSpy, "actionUser", paramsMap, null );
    actionRunner.call();
    Mockito.verify( actionBeanSpy ).execute();

    // Verify that, by default the isExecutionSuccessful returns true
    assertTrue( actionBeanSpy.isExecutionSuccessful() );
  }


  @Test
  @Ignore
  public void testCallWithStreamProvider() throws Exception {
    Map<String, Serializable> paramsMap = createMapWithUserLocale();
    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    IBackgroundExecutionStreamProvider mockStreamProvider = Mockito.mock( IBackgroundExecutionStreamProvider.class );
    InputStream mockInputStream = Mockito.mock( InputStream.class );
    OutputStream mockOutputStream = Mockito.mock( OutputStream.class );
    when( mockStreamProvider.getInputStream() ).thenReturn( mockInputStream );
    String mockOutputPath = "/someUser/someOutput";
    when( mockStreamProvider.getOutputPath() ).thenReturn( mockOutputPath );
    when( mockStreamProvider.getOutputStream() ).thenReturn( mockOutputStream );
    ISecurityHelper mockSecurityHelper = Mockito.mock( ISecurityHelper.class );
    SecurityHelper.setMockInstance( mockSecurityHelper );
    when( mockSecurityHelper.runAsUser( Mockito.anyString(), Mockito.any() ) ).thenReturn( mockOutputPath );
    PowerMockito.mockStatic( PentahoSystem.class );
    IUnifiedRepository mockRepository = Mockito.mock( IUnifiedRepository.class );
    when( PentahoSystem.get( isA( IUnifiedRepository.class.getClass() ), Mockito.any() ) )
      .thenReturn( mockRepository );
    IAuthorizationPolicy mockAuthorizationPolicy = Mockito.mock( IAuthorizationPolicy.class );
    when( PentahoSystem.get( isA( IAuthorizationPolicy.class.getClass() ), Mockito.any() ) )
      .thenReturn( mockAuthorizationPolicy );
    when( mockAuthorizationPolicy.isAllowed( SchedulerOutputPathResolver.SCHEDULER_ACTION_NAME ) ).thenReturn( true );
    String repoId = "SOME_REPO_ID";
    Map<String, Serializable> dummyMetaData = new HashMap<>();
    dummyMetaData.put( RepositoryFile.SCHEDULABLE_KEY, true );
    when( mockRepository.getFileMetadata( repoId ) ).thenReturn( dummyMetaData );
    RepositoryFile mockRepoFile = Mockito.mock( RepositoryFile.class );
    when( mockRepoFile.isFolder() ).thenReturn( true );
    when( mockRepoFile.getId() ).thenReturn( repoId );
    ActionRunner actionRunner = new ActionRunner( actionBeanSpy, "actionUser", paramsMap, mockStreamProvider );
    actionRunner.call();
    Mockito.verify( actionBeanSpy ).execute();
  }

  @Test
  @Ignore
  public void testCallWithStreamProviderAndVarargsAction() throws Exception {
    Map<String, Serializable> paramsMap = createMapWithUserLocale();
    TestVarArgsAction testVarArgsAction = new TestVarArgsAction();
    IBackgroundExecutionStreamProvider mockStreamProvider = Mockito.mock( IBackgroundExecutionStreamProvider.class );
    InputStream mockInputStream = Mockito.mock( InputStream.class );
    OutputStream mockOutputStream = Mockito.mock( OutputStream.class );
    when( mockStreamProvider.getInputStream() ).thenReturn( mockInputStream );
    String mockOutputPath = "/someUser/someOutput";
    when( mockStreamProvider.getOutputPath() ).thenReturn( mockOutputPath );
    when( mockStreamProvider.getOutputStream() ).thenReturn( mockOutputStream );
    ISecurityHelper mockSecurityHelper = Mockito.mock( ISecurityHelper.class );
    SecurityHelper.setMockInstance( mockSecurityHelper );
    when( mockSecurityHelper.runAsUser( Mockito.anyString(), Mockito.any() ) ).thenReturn( mockOutputPath );
    PowerMockito.mockStatic( PentahoSystem.class );
    IUnifiedRepository mockRepository = Mockito.mock( IUnifiedRepository.class );
    when( PentahoSystem.get( isA( IUnifiedRepository.class.getClass() ), Mockito.any() ) )
      .thenReturn( mockRepository );
    IAuthorizationPolicy mockAuthorizationPolicy = Mockito.mock( IAuthorizationPolicy.class );
    when( PentahoSystem.get( isA( IAuthorizationPolicy.class.getClass() ), Mockito.any() ) )
      .thenReturn( mockAuthorizationPolicy );
    when( mockAuthorizationPolicy.isAllowed( SchedulerOutputPathResolver.SCHEDULER_ACTION_NAME ) ).thenReturn( true );
    String repoId = "SOME_REPO_ID";
    Map<String, Serializable> dummyMetaData = new HashMap<>();
    dummyMetaData.put( RepositoryFile.SCHEDULABLE_KEY, true );
    when( mockRepository.getFileMetadata( repoId ) ).thenReturn( dummyMetaData );
    RepositoryFile mockRepoFile = Mockito.mock( RepositoryFile.class );
    when( mockRepoFile.isFolder() ).thenReturn( true );
    when( mockRepoFile.getId() ).thenReturn( repoId );
    ActionRunner actionRunner = new ActionRunner( testVarArgsAction, "actionUser", paramsMap, mockStreamProvider );
    actionRunner.call();
    assertThat( testVarArgsAction.isExecuteWasCalled(), is( true ) );
  }

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void testCallThrowsException() throws Exception {
    Map<String, Serializable> paramsMap = createMapWithUserLocale();
    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    IBackgroundExecutionStreamProvider mockStreamProvider = Mockito.mock( IBackgroundExecutionStreamProvider.class );
    when( mockStreamProvider.getInputStream() ).thenThrow( new Exception( "something went wrong" ) );
    ActionRunner actionRunner = new ActionRunner( actionBeanSpy, "actionUser", paramsMap, mockStreamProvider );
    exception.expect( ActionInvocationException.class );
    actionRunner.call();
  }

  private Map<String, Serializable> createMapWithUserLocale() {
    Map<String, Serializable> paramsMap = new HashMap<>();
    paramsMap.put( LocaleHelper.USER_LOCALE_PARAM, Locale.US );
    return paramsMap;
  }

  @Test
  @Ignore
  public void deleteFileIfEmpty() {
    PowerMockito.mockStatic( PentahoSystem.class );
    IUnifiedRepository mockRepository = Mockito.mock( IUnifiedRepository.class );
    when( PentahoSystem.get( isA( IUnifiedRepository.class.getClass() ), Mockito.any() ) )
      .thenReturn( mockRepository );

    Map<String, Serializable> paramsMap = createMapWithUserLocale();
    IAction actionBeanSpy = Mockito.spy( new TestAction() );
    ActionRunner actionRunner = new ActionRunner( actionBeanSpy, "actionUser", paramsMap, null );
    setInternalState( actionRunner, "outputFilePath", null, ActionRunner.class );
    actionRunner.deleteFileIfEmpty();

    verify( mockRepository, times( 0 ) ).getFile( anyObject() );
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
