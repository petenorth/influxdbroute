package org.pfry.influxdbttnroute.processor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.influxdb.dto.Point;
import org.pfry.influxdbttnroute.model.UplinkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpMessageToPointProcessor implements Processor {
	
	private static final Logger logger = LoggerFactory.getLogger(UpMessageToPointProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		
		UplinkMessage uplinkMessage = exchange.getIn().getBody(UplinkMessage.class);
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnn'Z'", Locale.ENGLISH);
		
		LocalDateTime date = LocalDateTime.parse(uplinkMessage.getMetadata().getTime(), formatter);
		
		ZoneId id = ZoneId.systemDefault();
		ZonedDateTime zdt = ZonedDateTime.of(date, id);
		
		logger.debug("Measurement time in milliseconds " + zdt.toInstant().toEpochMilli());
		
		long millisecondTime = zdt.toInstant().toEpochMilli();
		
		Point point = Point.measurement("temperature")
		.time(millisecondTime, TimeUnit.MILLISECONDS)
		.addField("temperature", (double)uplinkMessage.getPayloadFields().get("temperature"))
		.build();	
		
		exchange.getIn().setBody(point);

	}

}
