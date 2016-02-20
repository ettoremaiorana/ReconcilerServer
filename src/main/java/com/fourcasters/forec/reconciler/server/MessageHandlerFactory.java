package com.fourcasters.forec.reconciler.server;

public class MessageHandlerFactory {

	private static final Persister persister;
	private static final Forwarder forwarder;

	static {
		persister  = new Persister();
		forwarder = new Forwarder();
		persister.start();
	}
	public MessageHandler get(String topicName) {
		final String id = topicName.substring(0, topicName.indexOf("@"));
		switch(id) {
		case "RECONC":
			return forwarder;
		case "HISTORY":
			return persister;
		default:
			throw new IllegalArgumentException("The request" + id + " cannot be handled");
		}
	}

}
