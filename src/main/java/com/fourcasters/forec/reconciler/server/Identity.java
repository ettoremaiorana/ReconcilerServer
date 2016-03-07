package com.fourcasters.forec.reconciler.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Identity implements MessageHandler {

	private static final Logger LOG = LogManager.getLogger(Identity.class);

	@Override
	public void enqueue(String topic, String data) {
		LOG.warn("The request " + topic + "@" + data + " cannot be handled");
	}

}
