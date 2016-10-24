package com.fourcasters.forec.reconciler.server;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.CHARSET;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.HISTORY_TOPIC_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.LOG_INFO_TOPIC_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.MT4_TOPIC_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.NEW_TRADES_TOPIC_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.RECONCILER_TOPIC_NAME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.fourcasters.forec.reconciler.query.history.HistoryDAO;
public class ReconcilerBroker {

	private static final Application application = new Application();
	private static final Logger LOG = LogManager.getLogger(ReconcilerBroker.class);

	private static final int bufferSize = 10240*5;
	private static final byte[] TOPIC_NAME_IN_INPUT = new byte[bufferSize];
	private static final byte[] DATA_IN_INPUT = new byte[bufferSize];
	private static final ByteBuffer TOPIC_BUFFER = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
	private static final ByteBuffer DATA_BUFFER = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
	private static boolean running;
	private static String topicName;
	private static String data;
	private static SelectionKey key;


	public static void main(String[] args) throws IOException {
		final Context ctx = application.context();
		final Socket server = zmqSetup(ctx);
		final Socket newTradesListener = zmqSetupListener(ctx);
		final Selector s = Selector.open();
		final ServerSocketChannel httpServer = httpServerSetup(s);
		LOG.info("Http server listening on port " + httpServer.socket().getLocalPort());
		LOG.info("Zmq  server listening on port 51125");

		final ReconcilerMessageSender reconcMessageSender = new ReconcilerMessageSender(application);
		final StrategiesTracker strategiesTracker = new StrategiesTracker(application, new InitialStrategiesLoader());
		final HttpRequestHandler httpReqHandler = new HttpRequestHandler(strategiesTracker);
		final MessageHandlerFactory zmqMsgsHandlers = new MessageHandlerFactory(application, reconcMessageSender, strategiesTracker);
		final HistoryDAO dao = new HistoryDAO();
//		dao.dbhash("EURUSD.csv");
		application.executor().scheduleAtFixedRate(() -> consumer.accept(reconcMessageSender), 300L, 300L, TimeUnit.SECONDS);

		running = true;
		while (running) {
			zmqEventHandling(zmqMsgsHandlers, server, newTradesListener);

			httpEventHandling(s, httpServer, httpReqHandler);

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
		final int size = application.selectorTasks().size();
		int inc = 0;
		while ((task = application.selectorTasks().poll()) != null && inc < size) {
			task.run();
			inc++; //this is to avoid task to enqueue itself, so ending in an infinite loop.
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

	private static void httpEventHandling(Selector s, ServerSocketChannel httpServer, HttpRequestHandler httpReqHandler) throws IOException {
		if (s.selectNow() > 0) {
			final Future<?> f = application.executor().submit(new Runnable() {

				@Override
				public void run() {
					BufferedReader httpReader = null;
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
								httpReader = new BufferedReader(new InputStreamReader(client.getInputStream(), CHARSET));
								final HttpParser httpParser = new HttpParser(httpReader);
								httpReqHandler.respond(clientChannel, httpParser);
							}
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					finally {
						try {
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



	private static void zmqEventHandling(MessageHandlerFactory handlersFactory, final Socket... sockets) throws UnsupportedEncodingException {
		for (Socket socket : sockets) {
			try {
				int recvTopicSize = socket.recvByteBuffer(TOPIC_BUFFER, ZMQ.NOBLOCK);
				if (recvTopicSize > 0) {
					read(socket);

					LOG.info("topic = " + topicName);
					LOG.info("data  = " + data);
					final MessageHandler handler = handlersFactory.get(topicName);
					handler.enqueue(topicName, data);

					TOPIC_BUFFER.clear();
					DATA_BUFFER.clear();
				}
			}
			catch (Exception e) {
				LOG.error("zmq message event handling failed: ", e);
			}
		}

	}



	private static ServerSocketChannel httpServerSetup(Selector s) {
		ServerSocketChannel httpServer = null;
		try {
			httpServer = ServerSocketChannel.open();
			httpServer.configureBlocking(false);
			httpServer.bind(new InetSocketAddress(Integer.getInteger("http.port", 8080)));
			httpServer.socket().setReceiveBufferSize(1024);
			key = httpServer.register(s, SelectionKey.OP_ACCEPT);
			return httpServer;
		}
		catch (IOException e) {
			if (httpServer != null) {
				try {
					httpServer.close();
				} catch (IOException e1) {}
			}
			throw new RuntimeException("Unable to allocate new http server socket", e);
		}
	}



	private static Socket zmqSetup(final Context ctx) {
		final Socket server = ctx.socket(ZMQ.SUB);
		server.bind("tcp://*:51125");
		server.subscribe(HISTORY_TOPIC_NAME.getBytes(CHARSET));
		server.subscribe(RECONCILER_TOPIC_NAME.getBytes(CHARSET));
		server.subscribe(NEW_TRADES_TOPIC_NAME.getBytes(CHARSET));
		server.subscribe(MT4_TOPIC_NAME.getBytes(CHARSET));

		return server;
	}


	private static Socket zmqSetupListener(final Context ctx) {
		final Socket listener = ctx.socket(ZMQ.SUB);
		listener.connect("tcp://localhost:50027");
		listener.subscribe(NEW_TRADES_TOPIC_NAME.getBytes(CHARSET));
		if (Boolean.getBoolean("log.info")) {
			listener.subscribe(LOG_INFO_TOPIC_NAME.getBytes(CHARSET));
		}
		return listener;
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

	private static final Consumer<ReconcilerMessageSender> consumer = new Consumer<ReconcilerMessageSender>() {

		@Override
		public void accept(ReconcilerMessageSender t) {
			LOG.info("New scheduled task, asking for open trades");
			final Future<?> f = application.executor().submit(
					() -> t.askForOpenTrades("RECONC@ACTIVTRADES@EURUSD@1002")
					);
			application.selectorTasks().add(new SelectorTask() {
				@Override
				public void run() {
					application.futureTasks().add(f);
				}
			});
		}
	};
}
