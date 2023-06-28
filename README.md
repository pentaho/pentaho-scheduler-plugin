# pentaho-scheduler-plugin

**THINGS TO DO**
This POC only moved the basic web service functionality to a plugin. There is extra work need to completely move the scheduler to a plugin. 
1. Remove the "scheduler" module from the "pentaho-platform" project.
2. Remove the scheduler dependency from the extensions module on the "pentaho-platform" project. This will cause the project import/export to fail. That needs to be fixed.
3. Moving the code to a plugin requires all the links to the scheduler to be fixed.
4. The scheduler UI needs to me implemented in the plugin.
5. Make sure the PDI scheduling endpoints still work.
6. The file browse functionality is not showing the files.
7. Fix the issue with loading more than one Plugin life cycle listeners.
8. Update the Pentaho pipeline and add the scheduler plugin to the build.

**To build the project, from the root folder, execute:**

`mvn clean install`


**To deploy the plugin:**

1. Download the following pentaho-platform branch and build the "scheduler" and "extensions" modules:
https://github.com/pentaho/pentaho-platform/tree/BACKLOG-37772
2. Copy the resulting pentaho-platform-extensions-9.6.0.0-SNAPSHOT.jar to pentaho-server/tomcat/webapps/pentaho/WEB-INF/lib.
3. Remove pentaho-platform-scheduler-9.6.0.0-SNAPSHOT.jar from pentaho-server/tomcat/webapps/pentaho/WEB-INF/lib.
4. On the pentaho-server/pentaho-solutions/system directory, remove the listed lines from the indicated files:
* pentahoObjects.spring.xml:
  ```
  <bean id="IScheduler2" class="org.pentaho.platform.scheduler2.quartz.QuartzScheduler" scope="singleton">
    <pen:publish as-type="INTERFACES">
      <pen:attributes>
        <pen:attr key="priority" value="50"/>
      </pen:attributes>
    </pen:publish>
  </bean>
  ```

* pentahoServices.spring.xml:
    ```
    <wss:binding url="/webservices/Scheduler">
        <wss:service>
            <ws:service impl="org.pentaho.platform.scheduler2.ws.DefaultSchedulerService"/>
        </wss:service>
    </wss:binding>

    <bean class="org.pentaho.platform.web.http.api.resources.SchedulerResource"/>
  ```

* systemListeners.xml:
    ```
    <bean id="quartzSystemListener" class="org.pentaho.platform.scheduler2.quartz.EmbeddedQuartzSystemListener" />
    
    <bean id="versionCheckerSystemListener" class="org.pentaho.platform.scheduler2.versionchecker.EmbeddedVersionCheckSystemListener">
        <property name="repeatIntervalSeconds">
            <value>86400</value>
        </property>
        <property name="requestedReleases" value="Minor, GA" />
        <property name="disableVersionCheck">
            <value>false</value>
        </property>
    </bean>
  ```

5. Expand pentaho-scheduler-plugin-9.6.0.0-SNAPSHOT.zip and drop the scheduler-plugin directory in pentaho-server/pentaho-solutions/system.
6. To test, open a browser and go to "http://localhost:8080/pentaho/plugin/scheduler-plugin/api/scheduler/getJobs" You should get an empty jobs XML tag.
