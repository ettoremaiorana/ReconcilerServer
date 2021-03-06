package com.fourcasters.forec.reconciler.query.marketdata;

public class HistoryRecordInputStream {

	private int index;
	private final String record;

	public HistoryRecordInputStream(String recordAsString) {
		this.record = recordAsString;
		index = -1;
	}

	public double nextDouble(char c) {
		String token = next(c);
		try {
			return Double.parseDouble(token);
		}
		catch(NumberFormatException e) {
			throw new HistoryRecordParsingException(token + " could not be parsed into a double", e);
		}
	}

	public short nextShort(char c) {
		try {
			String token = next(c);
			return Short.parseShort(token);
		}
		catch(NumberFormatException e) {
			throw new HistoryRecordParsingException("the token could not be parsed into a short", e);
		}
	}

	String next(char c) {
		try {
			if (index >= record.length()) {
				throw new HistoryRecordParsingException("end of stream");
			}
			int prevIndex = index;
			index = record.indexOf(c, prevIndex+1);
			index = index < 0 ? record.length() : index;
			String token = record.substring(prevIndex+1, index);
			return token;
		}
		catch(IndexOutOfBoundsException e) {
			throw new HistoryRecordParsingException("character not found", e);
		}
	}



}
