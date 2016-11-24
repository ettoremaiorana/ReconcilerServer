package com.fourcasters.forec.reconciler.server.http;

public interface ServletCallback {

	void onSuucessfullRespond();
	void onValidation();
	void onError(Exception potentialException);
}
