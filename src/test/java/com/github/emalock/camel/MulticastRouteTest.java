package com.github.emalock.camel;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

/**
 * multicastのテスト
 * 
 * @author Shinobu Aoki
 */
public class MulticastRouteTest extends CamelTestSupport {
	
	@EndpointInject(uri = "mock:result1")
	protected MockEndpoint mockEndpoint1;
	@EndpointInject(uri = "mock:result2")
	protected MockEndpoint mockEndpoint2;
	@EndpointInject(uri = "mock:result3")
	protected MockEndpoint mockEndpoint3;
	@Produce(uri = "direct:start")
	protected ProducerTemplate template;

	@Test
	public void testMulticast() throws InterruptedException {
		String expected = "hoge";
		mockEndpoint1.expectedBodiesReceived(expected);
		mockEndpoint2.expectedBodiesReceived(expected);
		mockEndpoint3.expectedBodiesReceived(expected);
		template.sendBody(expected);
		mockEndpoint1.assertIsSatisfied();
		mockEndpoint2.assertIsSatisfied();
		mockEndpoint3.assertIsSatisfied();
	}

	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			public void configure() {
				from("direct:start")
					.multicast()
						.to("mock:result1")
						.to("mock:result2")
						.to("mock:result3")
					.end()
					.log("end");
			}
		};
	}

}
