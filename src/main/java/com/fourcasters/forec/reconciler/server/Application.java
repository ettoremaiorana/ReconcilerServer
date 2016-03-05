package com.fourcasters.forec.reconciler.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

public class Application {

	public static final Context context = ZMQ.context(1);
	public static final ExecutorService executor = Executors.newFixedThreadPool(Integer.getInteger("pool.thread.count", 1));
	public static final Deque<Future<?>> tasks = new ArrayDeque<>(512);
	
}
