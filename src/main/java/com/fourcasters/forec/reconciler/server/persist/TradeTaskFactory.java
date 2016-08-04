package com.fourcasters.forec.reconciler.server.persist;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.*;

public class TradeTaskFactory {

	private static final File closed_trades_file = new File(CLOSED_TRADES_FILE_NAME);
	private static final File open_trades_file = new File(OPEN_TRADES_FILE_NAME);
	private static final Logger LOG = LogManager.getLogger(TradeTaskFactory.class);
//	private static final Runnable EMPTY_TASK = new Runnable(){public void run(){}};

	public TradeTaskFactory(ApplicationInterface application) {
	}


	public OpenTradeTask newOpenTradeTask(String tradesInMessage, TransactionPhaseListener listener, int transId, boolean first) {
		LOG.info("TransId " + transId + " -> opens, first? " + first);
		return new OpenTradeTask(new MultiPartsTransaction(tradesInMessage, listener, transId, first, open_trades_file));
	}

	public FullTask newFullReconciliationTask(final String tradesInMessage, TransactionPhaseListener listener, int transId, boolean first) {
		LOG.info("TransId " + transId + " -> full, first? " + first);
		return new FullTask(new MultiPartsTransaction(tradesInMessage, listener, transId, first, closed_trades_file));
	}

	public SingleTradeTask newSingleTradeTask(String tradesInMessage, TransactionPhaseListener listener, int transId) {
		LOG.info("TransId " + transId + " -> single");
		return new SingleTradeTask(new Runnable() {
			@Override
			public void run() {
				final String[] trades = tradesInMessage.split("\\|");
				//if last bit of data is not 'more', we create a new selector task to end the transaction
				final boolean autoflush = false;
				if (trades[trades.length - 1].trim().equals("more")) {
					trades[trades.length - 1] = "";
				}
				try(final PrintWriter pw = new PrintWriter(new FileOutputStream(closed_trades_file, true), autoflush);) {
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

	private static class MultiPartsTransaction implements Runnable {
		private final File f;
		private final boolean first;
		private final int transId;
		private final TransactionPhaseListener listener;
		private final String tradesInMessage;

		private MultiPartsTransaction(String tradesInMessage, TransactionPhaseListener listener, int transId, boolean first, File f) {
			this.f = f;
			this.tradesInMessage = tradesInMessage;
			this.listener = listener;
			this.transId = transId;
			this.first = first;
		}

		@Override
		public void run() {
			final String[] trades = tradesInMessage.split("\\|");
			//if last bit of data is not 'more', we create a new selector task to end the transaction
			final boolean autoflush = false;
			final boolean toBeContinued = trades[trades.length - 1].trim().equals("more");
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
				if (!toBeContinued) {
					listener.onTransactionEnd(transId);
				}
			}

		}
	}

	static class OpenTradeTask implements Runnable {
		private final Runnable r;
		private OpenTradeTask(Runnable r) {
			this.r = r;
		}
		@Override
		public void run() {
			r.run();
		}
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
