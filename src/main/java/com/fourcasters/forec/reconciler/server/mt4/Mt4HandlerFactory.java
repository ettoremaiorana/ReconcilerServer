package com.fourcasters.forec.reconciler.server.mt4;

import com.fourcasters.forec.reconciler.server.Identity;
import com.fourcasters.forec.reconciler.server.Logger;
import com.fourcasters.forec.reconciler.server.MessageHandler;

public class Mt4HandlerFactory {


	private final HistoryHandler historyHandler;
	private final RealtimeHandler realtimeHandler;
	private final Logger loggerHandler;
	private final Identity identity;

	public Mt4HandlerFactory() {
		historyHandler = new HistoryHandler();
		realtimeHandler = new RealtimeHandler();
		loggerHandler = new Logger();
		identity = new Identity();
	}

	public MessageHandler get(String topicName) {
		switch(topicName) {
		case "MT4@ACTIVTRADES@HISTORYQUOTES":
			return historyHandler;
		case "MT4@ACTIVTRADES@REALTIMEQUOTES":
			return realtimeHandler;
		case "MT4@ACTIVTRADES@TRADEALLOWED":
			return loggerHandler;
		default:
			return identity;
		}
	}

}
