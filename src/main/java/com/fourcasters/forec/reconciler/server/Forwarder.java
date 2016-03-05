package com.fourcasters.forec.reconciler.server;

import java.util.concurrent.Future;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class Forwarder implements MessageHandler {
	final static Context ctx = Application.context;
	final static Socket socket = ctx.socket(ZMQ.PUB);

	static {
		socket.bind("tcp://*:51127");
	}

	@Override
	public void enqueue(String topic, String message) {
		final Future<?>f = Application.executor.submit(new Runnable() {
			@Override
			public void run() {
				final String newTopic = "RECONCILER" + topic.substring(topic.indexOf("@"));
				socket.send(newTopic.getBytes(), ZMQ.SNDMORE);
				socket.send(message.getBytes(), 0);		
			}
		});
		Application.tasks.offer(f);
	}

}
