package com.fourcasters.forec.reconciler.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

public class Persister implements MessageHandler {

	private static final String CREATE_TABLE = "CREATE TABLE trades (trade TEXT NOT NULL PRIMARY KEY)";

	private static final String CREATE_INDEX = "CREATE INDEX trade_index ON trades(trade)";

	private final int numOfThreads = 1; //TODO make me configurable
	private final ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);

	private boolean append = false;
	private final static Logger LOG = LogManager.getLogger(Persister.class);
	void start() {
		//TODO the executor service must be shared amongst message
		//handlers and not segregated here.
		//TODO do it numOfThreads time with tasks that take long enough
		//to start all the threads.
		executorService.execute(new Runnable() {
			public void run() {
			}
		});
	}

	void stop() {
		executorService.shutdown();
	}

	public void enqueue(String topic, String message) {
		//parse data
		final String[] tradesAsString = message.split("\\|");
		//TODO optimised for memory consumption as the server is low on memory.
		//Please use a byte buffer pool.
		final PersistTask persistTask = new PersistTask(tradesAsString, append);
		final WriteCsvTask persistCsvTask = new WriteCsvTask(tradesAsString, append);
		LOG.info("Persist task: " + persistTask);
		
		executorService.execute(persistTask);
		executorService.execute(persistCsvTask);

		//if last bit of data is 'more', next time we read we append the new records
		//to the existing ones.
		if (tradesAsString[tradesAsString.length - 1].equals("more")) {
			append = true;
		}
		else {
			append = false;
		}
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

	static class PersistTask implements Runnable {

		private final String[] trades;
		private final WriteMode writeMode;

		PersistTask(String[] trades, boolean append) {
			this.trades = trades;
			this.writeMode = append ? WriteMode.APPEND : WriteMode.OVERWRITE;
		}

		@Override
		public String toString() {
			return "PersistTask [trades=" + Arrays.toString(trades) + ", writeMode=" + writeMode + "]";
		}

		@Override
		public void run() {
			if (writeMode.equals(WriteMode.OVERWRITE)) {
				writeDb();
			}
		}

		private void writeDb() {
			File dbFile = new File("Trades.dat");
			dbFile.delete();
			SqlJetDb db = null;
			try {
				db = SqlJetDb.open(dbFile, true);

				db.getOptions().setAutovacuum(true);
				db.beginTransaction(SqlJetTransactionMode.WRITE);
				try {
					db.createTable(CREATE_TABLE);
					db.createIndex(CREATE_INDEX);
					ISqlJetTable table = db.getTable("trades");
					for (String trade : trades) {
						table.insert(trade);
					}
				} finally {
					db.commit();
				}
			}
			catch (SqlJetException e) {
				if (db != null && db.isOpen()) {
					try {
						db.close();
					} catch (SqlJetException e1) {}
				}
				e.printStackTrace();
			}
		}
	}

	static enum WriteMode {
		APPEND,
		OVERWRITE
	}

	public static void main(String[] args) {
		File dbFile = new File("Trades.dat");
		File csvFile = new File("Trades.csv");
		SqlJetDb db = null;
		PrintWriter writer = null;
		try {
			db = SqlJetDb.open(dbFile, true);
			writer = new PrintWriter(new FileOutputStream(csvFile));
			db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
			ISqlJetCursor cursor = null;
			ISqlJetTable table = null;
			try {
				table = db.getTable("trades");
				cursor = table.order(table.getPrimaryKeyIndexName());
				while (cursor.next()) {
					String trade = cursor.getString("trade").replace(";", ",");
					System.out.println(trade);
					writer.write(trade + "\n");
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
				if (writer != null) {
					writer.close();
				}
				if (db != null && db.isOpen()) {
					db.commit();
					db.close();
				}
			}
		}
		catch (SqlJetException e) {
			if (db != null && db.isOpen()) {
				try {
					db.close();
				} catch (SqlJetException e1) {}
			}
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}

