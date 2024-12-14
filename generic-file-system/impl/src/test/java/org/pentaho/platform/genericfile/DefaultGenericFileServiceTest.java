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

package org.pentaho.platform.genericfile;

import org.junit.Test;
import org.pentaho.platform.api.genericfile.GenericFilePath;
import org.pentaho.platform.api.genericfile.GetTreeOptions;
import org.pentaho.platform.api.genericfile.IGenericFileProvider;
import org.pentaho.platform.api.genericfile.exception.InvalidGenericFileProviderException;
import org.pentaho.platform.api.genericfile.exception.NotFoundException;
import org.pentaho.platform.api.genericfile.exception.OperationFailedException;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.api.genericfile.model.IGenericFileTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultGenericFileServiceTest {

  // region Construction Tests
  @Test( expected = InvalidGenericFileProviderException.class )
  public void testThrowsInvalidGenericFileProviderExceptionIfCreatedWithEmptyProviderList()
    throws InvalidGenericFileProviderException {
    List<IGenericFileProvider<?>> providers = new ArrayList<>();
    new DefaultGenericFileService( providers );
  }

  @Test
  public void testCanBeCreatedWithASingleProvider() throws InvalidGenericFileProviderException {
    IGenericFileProvider<?> providerMock = mock( IGenericFileProvider.class );

    DefaultGenericFileService service = new DefaultGenericFileService( Collections.singletonList( providerMock ) );

    assertTrue( service.isSingleProviderMode() );
  }

  @Test
  public void testCanBeCreatedWithTwoProviders() throws InvalidGenericFileProviderException {
    IGenericFileProvider<?> provider1Mock = mock( IGenericFileProvider.class );
    IGenericFileProvider<?> provider2Mock = mock( IGenericFileProvider.class );

    DefaultGenericFileService service = new DefaultGenericFileService( Arrays.asList( provider1Mock, provider2Mock ) );
    assertFalse( service.isSingleProviderMode() );
  }
  // endregion

  private static class MultipleProviderUseCase {
    public final IGenericFileProvider<?> provider1Mock;
    public final IGenericFileProvider<?> provider2Mock;
    public final DefaultGenericFileService service;

    public MultipleProviderUseCase() throws OperationFailedException, InvalidGenericFileProviderException {
      provider1Mock = mock( IGenericFileProvider.class );

      provider2Mock = mock( IGenericFileProvider.class );

      service = new DefaultGenericFileService( Arrays.asList( provider1Mock, provider2Mock ) );
    }
  }

  // region getTree()
  private static class GetTreeMultipleProviderUseCase extends MultipleProviderUseCase {
    public final IGenericFileTree tree1Mock;
    public final IGenericFileTree tree2Mock;
    public final GetTreeOptions optionsMock;

    public GetTreeMultipleProviderUseCase() throws OperationFailedException, InvalidGenericFileProviderException {
      tree1Mock = mock( IGenericFileTree.class );
      doReturn( tree1Mock ).when( provider1Mock ).getTree( any( GetTreeOptions.class ) );

      tree2Mock = mock( IGenericFileTree.class );
      doReturn( tree2Mock ).when( provider2Mock ).getTree( any( GetTreeOptions.class ) );

      optionsMock = mock( GetTreeOptions.class );
    }
  }

  @Test
  public void testGetTreeWithSingleProviderReturnsProviderTreeDirectly()
    throws InvalidGenericFileProviderException, OperationFailedException {

    IGenericFileProvider<?> providerMock = mock( IGenericFileProvider.class );

    IGenericFileTree treeMock = mock( IGenericFileTree.class );
    when( providerMock.getTree( any( GetTreeOptions.class ) ) ).thenReturn( treeMock );

    DefaultGenericFileService service = new DefaultGenericFileService( Collections.singletonList( providerMock ) );
    GetTreeOptions optionsMock = mock( GetTreeOptions.class );

    IGenericFileTree resultTree = service.getTree( optionsMock );

    assertEquals( treeMock, resultTree );
  }

  @Test
  public void testGetTreeWithMultipleProvidersAndNullBasePathAggregatesProviderTrees()
    throws InvalidGenericFileProviderException, OperationFailedException {

    GetTreeMultipleProviderUseCase useCase = new GetTreeMultipleProviderUseCase();

    IGenericFileTree aggregateTree = useCase.service.getTree( useCase.optionsMock );

    // ---

    assertNotNull( aggregateTree );
    assertNotSame( useCase.tree1Mock, aggregateTree );
    assertNotSame( useCase.tree2Mock, aggregateTree );

    // Test Aggregate Root File
    IGenericFile aggregateRoot = aggregateTree.getFile();
    assertNotNull( aggregateRoot );
    assertEquals( DefaultGenericFileService.MULTIPLE_PROVIDER_ROOT_NAME, aggregateRoot.getName() );
    assertEquals( DefaultGenericFileService.MULTIPLE_PROVIDER_ROOT_PROVIDER, aggregateRoot.getProvider() );
    assertTrue( aggregateRoot.isFolder() );

    // Test Aggregate Tree Children
    assertEquals( Arrays.asList( useCase.tree1Mock, useCase.tree2Mock ), aggregateTree.getChildren() );
  }

  @Test
  public void testGetTreeWithMultipleProvidersAndNullBasePathIgnoresFailedProviders()
    throws OperationFailedException, InvalidGenericFileProviderException {

    GetTreeMultipleProviderUseCase useCase = new GetTreeMultipleProviderUseCase();

    doThrow( mock( OperationFailedException.class ) )
      .when( useCase.provider1Mock )
      .getTree( any( GetTreeOptions.class ) );

    IGenericFileTree aggregateTree = useCase.service.getTree( useCase.optionsMock );

    // ---

    assertNotNull( aggregateTree );
    assertEquals( Collections.singletonList( useCase.tree2Mock ), aggregateTree.getChildren() );
  }

  @Test
  public void testGetTreeWithMultipleProvidersAndNullBasePathThrowsFirstExceptionIfAllFailed()
    throws OperationFailedException, InvalidGenericFileProviderException {

    GetTreeMultipleProviderUseCase useCase = new GetTreeMultipleProviderUseCase();

    OperationFailedException ex1 = mock( OperationFailedException.class );
    doThrow( ex1 )
      .when( useCase.provider1Mock )
      .getTree( any( GetTreeOptions.class ) );

    OperationFailedException ex2 = mock( OperationFailedException.class );
    doThrow( ex2 )
      .when( useCase.provider2Mock )
      .getTree( any( GetTreeOptions.class ) );

    try {
      useCase.service.getTree( useCase.optionsMock );
      fail();
    } catch ( OperationFailedException ex ) {
      assertSame( ex1, ex );
    }
  }

  @Test( expected = NotFoundException.class )
  public void testGetTreeWithMultipleProvidersAndUnknownProviderBasePathThrowsNotFoundException()
    throws OperationFailedException, InvalidGenericFileProviderException {

    GetTreeMultipleProviderUseCase useCase = new GetTreeMultipleProviderUseCase();

    doReturn( false ).when( useCase.provider1Mock ).owns( any( GenericFilePath.class ) );
    doReturn( false ).when( useCase.provider2Mock ).owns( any( GenericFilePath.class ) );

    doReturn( mock( GenericFilePath.class ) ).when( useCase.optionsMock ).getBasePath();

    useCase.service.getTree( useCase.optionsMock );
  }

  @Test
  public void testGetTreeWithMultipleProvidersAndKnownProviderBasePathReturnsProviderSubtree()
    throws OperationFailedException, InvalidGenericFileProviderException {

    GetTreeMultipleProviderUseCase useCase = new GetTreeMultipleProviderUseCase();

    doReturn( false ).when( useCase.provider1Mock ).owns( any( GenericFilePath.class ) );
    doReturn( true ).when( useCase.provider2Mock ).owns( any( GenericFilePath.class ) );

    doReturn( mock( GenericFilePath.class ) ).when( useCase.optionsMock ).getBasePath();

    IGenericFileTree resultTree = useCase.service.getTree( useCase.optionsMock );

    assertSame( useCase.tree2Mock, resultTree );
    verify( useCase.provider2Mock, times( 1 ) ).getTree( useCase.optionsMock );
    verify( useCase.provider1Mock, never() ).getTree( useCase.optionsMock );
  }
  // endregion

  // region getRootTrees()
  private static class GetRootTreesMultipleProviderUseCase extends MultipleProviderUseCase {
    public final IGenericFileTree tree1Mock;
    public final IGenericFileTree tree2Mock;
    public final IGenericFileTree tree3Mock;
    public final IGenericFileTree tree4Mock;

    public final GetTreeOptions optionsMock;

    public GetRootTreesMultipleProviderUseCase() throws OperationFailedException, InvalidGenericFileProviderException {
      tree1Mock = mock( IGenericFileTree.class );
      tree2Mock = mock( IGenericFileTree.class );
      doReturn( List.of( tree1Mock, tree2Mock) ).when( provider1Mock ).getRootTrees( any( GetTreeOptions.class ) );

      tree3Mock = mock( IGenericFileTree.class );
      tree4Mock = mock( IGenericFileTree.class );
      doReturn( List.of( tree3Mock, tree4Mock) ).when( provider2Mock ).getRootTrees( any( GetTreeOptions.class ) );

      optionsMock = mock( GetTreeOptions.class );
    }
  }

  @Test
  public void testGetTreeWithMultipleProvidersAggregatesAllProvidersRootTrees()
    throws InvalidGenericFileProviderException, OperationFailedException {

    GetRootTreesMultipleProviderUseCase useCase = new GetRootTreesMultipleProviderUseCase();

    List<IGenericFileTree> rootTrees = useCase.service.getRootTrees( useCase.optionsMock );

    // ---

    assertNotNull( rootTrees );
    assertEquals( 4, rootTrees.size() );
    assertSame( useCase.tree1Mock, rootTrees.get( 0 ) );
    assertSame( useCase.tree2Mock, rootTrees.get( 1 ) );
    assertSame( useCase.tree3Mock, rootTrees.get( 2 ) );
    assertSame( useCase.tree4Mock, rootTrees.get( 3 ) );
  }


  @Test
  public void testGetTreeWithMultipleProvidersIgnoresFailedProviders()
    throws InvalidGenericFileProviderException, OperationFailedException {

    GetRootTreesMultipleProviderUseCase useCase = new GetRootTreesMultipleProviderUseCase();

    doThrow( mock( OperationFailedException.class ) )
      .when( useCase.provider1Mock )
      .getRootTrees( any( GetTreeOptions.class ) );

    List<IGenericFileTree> rootTrees = useCase.service.getRootTrees( useCase.optionsMock );

    // ---

    assertNotNull( rootTrees );
    assertEquals( 2, rootTrees.size() );
    assertSame( useCase.tree3Mock, rootTrees.get( 0 ) );
    assertSame( useCase.tree4Mock, rootTrees.get( 1 ) );
  }
  // endregion

  // region getFile
  private static class GetFileMultipleProviderUseCase extends MultipleProviderUseCase {
    public final IGenericFile file1Mock;
    public final IGenericFile file2Mock;

    public GetFileMultipleProviderUseCase() throws Exception {
      file1Mock = mock( IGenericFile.class );
      doReturn( file1Mock ).when( provider1Mock ).getFile( any( GenericFilePath.class ) );

      file2Mock = mock( IGenericFile.class );
      doReturn( file2Mock ).when( provider2Mock ).getFile( any( GenericFilePath.class ) );
    }
  }

  @Test
  public void testGetFile() throws Exception {

    GetFileMultipleProviderUseCase useCase = new GetFileMultipleProviderUseCase();

    doReturn( false ).when( useCase.provider1Mock ).owns( any( GenericFilePath.class ) );
    doReturn( true ).when( useCase.provider2Mock ).owns( any( GenericFilePath.class ) );

    IGenericFile resultFile = useCase.service.getFile( mock( GenericFilePath.class ) );

    assertSame( useCase.file2Mock, resultFile );
    verify( useCase.provider2Mock, times( 1 ) ).getFile( any() );
    verify( useCase.provider1Mock, never() ).getFile( any() );
  }
  // endregion
}
