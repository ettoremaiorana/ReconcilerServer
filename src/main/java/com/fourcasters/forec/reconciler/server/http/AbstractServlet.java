package com.fourcasters.forec.reconciler.server.http;

import static com.fourcasters.forec.reconciler.server.ProtocolConstants.CHARSET;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.NOT_FOUND_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.NOT_FOUND_HEADER;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.WRONG_METHOD_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.WRONG_METHOD_HEADER;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.server.ReconcilerBroker;

abstract class AbstractServlet {

	abstract boolean validate(SocketChannel clientChannel) throws IOException;
	abstract long respond(SocketChannel clientChannel) throws IOException;

	final HttpParser httpParser;
	private static final Logger LOG = LogManager.getLogger(AbstractServlet.class);
	private static final Random random = new Random(System.currentTimeMillis());

	public AbstractServlet(HttpParser httpParser) {
		this.httpParser = httpParser;
	}

	boolean validateParameter(HttpParser httpParser, SocketChannel clientChannel, String parameter, String errorMessage) throws IOException {
		final String value = httpParser.getParam(parameter);
		if (value == null) {
			LOG.error(errorMessage);
			if (clientChannel != null) {
				sendFile(clientChannel, NOT_FOUND_HEADER, NOT_FOUND_FILE_NAME);
			}
			return false;
		}
		return true;
	}

	static void sendFile(final SocketChannel clientChannel, byte[] header, String fileName) throws IOException {
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

	static long sendFile(final SocketChannel clientChannel, byte[] header, Path filePath, long start, long end) throws IOException {
		FileChannel readChannel = null;
		long position = start;
		try {
			readChannel = FileChannel.open(filePath, StandardOpenOption.READ);
			clientChannel.write(ByteBuffer.wrap(header));
			position = sendFileToClient(clientChannel, readChannel, end, position);
			clientChannel.write(ByteBuffer.wrap("\r\n".getBytes(CHARSET)));

		}
		finally {
			if(readChannel != null) readChannel.close();
		}
		return position - start;
	}
	
	static long sendJson(final SocketChannel clientChannel, byte[] header, Path filePath, long start, long end) throws IOException {
		FileChannel readChannel = null;
		long position = start;
		try {
			readChannel = FileChannel.open(filePath, StandardOpenOption.READ);
			clientChannel.write(ByteBuffer.wrap(header));
			clientChannel.write(ByteBuffer.wrap("{\"data\":\"".getBytes(StandardCharsets.US_ASCII)));
			
			position = sendFileToClient(clientChannel, readChannel, end, position);
			clientChannel.write(ByteBuffer.wrap("\"}\r\n".getBytes(CHARSET)));

		}
		finally {
			if(readChannel != null) readChannel.close();
		}
		return position - start;
	}

	static long sendFileToClient(final SocketChannel clientChannel, FileChannel readChannel, 
			long end, long position) throws IOException {
		do {
			int remaining = (int)(end-position);
			int length = Math.min(1024*1024, remaining);
			long transfered = readChannel.transferTo(position, length, clientChannel);
			position += transfered;
			LOG.debug("Sending...");
		} while(position < end);
		return position;
	}

	boolean validateMethod(HttpParser httpParser, SocketChannel clientChannel, String param, String errorMessage) throws IOException {
		if(!httpParser.getMethod().equals(param)) {
			LOG.error(errorMessage);
			if (clientChannel != null) {
				sendFile(clientChannel, WRONG_METHOD_HEADER, WRONG_METHOD_FILE_NAME);
			}
			return false;
		}
		return true;
	}

}
