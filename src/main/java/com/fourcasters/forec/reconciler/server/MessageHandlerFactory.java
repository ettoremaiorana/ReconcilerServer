package com.fourcasters.forec.reconciler.server;

import com.fourcasters.forec.reconciler.server.mt4.Mt4HandlerFactory;
import com.fourcasters.forec.reconciler.server.persist.AnotherPersister;
import com.fourcasters.forec.reconciler.server.persist.TaskFactory;
import com.fourcasters.forec.reconciler.server.persist.TransactionManager;

public class MessageHandlerFactory {

	private final AnotherPersister persister;
	private final Forwarder forwarder;
	private final TradeAppender tradeAppender;
	private final Logger logger;
	private final Mt4HandlerFactory mt4HandlerFactory;
	private final Identity identity;

	public MessageHandlerFactory(ApplicationInterface application) {
		persister  = new AnotherPersister(new TransactionManager(new TaskFactory(application), application), application);
		forwarder = new Forwarder(application);
		tradeAppender = new TradeAppender(application);
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
