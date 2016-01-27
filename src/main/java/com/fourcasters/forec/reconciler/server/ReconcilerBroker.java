package com.fourcasters.forec.reconciler.server;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class ReconcilerBroker {
	private static final String HISTORY_TOPIC_NAME = "HISTORY@";
	private static final int bufferSize = 10240;
	private static final byte[] TOPIC_NAME_IN_INPUT = new byte[bufferSize];
	private static final byte[] DATA_IN_INPUT = new byte[bufferSize];
	private static final ByteBuffer TOPIC_BUFFER = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
	private static final ByteBuffer DATA_BUFFER = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
	private static final Charset CHARSET = Charset.forName("US-ASCII");
	private static final Persister persister = new Persister();
	private static boolean running;
	private static String topicName;
	private static String data;
	public static void main(String[] args) throws UnsupportedEncodingException {
		//bind to endpoint
		final Context ctx = ZMQ.context(1);
		final Socket server = ctx.socket(ZMQ.SUB);
		server.bind("tcp://*:51127");
		server.subscribe(HISTORY_TOPIC_NAME.getBytes());
		persister.start();
		running = true;
		boolean append = false;
		//start listening to messages
		registerSignalHandler();
		while (running) {
			//TODO _recvDirectBuffer has to change to take the starting position as a parameter
			int recvTopicSize = server._recvDirectBuffer(TOPIC_BUFFER, bufferSize, ZMQ.NOBLOCK);
			if (recvTopicSize <= 0) {
				//TODO Back off strategy
				LockSupport.parkNanos(1000000000L); //bleah
				continue;
			}
			int recvDataSize = readTopicAndData(server);
			//parse data
			final String[] tradesAsString = data.split("|");
			//TODO optimised for memory consumption as the server is low on memory.
			//Please use a byte buffer pool.
			persister.enqueue(new Persister.PersistTask(tradesAsString, append));
			
			TOPIC_BUFFER.clear();
			DATA_BUFFER.clear();

			//if last bit of data is 'more', next time we read we append the new records
			//to the existing ones.
			if (tradesAsString[tradesAsString.length - 1].equals("more")) {
				append = true;
			}
			else {
				append = false;
			}
		}
	}
	
	

	private static void registerSignalHandler() {
		ZMQ.register_signalhandler();
	}



	private static int readTopicAndData(final Socket server) throws UnsupportedEncodingException {
		TOPIC_BUFFER.get(TOPIC_NAME_IN_INPUT); //read only the bits just read
		topicName = new String(TOPIC_NAME_IN_INPUT, CHARSET);
		int recvDataSize = 0;
		while (recvDataSize == 0) {
			recvDataSize = server._recvDirectBuffer(DATA_BUFFER, bufferSize, ZMQ.NOBLOCK);
		}
		data = new String(DATA_IN_INPUT, CHARSET);
		return recvDataSize;
	}

}
