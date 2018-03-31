/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.pfry.influxdbttnroute;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.pfry.influxdbttnroute.model.UplinkMessage;
import org.pfry.influxdbttnroute.processor.UpMessageToPointProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

/**
 * A spring-boot application that includes a Camel route builder to setup the Camel routes
 */
@SpringBootApplication
@ImportResource({"classpath:spring/camel-context.xml"})
public class Application extends RouteBuilder {

    // must have a main method spring-boot can run
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @Value("${REGION}")
    private String region;
    
    @Value("${INFLUXDB_URL}")
    private String influxDbUrl;
    
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Override
    public void configure() throws Exception {
        
    	  String broker = validateBroker(region);
    	  
    	  Processor processor = new UpMessageToPointProcessor();
    	  
		  from("mqtt://ttn?host=" + broker + "&userName={{APP_ID}}&password={{ACCESS_KEY}}&subscribeTopicNames={{APP_ID}}/devices/{{DEVICE_ID}}/up" )
    	  .log(">>> ${body}")
    	  .unmarshal().json(JsonLibrary.Jackson, UplinkMessage.class)
    	  .log(">>> ${body}")
    	  .process(processor)
    	  .to("influxdb://influxDbBean?databaseName={{INFLUXDB_DB}}");
    }
    
    private String validateBroker(String _source) throws URISyntaxException {

        URI tempBroker = new URI(_source.contains(".") ? _source : (_source + ".thethings.network"));

        if ("tcp".equals(tempBroker.getScheme())) {
            if (tempBroker.getPort() == -1) {
                return tempBroker.toString() + ":1883";
            } else {
                return tempBroker.toString();
            }
        } else if ("ssl".equals(tempBroker.getScheme())) {
            if (tempBroker.getPort() == -1) {
                return tempBroker.toString() + ":8883";
            } else {
                return tempBroker.toString();
            }
        } else if (tempBroker.getPort() != -1) {
            return "tcp://" + tempBroker.toString();
        } else {
            return "tcp://" + tempBroker.toString() + ":1883";
        }
    }
    
    @Bean
    public InfluxDB influxDbBean() throws UnknownHostException {

        logger.debug("Creating new instance of a mocked influx db connection");
        InfluxDB mockedDbConnection = InfluxDBFactory.connect(influxDbUrl, "dummy", "dummy");
        return mockedDbConnection;
    }
}
