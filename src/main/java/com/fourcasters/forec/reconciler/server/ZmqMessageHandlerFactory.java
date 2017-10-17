package com.fourcasters.forec.reconciler.server;

import com.fourcasters.forec.reconciler.server.mt4.Mt4HandlerFactory;

import java.util.HashMap;
import java.util.Map;

public class ZmqMessageHandlerFactory {

	private final Forwarder forwarder;
	private final Logger logger;
	private final Mt4HandlerFactory mt4HandlerFactory;
	private final Identity identity;
	private final Map<String, MessageHandler> map;

	public ZmqMessageHandlerFactory(ApplicationInterface application, ZmqModule zmq) {
		forwarder = Forwarder.create(application, zmq);
		logger = new Logger();
		mt4HandlerFactory = new Mt4HandlerFactory();
		identity = new Identity();
		map = new HashMap<>(8);
		map.put("RECONC", forwarder);
		map.put("LOG", logger);
		map.put("MT4", mt4HandlerFactory);
	}

	public void registerMessageHandler(MessageHandler mh, String topicId) {
		if (map.get(topicId) != null) {
			throw new RuntimeException("Duplicate message handler for topic " + topicId);
		}
		map.put(topicId, mh);
	}

	public MessageHandler get(String topicName) {
		final String id = topicName.substring(0, topicName.indexOf("@"));

		MessageHandler messageHandler = map.get(id);
		if (messageHandler == null) {
			messageHandler = identity;
		}
		return messageHandler;
	}
}
