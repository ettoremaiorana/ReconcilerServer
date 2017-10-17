package com.fourcasters.forec.reconciler.server.http;

import static com.fourcasters.forec.reconciler.server.ProtocolConstants.CHARSET;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.CLOSED_TRADES_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.NOT_FOUND_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.NOT_FOUND_HEADER;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.OPEN_TRADES_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.PERFORMANCE_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.RESPONSE_OK_HEADER;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.WRONG_METHOD_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.WRONG_METHOD_HEADER;
import static com.fourcasters.forec.reconciler.server.http.AbstractServlet.sendFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import com.fourcasters.forec.reconciler.server.InitialStrategiesLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.query.marketdata.HistoryDAO;
import com.fourcasters.forec.reconciler.server.ReconcilerConfig;
import com.fourcasters.forec.reconciler.server.StrategiesTracker;

public class HttpRequestHandler {

	private static final Logger LOG = LogManager.getLogger(HttpRequestHandler.class);
	private final StrategiesTracker strategiesTracker;
	private final HistoryDAO historyDao;

	public HttpRequestHandler(HistoryDAO historyDao) {
		this.strategiesTracker = new StrategiesTracker(new InitialStrategiesLoader());
		LOG.debug(ReconcilerConfig.MD_DATA_PATH);
		this.historyDao = historyDao;
	}

	public int respond(final SocketChannel clientChannel, final HttpParser httpParser) throws IOException {
		int response = httpParser.parseRequest();
		if (response == 200){
			final String reqUrl = httpParser.getRequestURL();
			if (reqUrl.equals("/strategies")) {
				LOG.info("Set of strategies requested");
				sendString(clientChannel, Arrays.toString(strategiesTracker.getStrategies()));
			}
			else if (reqUrl.equals("/history/csv")) {
				LOG.info("Trades history requested in csv format");
				sendFile(clientChannel, RESPONSE_OK_HEADER, CLOSED_TRADES_FILE_NAME);
			}
			else if(reqUrl.equals("/open/csv")){
				LOG.info("Open trades requested in csv format");
				sendFile(clientChannel, RESPONSE_OK_HEADER, OPEN_TRADES_FILE_NAME);
			}
			else if (reqUrl.equals("/performance")) {
				LOG.info("Performance file requested");
				if(!httpParser.getMethod().equals("GET")) {
					LOG.error("Method must be GET");
					sendFile(clientChannel, WRONG_METHOD_HEADER, WRONG_METHOD_FILE_NAME);
				}
				else {
					final String magicAsString = httpParser.getParam("magic");
					if (magicAsString == null) {
						LOG.error("Magic must be a paremeter");
						sendFile(clientChannel, NOT_FOUND_HEADER, NOT_FOUND_FILE_NAME);
					}
					else {
						final String fileName = magicAsString + PERFORMANCE_FILE_NAME;
						if (!Files.exists(Paths.get(fileName))) {
							LOG.error("Magic " + magicAsString +" is not a number");
							sendFile(clientChannel, NOT_FOUND_HEADER, NOT_FOUND_FILE_NAME);
						}
						else {
							sendFile(clientChannel, RESPONSE_OK_HEADER, fileName);
						}
					}
				}
			}
			else if (reqUrl.equals("/csvdata")) {
				new MarketDataServlet(httpParser, historyDao).respond(clientChannel);
			}
			else if (reqUrl.equals("/algolog")) {
				new AlgoLogSenderServlet(httpParser).respond(clientChannel);
			}
			else { // requested page not found
				sendFile(clientChannel, NOT_FOUND_HEADER, NOT_FOUND_FILE_NAME);
			}
		}
		return response;
	}



	private static void sendString(SocketChannel clientChannel, String response) throws IOException {
		clientChannel.write(ByteBuffer.wrap(response.getBytes(CHARSET)));
		clientChannel.write(ByteBuffer.wrap("\r\n".getBytes(CHARSET)));
	}

}
