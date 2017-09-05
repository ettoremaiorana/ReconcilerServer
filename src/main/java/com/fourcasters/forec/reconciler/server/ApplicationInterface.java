package com.fourcasters.forec.reconciler.server;

import java.util.Deque;
import java.util.concurrent.*;

import org.zeromq.ZMQ.Context;

public interface ApplicationInterface {

	Context context();
	Deque<Future<?>> futureTasks();
    int select();
    boolean enqueue(SelectorTask task);
	ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long delay, long period, TimeUnit tu);
    boolean submit(Runnable r);
	int taskSize();
}
