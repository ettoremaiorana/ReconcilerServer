package com.fourcasters.forec.reconciler.server;

public interface MessageHandler {

	void enqueue(String topic, String data);

}
