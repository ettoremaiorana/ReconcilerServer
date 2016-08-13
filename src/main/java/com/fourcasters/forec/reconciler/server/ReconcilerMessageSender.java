package com.fourcasters.forec.reconciler.server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.CHARSET;
public class ReconcilerMessageSender {

	private final static Logger LOG = LogManager.getLogger(ReconcilerMessageSender.class);
	private final static byte[] OPEN_IN_BYTES = "OPEN".getBytes(CHARSET);
	private final Context ctx;
	private final Socket socket;

	public ReconcilerMessageSender(ApplicationInterface application) {
		ctx = application.context();
		socket  = ctx.socket(ZMQ.PUB);
		socket.connect("tcp://localhost:51125");
	}

	public boolean askForClosedTrades(String ticket, String topic) {
		final String message = "SINGLE="+ticket;
		LOG.info("Sending '" + message + "' on topic " + topic);
		socket.send(topic.getBytes(CHARSET), ZMQ.SNDMORE);
		return socket.send(message.getBytes(CHARSET), 0);
	}

	public boolean askForOpenTrades(String newTopic) {
		final String message = "OPEN";
		LOG.info("Sending '" + message + "' on topic " + newTopic);
		socket.send(newTopic.getBytes(CHARSET), ZMQ.SNDMORE);
		return socket.send(OPEN_IN_BYTES, 0);
	}
}
