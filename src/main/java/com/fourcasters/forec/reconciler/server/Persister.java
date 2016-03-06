package com.fourcasters.forec.reconciler.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Persister implements MessageHandler {

	private volatile boolean append = false;
	private final static Logger LOG = LogManager.getLogger(Persister.class);

	public void enqueue(String topic, String message) {
		//parse data
		final String[] tradesAsString = message.split("\\|");
		//TODO optimised for memory consumption as the server is low on memory.
		//Please use a byte buffer pool.
		final WriteCsvTask persistCsvTask = new WriteCsvTask(tradesAsString, append);
		LOG.info("Persisting: " + persistCsvTask);
		final Future<?> f = Application.executor.submit(persistCsvTask);
		LOG.info(f + " future added");
		Application.tasks.add(f);

		//if last bit of data is 'more', next time we read we append the new records
		//to the existing ones.
		if (tradesAsString[tradesAsString.length - 1].trim().equals("more")) {
			append = true;
		}
		else {
			append = false;
		}
	}

	static class WriteCsvTask implements Runnable {

		@Override
		public String toString() {
			return "WriteCsvTask [trades=" + Arrays.toString(trades) + ", writeMode=" + writeMode + "]";
		}

		private final String[] trades;
		private final WriteMode writeMode;

		WriteCsvTask(String[] trades, boolean append) {
			this.trades = trades;
			this.writeMode = append ? WriteMode.APPEND : WriteMode.OVERWRITE;
		}

		@Override
		public void run() {
			final boolean autoflush = false;
			final boolean append = writeMode == WriteMode.APPEND;
			final File f = new File("Trades.csv");
			if (trades[trades.length - 1].trim().equals("more")) {
				trades[trades.length - 1] = "";
			}
			try(final PrintWriter pw = new PrintWriter(new FileOutputStream(f, append), autoflush);) {
				for (int i = 0; i < trades.length; i++) {
					if (!trades[i].trim().equals("")) {
						pw.write(trades[i]);
						pw.write(",\n");
					}
				}
				pw.flush();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}


	static enum WriteMode {
		APPEND,
		OVERWRITE
	}
}

