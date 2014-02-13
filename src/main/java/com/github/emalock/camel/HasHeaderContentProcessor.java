package com.github.emalock.camel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.Builder;
import org.apache.camel.processor.aggregate.AggregationStrategy;

/**
 * 最初の一行の内容をヘッダとして次の行以降に設定して次の処理へ受け渡すsplitter用のAggregationStrategy
 */
class HasHeaderContentProcessor implements AggregationStrategy {
	private final String headerKey;
	private final String sendToUri;
	private volatile Object header;
	
	HasHeaderContentProcessor(String headerKey, String sendToUri) {
		this.headerKey = headerKey;
		this.sendToUri = sendToUri;
	}
	
	public Exchange aggregate(Exchange oldEx, Exchange newEx) {
		if (oldEx == null) {
			header = newEx.getIn().getBody();
			newEx.getIn().setHeader(headerKey, header);
			return newEx;
		}
		updateOldExchange(oldEx, newEx);
		sendTo(oldEx, newEx);
		return oldEx;
	}
	
	protected void updateOldExchange(Exchange oldEx, Exchange newEx) {
		oldEx.getIn().setBody(newEx.getIn().getBody());
	}
	
	protected void sendTo(Exchange oldEx, Exchange newEx) {
		Builder.sendTo(sendToUri).evaluate(oldEx, Void.class);
	}
}