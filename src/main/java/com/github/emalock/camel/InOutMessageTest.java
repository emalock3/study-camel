package com.github.emalock.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * InOutパターン（呼び出し元に結果を戻すパターン）の動作を確認します。
 * <p>
 * http://camel.apache.org/using-getin-or-getout-methods-on-exchange.html
 * </p>
 * 
 * @author Shinobu Aoki
 */
public class InOutMessageTest {

	public static void main(String[] args) throws Exception {
		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {
			public void configure() throws Exception {
				from("seda:start")
					.to("log:directStart?showAll=true")
					.to("seda:greeting");
				from("seda:greeting")
					.delay(1000L)
					.process(new Processor() {
						public void process(Exchange exchange) throws Exception {
							String body = exchange.getIn().getBody(String.class);
							// OutにセットしなければInの内容（hoge）が返る
							exchange.getOut().setBody("Hello " + body);
						}
					})
					.to("log:endGreeting?showAll=true");
			}
		});
		context.start();
		ProducerTemplate template = context.createProducerTemplate();
		// requestBodyは勝手にInOutのExchangeを作成して結果が返るのを待つ
		// 非同期にしたい場合にはasyncRequestBodyなどのメソッドを使う（Futureが返る）
		String response = template.requestBody("seda:start", "hoge", String.class);
		System.out.println(response);
		context.stop();
	}

}
