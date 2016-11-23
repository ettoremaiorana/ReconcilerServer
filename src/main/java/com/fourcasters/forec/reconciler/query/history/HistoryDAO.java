/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fourcasters.forec.reconciler.query.history;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
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
		HistoryRecord prev = null;
		HistoryRecordBuilder recordBuilder = new HistoryRecordBuilder(recordFormat);

		while((recordAsString = sc.readLine()) != null) {
			HistoryRecord curr = recordBuilder.newRecord(recordAsString);
			if (!HistoryRecord.sameHour(curr, prev)) {
				hash.put(curr.timestamp(), sc.getFilePointer() - recordAsString.length() - 2);
			}
			prev = curr;
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

	public static void main(String[] args) throws ParseException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		reader.readLine();
		HistoryDAO dao = new HistoryDAO();
		dao.dbhash("eurusd", "yyyy.mm.dd,HH:MM,o,h,l,c,v");
		reader.readLine();
		GregorianCalendar gc = new GregorianCalendar(2015, 12, 23, 13, 21, 00);
		long offset = dao.offset("eurusd.csv", new Date(gc.getTimeInMillis()), false);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		System.out.println(offset);
		Date d = sdf.parse("201512231321");
		offset = dao.offset("eurusd.csv", d, false);
		System.out.println(offset);
		
		String fileName = "./history/dev/public/eurusd.csv";
		long start = 31114608;
		long end =   31316663;
		System.out.println(new Date(1450875626997L));
		System.out.println(new Date(1450648826997L));
		RandomAccessFile raf = new RandomAccessFile(fileName, "r");
		raf.seek(start);
		String record = raf.readLine();
		System.out.println(record);
		raf.seek(end);
		record = raf.readLine();
		System.out.println(record);
		raf.close();

		final File file = new File(fileName);
		FileChannel readChannel = FileChannel.open(Paths.get(file.getAbsolutePath()), StandardOpenOption.READ);
		FileChannel writeChannel = null;
		ByteBuffer dst = ByteBuffer.allocateDirect(256*8);
		long position = start;
		do {
			int remaining = (int)(end-position);
			int length = Math.min(256*8, remaining);
			position += read(readChannel, dst, position, length);
			write(writeChannel, dst);
		} while(position < end);
		readChannel.close();
	}

	private static int read(FileChannel channel, ByteBuffer dst, long position, int length) throws IOException {
		dst.position(0);
		channel.position(position);
		dst.limit(length);
		int bytesRead = channel.read(dst);
		return bytesRead;
	}
	private static void write(FileChannel writeChannel, ByteBuffer dst) {
		dst.position(0);
		byte[] arr = new byte[dst.remaining()];
		dst.get(arr);
		String ouput = new String(arr);
		System.out.print(ouput);
	}
}
