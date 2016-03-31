package com.fourcasters.forec.reconciler.server;

import com.fourcasters.forec.reconciler.server.mt4.Mt4HandlerFactory;

public class MessageHandlerFactory {

	private static final Persister persister;
	private static final Forwarder forwarder;
	private static final TradeAppender tradeAppender;
	private static final Logger logger;
	private static final Mt4HandlerFactory mt4HandlerFactory;
	private static final Identity identity;

	static {
		persister  = new Persister();
		forwarder = new Forwarder();
		tradeAppender = new TradeAppender();
		logger = new Logger();
		mt4HandlerFactory = new Mt4HandlerFactory();
		identity = new Identity();
	}
	public MessageHandler get(String topicName) {
		final String id = topicName.substring(0, topicName.indexOf("@"));
		switch(id) {
		case "RECONC":
			return forwarder;
		case "HISTORY":
			return persister;
		case "STATUS":
			return tradeAppender;
		case "LOG":
			return logger;
		case "MT4":
			return mt4HandlerFactory.get(topicName);
		default:
			return identity;
		}
	}

}
