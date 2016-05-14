package com.fourcasters.forec.reconciler.server;

import java.util.concurrent.Future;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class Forwarder implements MessageHandler {
	private final Context ctx;
	private final Socket socket;
	private final ApplicationInterface application;

	public Forwarder(ApplicationInterface application) {
		this.application = application;
		this.ctx = application.context();
		this.socket = ctx.socket(ZMQ.PUB);
		socket.bind("tcp://*:51127");
	}

	@Override
	public void enqueue(String topic, String message) {
		final Future<?>f = application.executor().submit(new Runnable() {
			@Override
			public void run() {
				final String newTopic = "RECONCILER" + topic.substring(topic.indexOf("@"));
				socket.send(newTopic.getBytes(), ZMQ.SNDMORE);
				socket.send(message.getBytes(), 0);		
			}
		});
		application.futureTasks().offer(f);
	}

}
