package com.fourcasters.forec.reconciler.server.trades.persist;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import com.fourcasters.forec.reconciler.server.SelectorTask;
import com.fourcasters.forec.reconciler.server.trades.persist.TradeTaskFactory.FullTask;
import com.fourcasters.forec.reconciler.server.trades.persist.TradeTaskFactory.OpenTradeTask;
import com.fourcasters.forec.reconciler.server.trades.persist.TradeTaskFactory.SingleTradeTask;

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
		LOG.info("tasksToRun? " + tasksToRun);
	}


	public void onOpenTransaction(int transId, String tradesInMessage) {
		Transaction t = transactions.get(transId);
		LOG.info("TransId " + transId + " -> t? " + t);
		final OpenTradeTask task = taskFactory.newOpenTradeTask(tradesInMessage, this, transId, t == null);

		if (t == null) {
			final Deque<Runnable> tasks = new ArrayDeque<>(16);
			t = new Transaction(tasks);
			transactions.put(transId, t);
		}
		t.add(task);
		onTaskStart();
		LOG.info("tasksToRun? " + tasksToRun);
	}

	private final SelectorTask POLLING_TASK = new SelectorTask() {
		@Override
		public void run() {
			LOG.info("transaction.size.before? " + transactions.size());

			//TODO avoid iterator allocation using toArray(E[])
			final Iterator<Entry<Integer, Transaction>> it = transactions.entrySet().iterator();
			boolean toEnqueueAgain = false;
			while (it.hasNext()) {
				Entry<Integer, Transaction> e = it.next();
				if (!e.getValue().completed) {
					final Transaction t = e.getValue();
					final Runnable task = t.nextTask();
					LOG.info("First transaction in the queue: " + t);
					if (task != null) {
						LOG.info("Next task: " + task);
						application.submit(task);
					}
					else {
						t.waiting -= 1;
						if (t.waiting == 0) {
							LOG.warn("Expired transaction: " + t);
							it.remove();
						}
						toEnqueueAgain = true;
					}
					break;
				} 
				else {
					it.remove();
					toEnqueueAgain = true;
				}
			}
			int size = transactions.size();
			LOG.info("transaction.size.after? " + size);
			if (size > 0) {
				final Iterator<Entry<Integer, Transaction>> localIt = transactions.entrySet().iterator();
				Entry<Integer, Transaction> e;
				if ((e = localIt.next()) != null) {
					LOG.info("Pending transaction id " + e.getKey() + " = " + e.getValue());
				}
			}
			if (toEnqueueAgain) {
				application.enqueue(this);
			}
		}
	};

	private final SelectorTask DECREASE_TASK_COUNT = new SelectorTask() {
		@Override
		public void run() {
			tasksToRun--;
			LOG.info("Decrease: tasksToRun? " + tasksToRun);
		}
	};


	int numberOfTransactions() {
		return transactions.size();
	}

	int tasksToRun() {
		return tasksToRun;
	}

	static class Transaction {

		private int waiting;
		private final Deque<Runnable> tasks;
		private boolean completed;
		private boolean started;

		private Transaction(Deque<Runnable> tasks) {
			this.waiting = 10;
			this.completed = false;
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

		void start() {
			started = true;
		}

		@Override
		public String toString() {
			return "Transaction [waiting=" + waiting + ", tasks=" + tasks + ", completed=" + completed + "]";
		}
		
	}

	@Override
	public void onTransactionStart(int transId) {
		LOG.info("TransId " + transId + " -> on transaction start");
		application.enqueue(new SelectorTask() {
			@Override
			public void run() {
				LOG.info("transaction.size.before? " + transactions.size());
				final Transaction t = transactions.get(transId);
				if (t != null) {
					t.started = true;
					LOG.info("TransId " + transId + " -> started? " + t.started);
				}
				else {
					LOG.info("TransId " + transId + " -> NULL ");	
				}
				LOG.info("transaction.size.after? " + transactions.size());

			}
		});

	}

	@Override
	public void onTransactionEnd(int transId) {
		LOG.info("TransId " + transId + " -> on transaction end");
		application.enqueue(() -> {
            LOG.info("transaction.size.before? " + transactions.size());

            final Transaction t = transactions.remove(transId);
            if (t != null) {

                t.completed = true;
                LOG.info("TransId " + transId + " -> completed? " + t.completed);
            }
            else {
                LOG.info("TransId " + transId + " -> NULL ");
            }
            LOG.info("transaction.size.after? " + transactions.size());

        });
		LOG.info("Selector tasks in the queue: " + application.taskSize());
	}

	@Override
	public void onTaskEnd() {
		application.enqueue(DECREASE_TASK_COUNT);
	}

	@Override
	public void onTaskStart() {
		tasksToRun++;
		//add polling task to selector task queue
		application.enqueue(POLLING_TASK);
	}
}
