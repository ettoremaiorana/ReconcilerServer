package com.fourcasters.forec.reconciler.server.http;

import static com.fourcasters.forec.reconciler.server.ProtocolConstants.MD_DATA_EXTENSION;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.RESPONSE_OK_HEADER;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.RESPONSE_OK_HEADER_JSON;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.NOT_FOUND_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.NOT_FOUND_HEADER;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.query.marketdata.HistoryDAO;
import com.fourcasters.forec.reconciler.server.ReconcilerConfig;

public class MarketDataServlet extends AbstractServlet {

	private static final Logger LOG = LogManager.getLogger(MarketDataServlet.class);
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
	private final HistoryDAO historyDao;

	MarketDataServlet(HttpParser httpParser, HistoryDAO dao) {
		super(httpParser);
		this.historyDao = dao;
	}

	@Override
	long respond(SocketChannel clientChannel) throws IOException {
		if (validate(clientChannel)) {

			final String cross = httpParser.getParam("cross").toLowerCase();
			final Path path = Paths.get(ReconcilerConfig.MD_DATA_PATH, cross + MD_DATA_EXTENSION);
			LOG.debug("Path to history: " + path);
			if (!Files.exists(path)) {
				LOG.error("Cross " + cross +" has not been found");
				sendFile(clientChannel, NOT_FOUND_HEADER, NOT_FOUND_FILE_NAME);
				return -1;
			}
			long transfered = -1;
			try {
				Date from = sdf.parse(httpParser.getParam("from"));
				Date to = sdf.parse(httpParser.getParam("to"));
				LOG.debug("from: " + from);
				LOG.debug("to: " + to);
				long start = historyDao.offset(cross, from);
				long end = historyDao.offsetPlusOne(cross, to);
				String responseFormat = httpParser.getParam("format");
				if (responseFormat != null && responseFormat.equals("json")) {
					transfered = sendJson(clientChannel, RESPONSE_OK_HEADER_JSON, path, start, end);
				}
				else {
					transfered = sendFile(clientChannel, RESPONSE_OK_HEADER, path, start, end);
				}
			}
			catch (ParseException e){
				LOG.error(e.getMessage());
				sendFile(clientChannel, NOT_FOUND_HEADER, NOT_FOUND_FILE_NAME);
			}
			return transfered;
		}
		return -1;
	}

	@Override
	public boolean validate(SocketChannel clientChannel) throws IOException {
		return validateMethod(httpParser, clientChannel, "GET", "Method must be GET")
				&& validateParameter(httpParser, clientChannel, "cross", "Parameter 'cross' not well formatted")
				&& validateParameter(httpParser, clientChannel, "from", "Parameter 'from' not well formatted")
				&& validateParameter(httpParser, clientChannel, "to", "Parameter 'to' not well formatted");
	}

}
