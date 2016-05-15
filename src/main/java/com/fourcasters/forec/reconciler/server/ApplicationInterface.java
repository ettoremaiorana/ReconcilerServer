package com.fourcasters.forec.reconciler.server;

import java.util.Deque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.zeromq.ZMQ.Context;

public interface ApplicationInterface {

	Context context();
	ExecutorService executor();
	Deque<Future<?>> futureTasks();
	BlockingQueue<SelectorTask> selectorTasks();
	
}
