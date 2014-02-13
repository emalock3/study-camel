package com.github.emalock.camel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public class App {
	public static void main(String[] args) throws Exception {
		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {
			private final Processor P = new Processor() {
				private final Random R = new Random();
				public void process(Exchange exchange) throws Exception {
					exchange.getIn().setBody(Integer.valueOf(R.nextInt(100)));
				}
			};
			public void configure() throws Exception {
				from("timer:foo1?period=300").process(P).to("direct:foo");
				from("timer:foo2?period=250").process(P).to("direct:foo");
				from("timer:foo3?period=230").process(P).to("direct:foo");
				from("direct:foo")
					.resequence(body()).batch().size(100).timeout(5000L).allowDuplicates()
					.aggregate(constant(true), new AggregationStrategy() {
						public Exchange aggregate(Exchange oe, Exchange ne) {
							Integer nv = ne.getIn().getBody(Integer.class);
							if (oe == null) {
								List<Integer> list = new ArrayList<Integer>();
								list.add(nv);
								ne.getIn().setBody(list);
								return ne;
							} else {
								@SuppressWarnings("unchecked")
								List<Integer> list = oe.getIn().getBody(List.class);
								list.add(nv);
								return oe;
							}
						}
					}).completionTimeout(5000L)
					.to("log:end");
			}
		});
		context.start();
		Thread.sleep(1000000L);
	}
}
