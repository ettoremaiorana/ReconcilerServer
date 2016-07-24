package com.fourcasters.forec.reconciler.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import static com.fourcasters.forec.reconciler.server.FileConstants.*;

public class ReconcilerBroker {
	private static final Application application = new Application();
	private static final Logger LOG = LogManager.getLogger(ReconcilerBroker.class);
	private static final byte[] RESPONSE_OK_HEADER = "HTTP/1.1 200 OK\nContent-Type: text/csv; charset=UTF-8\n\r\n".getBytes();
	private static final Random random = new Random(System.currentTimeMillis());

	private static final String HISTORY_TOPIC_NAME = "HISTORY@";
	private static final String RECONCILER_TOPIC_NAME = "RECONC@";
	private static final String NEW_TRADES_TOPIC_NAME = "STATUS@";
	private static final String LOG_INFO_TOPIC_NAME = "LOGS@INFO";
	private static final String MT4_TOPIC_NAME = "MT4@";


	private static final int bufferSize = 10240*5;
	private static final byte[] TOPIC_NAME_IN_INPUT = new byte[bufferSize];
	private static final byte[] DATA_IN_INPUT = new byte[bufferSize];
	private static final ByteBuffer TOPIC_BUFFER = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
	private static final ByteBuffer DATA_BUFFER = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
	private static final Charset CHARSET = Charset.forName("US-ASCII");
	private static boolean running;
	private static String topicName;
	private static String data;
	private static MessageHandlerFactory handlers = new MessageHandlerFactory(application);
	private static SelectionKey key;

	public static void main(String[] args) throws IOException {
		final Context ctx = application.context();
		final Socket server = zmqSetup(ctx);
		final Socket newTradesListener = zmqSetupListener(ctx);

		final Selector s = Selector.open();
		final ServerSocketChannel httpServer = httpServerSetup(s);

		LOG.info("Http server listening on port " + httpServer.socket().getLocalPort());
		LOG.info("Zmq  server listening on port 51125");

		//start listening to messages
		running = true;
		while (running) {
			zmqEventHandling(server, newTradesListener);

			httpEventHandling(s, httpServer);

			tasksProcessing();
			selectorTaskProcessing();
			//TODO Back off strategy
			LockSupport.parkNanos(1_000_000L); //bleah
		}
		httpServer.close();
		s.close();
		server.close();
		ctx.close();
	}

	private static void selectorTaskProcessing() {
		SelectorTask task;
		while ((task = application.selectorTasks().poll()) != null) {
			task.run();
		}
	}

	private static void tasksProcessing() {
		if (application.futureTasks().size() > 0) {
			application.futureTasks().removeIf(
					f -> {
							return f.isDone() && logIfException(f);
						});
		}
	}

	private static boolean logIfException(Future<?> f) {
		try {
			f.get();
			LOG.info(f + " future has finished");
		} catch (InterruptedException | ExecutionException e) {
			LOG.error("Computation error", e);
		}
		return true;
	}

	private static void httpEventHandling(Selector s, ServerSocketChannel httpServer) throws IOException {
		if (s.selectNow() > 0) {
			final Future<?> f = application.executor().submit(new Runnable() {

				@Override
				public void run() {
					BufferedReader httpReader = null;
					PrintWriter httpWriter = null;
					java.net.Socket client = null;
					try {
						LOG.info("Request received");
						Iterator<SelectionKey> it = s.selectedKeys().iterator();
						while (it.hasNext()) {
							it.next();
							it.remove();
							if (!key.isValid()) {
								key = httpServer.register(s, SelectionKey.OP_ACCEPT);
							} else {
								final SocketChannel clientChannel = ((ServerSocketChannel)key.channel()).accept();
								client = clientChannel.socket();
								client.setSendBufferSize(2048);
								client.setSoTimeout(3000);
								httpReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
								httpWriter = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
								final HttpParser httpParser = new HttpParser(httpReader);
								int response = respond(clientChannel, httpParser);
								LOG.info(response);
							}
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					finally {
							try {
								if (httpWriter != null) httpWriter.close();
								if (httpReader != null) httpReader.close();
								if (client != null) client.close();
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
					}
				}
			});
			application.futureTasks().add(f);
		}
	}



	private static void zmqEventHandling(final Socket... sockets) throws UnsupportedEncodingException {
		for (Socket socket : sockets) {
			int recvTopicSize = socket.recvByteBuffer(TOPIC_BUFFER, ZMQ.NOBLOCK);
			if (recvTopicSize > 0) {
				read(socket);

				LOG.info("topic = " + topicName);
				LOG.info("data  = " + data);
				final MessageHandler handler = handlers.get(topicName);
				handler.enqueue(topicName, data);

				TOPIC_BUFFER.clear();
				DATA_BUFFER.clear();
			}
		}

	}



	private static ServerSocketChannel httpServerSetup(Selector s)
			throws IOException, SocketException, ClosedChannelException {
		ServerSocketChannel httpServer = ServerSocketChannel.open();
		httpServer.configureBlocking(false);
		httpServer.bind(new InetSocketAddress(Integer.getInteger("http.port", 8080)));
		httpServer.socket().setReceiveBufferSize(1024);
		key = httpServer.register(s, SelectionKey.OP_ACCEPT);
		return httpServer;
	}



	private static Socket zmqSetup(final Context ctx) {
		final Socket server = ctx.socket(ZMQ.SUB);
		server.bind("tcp://*:51125");
		server.subscribe(HISTORY_TOPIC_NAME.getBytes());
		server.subscribe(RECONCILER_TOPIC_NAME.getBytes());
		server.subscribe(NEW_TRADES_TOPIC_NAME.getBytes());
		server.subscribe(MT4_TOPIC_NAME.getBytes());

		return server;
	}


	private static Socket zmqSetupListener(final Context ctx) {
		final Socket listener = ctx.socket(ZMQ.SUB);
		listener.connect("tcp://localhost:50027");
		listener.subscribe(NEW_TRADES_TOPIC_NAME.getBytes());
		if (Boolean.getBoolean("log.info")) {
			listener.subscribe(LOG_INFO_TOPIC_NAME.getBytes());
		}
		return listener;
	}


	private static int respond(final SocketChannel clientChannel, final HttpParser httpParser) throws IOException {
		int response = httpParser.parseRequest();
		if (response == 200){
			if (httpParser.getRequestURL().equals("/history/csv")) {
				LOG.info("Trades history requested in csv format");
				sendFile(clientChannel, CLOSED_TRADES_FILE_NAME);
			}
			else if(httpParser.getRequestURL().equals("/open/csv")){
				LOG.info("Open trades requested in csv format");
				sendFile(clientChannel, OPEN_TRADES_FILE_NAME);
			}
		}
		return response;
	}



	private static void sendFile(final SocketChannel clientChannel, String fileName) throws IOException {
		FileChannel tmpChannel = null;
		FileChannel readChannel = null;
		File envelopTmp = null;
		try {
			final File file = new File(fileName);
			envelopTmp = new File(String.valueOf(ReconcilerBroker.class.hashCode()));
			if (!envelopTmp.exists()) {
				envelopTmp.createNewFile();
			}
			else {
				LOG.warn("Temp file already exists, please check");
				envelopTmp = new File(String.valueOf(ReconcilerBroker.class.hashCode()) + random.nextInt());
			}
			envelopTmp.deleteOnExit();
			tmpChannel = FileChannel.open(envelopTmp.toPath(), StandardOpenOption.WRITE);
			readChannel = FileChannel.open(envelopTmp.toPath(), StandardOpenOption.READ);
			tmpChannel.write(ByteBuffer.wrap(RESPONSE_OK_HEADER));
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
			clientChannel.write(ByteBuffer.wrap("\r\n".getBytes()));
			
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


	private static int read(final Socket server) throws UnsupportedEncodingException {
		TOPIC_BUFFER.flip();
		TOPIC_BUFFER.get(TOPIC_NAME_IN_INPUT, 0, TOPIC_BUFFER.limit()); //read only the bits just read
		topicName = new String(TOPIC_NAME_IN_INPUT, 0, TOPIC_BUFFER.limit(), CHARSET);
		int recvDataSize = 0;
		while (recvDataSize == 0) {
			recvDataSize = server.recvByteBuffer(DATA_BUFFER, ZMQ.NOBLOCK);
		}
		DATA_BUFFER.flip();
		DATA_BUFFER.get(DATA_IN_INPUT, 0, DATA_BUFFER.limit()); //read only the bits just read
		data = new String(DATA_IN_INPUT, 0, DATA_BUFFER.limit(), CHARSET);
		return recvDataSize;
	}

}
