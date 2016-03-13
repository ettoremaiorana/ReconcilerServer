package com.fourcasters.forec.reconciler.server;

import org.apache.logging.log4j.LogManager;

public class Logger implements MessageHandler {

	private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(Logger.class);

	@Override
	public void enqueue(String topic, String data) {
		LOG.info(topic + ": " + data);
	}

}
