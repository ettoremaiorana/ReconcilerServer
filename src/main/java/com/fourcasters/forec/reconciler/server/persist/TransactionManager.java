package com.fourcasters.forec.reconciler.server.persist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import com.fourcasters.forec.reconciler.server.SelectorTask;
import com.fourcasters.forec.reconciler.server.persist.TaskFactory.FullTask;
import com.fourcasters.forec.reconciler.server.persist.TaskFactory.SingleTradeTask;

public class TransactionManager implements TransactionPhaseListener {

	private int tasksToRun;
	private final TaskFactory taskFactory;
	private final LinkedHashMap<Integer, Transaction> transactions = new LinkedHashMap<>();
	private final ApplicationInterface application;

	public TransactionManager(TaskFactory taskFactory, ApplicationInterface application) {
		super();
		this.tasksToRun = 0;
		this.taskFactory = taskFactory;
		this.application = application;
	}

	public void onFullTransaction(Integer transId, String tradesInMessage) {
		final FullTask task = taskFactory.newFullReconciliationTask(tradesInMessage, this, transId);

		Transaction t = transactions.get(transId);
		if (t == null) {
			final ArrayList<Runnable> tasks = new ArrayList<>(16);
			t = new Transaction(tasks);
			transactions.put(transId, t);
		}
		t.add(task);
		onNewTask();
	}

	public void onSingleTransaction(int transId, String tradesInMessage) {
		final SingleTradeTask task = taskFactory.newSingleTradeTask(tradesInMessage, this, transId);
		final ArrayList<Runnable> tasks = new ArrayList<>(1);
		tasks.add(task);
		final Transaction t = new Transaction(tasks);
		transactions.put(transId, t);
		onNewTask();
	}

	private void onNewTask() {
		tasksToRun++;
		//add polling task to selector task queue
		application.selectorTasks().add(new SelectorTask() {
			@Override
			public void run() {
				final Entry<Integer, Transaction> e = transactions.entrySet().iterator().next();
				final Transaction t = e.getValue();
				final Future<?> future = application.executor().submit(t.nextTask());
				application.futureTasks().add(future);
				if (t.completed) {
					transactions.remove(e.getKey());
				}
			}
		});
	}


	int numberOfTransactions() {
		return transactions.size();
	}

	public int tasksToRun() {
		return tasksToRun;
	}

	static class Transaction {

		private final ArrayList<Runnable> tasks;
		private boolean completed;

		private Transaction(ArrayList<Runnable> tasks) {
			completed = false;
			this.tasks = tasks;
		}

		private void add(Runnable task) {
			tasks.add(task);
		}

		private Runnable nextTask() {
			assert tasks.size() > 0;
			return tasks.remove(0);
		}

		void complete() {
			completed = true;
		}
	}



	final Runnable ON_TASK_COMPLETE = new Runnable() {
		public void run() {
			tasksToRun--;
		}
	};

	@Override
	public void onTransactionStart(int transId) {

	}

	@Override
	public void onTransactionEnd(int transId) {
		application.selectorTasks().add(new SelectorTask() {
			@Override
			public void run() {
				transactions.remove(transId);
			}
		});
	}

}
