package com.github.emalock.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;

public class BeanRefTest {

	public static class TestBean {
		public void test() {
			System.out.println("call test.");
		}
	}
	
	public static class TestProcessor implements Processor {
		TestProcessor() {
			System.out.println("call constructor.");
		}
		public void process(Exchange exchange) throws Exception {
			System.out.println(String.format("call process: body=%s", exchange.getIn().getBody()));
		}
	}
	
	public static void main(String[] args) throws Exception {
		Main main = new Main();
		main.bind("testBean", new TestBean());
		main.bind("testProcessor", new TestProcessor());
		main.addRouteBuilder(new RouteBuilder() {
			public void configure() throws Exception {
				from("direct:start")
					.log("start")
					.beanRef("testBean", "test", true)
					.processRef("testProcessor")
					.log("finish");
			}
		});
		main.start();
		ProducerTemplate template = main.getCamelTemplate();
		template.requestBody("direct:start", "call run");
		main.shutdown();
	}

}
