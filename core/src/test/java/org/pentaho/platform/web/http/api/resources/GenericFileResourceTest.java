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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.pentaho.platform.api.genericfile.IGenericFileService;

import static org.mockito.Mockito.mock;

public class GenericFileResourceTest {

  GenericFileResource genericFileResource;

  private static final String ENCODED_SAMPLE_PATH = ":home:Ab`\t!@#$%^&()_+{}<>?'=-yZ~";
  private static final String EXPECTED_DECODED_SAMPLE_PATH = "/home/Ab`~!@#$%^&()_+{}<>?'=-yZ:";

  @Before
  public void setUp() throws Exception {
    genericFileResource = new GenericFileResource( mock( IGenericFileService.class ) );
  }

  @Test
  public void testDecodePath() {
    Assert.assertEquals( "Unexpected path decoding:", EXPECTED_DECODED_SAMPLE_PATH,
      genericFileResource.decodePath( ENCODED_SAMPLE_PATH ) );
  }
}
