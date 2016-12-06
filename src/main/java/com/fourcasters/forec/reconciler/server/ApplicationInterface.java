package com.fourcasters.forec.reconciler.server;

import java.util.Deque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.zeromq.ZMQ.Context;

public interface ApplicationInterface {

	Context context();
	ScheduledExecutorService executor();
	Deque<Future<?>> futureTasks();
	BlockingQueue<SelectorTask> selectorTasks();
	
}
