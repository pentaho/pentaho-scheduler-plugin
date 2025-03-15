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

import org.pentaho.platform.api.engine.IPluginResourceLoader;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration class for retrieving settings from the configuration file.
 * This class loads the setting from the plugin configuration based on the provided path.
 */
public class SchedulerConfig<T> {
  private static final ConcurrentHashMap<String, SchedulerConfig<?>> instances = new ConcurrentHashMap<>(  );
  private final String settingsPath;
  private T value;

  private SchedulerConfig( String settingsPath, Class<T> type, String defaultValue  ) {
    this.settingsPath = settingsPath;
    this.value = initializeFallback(  type, defaultValue  );
  }


  /**
   * Initializes the value by loading the setting from the plugin configuration.
   *
   * @param type The class type of the setting.
   * @param defaultValue The default value if the setting is not found.
   * @return The setting value.
   */
  private T initializeFallback( Class<T> type, String defaultValue ) {
    IPluginResourceLoader resourceLoader = PentahoSystem.get( IPluginResourceLoader.class, null );
    String fallbackSetting = resourceLoader.getPluginSetting( SchedulerConfig.class, settingsPath, defaultValue );
    if ( type == Boolean.class ) {
      return type.cast( Boolean.parseBoolean( fallbackSetting ) );
    } else if ( type == Integer.class ) {
      return type.cast( Integer.parseInt( fallbackSetting ) );
    } else if ( type == String.class ) {
      return type.cast( fallbackSetting );
    }
    throw new IllegalArgumentException( "Unsupported type: " + type );
  }

  /**
   * Gets the singleton instance of the SchedulerConfig.
   *
   * @param settingsPath The path to the settings.
   * @param type The class type of the setting.
   * @param defaultValue The default value if the setting is not found.
   * @return The singleton instance.
   */
  public static <T> SchedulerConfig<T> getInstance( String settingsPath, Class<T> type, String defaultValue ) {
    return (SchedulerConfig<T>) instances.computeIfAbsent( settingsPath, key -> new SchedulerConfig<>( key, type, defaultValue));
  }


  /**
   * Gets the setting value.
   *
   * @return The setting value.
   */
  public T getValue() {
    return value;
  }

  /**
   * Resets the value by reinitializing it.
   *
   * @param type The class type of the setting.
   * @param defaultValue The default value if the setting is not found.
   */
  public void resetValue( Class<T> type, String defaultValue ) {
    this.value = initializeFallback( type, defaultValue );
  }
}
