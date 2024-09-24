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

package org.pentaho.mantle.client.dialogs.scheduling;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

import org.pentaho.gwt.widgets.client.panel.VerticalFlexPanel;
import org.pentaho.gwt.widgets.client.wizards.AbstractWizardPanel;
import org.pentaho.mantle.client.messages.Messages;

import java.util.StringTokenizer;

/**
 * @author wseyler
 * 
 */
public class ScheduleParamsWizardPanel extends AbstractWizardPanel {

  private static final String PENTAHO_SCHEDULE = "pentaho-schedule-create"; //$NON-NLS-1$

  private static final String SCHEDULE_PARAMS_WIZARD_PANEL = "schedule-params-wizard-panel";

  boolean parametersComplete = true;
  SimplePanel scheduleParameterPanel = new SimplePanel();
  Label scheduleDescription = new Label();
  Frame parametersFrame;
  String scheduledFilePath;

  public ScheduleParamsWizardPanel( String scheduledFile ) {
    super();
    scheduledFilePath = scheduledFile;
    layout();
    ScheduleParamsWizardPanel thisInstance = this;
    registerSchedulingCallbacks( thisInstance );
    scheduleDescription.setText( Messages.getString( "scheduleWillRun" ) );
  }

  public JsArray<JsSchedulingParameter> getParams() {
    return getParams( false );
  }

  /**
   * 
   * @param suppressAlerts
   *          Added so that getParams can be executed for a terminated dialog and still collect any values entered.
   *          (eg: set to true when the "back" button clicked so that parameter values can be collected but not
   *          throw up alerts if mandatory fields are left blank.
   * @return
   */
  public native JsArray<JsSchedulingParameter> getParams( boolean suppressAlerts ) /*-{
    var params = $doc.getElementById('schedulerParamsFrame').contentWindow.getParams(suppressAlerts);
    var paramEntries = new Array();
    for (var key in params) {
      var type = null;
      var value = new Array();
      if (Object.prototype.toString.apply(params[key]) === '[object Array]') {
        var theArray = params[key];
        if (theArray.length > 0) {
           for(var i=0; i < theArray.length; i++) {
            if (typeof theArray[i] == 'number') {
              if (type == null) {
                type = "number[]";
              }
              value.push('' + theArray[i]);
            } else if (typeof theArray[i] == 'boolean') {
              if (type == null) {
                type = "boolean[]";
              }
              value.push(theArray[i] ? "true" : "false");
            } else if (typeof theArray[i] instanceof Date) {
              if (type == null) {
                type = "date[]";
              }
              value.push(DateTimeFormat.getFormat(PredefinedFormat.ISO_8601).format(theArray[i]));
            } else if (typeof theArray[i] == 'string') {
              if (type == null) {
                type = "string[]"
              }
              value.push(theArray[i]);
            } else if (theArray[i] == null) {
              value.push(null);
            }
          }
        }
      } else if (typeof params[key] == 'number') {
        type = "number";
        value.push('' + params[key]);
      } else if (typeof params[key] == 'boolean') {
        type = "boolean";
        value.push(params[key] ? "true" : "false");
      } else if (params[key] instanceof Date) {
        type = "date";
        value.push(DateTimeFormat.getFormat(PredefinedFormat.ISO_8601).format(params[key]));
      } else if (typeof params[key] == 'string') {
        type = "string";
        value.push(params[key]);
      }
      if (type != null) {
        paramEntries.push({
          name: key,
          stringValue: value,
          type: type
        });
      }
    }
    return paramEntries;
  }-*/;

  public native void schedulerParamsLoadedCallback( String filePath ) /*-{
    //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
    $doc.getElementById('schedulerParamsFrame').contentWindow.initSchedulingParams(filePath, $wnd.schedulerParamsCompleteCallback, $wnd.schedulerParamsContentResizeCallback);
    $wnd.schedulerParamsContentResizeCallback();
  }-*/;

  /**
   * Analyzer 4419
   */
  public void setRadioParameterValue( String url ) {
    String params = url.substring( url.indexOf( "?" ) + 1 );
    params += "&null"; // support for back button
    String token;
    StringTokenizer str = new StringTokenizer( params, "&" );
    try {
      while ( str.hasMoreTokens() ) {
        token = str.nextToken();
        if ( token.startsWith( "REPORT_FORMAT_TYPE" ) ) {
          setRadioButton( token.substring( token.indexOf( "=" ) + 1 ) );
          break;
        }
      }
    } catch ( Exception ex ) {
      //ignore
    }
  }

  protected native void setRadioButton( String value )/*-{
    //CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINES
    var elementTypes = $doc.getElementById('schedulerParamsFrame').contentWindow.document.body.getElementsByTagName('input');
      if(elementTypes.length){
        for(idx = 0 ; idx < elementTypes.length; idx++){
            var element = elementTypes[idx];
            if(element.type == "radio" && element.value == value) {
              element.checked = true;
              break;
            }
         }
      }
   }-*/;

  public void schedulerParamsCompleteCallback( boolean complete ) {
    parametersComplete = complete;
    setCanContinue( complete );
    setCanFinish( complete );
    setRadioParameterValue( parametersFrame.getUrl() );
  }

  public native void schedulerParamsContentResizeCallback() /*-{
    var frame = $doc.getElementById('schedulerParamsFrame');
    var body = frame.contentWindow.document.body;
    if(body.classList.contains("responsive")) {
      var jQueryDoc = $wnd.$(body);
      frame.style.setProperty("--frame-content-width", jQueryDoc.outerWidth(true) + "px");
      frame.style.setProperty("--frame-content-height", jQueryDoc.outerHeight(true) + "px");
    }
  }-*/;

  private native void registerSchedulingCallbacks( ScheduleParamsWizardPanel thisInstance )/*-{
    //CHECKSTYLE IGNORE LineLength FOR NEXT 2 LINES
    $wnd.schedulerParamsLoadedCallback = function(filePath) {
      thisInstance.@org.pentaho.mantle.client.dialogs.scheduling.ScheduleParamsWizardPanel::schedulerParamsLoadedCallback(Ljava/lang/String;)(filePath);
    };

    //CHECKSTYLE IGNORE LineLength FOR NEXT 2 LINES
    $wnd.schedulerParamsCompleteCallback = function(flag) {
      thisInstance.@org.pentaho.mantle.client.dialogs.scheduling.ScheduleParamsWizardPanel::schedulerParamsCompleteCallback(Z)(flag);
    };

    $wnd.schedulerParamsContentResizeCallback = function() {
      thisInstance.@org.pentaho.mantle.client.dialogs.scheduling.ScheduleParamsWizardPanel::schedulerParamsContentResizeCallback()();
    };
  }-*/;

  private native void addOnLoad( Element ele, String scheduledFilePath )
  /*-{
    var onloadFunc = function(){
      $wnd.schedulerParamsLoadedCallback(scheduledFilePath);
    };

    if(ele.attachEvent){
      // can't use onload to an iframe in IE8 using the DOM property once the page has loaded
      ele.attachEvent('onload', onloadFunc);
    }
    else{
      ele.onload = onloadFunc;
    }
  }-*/;

  /**
   * 
   */
  private void layout() {
    this.addStyleName( PENTAHO_SCHEDULE );
    this.addStyleName( SCHEDULE_PARAMS_WIZARD_PANEL );

    // Ideally, there would be a flex version of DockPanel.
    // The following relies on the local current use of DockPanel being restricted to a single column.
    this.addStyleName( VerticalFlexPanel.STYLE_NAME );
    this.addStyleName( "with-scroll-child" );

    this.add( scheduleDescription, NORTH );

    scheduleParameterPanel.setStyleName( "schedule-parameter-panel" );
    scheduleParameterPanel.addStyleName( "flex-column with-scroll-child" );
    this.add( scheduleParameterPanel, CENTER );
    this.setCellHeight( scheduleParameterPanel, "100%" );
    scheduleParameterPanel.setHeight( "100%" );
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.pentaho.gwt.widgets.client.wizards.IWizardPanel#getName()
   */
  public String getName() {
    // TODO Auto-generated method stub
    return Messages.getString( "schedule.scheduleEdit" );
  }

  public void setParametersUrl( String url ) {
    if ( url == null ) {
      if ( parametersFrame != null ) {
        scheduleParameterPanel.remove( parametersFrame );
        parametersFrame = null;
      }
    } else {
      if ( parametersFrame == null ) {
        parametersFrame = new Frame();
        parametersFrame.getElement().setTabIndex( 0 );
        parametersFrame.addStyleName( "schedule-parameter-frame" );
        scheduleParameterPanel.add( parametersFrame );
        parametersFrame.setHeight( "100%" ); //$NON-NLS-1$

        addOnLoad( parametersFrame.getElement(), scheduledFilePath );
        DOM.setElementAttribute( parametersFrame.getElement(), "id", "schedulerParamsFrame" ); //$NON-NLS-1$ //$NON-NLS-2$
        parametersFrame.setUrl( url );
      } else if ( !url.equals( parametersFrame.getUrl() ) ) {
        parametersFrame.setUrl( url );
      }
      setRadioParameterValue( parametersFrame.getUrl() );
    }
  }

  @Override
  public void onResize() {
    super.onResize();
    
    // E.g. width is 100%, but then text reflows and should read new iframe content's height.
    schedulerParamsContentResizeCallback();
  }
}
