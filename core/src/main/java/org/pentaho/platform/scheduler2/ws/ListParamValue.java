/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


package org.pentaho.platform.scheduler2.ws;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings( "serial" )
@XmlRootElement
public class ListParamValue extends ArrayList<String> implements ParamValue {
}
