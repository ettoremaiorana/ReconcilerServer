package com.fourcasters.forec.reconciler.server.persist;


import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import com.fourcasters.forec.reconciler.server.MessageHandler;

public class AnotherPersister implements MessageHandler {

	private final ApplicationInterface application;
	private final TransactionManager transactionManager;

	public AnotherPersister(TransactionManager transactionManager, ApplicationInterface application) {
		this.application = application;
		this.transactionManager = transactionManager;
	}

	@Override
	public void enqueue(String topic, String message) {
		final int transIdIndex = message.indexOf("=");
		final Integer transId = Integer.parseInt(message.substring(0, transIdIndex));
		final int transModeIndex = message.indexOf("=", transIdIndex+1);
		final TransactionMode mode = TransactionMode.valueOf(message.substring(transIdIndex+1, transModeIndex));
		final String tradesInMessage = message.substring(transModeIndex + 1);

		if (mode == TransactionMode.SINGLE) {
			transactionManager.onSingleTransaction(transId, tradesInMessage);
		}
		else {
			transactionManager.onFullTransaction(transId, tradesInMessage);
		}
	}


	static enum TransactionMode {
		SINGLE,
		FULL
	}
}