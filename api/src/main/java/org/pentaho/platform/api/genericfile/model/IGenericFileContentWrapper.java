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
 * Copyright (c) 2024 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.api.genericfile.model;

import java.io.InputStream;


/**
 * The {@code IGenericFileContentWrapper} interface contains the necessary information for returning a {@code IGenericFile}'s content.
 *
 * It is a Generic implementation of the existing org.pentaho.platform.web.http.api.resources.services.FileService inner-class RepositoryFileToStreamWrapper.
 */
public interface IGenericFileContentWrapper {

  /**
   * Gets the file's content InputStream.
   */
  InputStream getInputStream();

  /**
   * Gets the name of the file associated with the content InputStream.
   */
  String getFileName();

  /**
   * Gets the MIME type of the file's content.
   *
   * TODO This value is
   *
   * PUC can render
   */
  String getMimeType();
}
