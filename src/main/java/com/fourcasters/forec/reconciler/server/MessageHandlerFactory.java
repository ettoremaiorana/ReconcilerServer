package com.fourcasters.forec.reconciler.server;

public class MessageHandlerFactory {

	private static final Persister persister;
	private static final Forwarder forwarder;
	private static final TradeAppender tradeAppender;
	
	static {
		persister  = new Persister();
		forwarder = new Forwarder();
		tradeAppender = new TradeAppender();
	}
	public MessageHandler get(String topicName) {
		final String id = topicName.substring(0, topicName.indexOf("@"));
		switch(id) {
		case "RECONC":
			return forwarder;
		case "HISTORY":
			return persister;
		case "OPERATION":
			return tradeAppender;
		default:
			throw new IllegalArgumentException("The request" + id + " cannot be handled");
		}
	}

}
