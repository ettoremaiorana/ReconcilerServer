package com.fourcasters.forec.reconciler.server;

public class TradeAppender implements MessageHandler {

	@Override
	public void enqueue(String topic, String data) {
		System.out.println(topic + " = " + data);
	}

}
