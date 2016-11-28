package com.fourcasters.forec.reconciler.query;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.server.ReconcilerConfig;

public abstract class IndexableDAO {

	private final Map<String, TreeMap<Long, Long>> indexes = new HashMap<>(16);
	protected final Logger LOG = LogManager.getLogger(getClass());

	public abstract Path getFilePath(String cross);
	public abstract RecordBuilder getRecordBuilder(String recordFormat);
	public abstract RecordBuilder getRecordBuilder();

	protected TreeMap<Long, Long> javaDbHash(String cross, String recordFormat) throws IOException {
		cross = cross.toLowerCase();
		TreeMap<Long, Long> hash = new TreeMap<>();
		final Path path = getFilePath(cross);
		try(RandomAccessFile sc = new RandomAccessFile(path.toFile(), "r");){
			String recordAsString;
			RecordBuilder recordBuilder = getRecordBuilder(recordFormat);

			byte[] buff = new byte[4096];
			boolean finished = false;
			int rem = 0;
			long totalBytes = 0;
			Record prev = null;
			while (!finished) {
				int bytesRead = sc.read(buff, rem, 4096);
				ByteBuffer bb = ByteBuffer.wrap(buff);
				while ((recordAsString = readLine(bb)) != null) {
					Record curr = recordBuilder.newRecord(recordAsString);
					if (!curr.hasToIndex(prev)) {
						hash.put(curr.index(), totalBytes);
					}
					prev = curr;
					totalBytes += (recordAsString.length() + 2); // /r/n
				}
				rem = 0;
				if (bb.remaining() > 0 ) {
					rem = bb.remaining();
				}
				buff = new byte[4096+rem];
				bb.get(buff, 0 , rem);
				if (bytesRead < 4096) {
					finished = true;
				}
			}
		}
		LOG.info(cross + " generated an index file of " + hash.size() + " entries");
		return hash;
	}

	final String readLine(ByteBuffer bb) throws IOException {
		StringBuilder input = new StringBuilder();
		int c = -1;
		boolean eol = false;
		while (!eol) {
			if (!bb.hasRemaining()) {
				break;
			}
			switch ((c = bb.get())) {
			case -1:
			case '\n':
				eol = true;
				break;
			case '\r':
				if (bb.hasRemaining()) {
					eol = true;
					if ((bb.get()) != '\n') {
						throw new RuntimeException();
					}
					break;
				}
				input.append((char)c);
				break;
			default:
				input.append((char)c);
				break;
			}
		}
		if (!eol) {
			bb.position(bb.limit() - input.length());
			return null;
		}
		if ((c == -1) && (input.length() == 0)) {
			return null;
		}
		return input.toString();
	}

	public boolean dbhash(String cross, String format) throws IOException {
		return indexes.put(cross, javaDbHash(cross, format)) == null;
	}
	public boolean dbhash(String cross) throws IOException {
		return indexes.put(cross, javaDbHash(cross)) == null;
	}
	public int hashCount(String cross) {
		TreeMap<Long, Long> index = indexes.get(cross);
		if (index == null) {
			return -1;
		}
		return index.size();
	}
	TreeMap<Long, Long> javaDbHash(String cross) throws IOException {
		String format = ReconcilerConfig.DEFAULT_HISTORY_PATTERN;
		return javaDbHash(cross, format);
	}
	public long offset(String cross, Date date, boolean exact) throws IOException {
		long timestamp = date.getTime();

		TreeMap<Long, Long> index = indexes.get(cross);
		if (index == null) {
			return -1;
		}
		Entry<Long, Long> checkpoint = index.floorEntry(timestamp);
		if (checkpoint == null) {
			return -1;
		}
		LOG.debug("Checkpoint: " + checkpoint);
		long offset = -1;
		boolean found = false;
		final Path path = getFilePath(cross);
		try(RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");) {
			RecordBuilder hrb = getRecordBuilder();
			raf.seek(checkpoint.getValue());
			String recordAsString;
			while (!found && (recordAsString = raf.readLine()) != null) {
				Record record = hrb.newRecord(recordAsString);
				LOG.info("Record: " + record);
				if (record.index() >= timestamp) {
					found = true;
					offset = exact ? raf.getFilePointer()
							: raf.getFilePointer() - recordAsString.length() - 2;
				}
			}
		}
		return offset;
	}
	public abstract Path getRootPath();

	public void dbhashAll(String format) throws IOException {
		final File root = getRootPath().toFile();
		if (!root.isDirectory()) {
			throw new IllegalArgumentException("Unable to read table files from non directory " + root);
		}
		for(File f : root.listFiles()) {
			String cross = f.getName();
			int pos = cross.lastIndexOf(".");
			if (pos > 0) {
				cross = cross.substring(0, pos);
			}
			dbhash(cross, format);
		}
	}
	public abstract String getDefaultFormat();


	public void dbhashAll() throws IOException {
		String format = getDefaultFormat();
		dbhashAll(format);
	}
}
