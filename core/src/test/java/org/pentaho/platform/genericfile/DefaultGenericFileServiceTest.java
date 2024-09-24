/*!
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
 * Copyright (c) 2024 Hitachi Vantara. All rights reserved.
 */

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

  // region getTree()
  private static class MultipleProviderUseCase {
    public final IGenericFileProvider<?> provider1Mock;
    public final IGenericFileTree tree1Mock;
    public final IGenericFileProvider<?> provider2Mock;
    public final IGenericFileTree tree2Mock;
    public final DefaultGenericFileService service;
    public final GetTreeOptions optionsMock;

    public MultipleProviderUseCase() throws OperationFailedException, InvalidGenericFileProviderException {
      provider1Mock = mock( IGenericFileProvider.class );
      tree1Mock = mock( IGenericFileTree.class );
      doReturn( tree1Mock ).when( provider1Mock ).getTree( any( GetTreeOptions.class ) );

      provider2Mock = mock( IGenericFileProvider.class );
      tree2Mock = mock( IGenericFileTree.class );
      doReturn( tree2Mock ).when( provider2Mock ).getTree( any( GetTreeOptions.class ) );

      service = new DefaultGenericFileService( Arrays.asList( provider1Mock, provider2Mock ) );

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

    MultipleProviderUseCase useCase = new MultipleProviderUseCase();

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

    MultipleProviderUseCase useCase = new MultipleProviderUseCase();

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

    MultipleProviderUseCase useCase = new MultipleProviderUseCase();

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

    MultipleProviderUseCase useCase = new MultipleProviderUseCase();

    doReturn( false ).when( useCase.provider1Mock ).owns( any( GenericFilePath.class ) );
    doReturn( false ).when( useCase.provider2Mock ).owns( any( GenericFilePath.class ) );

    doReturn( mock( GenericFilePath.class ) ).when( useCase.optionsMock ).getBasePath();

    useCase.service.getTree( useCase.optionsMock );
  }

  @Test
  public void testGetTreeWithMultipleProvidersAndKnownProviderBasePathReturnsProviderSubtree()
    throws OperationFailedException, InvalidGenericFileProviderException {

    MultipleProviderUseCase useCase = new MultipleProviderUseCase();

    doReturn( false ).when( useCase.provider1Mock ).owns( any( GenericFilePath.class ) );
    doReturn( true ).when( useCase.provider2Mock ).owns( any( GenericFilePath.class ) );

    doReturn( mock( GenericFilePath.class ) ).when( useCase.optionsMock ).getBasePath();

    IGenericFileTree resultTree = useCase.service.getTree( useCase.optionsMock );

    assertSame( useCase.tree2Mock, resultTree );
    verify( useCase.provider2Mock, times( 1 ) ).getTree( useCase.optionsMock );
    verify( useCase.provider1Mock, never() ).getTree( useCase.optionsMock );
  }
  // endregion
}
