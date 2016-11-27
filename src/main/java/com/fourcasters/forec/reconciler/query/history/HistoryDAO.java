/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fourcasters.forec.reconciler.query.history;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.query.history.HistoryRecordBuilder.HistoryRecord;
import com.fourcasters.forec.reconciler.server.ProtocolConstants;
import com.fourcasters.forec.reconciler.server.ReconcilerConfig;

/**
 *
 * @author ettoremaiorana
 */
public class HistoryDAO {

	private static final Logger LOG = LogManager.getLogger(HistoryDAO.class);
	private final Map<String, TreeMap<Long, Long>> indexes = new HashMap<>(16);

	public boolean dbhash(String cross, String format) throws IOException {
		return indexes.put(cross, javaDbHash(cross, format)) == null;
	}
	public boolean dbhash(String cross) throws IOException {
		return indexes.put(cross, javaDbHash(cross)) == null;
	}
		TreeMap<Long, Long> javaDbHash(String pathToFile) throws IOException {
		return javaDbHash(pathToFile, "o,h,l,c,v dd/mm/yyyy HH:MM");
	}   

	TreeMap<Long, Long> javaDbHash(String pathToFile, String recordFormat) throws IOException {
		pathToFile = pathToFile.toLowerCase();
		TreeMap<Long, Long> hash = new TreeMap<>();
		final Path path = Paths.get(ReconcilerConfig.BKT_DATA_PATH, pathToFile + ProtocolConstants.BKT_DATA_EXTENSION);
		RandomAccessFile sc = new RandomAccessFile(path.toFile(), "r");
		String recordAsString;
		HistoryRecordBuilder recordBuilder = new HistoryRecordBuilder(recordFormat);

		byte[] buff = new byte[4096];
		boolean finished = false;
		int rem = 0;
		long totalBytes = 0;
		HistoryRecord prev = null;
		while (!finished) {
			int bytesRead = sc.read(buff, rem, 4096);
			ByteBuffer bb = ByteBuffer.wrap(buff);
			while ((recordAsString = readLine(bb)) != null) {
				HistoryRecord curr = recordBuilder.newRecord(recordAsString);
				if (!HistoryRecord.sameHour(curr, prev)) {
					hash.put(curr.timestamp(), totalBytes);
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
		
		sc.close();
		LOG.info(pathToFile + " generated an index file of " + hash.size() + " entries");
		return hash;
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
		final Path path = Paths.get(ReconcilerConfig.BKT_DATA_PATH, cross + ProtocolConstants.BKT_DATA_EXTENSION);
		RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");
		HistoryRecordBuilder hrb = new HistoryRecordBuilder("yyyy.mm.dd,HH:MM,o,h,l,c,v");
		raf.seek(checkpoint.getValue());
		String recordAsString;
		while (!found && (recordAsString = raf.readLine()) != null) {
			HistoryRecord record = hrb.newRecord(recordAsString);
			LOG.info("Record: " + record);
			if (record.timestamp() >= timestamp) {
				found = true;
				offset = exact ? raf.getFilePointer()
						: raf.getFilePointer() - recordAsString.length() - 2;
			}
		}
		raf.close();
		return offset;
	}

	int hashCount(String cross) {
		TreeMap<Long, Long> index = indexes.get(cross);
		if (index == null) {
			return -1;
		}
		return index.size();
	}

	final String readLine(ByteBuffer bb) throws IOException {
        StringBuffer input = new StringBuffer();
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

	public static void main(String[] args) throws IOException {
		System.setProperty("ENV", "test");
		HistoryDAO dao = new HistoryDAO();
		TreeMap<Long, Long> map = dao.javaDbHash("eurusd",  "yyyy.mm.dd,HH:MM,o,h,l,c,v");
		RandomAccessFile raf = new RandomAccessFile("./history/test/eurusd.csv", "r");
		for (Entry<Long, Long> entry : map.entrySet()) {
			raf.seek(entry.getValue());
			System.out.println(raf.readLine());
		}
		raf.close();
	}
}
