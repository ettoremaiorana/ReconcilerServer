package com.fourcasters.forec.reconciler.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

public class Application implements ApplicationInterface {

	private static final Context context = ZMQ.context(1);
	//Thread pool executing async tasks
	private static final ExecutorService executor = Executors.newFixedThreadPool(Integer.getInteger("pool.thread.count", 1));
	//Queue of pending tasks. Not thread safe, but doesn't matter here because offer/poll is performed by a single thread.
	private static final Deque<Future<?>> futureTasks = new ArrayDeque<>(512);
	//Queue of tasks to be executed by the main thread, so to avoid lock and contention.
	private static final BlockingQueue<SelectorTask> selectorTasks = new ArrayBlockingQueue<>(512);
	@Override
	public Context context() {
		return context;
	}
	@Override
	public ExecutorService executor() {
		return executor;
	}
	@Override
	public Deque<Future<?>> futureTasks() {
		return futureTasks;
	}
	@Override
	public BlockingQueue<SelectorTask> selectorTasks() {
		return selectorTasks;
	}

	
}
