package com.fourcasters.forec.reconciler.mocks;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fourcasters.forec.reconciler.server.EventHandler;
import org.mockito.Mockito;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import com.fourcasters.forec.reconciler.server.SelectorTask;

public class ApplicationMock implements ApplicationInterface {

	private Queue<SelectorTask> selectorTask = new ArrayDeque<>(64);
	private Deque<Future<?>> futureTasks = new ArrayDeque<>();
	private ScheduledExecutorService executor = Mockito.mock(ScheduledExecutorService.class);
	@SuppressWarnings("rawtypes")
	private final Future DUMMY_FUTURE = Mockito.mock(Future.class);
	private final ScheduledFuture DUMMY_SCH_FUTURE = Mockito.mock(ScheduledFuture.class);
    private final List<TaskEvent> submittedEvents = new ArrayList<>(64);
    private final List<TaskEvent> scheduledEvents = new ArrayList<>(64);
    private final List<EventHandler> handlers = new ArrayList<>();

	@SuppressWarnings("unchecked")
	public ApplicationMock() {
		when(executor.submit(any(Runnable.class))).thenReturn(DUMMY_FUTURE);
		when(executor.scheduleAtFixedRate(any(Runnable.class),
				any(Long.class),
				any(Long.class),
				any(TimeUnit.class))).thenReturn(DUMMY_SCH_FUTURE);
	}

	@Override
	public int select() {
	    int n = 0;
		while(!selectorTask.isEmpty()) {
			SelectorTask task = selectorTask.remove();
			task.run();
			n++;
		}
		return n;
	}

	@Override
	public boolean enqueue(SelectorTask task) {
		return selectorTask.add(task);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long delay, long period, TimeUnit tu)
    {
        scheduledEvents.add(new TaskEvent(runnable.getClass(), runnable));
		return null;
	}

	@Override
	public boolean submit(Runnable r) {
		submittedEvents.add(new TaskEvent(r.getClass(), r));
		return true;
	}

	@Override
	public int taskSize() {
		return selectorTask.size();
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
		// nothing to close, I'm just a mock
	}

	public void reset() {
	    submittedEvents.clear();
    }

    public void execute() {
	    submittedEvents.stream().filter(e -> !e.executed).forEach(e -> {
	        e.task.run();
	        e.executed = true;
        });
    }

    public void executeScheduled() {
        scheduledEvents.stream().filter(e -> !e.executed).forEach(e -> {
            e.task.run();
            e.executed = true;
        });
    }

    private static class TaskEvent {
	    private final Class<?> eventType;
        private final Runnable task;
        private boolean executed;

        private TaskEvent(Class<?> eventType, Runnable task) {
            this.eventType = eventType;
            this.task = task;
            this.executed = false;
        }
    }

    public boolean hasScheduledEvent(Class<?> c) {
        return scheduledEvents.stream().anyMatch(e -> e.eventType.equals(c));
    }
    public boolean hadSubmittedEvent(Class<?> c) {
	    return submittedEvents.stream().anyMatch(e -> e.eventType.equals(c));
    }

    public boolean hadScheduledEvents(List<Class<?>> c) {
        List<TaskEvent> copy = new ArrayList<>(scheduledEvents);
        c.removeAll(copy.stream().map(e -> e.eventType).collect(Collectors.toList()));
        return c.isEmpty();
    }
    public boolean hadSubmittedEvents(List<Class<?>> c) {
	    List<TaskEvent> copy = new ArrayList<>(submittedEvents);
        c.removeAll(copy.stream().map(e -> e.eventType).collect(Collectors.toList()));
        return c.isEmpty();
    }

    public boolean hadExactlyScheduledEvents(List<Class<?>> c) {
        boolean sameSize = c.size() == scheduledEvents.size();
        return sameSize && hadSubmittedEvents(c);
    }
    public boolean hadExactlySubmittedEvents(List<Class<?>> c) {
	    boolean sameSize = c.size() == submittedEvents.size();
	    return sameSize && hadSubmittedEvents(c);
    }
}
