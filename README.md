# Using Apache Camel to Store and Visualize Data From The Things Network using Grafana and InfluxDB

This example was inspired by Laurens Slats example 

https://www.thethingsnetwork.org/labs/story/store-and-visualize-data-using-influxdb-and-grafana

This example demonstrates how you can use Apache Camel with Spring Boot to consume messages sent to The Things Network by motes (devices) and send them to an InfluxDB database.

The example can be run locally but this requires an existing InfluxDB instance. There is also a description of how to run the example in Openshift including how to deploy InfluxDB and Grafana so that a full end to end solution can be demonstrated.

### Building

The example can be built with

    mvn clean install
    
### Running locally

The example can be run locally (replacing the exported values with those applicable to your TTN application and your Influx DB).

    export REGION=eu #this is TTN region
    export APP_ID=<<your app id>>
    export ACCESS_KEY=<<your access key>>
    export DEVICE_ID=<<your device id>>
    export INFLUXDB_DB=<<the influx db name>>
    export INFLUXDB_URL='http://<<influx db host>>:<<influx db port'

    mvn spring-boot:run

### Running the example in OpenShift

This code base was generated using the FIS 2.0 maven archetypes https://access.redhat.com/documentation/en-us/red_hat_jboss_fuse/6.3/html/fuse_integration_services_2.0_for_openshift/ . Obviously to deploy to Openshift an Openshift environment is needed. The easiest way to do this in a local environment is to use minishift https://access.redhat.com/documentation/en-us/red_hat_jboss_fuse/6.3/html/fuse_integration_services_2.0_for_openshift/get-started-dev#get-started-prereq .

Firstly you will need to deploy an InfluxDB server and a Grafana instance into your Openshift instance.

    git clone https://github.com/feedhenry/sync-metrics-openshift
    export INFLUXDB_STORAGE=1Gi
    cd sync-metrics-openshift/
    oc new-app -p STORAGE_SIZE="$INFLUXDB_STORAGE" -f ./influxdb-template.yaml
    export GRAFANA_STORAGE=2Gi 
    oc new-app -p STORAGE_SIZE="$GRAFANA_STORAGE" -f ./grafana-template.yaml
    
Then use `oc get pod` to find out the name of the influxdb pod. Then obtain a remote shell to that pod `oc rsh <<pod name>> bash` and then 

    influx
      Connected to http://localhost:8086 version 1.5.1
      InfluxDB shell version: 1.5.1
    > CREATE DATABASE ttndb
    > USE ttndb
      Using database ttndb
    > create retention policy "default" on ttndb duration 0d replication 1 default

The example can be built and run on OpenShift using a single goal (if you experience issues with missing image streams then follow instructions in the section 'Importing Image Streams'. Update the file `src/main/fabric8/deployment.yml` with values applicable to your TTN application, device:

    mvn fabric8:deploy

When the example runs in OpenShift, you can use the OpenShift client tool to inspect the status

To list all the running pods:

    oc get pods

Then find the name of the pod that runs this quickstart, and output the logs from the running pods with:

    oc logs <name of pod>

You can also use the openshift [web console](https://docs.openshift.com/container-platform/3.3/getting_started/developers_console.html#developers-console-video) to manage the
running pods, and view logs and much more.

### Setting up a Grafana Dashboard

Then follow the Grafana documentation to setup a dashboard http://docs.grafana.org/features/datasources/influxdb/#using-influxdb-in-grafana . When setting up the InfluxDB datasource use the following for the properties:

* The name of the influx db `ttndb` 
* The username can be left blank
* The password can be left blank
* Leave the 'Access' mode as 'proxy'
* For the URL use the Openshift service name `http://influxdb-svc:8086`

The measurement name is 'temperature' and the value name is 'temperature' i.e. the Grafana query should look something like

    SELECT mean("temperature") FROM "temperature" WHERE $timeFilter GROUP BY time($interval) fill(none)

### Importing Image Streams

Import the Fuse image streams:

    oc create -f https://raw.githubusercontent.com/jboss-fuse/application-templates/GA/fis-image-streams.json



