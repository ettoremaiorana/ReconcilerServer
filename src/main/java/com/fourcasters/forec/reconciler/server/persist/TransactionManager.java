package com.fourcasters.forec.reconciler.server.persist;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import com.fourcasters.forec.reconciler.server.SelectorTask;
import com.fourcasters.forec.reconciler.server.persist.TradeTaskFactory.FullTask;
import com.fourcasters.forec.reconciler.server.persist.TradeTaskFactory.SingleTradeTask;

public class TransactionManager implements TransactionPhaseListener {

	private static final Logger LOG = LogManager.getLogger(TransactionManager.class);
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
		Transaction t = transactions.get(transId);
		LOG.info("TransId " + transId + " -> t? " + t);
		final FullTask task = taskFactory.newFullReconciliationTask(tradesInMessage, this, transId, t == null);

		if (t == null) {
			final Deque<Runnable> tasks = new ArrayDeque<>(16);
			t = new Transaction(tasks);
			transactions.put(transId, t);
		}
		t.add(task);
		onTaskStart();
		LOG.info("tasksToRun? " + tasksToRun);
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
			LOG.info("transaction.size.before? " + transactions.size());

			//TODO avoid iterator allocation using toArray(E[])
			final Iterator<Entry<Integer, Transaction>> it = transactions.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Integer, Transaction> e = it.next();
				if (!e.getValue().completed) {
					final Transaction t = e.getValue();
					final Runnable task = t.nextTask();
					if (task != null) {
						final Future<?> future = application.executor().submit(task);
						application.futureTasks().add(future);
					}
					else {
						application.selectorTasks().add(this);
					}
					break;
				}
				it.remove();
			}
			LOG.info("transaction.size.after? " + transactions.size());
		}
	};

	private final SelectorTask DECREASE_TASK_COUNT = new SelectorTask() {
		@Override
		public void run() {
			tasksToRun--;
			LOG.info("tasksToRun? " + tasksToRun);
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
		LOG.info("TransId " + transId + " -> on transaction end");
		application.selectorTasks().add(new SelectorTask() {
			@Override
			public void run() {
				LOG.info("transaction.size.before? " + transactions.size());

				final Transaction t = transactions.get(transId);
				if (t != null) {

					t.completed = true;
				}
				LOG.info("TransId " + transId + " -> completed? " + t.completed);
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
