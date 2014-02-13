package com.github.emalock.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * interceptorの動作確認用クラスです。
 * <p>
 * http://camel.apache.org/intercept.html
 * </p>
 * 
 * @author Shinobu Aoki
 */
public class InterceptorTest {

	public static void main(String[] args) throws Exception {
		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {
			public void configure() throws Exception {
				intercept().to("log:interceptLog?showAll=true");
				interceptSendToEndpoint("mock:result").to("log:calledMockResult?showAll=true");
				from("direct:start")
					.to("log:directStart?showAll=true")
					.to("seda:process");
				from("seda:process")
					.to("log:sedaProcess?showAll=true")
					.to("mock:result");
			}
		});
		context.start();
		ProducerTemplate producerTemplate = context.createProducerTemplate();
		MockEndpoint resultEndpoint = context.getEndpoint("mock:result", MockEndpoint.class);
		resultEndpoint.expectedBodiesReceived("hoge");
		producerTemplate.sendBody("direct:start", "hoge");
		resultEndpoint.assertIsSatisfied();
		context.stop();
	}

}
