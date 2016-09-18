package com.fourcasters.forec.reconciler.server;

import static com.fourcasters.forec.reconciler.server.ProtocolConstants.BKT_DATA_EXTENSION;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.CHARSET;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.CLOSED_TRADES_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.NOT_FOUND_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.NOT_FOUND_HEADER;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.OPEN_TRADES_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.PERFORMANCE_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.RESPONSE_OK_HEADER;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.WRONG_METHOD_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.WRONG_METHOD_HEADER;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpRequestHandler {

	private final StrategiesTracker strategiesTracker;

	HttpRequestHandler(StrategiesTracker strategiesTracker) {
		this.strategiesTracker = strategiesTracker;
		LOG.debug(ReconcilerConfig.BKT_DATA_PATH);
	}
	private static final Random random = new Random(System.currentTimeMillis());
	private static final Logger LOG = LogManager.getLogger(HttpRequestHandler.class);

	int respond(final SocketChannel clientChannel, final HttpParser httpParser) throws IOException {
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
				LOG.info("Performace file requested");
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
				if (validateMethod(httpParser, clientChannel, "GET", "Method must be GET")
						&& validateParameter(httpParser, clientChannel, "cross", "Parameter 'cross' not well formatted")
						&& validateParameter(httpParser, clientChannel, "from", "Parameter 'from' not well formatted")
						&& validateParameter(httpParser, clientChannel, "to", "Parameter 'to' not well formatted")) {

					final String cross = httpParser.getParam("cross").toLowerCase();
					final String fileName = cross + BKT_DATA_EXTENSION;
					final Path path = Paths.get(ReconcilerConfig.BKT_DATA_PATH, fileName);
					if (!Files.exists(path)) {
						LOG.error("Cross " + cross +" has not been found");
						sendFile(clientChannel, NOT_FOUND_HEADER, NOT_FOUND_FILE_NAME);
					}
					else {
						sendFile(clientChannel, RESPONSE_OK_HEADER, path.toString());
					}
				}
			}
			else { // requested page not found
				sendFile(clientChannel, NOT_FOUND_HEADER, NOT_FOUND_FILE_NAME);
			}
		}
		return response;
	}



	private boolean validateParameter(HttpParser httpParser, SocketChannel clientChannel, String parameter, String errorMessage) throws IOException {
		final String value = httpParser.getParam(parameter);
		if (value == null) {
			LOG.error(errorMessage);
			sendFile(clientChannel, NOT_FOUND_HEADER, NOT_FOUND_FILE_NAME);
			return false;
		}
		return true;
	}



	private boolean validateMethod(HttpParser httpParser, SocketChannel clientChannel, String param, String errorMessage) throws IOException {
		if(!httpParser.getMethod().equals(param)) {
			LOG.error(errorMessage);
			sendFile(clientChannel, WRONG_METHOD_HEADER, WRONG_METHOD_FILE_NAME);
			return false;
		}
		return true;
	}



	private static void sendString(SocketChannel clientChannel, String response) throws IOException {
		clientChannel.write(ByteBuffer.wrap(response.getBytes(CHARSET)));
		clientChannel.write(ByteBuffer.wrap("\r\n".getBytes(CHARSET)));
	}

	private static void sendFile(final SocketChannel clientChannel, byte[] header, String fileName) throws IOException {
		FileChannel tmpChannel = null;
		FileChannel readChannel = null;
		File envelopTmp = null;
		try {
			final File file = new File(fileName);
			envelopTmp = new File(String.valueOf(ReconcilerBroker.class.hashCode()));
			if (!envelopTmp.exists() && !envelopTmp.createNewFile()) {
				LOG.warn("Temp file already exists or cannot be created, please check");
				envelopTmp = new File(String.valueOf(ReconcilerBroker.class.hashCode()) + random.nextInt());
			}
			envelopTmp.deleteOnExit();
			tmpChannel = FileChannel.open(envelopTmp.toPath(), StandardOpenOption.WRITE);
			readChannel = FileChannel.open(envelopTmp.toPath(), StandardOpenOption.READ);
			tmpChannel.write(ByteBuffer.wrap(header));
			long position = 0;
			do {
				long transfered =  FileChannel.open(file.toPath(), StandardOpenOption.READ).transferTo(position, position + 256*8, tmpChannel);
				position += transfered;
			} while(position < file.length());
			tmpChannel.force(true);
			position = 0;
			do {
				long transfered = readChannel.transferTo(position, position + 256*8, clientChannel);
				position += transfered;
				LOG.debug("Sending...");
			} while(position < envelopTmp.length());
			clientChannel.write(ByteBuffer.wrap("\r\n".getBytes(CHARSET)));

		}
		finally {
			if(tmpChannel != null) tmpChannel.close();
			if(readChannel != null) readChannel.close();
			if(envelopTmp != null) {
				if(!envelopTmp.delete()) {
					LOG.warn("Unable to delete temporary file {}", envelopTmp.getAbsoluteFile());
				}

			}
		}
	}

}
