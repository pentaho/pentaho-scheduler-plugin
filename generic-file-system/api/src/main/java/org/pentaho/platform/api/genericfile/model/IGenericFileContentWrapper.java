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

package org.pentaho.platform.api.genericfile.model;

import java.io.InputStream;

// NOTE: Designed after the class
// {@code org.pentaho.platform.web.http.api.resources.services.FileService.RepositoryFileToStreamWrapper}.
/**
 * The {@code IGenericFileContentWrapper} interface contains the necessary information for returning a
 * {@code IGenericFile}'s content.
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
   * <p>
   * For more information on MIME types, @see <a href="https://www.w3.org/wiki/WebIntents/MIME_Types">MIME Types</a>
   */
  String getMimeType();
}
