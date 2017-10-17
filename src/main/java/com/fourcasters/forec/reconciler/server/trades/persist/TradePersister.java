package com.fourcasters.forec.reconciler.server.trades.persist;


import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import com.fourcasters.forec.reconciler.server.MessageHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TradePersister implements MessageHandler {

    private static final Logger LOG = LogManager.getLogger(TradePersister.class);
	private final TransactionManager transactionManager;

	public TradePersister(TransactionManager transactionManager, ApplicationInterface application) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void enqueue(String topic, String message) {
		final int transIdIndex = message.indexOf("=");
		final Integer transId = Integer.parseInt(message.substring(0, transIdIndex));
		final int transModeIndex = message.indexOf("=", transIdIndex+1);
		final String transactionModeAsString = message.substring(transIdIndex+1, transModeIndex);
		final TransactionMode mode = TransactionMode.valueOf(transactionModeAsString);
		final String tradesInMessage = message.substring(transModeIndex + 1);

		if (mode == TransactionMode.SINGLE) {
			transactionManager.onSingleTransaction(transId, tradesInMessage);
		}
		else if (mode == TransactionMode.OPEN) {
			transactionManager.onOpenTransaction(transId, tradesInMessage);
		}
		else if (mode == TransactionMode.FULL) {
			transactionManager.onFullTransaction(transId, tradesInMessage);
		}
	}


	enum TransactionMode {
		SINGLE,
		OPEN,
		FULL
	}
}