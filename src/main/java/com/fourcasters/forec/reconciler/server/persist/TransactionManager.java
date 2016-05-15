package com.fourcasters.forec.reconciler.server.persist;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import com.fourcasters.forec.reconciler.server.SelectorTask;
import com.fourcasters.forec.reconciler.server.persist.TradeTaskFactory.FullTask;
import com.fourcasters.forec.reconciler.server.persist.TradeTaskFactory.SingleTradeTask;

public class TransactionManager implements TransactionPhaseListener {

	private int tasksToRun;
	private final TradeTaskFactory taskFactory;
	private final LinkedHashMap<Integer, Transaction> transactions = new LinkedHashMap<>();
	private final ApplicationInterface application;

	public TransactionManager(TradeTaskFactory taskFactory, ApplicationInterface application) {
		super();
		this.tasksToRun = 0;
		this.taskFactory = taskFactory;
		this.application = application;
	}

	public void onFullTransaction(Integer transId, String tradesInMessage) {
		final FullTask task = taskFactory.newFullReconciliationTask(tradesInMessage, this, transId);

		Transaction t = transactions.get(transId);
		if (t == null) {
			final Deque<Runnable> tasks = new ArrayDeque<>(16);
			t = new Transaction(tasks);
			transactions.put(transId, t);
		}
		t.add(task);
		onTaskStart();
	}

	public void onSingleTransaction(int transId, String tradesInMessage) {
		final SingleTradeTask task = taskFactory.newSingleTradeTask(tradesInMessage, this, transId);
		final Deque<Runnable> tasks = new ArrayDeque<>(1);
		tasks.add(task);
		final Transaction t = new Transaction(tasks);
		transactions.put(transId, t);
		onTaskStart();
	}

	private final SelectorTask POLLING_TASK = new SelectorTask() {
		@Override
		public void run() {
			//TODO avoid iterator allocation using toArray(E[])
			final Iterator<Entry<Integer, Transaction>> it = transactions.entrySet().iterator();
			Entry<Integer, Transaction> e = it.next();
			while (e.getValue().completed && it.hasNext()) {
				transactions.remove(e.getKey());
				e = it.next();
			}

			final Transaction t = e.getValue();
			final Runnable task = t.nextTask();
			if (task != null) {
				final Future<?> future = application.executor().submit(task);
				application.futureTasks().add(future);
			}
			else {
				application.selectorTasks().add(this);
			}
		}
	};

	private final SelectorTask DECREASE_TASK_COUNT = new SelectorTask() {
		@Override
		public void run() {
			tasksToRun--;
		}
	};


	int numberOfTransactions() {
		return transactions.size();
	}

	public int tasksToRun() {
		return tasksToRun;
	}

	static class Transaction {

		private final Deque<Runnable> tasks;
		private boolean completed;

		private Transaction(Deque<Runnable> tasks) {
			completed = false;
			this.tasks = tasks;
		}

		private void add(Runnable task) {
			tasks.offer(task);
		}

		private Runnable nextTask() {
			assert tasks.size() > 0;
			return tasks.poll();
		}

		void complete() {
			completed = true;
		}
	}

	@Override
	public void onTransactionStart(int transId) {

	}

	@Override
	public void onTransactionEnd(int transId) {
		application.selectorTasks().add(new SelectorTask() {
			@Override
			public void run() {
				final Transaction t = transactions.get(transId);
				if (t != null) {
					t.completed = true;
				}
			}
		});
	}

	@Override
	public void onTaskEnd() {
		application.selectorTasks().add(DECREASE_TASK_COUNT);
	}

	@Override
	public void onTaskStart() {
		tasksToRun++;
		//add polling task to selector task queue
		application.selectorTasks().add(POLLING_TASK);
	}

}
