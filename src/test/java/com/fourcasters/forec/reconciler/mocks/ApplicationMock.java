package com.fourcasters.forec.reconciler.mocks;
import static org.mockito.Mockito.when;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.BeforeClass;
import org.mockito.Mockito;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import com.fourcasters.forec.reconciler.server.SelectorTask;

import static org.mockito.Matchers.any;
public class ApplicationMock implements ApplicationInterface {

	private BlockingQueue<SelectorTask> selectorTask = new ArrayBlockingQueue<>(512);
	private Deque<Future<?>> futureTasks = new ArrayDeque<>();
	private ExecutorService executor = Mockito.mock(ExecutorService.class);
	private final Future DUMMY_FUTURE = Mockito.mock(Future.class);
	
	public ApplicationMock() {
		when(executor.submit(any(Runnable.class))).thenReturn(DUMMY_FUTURE);
	}

	@Override
	public Context context() {
		return ZMQ.context(1);
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
		return selectorTask;
	}

}