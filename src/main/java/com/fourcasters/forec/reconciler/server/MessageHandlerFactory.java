package com.fourcasters.forec.reconciler.server;

import com.fourcasters.forec.reconciler.server.mt4.Mt4HandlerFactory;
import com.fourcasters.forec.reconciler.server.persist.TradePersister;
import com.fourcasters.forec.reconciler.server.persist.TradeTaskFactory;
import com.fourcasters.forec.reconciler.server.persist.TransactionManager;

public class MessageHandlerFactory {

	private final TradePersister persister;
	private final Forwarder forwarder;
	private final TradeEventCapturer tradeAppender;
	private final Logger logger;
	private final Mt4HandlerFactory mt4HandlerFactory;
	private final Identity identity;

	public MessageHandlerFactory(ApplicationInterface application, ReconcilerMessageSender reconcMessageSender, StrategiesTracker strategiesTracker) {
		persister  = new TradePersister(new TransactionManager(new TradeTaskFactory(application), application), application);
		forwarder = Forwarder.create(application);
		tradeAppender = new TradeEventCapturer(application, reconcMessageSender, strategiesTracker);
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
