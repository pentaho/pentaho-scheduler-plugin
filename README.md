# pentaho-scheduler-plugin

The current working source for pentaho-scheduler-plugin is contained in the "scheduler-plugin" branch.
There is currently a pipeline for this branch that will build all the code

**To build the project, from the root folder, execute:**

To build the project locally you check-out the following project.  Since the pipeline that builds the
"pentaho-scheduler" branches doesn't save to artifactory you need to build these in order to have the
artifacts in you local m2 repository.

**NOTE:** You should be using the "scheduler-plugin" branch for all of these projects.
pentaho-commons-gwt-modules
pentaho-platform
pentaho-scheduler-plugin

In the pentaho-platform/assemblies/pentaho-solution/pom.xml comment out the following entry

```
    <artifactItem>
      <groupId>pentaho</groupId>
      <artifactId>pentaho-scheduler-plugin</artifactId>
      <version>${project.version}</version>
      <type>zip</type>
      <outputDirectory>${prepared.plugins.directory}</outputDirectory>
    </artifactItem>
```

Build each of the above projects using ***mvn clean install -DskipTests**

**To deploy the plugin:**

1. Download the Pentaho Platforn from the following location:
https://build.orl.eng.hitachivantara.com/hosted/scheduler-plugin/latest/pentaho-server-ce-10.1.0.0-SNAPSHOT.zip
2. Unzip the assembly of the pentaho-scheduler-plugin it should create a directory called "pentaho-scheduler"
3. Expand pentaho-scheduler-plugin-10.1.0.0-SNAPSHOT.zip and drop the scheduler-plugin directory in pentaho-server/pentaho-solutions/system.
4. To test, open a browser and go to "http://localhost:8080/pentaho/plugin/scheduler-plugin/api/scheduler/getJobs" You should get an empty jobs XML tag.
5. Browse to content and exercise the scheduler.  Go to the scheduler perspective and exercise the scheduler.

At this point the server and schedules should behave as normal.
