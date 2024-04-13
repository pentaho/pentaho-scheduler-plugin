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

import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.Test;
import org.pentaho.platform.api.genericfile.GenericFilePath;
import org.pentaho.platform.api.genericfile.GetTreeOptions;
import org.pentaho.platform.api.genericfile.model.IGenericFile;
import org.pentaho.platform.genericfile.model.BaseGenericFileTree;

/**
 * Tests for the {@link BaseGenericFileProvider} class.
 */
public class BaseGenericFileProviderTest {
  static class GenericFileProviderForTesting<T extends IGenericFile> extends BaseGenericFileProvider<T> {
    @Override
    protected boolean createFolderCore( @NonNull GenericFilePath path ) {
      throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    protected BaseGenericFileTree getFileTreeCore( @NonNull GetTreeOptions options ) {
      throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public Class<T> getFileClass() {
      throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public String getName() {
      throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public String getType() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean doesFileExist( @NonNull GenericFilePath path ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean owns( @NonNull GenericFilePath path ) {
      throw new UnsupportedOperationException();
    }
  }

  // region getFolderTree
  @Test
  public void testGetFolderTree() {

  }
  // endregion

  // region clearFolderCache
  // endregion
}
