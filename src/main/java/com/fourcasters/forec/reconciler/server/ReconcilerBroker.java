package com.fourcasters.forec.reconciler.server;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.CHARSET;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.locks.LockSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.query.marketdata.HistoryDAO;
import com.fourcasters.forec.reconciler.server.http.HttpParser;
import com.fourcasters.forec.reconciler.server.http.HttpRequestHandler;
public class ReconcilerBroker {

	private static final Application application = new Application();
	private static final Logger LOG = LogManager.getLogger(ReconcilerBroker.class);

	private static volatile boolean running;
	private static SelectionKey key;
	private static Thread t;


	public static void main(String[] args) throws InterruptedException {
		t = new Thread(() -> {
            try {
                new ReconcilerBroker().run();
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }, "Event-loop-thread");
		t.setDaemon(true);
		t.start();
		t.join();
	}

	final void run() throws IOException, URISyntaxException {
		if ("stop".equals(System.getProperty("debug"))) {
			LOG.info("Write anything and then press enter to let the program start");
			new BufferedReader(new InputStreamReader(System.in, StandardCharsets.US_ASCII)).readLine();
		}
		final Selector s = Selector.open();
		final ServerSocketChannel httpServer = httpServerSetup(s);

		final ZmqModule zmqModule = new ZmqModule(application);
		zmqModule.start();

		final HistoryDAO dao = new HistoryDAO();
		final HttpRequestHandler httpReqHandler = new HttpRequestHandler(dao);
		dao.createIndexAll();

		running = true;
		LOG.info("Http server listening on port " + httpServer.socket().getLocalPort());
		LOG.info("Zmq  server listening on port 51125");
		while (running) {
			int events = application.handleEvents();
			if (events > 0) {
			    LOG.info("Nr of events handled: " + events);
            }
			httpEventHandling(s, httpServer, httpReqHandler);

			application.select();
			//TODO Back off strategy
			LockSupport.parkNanos(1_000_000L); //bleah
		}
		httpServer.close();
		s.close();
		zmqModule.stop();
		application.close();
	}

	private static void httpEventHandling(Selector s, ServerSocketChannel httpServer, HttpRequestHandler httpReqHandler) throws IOException {
		if (s.selectNow() > 0) {
			application.submit(() -> {
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
            });
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

	void stop() {
		running = false;
	}

	boolean isStopped() {
		return !t.isAlive();
	}
}
