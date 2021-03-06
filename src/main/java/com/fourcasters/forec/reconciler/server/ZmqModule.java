package com.fourcasters.forec.reconciler.server;

import com.fourcasters.forec.reconciler.server.marketdata.MarketDataModule;
import com.fourcasters.forec.reconciler.server.trades.TradeModule;
import org.apache.logging.log4j.*;
import org.zeromq.ZMQ;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static com.fourcasters.forec.reconciler.server.ProtocolConstants.*;

public class ZmqModule implements EventHandler, Module {

    private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(ZmqModule.class);

    private final int bufferSize = 10240*5;
    private final ApplicationInterface application;
    private final List<Module> modules;
    private ZmqMessageHandlerFactory handlersFactory;
    private String topicName;
    private String data;

    private final byte[] TOPIC_NAME_IN_INPUT = new byte[bufferSize];
    private final byte[] DATA_IN_INPUT = new byte[bufferSize];
    private final ByteBuffer TOPIC_BUFFER = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
    private final ByteBuffer DATA_BUFFER = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
    private ZMQ.Socket server;
    private ZMQ.Socket newTradesListener;
    private ZMQ.Context ctx;

    public ZmqModule(ApplicationInterface app) {
        application = app;
        modules = new ArrayList<>();
    }

    private int zmqEventHandling(final ZMQ.Socket... sockets) {
        int events = 0;
        for (ZMQ.Socket socket : sockets) {
            try {
                int recvTopicSize = socket.recvByteBuffer(TOPIC_BUFFER, ZMQ.NOBLOCK);
                if (recvTopicSize > 0) {
                    read(socket);

                    LOG.info("topic = " + topicName);
                    LOG.info("data  = " + data);

                    final MessageHandler handler = handlersFactory.get(topicName);
                    handler.enqueue(topicName, data);
                    events++;

                    TOPIC_BUFFER.clear();
                    DATA_BUFFER.clear();
                }
            }
            catch (Exception e) {
                LOG.error("zmq message event handling failed: ", e);
            }
        }
        return events;
    }

    private int read(final ZMQ.Socket server) throws UnsupportedEncodingException {
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

    @Override
    public int handle() {
        return zmqEventHandling();
    }

    @Override
    public void start() {
        ctx = ZMQ.context(1);
        server = zmqSetup(ctx);
        newTradesListener = zmqSetupListener(ctx);
        handlersFactory = new ZmqMessageHandlerFactory(application, this);
        application.registerEventHandler(this);

        modules.add(new TradeModule(application, ctx, handlersFactory));
        modules.add(new MarketDataModule());
        modules.forEach(module -> module.start());
    }

    @Override
    public void stop() {
        modules.forEach(module -> module.stop());
        server.close();
    }

    private static ZMQ.Socket zmqSetup(final ZMQ.Context ctx) {
        final ZMQ.Socket server = ctx.socket(ZMQ.SUB);
        server.bind("tcp://*:51125");
        server.subscribe(HISTORY_TOPIC_NAME.getBytes(CHARSET));
        server.subscribe(RECONCILER_TOPIC_NAME.getBytes(CHARSET));
        server.subscribe(NEW_TRADES_TOPIC_NAME.getBytes(CHARSET));
        server.subscribe(MT4_TOPIC_NAME.getBytes(CHARSET));

        return server;
    }

    private static ZMQ.Socket zmqSetupListener(final ZMQ.Context ctx) {
        final ZMQ.Socket listener = ctx.socket(ZMQ.SUB);
        listener.connect("tcp://localhost:50027");
        listener.subscribe(NEW_TRADES_TOPIC_NAME.getBytes(CHARSET));
        if (Boolean.getBoolean("log.info")) {
            listener.subscribe(LOG_INFO_TOPIC_NAME.getBytes(CHARSET));
        }
        return listener;
    }

    public ZMQ.Socket newBoundSocket(String s) {
        final ZMQ.Socket socket = ctx.socket(ZMQ.PUB);
	    socket.bind("tcp://*:51127");
        return socket;
    }

    protected MessageHandler getMsgHandler(String key) {
        return handlersFactory.get(key);
    }
}
