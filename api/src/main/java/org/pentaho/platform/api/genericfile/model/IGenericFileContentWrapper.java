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
