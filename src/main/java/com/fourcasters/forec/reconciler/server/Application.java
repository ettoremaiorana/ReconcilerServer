package com.fourcasters.forec.reconciler.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.*;

import org.apache.logging.log4j.*;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

public class Application implements ApplicationInterface {

	//Thread pool executing async tasks
	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Integer.getInteger("pool.thread.count", 1));
	//Queue of pending tasks. Not thread safe, but doesn't matter here because offer/poll is performed by a single thread.
	private static final Deque<Future<?>> futureTasks = new ArrayDeque<>(512);
	//Queue of tasks to be executed by the main thread, so to avoid lock and contention.
	private static final BlockingQueue<SelectorTask> selectorTasks = new ArrayBlockingQueue<>(512);
    //List of event handlers
	private static final List<EventHandler> handlers = new ArrayList<>(15);
	private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(Application.class);

	@Override
	public int select() {
		Deque<Future<?>> tasks = futureTasks;
		if (tasks.size() > 0) {
			tasks.removeIf(
					f -> f.isDone() && logIfException(f));
		}

		SelectorTask task;
		final int size = selectorTasks.size() * 2;
		int inc = 0;
		while (inc < size && (task = selectorTasks.poll()) != null) {
			task.run();
			inc++; //this is to avoid task to enqueue itself, so ending in an infinite loop.
		}
		return inc;
	}

	@Override
	public boolean enqueue(SelectorTask task) {
		return selectorTasks.add(task);
	}


	private static boolean logIfException(Future<?> f) {
		try {
			f.get();
			LOG.info(f + " future has finished");
		} catch (InterruptedException | ExecutionException e) {
			LOG.error("Computation error", e);
		}
		return true;
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long delay, long period, TimeUnit tu) {
		return executor.scheduleAtFixedRate(r, delay, period, tu);
	}

	@Override
	public boolean submit(Runnable r) {
		final Future<?> f = executor.submit(r);
		return selectorTasks.add(() -> futureTasks.add(f));
	}

	@Override
	public int taskSize() {
    	return selectorTasks.size();
	}

	@Override
	public void registerEventHandler(EventHandler handler) {
		handlers.add(handler);
	}

	@Override
	public int handleEvents() {
		return handlers.stream().mapToInt(eh -> eh.handle()).sum();
	}

	@Override
	public void close() {
		executor.shutdown();
	}
}
