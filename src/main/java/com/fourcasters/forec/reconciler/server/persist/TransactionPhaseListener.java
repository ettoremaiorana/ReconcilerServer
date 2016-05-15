package com.fourcasters.forec.reconciler.server.persist;

public interface TransactionPhaseListener {

	void onTransactionStart(int transId);
	void onTransactionEnd(int transId);
	void onTaskEnd();
	void onTaskStart();
}
