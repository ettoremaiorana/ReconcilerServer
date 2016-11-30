package com.fourcasters.forec.reconciler.server;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ProtocolConstants {
	public static final Charset CHARSET = StandardCharsets.US_ASCII;

	public static final String CLOSED_TRADES_FILE_NAME = "Trades.csv";
	public static final String OPEN_TRADES_FILE_NAME = "Opens.csv";
	public static final String WRONG_METHOD_FILE_NAME = "method_error.html";
	public static final String NOT_FOUND_FILE_NAME = "not_found.html";
	public static final String PERFORMANCE_FILE_NAME = "_performance.csv";
	public static final String BKT_DATA_EXTENSION = ".csv";
	
	public static final byte[] RESPONSE_OK_HEADER = "HTTP/1.2 200 OK\nContent-Type: text/csv; charset=UTF-8\n\r\n".getBytes(CHARSET);
	public static final byte[] RESPONSE_OK_HEADER_JSON = "HTTP/1.2 200 OK\nContent-Type: application/json; charset=UTF-8\n\r\n".getBytes(CHARSET);
	
	public static final byte[] WRONG_METHOD_HEADER = "HTTP/1.2 405 Method Not Allowed\nAllow: GET\n\r\n".getBytes(CHARSET);
	public static final byte[] NOT_FOUND_HEADER = "HTTP/1.2 404 Not Found\n\r\n".getBytes(CHARSET);	

	public static final String HISTORY_TOPIC_NAME = "HISTORY@";
	public static final String RECONCILER_TOPIC_NAME = "RECONC@";
	public static final String NEW_TRADES_TOPIC_NAME = "STATUS@";
	public static final String LOG_INFO_TOPIC_NAME = "LOGS@INFO";
	public static final String MT4_TOPIC_NAME = "MT4@";
}
