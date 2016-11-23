package com.fourcasters.forec.reconciler.server.http;

import static com.fourcasters.forec.reconciler.server.ProtocolConstants.BKT_DATA_EXTENSION;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.NOT_FOUND_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.NOT_FOUND_HEADER;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.RESPONSE_OK_HEADER;

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

import com.fourcasters.forec.reconciler.query.history.HistoryDAO;
import com.fourcasters.forec.reconciler.server.ReconcilerConfig;

public class HistoryServlet extends AbstractServlet {

	private static final Logger LOG = LogManager.getLogger(HistoryServlet.class);
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
	private final HistoryDAO historyDao;
	
	HistoryServlet(HttpParser httpParser, HistoryDAO dao) {
		super(httpParser);
		this.historyDao = dao;
	}

	@Override
	void respond(SocketChannel clientChannel) throws IOException {
		if (validateMethod(httpParser, clientChannel, "GET", "Method must be GET")
				&& validateParameter(httpParser, clientChannel, "cross", "Parameter 'cross' not well formatted")
				&& validateParameter(httpParser, clientChannel, "from", "Parameter 'from' not well formatted")
				&& validateParameter(httpParser, clientChannel, "to", "Parameter 'to' not well formatted")) {

			final String cross = httpParser.getParam("cross").toLowerCase();
			final String fileName = cross + BKT_DATA_EXTENSION;
			final Path path = Paths.get(ReconcilerConfig.BKT_DATA_PATH, fileName);
			LOG.debug("Path to history: " + path);
			if (!Files.exists(path)) {
				LOG.error("Cross " + cross +" has not been found");
				sendFile(clientChannel, NOT_FOUND_HEADER, NOT_FOUND_FILE_NAME);
			}
			else {
				try {
					Date from = sdf.parse(httpParser.getParam("from"));
					Date to = sdf.parse(httpParser.getParam("to"));
					LOG.debug("from: " + from);
					LOG.debug("to: " + to);
					long start = historyDao.offset(cross+BKT_DATA_EXTENSION, from, false);
					long end = historyDao.offset(cross+BKT_DATA_EXTENSION, to, true);
					sendFile(clientChannel, RESPONSE_OK_HEADER, path.toString(), start, end);
				}
				catch (ParseException e){
					LOG.error(e.getMessage());
					sendFile(clientChannel, NOT_FOUND_HEADER, NOT_FOUND_FILE_NAME);
				}
			}
		}		
	}

	@Override
	public boolean validate() {
		return false;
	}

}
