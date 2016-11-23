package com.fourcasters.forec.reconciler.query.history;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fourcasters.forec.reconciler.query.history.HistoryRecordBuilder.HistoryRecord;

public class HistoryDAOTest {

	private static HistoryDAO dao;
	private static String oldProp;

	@BeforeClass
	public static void setupAll() throws IOException {
		oldProp = System.getProperty("ENV");
		System.setProperty("ENV", "test");
		dao = new HistoryDAO();
		dao.dbhash("eurusd", "yyyy.mm.dd,HH:MM,o,h,l,c,v");
	}

	private String pattern;
	private SimpleDateFormat sdf;

	@Before
	public void setup() {
		pattern = "yyyy.MM.dd,HH:mm";
		sdf = new SimpleDateFormat(pattern);
	}

	@After
	public void after() {
		if (oldProp != null) {
			System.setProperty("ENV", oldProp);
		}
		else {
			System.clearProperty("ENV");
		}
	}


	@Test
	public void testHashCount() throws IOException {
		assertEquals(-1, dao.hashCount("audcad"));
		RandomAccessFile file = new RandomAccessFile("./history/test/eurusd.csv", "r");
		HistoryRecordBuilder builder = new HistoryRecordBuilder("yyyy.mm.dd,HH:MM,o,h,l,c,v");
		String record;
		int counter = 0;
		while ((record = file.readLine()) != null) {
			HistoryRecord r = builder.newRecord(record);
			if (r.minute() == 0) {
				counter++;
			}
		}
		file.close();
		assertEquals(counter, dao.hashCount("eurusd"));
	}

	@Test
	public void testOutOfBoundariesRecord() throws IOException, ParseException {
		Date d = sdf.parse("2014.12.21,00:00");
		long offset = dao.offset("eurusd", d, false);
		assertEquals(-1, offset);
		d = sdf.parse("2017.12.21,00:00");
		offset = dao.offset("eurusd", d, false);
		assertEquals(-1, offset);
	}
	
	@Test
	public void testOffsetAt10thRecord() throws IOException, ParseException {
		Date d = sdf.parse("2015.12.21,00:09");
		long offset = dao.offset("eurusd", d, false);
		assertEquals(45*7+44*2, offset);
		RandomAccessFile file = new RandomAccessFile("./history/test/eurusd.csv", "r");
		file.seek(offset);
		String line = file.readLine();
		assertEquals("2015.12.21,00:09,10851,10852,10851,10851,24", line);
		file.close();
	}

	@Test
	public void testOffsetAt0() throws IOException, ParseException {
		Date d = sdf.parse("2015.12.21,00:00");
		long offset = dao.offset("eurusd", d, false);
		assertEquals(0, offset);
		RandomAccessFile file = new RandomAccessFile("./history/test/eurusd.csv", "r");
		file.seek(offset);
		String line = file.readLine();
		assertEquals("2015.12.21,00:00,10851,10851,10851,10851,16", line);
		file.close();
	}

	@Test
	public void testOffsetExactOf0() throws IOException, ParseException {
		Date d = sdf.parse("2015.12.21,00:00");
		long offset = dao.offset("eurusd", d, true);
		assertEquals(45, offset);
		RandomAccessFile file = new RandomAccessFile("./history/test/eurusd.csv", "r");
		file.seek(offset);
		String line = file.readLine();
		assertEquals("2015.12.21,00:01,10851,10852,10851,10852,9", line);
		file.close();
	}

	@Test
	public void testOffsetExactOf10thRecord() throws IOException, ParseException {
		Date d = sdf.parse("2015.12.21,00:09");
		long offset = dao.offset("eurusd", d, true);
		assertEquals(45*8+44*2, offset);
		RandomAccessFile file = new RandomAccessFile("./history/test/eurusd.csv", "r");
		file.seek(offset);
		String line = file.readLine();
		assertEquals("2015.12.21,00:10,10852,10857,10852,10856,27", line);
		file.close();
	}

	@Test
	public void testWeekendJump() throws ParseException, IOException {
		Date d = sdf.parse("2015.12.24,23:00");
		long offset = dao.offset("eurusd", d, false);
		RandomAccessFile file = new RandomAccessFile("./history/test/eurusd.csv", "r");
		file.seek(offset);
		String line = file.readLine();
		assertEquals("2015.12.28,00:00,10945,10947,10944,10945,7", line);
		file.close();
	}

	@Test
	public void testRange() throws ParseException, IOException {
		Date d = sdf.parse("2015.12.21,00:19");
		long end = dao.offset("eurusd", d, true);
		d = sdf.parse("2015.12.21,00:10");
		long start = dao.offset("eurusd", d, false);
		byte[] buffer = new byte[(int) (end-start)];
		
		RandomAccessFile file = new RandomAccessFile("./history/test/eurusd.csv", "r");
		file.seek(start);
		file.read(buffer);
		file.close();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer)));
		int counter = 1;
		String first = reader.readLine();
		String temp;
		String last = null;
		while ((temp = reader.readLine()) != null) {
			counter ++;
			last = temp;
		}
		reader.close();
		assertEquals(10, counter);
		assertEquals(first, "2015.12.21,00:10,10852,10857,10852,10856,27");
		assertEquals(last,  "2015.12.21,00:19,10858,10858,10857,10857,27");
	}
}
