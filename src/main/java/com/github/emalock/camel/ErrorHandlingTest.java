package com.github.emalock.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;

public class ErrorHandlingTest {

	public static void main(String[] args) throws Exception {
		Main main = new Main();
		main.addRouteBuilder(new RouteBuilder() {
			public void configure() throws Exception {
				onException(IllegalArgumentException.class).continued(true);
				
				from("direct:ignoreException")
					.id("ignoreException")
					.log("start")
					.throwException(new IllegalArgumentException("test"))
					.log("end");
			}
		});
		main.addRouteBuilder(new RouteBuilder() {
			public void configure() throws Exception {
				onException(RuntimeException.class)
					.handled(true)
					.to("log:error?showAll=true&multiline=true");
				
				from("direct:logError")
					.id("logError")
					.log("start")
					.throwException(new RuntimeException("error"))
					.log("end");
					
			}
		});
		main.addRouteBuilder(new RouteBuilder() {
			public void configure() throws Exception {
				//errorHandler(loggingErrorHandler("error.redelivery").level(LoggingLevel.INFO));
				errorHandler(deadLetterChannel("direct:deadLetter").maximumRedeliveries(3));
				
//				onException(RuntimeException.class)
//					.maximumRedeliveries(3)
//					.maximumRedeliveryDelay(1000L)
//					.to("log:error?showAll=true&multiline=true");
				
				from("seda:redeliveryTest")
					.id("redeliveryTest")
					.log("start")
					.process(new Processor() {
						public void process(Exchange exchange) throws Exception {
							log.info("before throw exception");
							throw new RuntimeException("test error");
						}
					})
					.log("end");
				
				from("direct:deadLetter")
					.to("log:deadLetter?showAll=true&multiline=true")
					.stop();
			}
		});
		main.start();
		ProducerTemplate template = main.getCamelTemplate();
		template.sendBody("direct:ignoreException", "hoge1");
		template.sendBody("direct:logError", "hoge2");
		template.sendBody("seda:redeliveryTest", "hoge3");
//		System.out.println("result: " + result);
		main.stop();
	}

}
