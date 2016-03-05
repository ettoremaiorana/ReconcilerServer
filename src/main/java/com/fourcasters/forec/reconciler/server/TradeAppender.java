package com.fourcasters.forec.reconciler.server;

import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class TradeAppender implements MessageHandler {

	private final static Logger LOG = LogManager.getLogger(TradeAppender.class);
	final static Context ctx = Application.context;
	final static Socket socket = ctx.socket(ZMQ.PUB);

	static {
		socket.connect("tcp://localhost:51127");
	}
	@Override
	public void enqueue(String topic, String data) {
		final Future<?> future = Application.executor.submit(new Runnable() {
			@Override
			public void run() {
				LOG.info(topic + " = " + data);
				//example: buffer = status + "," + type + "," + price + "," + ticket;
				final long ticket = Long.parseLong(data.split(",")[3]);
				
				final String newTopic = "RECONCILER" + topic.substring(topic.indexOf("@"));
				socket.send(newTopic.getBytes(), ZMQ.SNDMORE);
				
				//TODO
//				final String message = "ticket="+ticket;
				final String message = "full";
				socket.send(message.getBytes(), 0);
				LOG.info(newTopic + " : " + message);
			}
		});
		Application.tasks.add(future);
	}

}
