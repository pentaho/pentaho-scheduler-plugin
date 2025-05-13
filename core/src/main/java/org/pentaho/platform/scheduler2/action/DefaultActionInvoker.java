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

package org.pentaho.platform.scheduler2.action;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.action.ActionInvokeStatus;
import org.pentaho.platform.api.action.ActionInvocationException;
import org.pentaho.platform.api.action.IAction;
import org.pentaho.platform.api.action.IActionInvokeStatus;
import org.pentaho.platform.api.action.IActionInvoker;
import org.pentaho.platform.api.scheduler2.IBackgroundExecutionStreamProvider;
import org.pentaho.platform.api.scheduler2.IScheduler;
import org.pentaho.platform.api.scheduler2.PentahoUserSync;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.scheduler2.messsages.Messages;
import org.pentaho.platform.util.ActionUtil;
import org.pentaho.platform.util.StringUtil;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.platform.workitem.WorkItemLifecycleEventUtil;
import org.pentaho.platform.workitem.WorkItemLifecyclePhase;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * A concrete implementation of the {@link IActionInvoker} interface that invokes the {@link IAction} locally.
 */
public class DefaultActionInvoker implements IActionInvoker {

  private static final Log logger = LogFactory.getLog( DefaultActionInvoker.class );

  /**
   * Gets the stream provider from the {@code RESERVEDMAPKEY_STREAMPROVIDER} key within the {@code params} {@link Map}.
   *
   * @param params the {@link Map} or parameters needed to invoke the {@link IAction}
   * @return a {@link IBackgroundExecutionStreamProvider} represented in the {@code params} {@link Map}
   */
  protected IBackgroundExecutionStreamProvider getStreamProvider( final Map<String, Object> params ) {
    if ( params == null ) {
      logger.warn( Messages.getInstance().getMapNullCantReturnSp() );
      return null;
    }

    final Object obj = params.get( IScheduler.RESERVEDMAPKEY_STREAMPROVIDER );
    return ( obj instanceof IBackgroundExecutionStreamProvider ) ? (IBackgroundExecutionStreamProvider) obj : null;
  }

  /**
   *
   * Validates that the conditions required for the {@link IAction} to be invoked are true, throwing an
   * {@link ActionInvocationException}, if the conditions are not met.
   *
   * @param actionBean The {@link IAction} to be invoked
   * @param actionUser The user invoking the {@link IAction}
   * @param params     the {@link Map} or parameters needed to invoke the {@link IAction}
   * @return the {@link IActionInvokeStatus} object containing information about the action invocation
   * @throws ActionInvocationException when conditions needed to invoke the {@link IAction} are not met
   */
  public void validate( final IAction actionBean, final String actionUser,
                        final Map<String, Object> params ) throws ActionInvocationException {

    final String workItemName = ActionUtil.extractName( params );

    if ( actionBean == null || params == null ) {
      final String failureMessage = Messages.getInstance().getCantInvokeNullAction();
      WorkItemLifecycleEventUtil.publish( workItemName, params, WorkItemLifecyclePhase.FAILED,  failureMessage );
      throw new ActionInvocationException( failureMessage );
    }

    if ( !isSupportedAction( actionBean ) ) {
      final String failureMessage = Messages.getInstance().getUnsupportedAction( actionBean.getClass().getName() );
      WorkItemLifecycleEventUtil.publish( workItemName, params, WorkItemLifecyclePhase.FAILED, failureMessage );
      throw new ActionInvocationException( failureMessage );
    }
  }

  /**
   * Invokes the provided {@link IAction} as the provided {@code actionUser}.
   *
   * @param actionBean the {@link IAction} being invoked
   * @param actionUser The user invoking the {@link IAction}
   * @param params     the {@link Map} or parameters needed to invoke the {@link IAction}
   * @return the {@link IActionInvokeStatus} object containing information about the action invocation
   * @throws Exception when the {@code IAction} cannot be invoked for some reason.
   */
  @Override
  public IActionInvokeStatus invokeAction( final IAction actionBean,
                                           final String actionUser,
                                           final Map<String, Object> params ) throws Exception {
    validate( actionBean, actionUser, params );
    return invokeActionImpl( actionBean, actionUser, params );
  }

  /**
   * Invokes the provided {@link IAction} as the provided {@code actionUser}.
   *
   * @param actionBean the {@link IAction} being invoked
   * @param actionUser The user invoking the {@link IAction}
   * @param params     the {@link Map} or parameters needed to invoke the {@link IAction}
   * @return the {@link IActionInvokeStatus} object containing information about the action invocation
   * @throws Exception when the {@code IAction} cannot be invoked for some reason.
   */
  protected IActionInvokeStatus invokeActionImpl( final IAction actionBean,
                                           final String actionUser,
                                           final Map<String, Object> params ) throws Exception {

    final String workItemName = ActionUtil.extractName( params );

    if ( actionBean == null || params == null ) {
      final String failureMessage = Messages.getInstance().getCantInvokeNullAction();
      WorkItemLifecycleEventUtil.publish( workItemName, params, WorkItemLifecyclePhase.FAILED, failureMessage );
      throw new ActionInvocationException( failureMessage );
    }

    WorkItemLifecycleEventUtil.publish( workItemName, params, WorkItemLifecyclePhase.IN_PROGRESS );

    if ( logger.isDebugEnabled() ) {
      logger.debug( Messages.getInstance().getRunningInBackgroundLocally( actionBean.getClass().getName(), params ) );
    }

    // set the locale, if not already set
    if ( params.get( LocaleHelper.USER_LOCALE_PARAM ) == null || StringUtils.isEmpty(
      params.get( LocaleHelper.USER_LOCALE_PARAM ).toString() ) ) {
      params.put( LocaleHelper.USER_LOCALE_PARAM, LocaleHelper.getLocale() );
    }

    // remove the scheduling infrastructure properties
    ActionUtil.removeKeyFromMap( params, ActionUtil.INVOKER_ACTIONCLASS );
    ActionUtil.removeKeyFromMap( params, ActionUtil.INVOKER_ACTIONID );
    ActionUtil.removeKeyFromMap( params, ActionUtil.INVOKER_ACTIONUSER );
    // build the stream provider
    final IBackgroundExecutionStreamProvider streamProvider = getStreamProvider( params );
    ActionUtil.removeKeyFromMap( params, ActionUtil.INVOKER_STREAMPROVIDER );
    ActionUtil.removeKeyFromMap( params, ActionUtil.INVOKER_UIPASSPARAM );

    final IActionRunner actionBeanRunner = PentahoSystem.get( IActionRunner.class );
    actionBeanRunner.setAction( actionBean );
    actionBeanRunner.setActionUser( actionUser );
    actionBeanRunner.setParams( params );
    actionBeanRunner.setStreamProvider( streamProvider );

    final IActionInvokeStatus status = new ActionInvokeStatus();
    status.setStreamProvider( streamProvider );

    boolean requiresUpdate = false;
    try {
      if ( ( StringUtil.isEmpty( actionUser ) ) || ( actionUser.equals( "system session" ) ) ) { //$NON-NLS-1$
        // For now, don't try to run quartz jobs as authenticated if the user
        // that created the job is a system user. See PPP-2350
        requiresUpdate = SecurityHelper.getInstance().runAsAnonymous( actionBeanRunner );
      } else {
        final PentahoUserSync pentahoUserSync = PentahoSystem.get( PentahoUserSync.class, "enterprisePentahoIdpHandler",
          PentahoSessionHolder.getSession() );
        if ( Objects.nonNull( pentahoUserSync ) ) {
          final String username = StringUtils.defaultIfEmpty( PentahoSystem.get( String.class
            , "singleTenantAdminUserName", null ), "admin" );
          SecurityHelper.getInstance().runAsUser( username, new Callable<Object>() {
            @Override public Void call() throws Exception {
              pentahoUserSync.updateRolesForUser( actionUser );
              return null;
            }
          } );
        }

        requiresUpdate = SecurityHelper.getInstance().runAsUser( actionUser, actionBeanRunner );
      }
    } catch ( final Throwable t ) {
      WorkItemLifecycleEventUtil.publish( workItemName, params, WorkItemLifecyclePhase.FAILED, t.toString() );
      status.setThrowable( t );
    }
    status.setRequiresUpdate( requiresUpdate );
    // Set the execution Status
    status.setExecutionStatus( actionBean.isExecutionSuccessful() );
    return status;
  }

  @Override
  public boolean isSupportedAction( IAction action ) {
    return true; // supports all
  }
}
