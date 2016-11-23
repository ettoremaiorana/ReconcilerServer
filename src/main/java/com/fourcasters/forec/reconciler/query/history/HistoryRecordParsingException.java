package com.fourcasters.forec.reconciler.query.history;

public class HistoryRecordParsingException extends RuntimeException {

	public HistoryRecordParsingException(String string) {
		super(string);
	}

	public HistoryRecordParsingException(String string, Throwable e) {
		super(string, e);
	}

	private static final long serialVersionUID = -4158871962477180856L;

}
