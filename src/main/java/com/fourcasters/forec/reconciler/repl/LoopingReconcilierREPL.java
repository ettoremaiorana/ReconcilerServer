package com.fourcasters.forec.reconciler.repl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class LoopingReconcilierREPL {

	public static void main(String[] args) throws IOException {
		final Context ctx = ZMQ.context(1);
		final Socket socket = ctx.socket(ZMQ.PUB);
		//socket.connect("tcp://52.88.34.166:51125");
//		socket.connect("tcp://52.33.13.29:51125");
		socket.connect("tcp://localhost:51125");
		
		//socket.connect("tcp://52.33.13.29:50026");
		
		while (true) {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("Topic: ");
			final String topic = reader.readLine();
			System.out.print("Message: ");
			assert topic != null;
			final String message = reader.readLine();
			socket.send(topic.getBytes(), ZMQ.SNDMORE);
			assert message != null;
			socket.send(message.getBytes(), 0);
		}
	}
	
}
