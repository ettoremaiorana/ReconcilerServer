package com.fourcasters.forec.reconciler.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.concurrent.locks.LockSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class ReconcilerBroker {
	private static final Logger LOG = LogManager.getLogger(ReconcilerBroker.class);
	private static final String RESPONSE_OK_HEADER = "HTTP/1.1 200 OK\n" +
			"\r\n";


	private static final String HISTORY_TOPIC_NAME = "HISTORY@";
	private static final String RECONCILER_TOPIC_NAME = "RECONC@";
	private static final int bufferSize = 10240;
	private static final byte[] TOPIC_NAME_IN_INPUT = new byte[bufferSize];
	private static final byte[] DATA_IN_INPUT = new byte[bufferSize];
	private static final ByteBuffer TOPIC_BUFFER = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
	private static final ByteBuffer DATA_BUFFER = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
	private static final Charset CHARSET = Charset.forName("US-ASCII");
	private static boolean running;
	private static String topicName;
	private static String data;
	private static MessageHandlerFactory handlers = new MessageHandlerFactory();
	private static SelectionKey key;

	public static void main(String[] args) throws IOException {
		//bind to endpoint
		final Context ctx = ZMQ.context(1);
		final Socket server = ctx.socket(ZMQ.SUB);
		server.bind("tcp://*:51125");
		server.subscribe(HISTORY_TOPIC_NAME.getBytes());
		server.subscribe(RECONCILER_TOPIC_NAME.getBytes());

		Selector s = Selector.open();
		ServerSocketChannel httpServer = ServerSocketChannel.open();
		httpServer.configureBlocking(false);
		httpServer.bind(new InetSocketAddress("localhost", 8080));
		httpServer.socket().setReceiveBufferSize(1024);
		key = httpServer.register(s, SelectionKey.OP_ACCEPT);

		running = true;
		//start listening to messages
		while (running) {
			//TODO _recvDirectBuffer has to change to take the starting position as a parameter
			int recvTopicSize = server._recvDirectBuffer(TOPIC_BUFFER, bufferSize, ZMQ.NOBLOCK);
			if (recvTopicSize > 0) {
				read(server);

				final MessageHandler handler = handlers.get(topicName);
				handler.enqueue(topicName, data);

				TOPIC_BUFFER.clear();
				DATA_BUFFER.clear();
			}

			if (s.selectNow() > 0) {
				Iterator<SelectionKey> it = s.selectedKeys().iterator();
				it.next();
				it.remove();
				if (!key.isValid()) {
					key = httpServer.register(s, SelectionKey.OP_ACCEPT);
					continue;
				}
				final SocketChannel clientChannel = ((ServerSocketChannel)key.channel()).accept();
				final java.net.Socket client = clientChannel.socket();
				client.setSendBufferSize(256);
				final BufferedReader httpReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
				final PrintWriter httpWriter = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
				final HttpParser httpParser = new HttpParser(httpReader);
				int response = httpParser.parseRequest();
				if (response == 200) {
					if (httpParser.getRequestURL().equals("/history/dat")) {
						sendFile(clientChannel, "Trades.dat");
					}
					else if (httpParser.getRequestURL().equals("/history/csv")) {
						sendFile(clientChannel, "Trades.csv");
					}
				}
				LOG.info(response);
				httpWriter.close();
				httpReader.close();
				client.close();

			}
			//TODO Back off strategy
			LockSupport.parkNanos(1_000_000_000L); //bleah
		}
		httpServer.close();
		server.close();
		ctx.close();
	}



	private static void sendFile(final SocketChannel clientChannel, String fileName) throws IOException {
		File file = new File(fileName);
		File envelopTmp = new File(String.valueOf(ReconcilerBroker.class.hashCode()));	
		envelopTmp.createNewFile();
		envelopTmp.deleteOnExit();
		FileChannel tmpChannel = FileChannel.open(envelopTmp.toPath(), StandardOpenOption.WRITE);
		FileChannel readChannel = FileChannel.open(envelopTmp.toPath(), StandardOpenOption.READ);
		tmpChannel.write(ByteBuffer.wrap(RESPONSE_OK_HEADER.getBytes()));
		long position = 0;
		do {
			long transfered =  FileChannel.open(file.toPath(), StandardOpenOption.READ).transferTo(position, position + 256, tmpChannel);
			position += transfered;
		} while(position < file.length());
		tmpChannel.force(true);
		tmpChannel.close();
		position = 0;
		do {
			long transfered = readChannel.transferTo(position, position + 256, clientChannel);
			position += transfered;
			LOG.debug("Looping...");
		} while(position < envelopTmp.length());
		clientChannel.write(ByteBuffer.wrap("\r\n".getBytes()));
		readChannel.close();
		envelopTmp.delete();
	}



	private static void registerSignalHandler() {
		ZMQ.register_signalhandler();
	}



	private static int read(final Socket server) throws UnsupportedEncodingException {
		TOPIC_BUFFER.flip();
		TOPIC_BUFFER.get(TOPIC_NAME_IN_INPUT, 0, TOPIC_BUFFER.limit()); //read only the bits just read
		topicName = new String(TOPIC_NAME_IN_INPUT, CHARSET);
		int recvDataSize = 0;
		while (recvDataSize == 0) {
			recvDataSize = server._recvDirectBuffer(DATA_BUFFER, bufferSize, ZMQ.NOBLOCK);
		}
		DATA_BUFFER.flip();
		DATA_BUFFER.get(DATA_IN_INPUT, 0, DATA_BUFFER.limit()); //read only the bits just read
		data = new String(DATA_IN_INPUT, 0, DATA_BUFFER.limit(), CHARSET);
		return recvDataSize;
	}

}
