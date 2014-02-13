package com.github.emalock.camel;

import java.util.concurrent.CountDownLatch;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;

public class MultiThreadFileTest {

	public static void main(String[] args) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		Main main = new Main();
		main.addRouteBuilder(new RouteBuilder() {
			public void configure() throws Exception {
				from("file:.?noop=true&recursive=true")
					.threads(4, 4, "consumeFilesThread")
					.log("${header.CamelFileName}")
					.to("direct:countDownIfBatchComplete");
				from("direct:countDownIfBatchComplete")
					.filter(header(Exchange.BATCH_COMPLETE))
					.process(new Processor() {
						public void process(Exchange exchange) throws Exception {
							latch.countDown();
						}
					});
			}
		});
		main.start();
		latch.await();
		main.shutdown();
	}

}
