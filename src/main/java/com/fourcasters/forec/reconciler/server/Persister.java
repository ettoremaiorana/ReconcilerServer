package com.fourcasters.forec.reconciler.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Persister implements MessageHandler {

	private boolean append = false;
	private final static Logger LOG = LogManager.getLogger(Persister.class);

	public void enqueue(String topic, String message) {
		//parse data
		final String[] tradesAsString = message.split("\\|");
		//TODO optimised for memory consumption as the server is low on memory.
		//Please use a byte buffer pool.
		final WriteCsvTask persistCsvTask = new WriteCsvTask(tradesAsString, append);
		
		//if last bit of data is 'more', next time we read we append the new records
		//to the existing ones.
		if (tradesAsString[tradesAsString.length - 1].equals("more")) {
			append = true;
		}
		else {
			append = false;
		}
		final Future<?> f = Application.executor.submit(persistCsvTask);
		Application.tasks.add(f);
	}

	static class WriteCsvTask implements Runnable {

		private final String[] trades;
		private final WriteMode writeMode;

		WriteCsvTask(String[] trades, boolean append) {
			this.trades = trades;
			this.writeMode = append ? WriteMode.APPEND : WriteMode.OVERWRITE;
		}

		@Override
		public void run() {
			boolean autoflush = false;
			boolean append = writeMode == WriteMode.APPEND;
			File f = new File("Trades.csv");
			try(PrintWriter pw = new PrintWriter(new FileOutputStream(f, append), autoflush);) {
				for (String trade : trades) {
					pw.write(trade + ",\n");
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

