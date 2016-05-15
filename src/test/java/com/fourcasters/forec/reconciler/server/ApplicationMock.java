package com.fourcasters.forec.reconciler.server;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

public class ApplicationMock implements ApplicationInterface {

	private BlockingQueue<SelectorTask> selectorTask = new ArrayBlockingQueue<>(512);
	private Deque<Future<?>> futureTasks = new ArrayDeque<>();
	

	@Override
	public Context context() {
		return ZMQ.context(1);
	}

	@Override
	public ExecutorService executor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Deque<Future<?>> futureTasks() {
		return futureTasks;
	}

	@Override
	public BlockingQueue<SelectorTask> selectorTasks() {
		return selectorTask;
	}

}
