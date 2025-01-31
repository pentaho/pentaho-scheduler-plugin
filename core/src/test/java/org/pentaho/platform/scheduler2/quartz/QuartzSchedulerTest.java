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



package org.pentaho.platform.scheduler2.quartz;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.scheduler2.Job;
import org.pentaho.platform.api.scheduler2.SchedulerException;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

public class QuartzSchedulerTest {


  private static IUnifiedRepository repo;
  private static IUnifiedRepository oldRepo;


  @BeforeClass
  public static void setUp() throws Exception {

    oldRepo = PentahoSystem.get( IUnifiedRepository.class );
    repo = Mockito.mock( IUnifiedRepository.class );
    Mockito.when( repo.getFile( Mockito.anyString() ) ).then( new Answer<Object>() {
      @Override public Object answer( InvocationOnMock invocationOnMock ) throws Throwable {
        final RepositoryFile repositoryFile = Mockito.mock( RepositoryFile.class );
        final String param = (String) invocationOnMock.getArguments()[ 0 ];
        if ( "/home/admin/notexist.ktr".equals( param ) ) {
          return null;
        }
        if ( "/home/admin".equals( param ) ) {
          Mockito.when( repositoryFile.isFolder() ).thenReturn( true );
        }
        if ( "/home/admin/notallowed.ktr".equals( param ) ) {
          Mockito.when( repositoryFile.isFolder() ).thenReturn( false );
          Mockito.when( repositoryFile.isSchedulable() ).thenReturn( false );
        }
        if ( "/home/admin/allowed.ktr".equals( param ) ) {
          Mockito.when( repositoryFile.isFolder() ).thenReturn( false );
          Mockito.when( repositoryFile.isSchedulable() ).thenReturn( true );
        }
        return repositoryFile;
      }
    } );
    PentahoSystem.registerObject( repo, IUnifiedRepository.class );
  }

  @AfterClass
  public static void tearDown() throws Exception {
    repo = null;
    if ( oldRepo != null ) {
      PentahoSystem.registerObject( oldRepo, IUnifiedRepository.class );
    }
  }

  @Test
  public void testValidateParamsNoStreamProviderParam() throws SchedulerException {
    new QuartzScheduler().validateJobParams( Collections.emptyMap() );
  }

  @Test
  public void testValidateParamsNoStringConf() throws SchedulerException {
    new QuartzScheduler()
      .validateJobParams( Collections.singletonMap( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER, 1L ) );
  }

  @Test
  public void testValidateParamsNoInputFile() throws SchedulerException {
    new QuartzScheduler()
      .validateJobParams( Collections.singletonMap( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER, "someinputfile" ) );
  }

  @Test( expected = SchedulerException.class )
  public void testValidateParamsFileNotFound() throws SchedulerException {
    new QuartzScheduler()
      .validateJobParams( Collections.singletonMap( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER,
        "input = /home/admin/notexist.ktr : output = /home/admin/notexist" ) );
  }

  @Test( expected = SchedulerException.class )
  public void testValidateParamsFileIsFolder() throws SchedulerException {
    new QuartzScheduler()
      .validateJobParams( Collections.singletonMap( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER,
        "input = /home/admin : output = /home/admin/notexist" ) );
  }

  @Test( expected = SchedulerException.class )
  public void testValidateParamsSchedulingNotAllowed() throws SchedulerException {
    new QuartzScheduler()
      .validateJobParams( Collections.singletonMap( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER,
        "input = /home/admin/notallowed.ktr : output = /home/admin/notallowed" ) );
  }

  @Test
  public void testValidateParamsSchedulingAllowed() throws SchedulerException {
    new QuartzScheduler()
      .validateJobParams( Collections.singletonMap( QuartzScheduler.RESERVEDMAPKEY_STREAMPROVIDER,
        "input = /home/admin/allowed.ktr : output = /home/admin/allowed." ) );
  }

  @Test
  public void testSetTimezone() throws Exception {
    CronTrigger cronTrigger = TriggerBuilder.newTrigger()
      .withSchedule( CronScheduleBuilder.cronSchedule( new CronExpression( "0 15 10 ? * 6L 2002-2018" ) ) ).build();
    String currentTimezoneId = TimeZone.getDefault().getID();

    new QuartzScheduler().setTimezone( cronTrigger, currentTimezoneId );

    assertNotNull( cronTrigger.getTimeZone() );
    assertEquals( currentTimezoneId, cronTrigger.getTimeZone().getID() );
  }

  @Test
  @Ignore
  public void testSetJobNextRunToTheFuture() {

    Trigger trigger = Mockito.mock( Trigger.class );
    Job job = new Job();
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    long nowDate = new Date().getTime();
    long futureDate = nowDate+1000000000;

    Mockito.when( trigger.getNextFireTime() ).thenReturn( new Date( futureDate ) );
    Mockito.when( trigger.getFireTimeAfter( any() ) ).thenReturn( new Date( nowDate ) );

    quartzScheduler.setJobNextRun( job, trigger );

    assertEquals( new Date( futureDate ), job.getNextRun() );
  }

  @Test
  @Ignore
  public void testSetJobNextRunToThePast() {

    Trigger trigger = Mockito.mock( Trigger.class );
    Job job = new Job();
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    long nowDate = new Date().getTime();
    long pastDate = nowDate-1000000000;

    Mockito.when( trigger.getNextFireTime() ).thenReturn( new Date( pastDate ) );
    Mockito.when( trigger.getFireTimeAfter( any() ) ).thenReturn( new Date( nowDate ) );

    quartzScheduler.setJobNextRun( job, trigger );

    assertEquals( new Date( nowDate ), job.getNextRun() );
  }

  @Test
  @Ignore
  public void testSetJobNextRunToNullDate() {

    Trigger trigger = Mockito.mock( Trigger.class );
    Job job = new Job();
    QuartzScheduler quartzScheduler = new QuartzScheduler();
    long nowDate = new Date().getTime();

    Mockito.when( trigger.getNextFireTime() ).thenReturn( null );
    Mockito.when( trigger.getFireTimeAfter( any() ) ).thenReturn( new Date( nowDate ) );

    quartzScheduler.setJobNextRun( job, trigger );

    assertEquals( null,  job.getNextRun() );
  }

}
