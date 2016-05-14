package com.fourcasters.forec.reconciler.server.persist;

import java.util.ArrayDeque;

import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import com.fourcasters.forec.reconciler.server.SelectorTask;

public class ApplicationMock implements ApplicationInterface {

	private Deque<SelectorTask> selectorTask = new ArrayDeque<>();
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
	public Deque<SelectorTask> selectorTasks() {
		return selectorTask;
	}

}
