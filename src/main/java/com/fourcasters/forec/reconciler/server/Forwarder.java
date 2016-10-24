package com.fourcasters.forec.reconciler.server;

import java.util.concurrent.Future;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class Forwarder implements MessageHandler, AutoCloseable {

    public static Forwarder create(ApplicationInterface application) {
        final Context ctx = application.context();
        final Socket socket = ctx.socket(ZMQ.PUB);
	socket.bind("tcp://*:51127");
        return new Forwarder(application, socket);
    }
	private final Socket socket;
	private final ApplicationInterface application;

	private Forwarder(ApplicationInterface application, Socket socket) {
		this.application = application;
		this.socket = socket;
	}

	@Override
	public void enqueue(String topic, String message) {
		final Future<?>f = application.executor().submit(new Runnable() {
			@Override
			public void run() {
				final String newTopic = "RECONCILER" + topic.substring(topic.indexOf("@"));
				socket.send(newTopic.getBytes(ProtocolConstants.CHARSET), ZMQ.SNDMORE);
				socket.send(message.getBytes(ProtocolConstants.CHARSET), 0);		
			}
		});
		application.futureTasks().offer(f);
	}

    @Override
    public void close() throws Exception {
        if (socket != null) {
            socket.close();
        }
    }

}
