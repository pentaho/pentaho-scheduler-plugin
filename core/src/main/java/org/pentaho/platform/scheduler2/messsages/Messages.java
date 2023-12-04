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
 * Copyright (c) 2002-2023 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.scheduler2.messsages;

import org.pentaho.platform.util.StringUtil;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.platform.util.messages.MessageUtil;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Wrapper class for i18n messages.
 */
public class Messages implements Serializable {

  private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$
  private static final long serialVersionUID = 2701028757443485586L;

  private static Messages instance = new Messages();

  public static Messages getInstance() {
    return instance;
  }

  private static final Map<Locale, ResourceBundle> locales = Collections
    .synchronizedMap( new HashMap<Locale, ResourceBundle>() );

  /*
   * NOTE: Do not extend pentaho-platform-extension's org.pentaho.platform.util.messages.MessagesBase.
   * With current Pentaho's plugin, Message.properties files will not be found by ResourceBundle#getBundle.
   * Most likely when deployed, pentaho-platform-extension and other plugin's jar are in different folders.
   */
  private static ResourceBundle getBundle() {
    Locale locale = LocaleHelper.getLocale();
    ResourceBundle bundle = Messages.locales.get( locale );
    if ( bundle == null ) {
      bundle = ResourceBundle.getBundle( Messages.BUNDLE_NAME, locale );
      Messages.locales.put( locale, bundle );
    }
    return bundle;
  }

  public static String getString( final String key ) {
    try {
      return Messages.getBundle().getString( key );
    } catch ( MissingResourceException e ) {
      return '!' + key + '!';
    }
  }

  public String getString( final String key, final String param1 ) {
    return MessageUtil.getString( Messages.getBundle(), key, param1 );
  }

  public String getString( final String key, final String param1, final String param2 ) {
    return MessageUtil.getString( Messages.getBundle(), key, param1, param2 );
  }

  public String getString( final String key, final String param1, final String param2, final String param3 ) {
    return MessageUtil.getString( Messages.getBundle(), key, param1, param2, param3 );
  }

  public String getString( final String key, final String param1, final String param2, final String param3,
                           final String param4 ) {
    return MessageUtil.getString( Messages.getBundle(), key, param1, param2, param3, param4 );
  }

  public  String getErrorString( final String key ) {
    return MessageUtil.formatErrorMessage( key, Messages.getString( key ) );
  }

  public String getErrorString( final String key, final String param1 ) {
    return MessageUtil.getErrorString( Messages.getBundle(), key, param1 );
  }

  public String getErrorString( final String key, final String param1, final String param2 ) {
    return MessageUtil.getErrorString( Messages.getBundle(), key, param1, param2 );
  }

  public String getErrorString( final String key, final String param1, final String param2, final String param3 ) {
    return MessageUtil.getErrorString( Messages.getBundle(), key, param1, param2, param3 );
  }

  public String getRunningInBackgroundLocally( final String actionIdentifier, final Map params ) {
    return getString( "ActionInvoker.INFO_0001_RUNNING_IN_BG_LOCALLY", actionIdentifier,
      StringUtil.getMapAsPrettyString( params ) );
  }

  public String getUnsupportedAction( final String action ) {
    return getErrorString( "ActionInvoker.ERROR_0006_ACTION_NULL", action );
  }

  public String getCantInvokeNullAction() {
    return getErrorString( "ActionInvoker.ERROR_0005_ACTION_NULL" );
  }

  public String getActionFailedToExecute( final String actionIdentifier ) {
    return getErrorString( "ActionInvoker.ERROR_0004_ACTION_FAILED", actionIdentifier );
  }

  public String getSkipRemovingOutputFile( final String fileName ) {
    return getString( "ActionInvoker.WARN_0001_SKIP_REMOVING_OUTPUT_FILE", fileName );
  }

  public String getCannotGetRepoFile( final String fileName, final String msg ) {
    return getErrorString( "ActionInvoker.ERROR_0010_CANNOT_GET_REPO_FILE", fileName, msg );
  }

  public String getMapNullCantReturnSp() {
    return getErrorString( "ActionInvoker.ERROR_0008_MAP_NULL_CANT_RETURN_SP" );
  }
}
