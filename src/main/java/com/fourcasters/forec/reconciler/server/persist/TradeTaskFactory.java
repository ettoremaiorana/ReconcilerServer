package com.fourcasters.forec.reconciler.server.persist;

import static com.fourcasters.forec.reconciler.server.ProtocolConstants.CHARSET;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.CLOSED_TRADES_FILE_NAME;
import static com.fourcasters.forec.reconciler.server.ProtocolConstants.OPEN_TRADES_FILE_NAME;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import com.fourcasters.forec.reconciler.server.ReconcilerMessageSender;
import com.fourcasters.forec.reconciler.server.SelectorTask;

public class TradeTaskFactory {

	private static final File closed_trades_file = new File(CLOSED_TRADES_FILE_NAME);
	private static final File open_trades_file = new File(OPEN_TRADES_FILE_NAME);
	private static final Logger LOG = LogManager.getLogger(TradeTaskFactory.class);
	private final ReconcilerMessageSender reconcilerMessageSender;
	private final ApplicationInterface application;

	private final Runnable OPEN_TRADES_REQUEST = new Runnable() {
		@Override
		public void run() {
			LOG.info("Open trades transaction completed, starting a new one");
			reconcilerMessageSender.askForOpenTrades("RECONC@ACTIVTRADES@EURUSD@1002");
		}
	};
	
	private final TransactionPhaseListener openTradesTransactionCompletionListener =
			new TransactionPhaseListener() {

		@Override
		public void onTransactionStart(int transId) {
		}

		@Override
		public void onTransactionEnd(int transId) {
			application.enqueue(() -> application.scheduleAtFixedRate(OPEN_TRADES_REQUEST, 0l, 5000l, TimeUnit.MILLISECONDS));
		}

		@Override
		public void onTaskStart() {
		}

		@Override
		public void onTaskEnd() {
		}
	};
	
	public TradeTaskFactory(ApplicationInterface application, ReconcilerMessageSender reconcilerMessageSender) {
		this.application = application;
		this.reconcilerMessageSender = reconcilerMessageSender;
	}

	public OpenTradeTask newOpenTradeTask(String tradesInMessage, TransactionPhaseListener listener, int transId, boolean first) {
		LOG.info("TransId " + transId + " -> opens, first? " + first);
		return new OpenTradeTask(new MultiPartsTransaction(tradesInMessage, Arrays.asList(listener, openTradesTransactionCompletionListener) , transId, first, open_trades_file));
	}

	public FullTask newFullReconciliationTask(final String tradesInMessage, TransactionPhaseListener listener, int transId, boolean first) {
		LOG.info("TransId " + transId + " -> full, first? " + first);
		return new FullTask(new MultiPartsTransaction(tradesInMessage, Arrays.asList(listener), transId, first, closed_trades_file));
	}

	public SingleTradeTask newSingleTradeTask(String tradesInMessage, TransactionPhaseListener listener, int transId) {
		LOG.info("TransId " + transId + " -> single");
		return new SingleTradeTask(new SinglePartTransaction(tradesInMessage, listener, transId));
	}

	private static class SinglePartTransaction implements Runnable {
		private final String tradesInMessage;
		private final TransactionPhaseListener listener;
		private final int transId;

		public SinglePartTransaction(String tradesInMessage, TransactionPhaseListener listener, int transId) {
			super();
			this.tradesInMessage = tradesInMessage;
			this.listener = listener;
			this.transId = transId;
		}

		@Override
		public void run() {
			final String[] trades = tradesInMessage.split("\\|");
			try(final FileOutputStream pw = new FileOutputStream(closed_trades_file, true);) {
				for (int i = 0; i < trades.length-1; i++) {
					if (!trades[i].trim().equals("")) {
						pw.write(trades[i].getBytes(CHARSET));
					}
					pw.write("\n".getBytes(CHARSET));
				}
				pw.write(trades[trades.length - 1].getBytes(CHARSET));
				pw.flush();
			} catch (IOException e) {
				throw new RuntimeException("Unable to append new trade", e);
			} finally {
				listener.onTaskEnd();
				listener.onTransactionEnd(transId);
			}
		}


	}

	private static class MultiPartsTransaction implements Runnable {
		private final File file;
		private final boolean first;
		private final int transId;
		private final List<TransactionPhaseListener> listeners;
		private final String tradesInMessage;

		private MultiPartsTransaction(String tradesInMessage, List<TransactionPhaseListener> listeners, int transId, boolean first, File f) {
			this.file = f;
			this.tradesInMessage = tradesInMessage;
			this.listeners = listeners;
			this.transId = transId;
			this.first = first;
		}


		@Override
		public String toString() {
			return "MultiPartsTransaction [file=" + file + ", first=" + first + ", transId=" + transId + ", listeners="
					+ listeners + ", tradesInMessage=" + tradesInMessage + "]";
		}


		@Override
		public void run() {
			if (first) {
				for (TransactionPhaseListener listener : listeners) {
					listener.onTransactionStart(transId);
				}
			}
			final String[] trades = tradesInMessage.split("\\|");
			//if last bit of data is not 'more', we create a new selector task to end the transaction
			final boolean append = !first;
			boolean toBeContinued = trades[trades.length - 1].trim().equals("more");
			if (toBeContinued) {
				trades[trades.length - 1] = "";
			}
			try(final FileOutputStream pw = new FileOutputStream(file, append);) {
				for (int i = 0; i < trades.length-1; i++) {
					if (!trades[i].trim().equals("")) {
						pw.write(trades[i].getBytes(CHARSET));
					}
					pw.write("\n".getBytes(CHARSET));
				}
				pw.write(trades[trades.length - 1].getBytes(CHARSET));
				pw.flush();
			} catch (IOException e) {
				toBeContinued = false;
				throw new RuntimeException("Unable to write", e);
			}
			finally {
				for (TransactionPhaseListener listener : listeners) {
					listener.onTaskEnd();
					if (!toBeContinued) {
						listener.onTransactionEnd(transId);
					}
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
		@Override
		public String toString() {
			return "OpenTradeTask [r=" + r + "]";
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
		@Override
		public String toString() {
			return "SingleTradeTask [r=" + r + "]";
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

		@Override
		public String toString() {
			return "FullTask [r=" + r + "]";
		}

	}

}
