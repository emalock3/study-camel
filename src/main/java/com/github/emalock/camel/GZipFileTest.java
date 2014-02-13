package com.github.emalock.camel;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.zip.GZIPInputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public class GZipFileTest {

	/**
	 * InputStreamをGZIPInputStreamに変換するコンバータ
	 */
	private static final Processor GZIP_INPUTSTREAM_CONVERTER = new Processor() {
		public void process(Exchange exchange) throws Exception {
			InputStream in = exchange.getIn().getBody(InputStream.class);
			exchange.getIn().setBody(new GZIPInputStream(new BufferedInputStream(in)));
		}
	};
	
	private static final AggregationStrategy LINE_PROCESSOR = 
			new HasHeaderContentProcessor("fileHeaderValue", "direct:viewLine");
	
	public static void main(String[] args) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		Main main = new Main();
		main.addRouteBuilder(new RouteBuilder() {
			public void configure() throws Exception {
				from("file:src/data?antInclude=*.gz&noop=true")
					.convertBodyTo(InputStream.class)
					.process(GZIP_INPUTSTREAM_CONVERTER)
					.split(body().tokenize("\n"), LINE_PROCESSOR)
						.streaming()
						.transform().simple("${body.trim()}")
					.end()
					.log("finish")
					.filter(header(Exchange.BATCH_COMPLETE))
					.process(new Processor() {
						public void process(Exchange exchange) throws Exception {
							latch.countDown();
						}
					});
				
				from("direct:viewLine")
					.to("log:viewLine?showAll=true");
			}
		});
		main.start();
		latch.await();
		main.shutdown();
	}

}
