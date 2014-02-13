package com.github.emalock.camel;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.Builder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.aggregate.AbstractListAggregationStrategy;

public class FileComponentTest {
	
	static class ListAggregationStrategy<T> extends AbstractListAggregationStrategy<T> {
		private final TypedValueBuilder<T> valueBuilder;
		ListAggregationStrategy(TypedValueBuilder<T> valueBuilder) {
			this.valueBuilder = valueBuilder;
		}
		@Override
		public T getValue(Exchange exchange) {
			return valueBuilder.evaluate(exchange);
		}
		static <T1> ListAggregationStrategy<T1> create(Expression expression, Class<T1> clazz) {
			return new ListAggregationStrategy<T1>(TypedValueBuilder.create(expression, clazz));
		}
		private static class TypedValueBuilder<T> extends ValueBuilder {
			private final Class<T> clazz;
			private TypedValueBuilder(Expression expression, Class<T> clazz) {
				super(expression);
				this.clazz = clazz;
			}
			public T evaluate(Exchange exchange) {
				return clazz.cast(evaluate(exchange, clazz));
			}
			static <T1> TypedValueBuilder<T1> create(Expression expression, Class<T1> clazz) {
				return new TypedValueBuilder<T1>(expression, clazz);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {
			public void configure() throws Exception {
				from("file:.?noop=true")
					.to("log:listFiles?showAll=true")
					.aggregate(constant(true), ListAggregationStrategy.create(Builder.body(), File.class)).completionFromBatchConsumer()
						.process(new Processor() {
							public void process(Exchange exchange) throws Exception {
								Message m = exchange.getIn();
								m.setHeader("PROCESS_FILE_LIST_SIZE", m.getBody(List.class).size());
							}
						})
						.to("direct:processFileList");
				from("direct:processFileList")
					.to("log:directProcessFileList?showAll=true")
					.split(body())
						.setHeader(Exchange.FILE_NAME, simple("${body.getPath()}"))
						.setHeader(Exchange.FILE_PATH, simple("${body.getPath()}"))
						.setHeader(Exchange.FILE_NAME_ONLY, simple("${body.getName()}"))
						.to("seda:processFile")
					.end()
					.to("log:splitEnd");
				from("seda:processFile").threads(4)
					.delay(1000L)
					.to("log:file?showAll=true")
					.to("direct:completeProcFile");
				from("direct:completeProcFile")
					.setBody(header(Exchange.FILE_PATH))
					.aggregate(constant(true), ListAggregationStrategy.create(Builder.body(), String.class)).completionSize(header("PROCESS_FILE_LIST_SIZE"))
					.process(new Processor() {
						public void process(Exchange e) {
							latch.countDown();
						}
					})
					.to("log:complete?showAll=true");
			}
		});
		context.start();
		latch.await();
		Thread.sleep(1000L);
		context.stop();
	}

}
