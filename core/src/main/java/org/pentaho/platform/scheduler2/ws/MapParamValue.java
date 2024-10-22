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


package org.pentaho.platform.scheduler2.ws;

import java.util.HashMap;
import jakarta.xml.bind.annotation.XmlRootElement;

@SuppressWarnings( "serial" )
@XmlRootElement
public class MapParamValue extends HashMap<String, String> implements ParamValue {
}
