package com.fourcasters.forec.reconciler.server.persist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import com.fourcasters.forec.reconciler.server.ApplicationInterface;

public class TradeTaskFactory {


	public TradeTaskFactory(ApplicationInterface application) {
	}

	public SingleTradeTask newSingleTradeTask(String tradesInMessage, TransactionPhaseListener listener, int transId) {
		return new SingleTradeTask(new Runnable() {
			@Override
			public void run() {
				final String[] trades = tradesInMessage.split("\\|");
				//if last bit of data is not 'more', we create a new selector task to end the transaction
				final boolean autoflush = false;
				final File f = new File("Trades.csv");
				if (trades[trades.length - 1].trim().equals("more")) {
					trades[trades.length - 1] = "";
				}
				try(final PrintWriter pw = new PrintWriter(new FileOutputStream(f, true), autoflush);) {
					for (int i = 0; i < trades.length-1; i++) {
						if (!trades[i].trim().equals("")) {
							pw.write(trades[i]);
						}
						pw.write(",\n");
					}
					pw.write(trades[trades.length - 1]);
					pw.flush();
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				} finally {
					listener.onTaskEnd();
					listener.onTransactionEnd(transId);
				}
			}
		});
	}

	public FullTask newFullReconciliationTask(final String tradesInMessage, TransactionPhaseListener listener, int transId, boolean first) {
		return new FullTask(new Runnable() {

			@Override
			public void run() {
				final String[] trades = tradesInMessage.split("\\|");
				//if last bit of data is not 'more', we create a new selector task to end the transaction
				final boolean autoflush = false;
				final boolean toBeContinued = trades[trades.length - 1].trim().equals("more");
				final File f = new File("Trades.csv");
				if (toBeContinued) {
					trades[trades.length - 1] = "";
				}
				try(final PrintWriter pw = new PrintWriter(new FileOutputStream(f, !first), autoflush);) {
					for (int i = 0; i < trades.length-1; i++) {
						if (!trades[i].trim().equals("")) {
							pw.write(trades[i]);
						}
						pw.write(",\n");
					}
					pw.write(trades[trades.length - 1]);
					pw.flush();
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
				finally {
					listener.onTaskEnd();
					if (!trades[trades.length - 1].trim().equals("more")) {
						listener.onTransactionEnd(transId);
					}
				}
			}
		});
	}

	static class SingleTradeTask implements Runnable {
		private final Runnable r;
		private SingleTradeTask(Runnable r) {
			this.r = r;
		}
		@Override
		public void run() {
			r.run();
		}
	}

	static class FullTask implements Runnable {
		private final Runnable r;
		private FullTask(Runnable r) {
			this.r = r;
		}

		@Override
		public void run() {
			r.run();
		}
	}
}
