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

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.api.engine.PluginBeanException;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.UnifiedRepositoryException;
import org.pentaho.platform.api.scheduler2.CronJobTrigger;
import org.pentaho.platform.api.scheduler2.IComplexJobTrigger;
import org.pentaho.platform.api.scheduler2.IJobTrigger;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.api.scheduler2.ISimpleJobTrigger;
import org.pentaho.platform.api.scheduler2.IActionClassResolver;
import org.pentaho.platform.api.util.IPdiContentProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.exporter.ScheduleExportUtil;
import org.pentaho.platform.repository.RepositoryFilenameUtils;
import org.pentaho.platform.scheduler2.action.SchedulerHelper;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek.DayOfWeek;
import org.pentaho.platform.scheduler2.recur.QualifiedDayOfWeek.DayOfWeekQualifier;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import static com.cronutils.model.field.expression.FieldExpressionFactory.always;
import static com.cronutils.model.field.expression.FieldExpressionFactory.every;
import static com.cronutils.model.field.expression.FieldExpressionFactory.on;
import static com.cronutils.model.field.expression.FieldExpressionFactory.questionMark;

public class SchedulerResourceUtil {

  private static final Log logger = LogFactory.getLog( SchedulerResourceUtil.class );

  public static final String RESERVEDMAPKEY_LINEAGE_ID = "lineage-id";
  public static final String RESERVED_BACKGROUND_EXECUTION_ACTION_ID = ".backgroundExecution";
  //$NON-NLS-1$ //$NON-NLS-2$

  public static IJobTrigger convertScheduleRequestToJobTrigger( JobScheduleRequest scheduleRequest,
                                                                IScheduler scheduler )
    throws SchedulerException, UnifiedRepositoryException {

    // Used to determine if created by a RunInBackgroundCommand
    boolean runInBackground =
      scheduleRequest.getSimpleJobTrigger() == null && scheduleRequest.getComplexJobTrigger() == null
        && scheduleRequest.getCronJobTrigger() == null;

    // add 10 seconds to the RIB to ensure execution (see PPP-3264)
    IJobTrigger jobTrigger =
      runInBackground ?
        scheduler.createSimpleJobTrigger( new Date( System.currentTimeMillis() + 10000 ), null, 0, 0 )
        : scheduleRequest.getSimpleJobTrigger();

    if ( runInBackground ) {
      jobTrigger.setUiPassParam( "RUN_ONCE" );
    }

    if ( scheduleRequest.getSimpleJobTrigger() != null ) {
      ISimpleJobTrigger simpleJobTrigger = scheduleRequest.getSimpleJobTrigger();

      if ( simpleJobTrigger.getStartTime() == null ) {
        simpleJobTrigger.setStartTime( new Date() );
      }

      simpleJobTrigger.setTimeZone( scheduleRequest.getTimeZone() );

      jobTrigger = simpleJobTrigger;

    } else if ( scheduleRequest.getComplexJobTrigger() != null ) {

      ComplexJobTriggerProxy proxyTrigger = scheduleRequest.getComplexJobTrigger();
      String cronString = proxyTrigger.getCronString();
      IComplexJobTrigger complexJobTrigger;
      /**
       * We will have two options. Either it is a daily scehdule to ignore DST or any other
       * complex schedule
       */
      if ( cronString != null && cronString.equals( "TO_BE_GENERATED" ) ) {
        cronString = generateCronString( (int) proxyTrigger.getRepeatInterval() / 86400
          , proxyTrigger.getStartHour(), proxyTrigger.getStartMin(), proxyTrigger.getStartYear(), proxyTrigger.getStartMonth(), proxyTrigger.getStartDay() );
        complexJobTrigger = scheduler.createComplexTrigger( cronString );
      } else {
        complexJobTrigger = scheduler.createComplexJobTrigger();
        if ( proxyTrigger.getDaysOfWeek().length > 0 ) {
          if ( proxyTrigger.getWeeksOfMonth().length > 0 ) {
            for ( int dayOfWeek : proxyTrigger.getDaysOfWeek() ) {
              for ( int weekOfMonth : proxyTrigger.getWeeksOfMonth() ) {

                QualifiedDayOfWeek qualifiedDayOfWeek = new QualifiedDayOfWeek();
                qualifiedDayOfWeek.setDayOfWeek( DayOfWeek.values()[ dayOfWeek ] );

                if ( weekOfMonth == JobScheduleRequest.LAST_WEEK_OF_MONTH ) {
                  qualifiedDayOfWeek.setQualifier( DayOfWeekQualifier.LAST );
                } else {
                  qualifiedDayOfWeek.setQualifier( DayOfWeekQualifier.values()[ weekOfMonth ] );
                }
                complexJobTrigger.addDayOfWeekRecurrence( qualifiedDayOfWeek );
              }
            }
          } else {
            for ( int dayOfWeek : proxyTrigger.getDaysOfWeek() ) {
              complexJobTrigger.addDayOfWeekRecurrence( dayOfWeek + 1 );
            }
          }
        } else {
          proxyTrigger.getDaysOfMonth();
          for ( int dayOfMonth : proxyTrigger.getDaysOfMonth() ) {
            complexJobTrigger.addDayOfMonthRecurrence( dayOfMonth );
          }
        }

        for ( int month : proxyTrigger.getMonthsOfYear() ) {
          complexJobTrigger.addMonthlyRecurrence( month + 1 );
        }

        for ( int year : proxyTrigger.getYears() ) {
          complexJobTrigger.addYearlyRecurrence( year );
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set( proxyTrigger.getStartYear(), proxyTrigger.getStartMonth(), proxyTrigger.getStartDay(), proxyTrigger.getStartHour(), proxyTrigger.getStartMin(), 0 );
        complexJobTrigger.setHourlyRecurrence( calendar.get( Calendar.HOUR_OF_DAY ) );
        complexJobTrigger.addMinuteRecurrence( calendar.get( Calendar.MINUTE ) );
      }

      complexJobTrigger.setStartHour( proxyTrigger.getStartHour() );
      complexJobTrigger.setStartMin( proxyTrigger.getStartMin() );
      complexJobTrigger.setStartYear( proxyTrigger.getStartYear() );
      complexJobTrigger.setStartMonth( proxyTrigger.getStartMonth() );
      complexJobTrigger.setStartDay( proxyTrigger.getStartDay() );
      complexJobTrigger.setTimeZone( scheduleRequest.getTimeZone() );

      complexJobTrigger.setEndTime( proxyTrigger.getEndTime() );
      complexJobTrigger.setDuration( scheduleRequest.getDuration() );
      complexJobTrigger.setUiPassParam( scheduleRequest.getComplexJobTrigger().getUiPassParam() );
      jobTrigger = complexJobTrigger;

    } else if ( scheduleRequest.getCronJobTrigger() != null ) {

      if ( scheduler instanceof IScheduler ) {
        CronJobTrigger proxyTrigger = scheduleRequest.getCronJobTrigger();
        String cronString = proxyTrigger.getCronString();
        String delims = "[ ]+"; //$NON-NLS-1$
        String[] tokens = cronString.split( delims );
        if ( tokens.length < 7 ) {
          cronString += " *";
        }
        IComplexJobTrigger complexJobTrigger = scheduler.createComplexTrigger( cronString );
        complexJobTrigger.setStartHour( proxyTrigger.getStartHour() );
        complexJobTrigger.setStartMin( proxyTrigger.getStartMin() );
        complexJobTrigger.setStartYear( proxyTrigger.getStartYear() );
        complexJobTrigger.setStartMonth( proxyTrigger.getStartMonth() );
        complexJobTrigger.setStartDay( proxyTrigger.getStartDay() );
        complexJobTrigger.setTimeZone( scheduleRequest.getTimeZone() );

        complexJobTrigger.setStartTime( proxyTrigger.getStartTime() );
        complexJobTrigger.setEndTime( proxyTrigger.getEndTime() );
        complexJobTrigger.setDuration( proxyTrigger.getDuration() );
        complexJobTrigger.setUiPassParam( proxyTrigger.getUiPassParam() );
        jobTrigger = complexJobTrigger;
      } else {
        throw new IllegalArgumentException();
      }
    }

    return jobTrigger;
  }

  public static HashMap<String, Object> handlePDIScheduling( String fileName, String path,
                                                                   HashMap<String, Object> parameterMap,
                                                                   Map<String, String> pdiParameters ) {

    HashMap<String, Object> convertedParameterMap = new HashMap<>();
    IPdiContentProvider provider = null;
    Map<String, String> kettleParams = new HashMap<>();
    Map<String, String> kettleVars = new HashMap<>();
    Map<String, String> scheduleKettleVars = new HashMap<>();
    boolean fallbackToOldBehavior = false;
    try {
      provider = getiPdiContentProvider();
      kettleParams = provider.getUserParameters( path );
      kettleVars = provider.getVariables( path );
    } catch ( PluginBeanException e ) {
      logger.error( e );
      fallbackToOldBehavior = true;
    }

    boolean paramsAdded = false;
    if ( pdiParameters != null ) {
      convertedParameterMap.put( ScheduleExportUtil.RUN_PARAMETERS_KEY, (Serializable) pdiParameters );
      paramsAdded = true;
    } else {
      pdiParameters = new HashMap<String, String>();
    }

    if ( isPdiFile( fileName ) ) {

      Iterator<String> it = parameterMap.keySet().iterator();

      while ( it.hasNext() ) {

        String param = it.next();

        if ( !StringUtils.isEmpty( param ) && parameterMap.containsKey( param ) ) {
          convertedParameterMap.put( param, parameterMap.get( param ).toString() );
          if ( !paramsAdded && ( fallbackToOldBehavior || kettleParams.containsKey( param ) ) ) {
            pdiParameters.put( param, parameterMap.get( param ).toString() );
          }
          if ( kettleVars.containsKey( param ) ) {
            scheduleKettleVars.put( param, parameterMap.get( param ).toString() );
          }
        }
      }

      convertedParameterMap.put( "directory", FilenameUtils.getPathNoEndSeparator( path ) );
      String type = isTransformation( fileName ) ? "transformation" : "job";
      convertedParameterMap.put( type, FilenameUtils.getBaseName( path ) );

    } else {
      convertedParameterMap.putAll( parameterMap );
    }
    convertedParameterMap.putIfAbsent( ScheduleExportUtil.RUN_PARAMETERS_KEY, (Serializable) pdiParameters );
    convertedParameterMap.putIfAbsent( "variables", (Serializable) scheduleKettleVars );
    return convertedParameterMap;
  }

  public static IPdiContentProvider getiPdiContentProvider() throws PluginBeanException {
    IPdiContentProvider provider;
    provider = (IPdiContentProvider) PentahoSystem.get( IPluginManager.class ).getBean(
      IPdiContentProvider.class.getSimpleName() );
    return provider;
  }

  public static String getHideInternalVariable(){
    IPdiContentProvider provider = null;
    String hideInternalVariable = null;
    try {
      SchedulerHelper helper = new SchedulerHelper();
      hideInternalVariable = helper.getHideInternalVarible();
    } catch ( Exception e ) {
      logger.error( e );
    }
    return hideInternalVariable;
  }

  @Deprecated
  public static boolean isPdiFile( RepositoryFile file ) {
    return isTransformation( file ) || isJob( file );
  }

  public static boolean isPdiFile( String fileName ) {
    return isTransformation( fileName ) || isJob( fileName );
  }

  @Deprecated
  public static boolean isTransformation( RepositoryFile file ) {
    return file != null && isTransformation( file.getName() );
  }

  public static boolean isTransformation( String fileName ) {
    return "ktr".equalsIgnoreCase( FilenameUtils.getExtension( fileName ) );
  }

  @Deprecated
  public static boolean isJob( RepositoryFile file ) {
    return file != null && isJob( file.getName() );
  }

  public static boolean isJob( String fileName ) {
    return "kjb".equalsIgnoreCase( FilenameUtils.getExtension( fileName ) );
  }

  public static String resolveActionIdFromClass( final String actionClass ) {
    IActionClassResolver actionClassResolver = PentahoSystem.get( IActionClassResolver.class );
    if ( !StringUtils.isEmpty( actionClass ) && actionClassResolver != null ) {
      return actionClassResolver.resolve( actionClass );
    }
    return null;
  }
  public static String resolveActionId( final String inputFile ) {
    // unchanged logic, ported over from its original location ( SchedulerService ) into this SchedulerUtil class
    if ( !StringUtils.isEmpty( inputFile ) && !StringUtils.isEmpty( getExtension( inputFile ) ) ) {
      return getExtension( inputFile ) + RESERVED_BACKGROUND_EXECUTION_ACTION_ID;
    }
    return null;
  }

  public static String getExtension( final String filename ) {
    // unchanged logic, ported over from its original location ( SchedulerService ) into this SchedulerUtil class
    return RepositoryFilenameUtils.getExtension( filename );
  }

  private static String generateCronString( long interval, int startHour, int startMin, int startYear, int startMonth, int startDay ) {
    Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
    calendar.set( startYear, startMonth, startDay, startHour, startMin, 0 );
    int hour = calendar.get( Calendar.HOUR_OF_DAY );
    int minute = calendar.get( Calendar.MINUTE );

    Cron cron = CronBuilder.cron( CronDefinitionBuilder.instanceDefinitionFor( CronType.QUARTZ ) )
      .withYear( always() )
      .withDoM( every( (int) interval ) )
      .withMonth( always() )
      .withDoW( questionMark() )
      .withHour( on( hour ) )
      .withMinute( on( minute ) )
      .withSecond( on( 0 ) ).instance();
    return cron.asString();
  }
}
