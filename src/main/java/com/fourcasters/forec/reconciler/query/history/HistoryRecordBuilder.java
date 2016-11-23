package com.fourcasters.forec.reconciler.query.history;

import java.util.GregorianCalendar;
import java.util.function.BiConsumer;

public class HistoryRecordBuilder {

	private final static BiConsumer<String, HistoryRecord> DEFAULT_PARSER = (s, hr) -> {
		HistoryRecordInputStream stream = new HistoryRecordInputStream(s);
		hr.parseOpen(stream, ',');
		hr.parseHigh(stream, ',');
		hr.parseLow(stream, ',');
		hr.parseClose(stream, ',');
		hr.parseVolume(stream, ' ');
		hr.parseMonth(stream, '/');
		hr.parseDay(stream, '/');
		hr.parseYear(stream, ' ');
		hr.parseHour(stream, ':');
		hr.parseMinute(stream, '\n');
	};


	private static final BiConsumer<String, HistoryRecord> ALTERNATIVE_PARSER = (s, hr) -> {
		HistoryRecordInputStream stream = new HistoryRecordInputStream(s);
		hr.parseYear(stream, '.');
		hr.parseMonth(stream, '.');
		hr.parseDay(stream, ',');
		hr.parseHour(stream, ':');
		hr.parseMinute(stream, ',');
		hr.parseOpen(stream, ',');
		hr.parseHigh(stream, ',');
		hr.parseLow(stream, ',');
		hr.parseClose(stream, ',');
		hr.parseVolume(stream, '\n');
	};


	private final GregorianCalendar gc = new GregorianCalendar();
	private BiConsumer<String, HistoryRecord> recordParser;


	HistoryRecordBuilder(String recordFormat) {
		this.recordParser = matchExpression(recordFormat);
	}

	//2015.01.01,13:09,1.209960,1.209960,1.209960,1.209960,0
	private BiConsumer<String, HistoryRecord> matchExpression(String recordFormat) {
		if (recordFormat.equals("yyyy.mm.dd,HH:MM,o,h,l,c,v")) {
			return ALTERNATIVE_PARSER;
		}
		return DEFAULT_PARSER; //TODO
	}

	HistoryRecord newRecord(String recordAsString) {
		if (!recordAsString.isEmpty()) {
			final HistoryRecord record = new HistoryRecord();
			recordParser.accept(recordAsString, record);
			gc.set(record.year, record.month - 1, record.day, record.hour, record.minute, 0); //-1 because month is 0 based according to the api
			gc.set(GregorianCalendar.MILLISECOND, 0);
			record.timestamp = gc.getTimeInMillis();
			return record;
		}
		else {
			throw new HistoryRecordParsingException("Cannot parse a record from an empty string");
		}
		
	}

	static final class HistoryRecord {
		private short volume;
		private short day;
		private short month;
		private short year;
		private short hour;
		private short minute;
		private long timestamp;
		private double close;
		private double open;
		private double high;
		private double low;

		private HistoryRecord() {
		}
		long timestamp() {
			return timestamp;
		}
		short minute() {
			return minute;
		}

		private void parseVolume(HistoryRecordInputStream stream, char c) {
			volume = stream.nextShort(c);
		}
		private void parseMonth(HistoryRecordInputStream stream, char c) {
			month = stream.nextShort(c);
		}
		private void parseYear(HistoryRecordInputStream stream, char c) {
			year = stream.nextShort(c);
		}
		private void parseDay(HistoryRecordInputStream stream, char c) {
			day = stream.nextShort(c);
		}
		private void parseHour(HistoryRecordInputStream stream, char c) {
			hour = stream.nextShort(c);
		}
		private void parseMinute(HistoryRecordInputStream stream, char c) {
			minute = stream.nextShort(c);
		}

		private void parseOpen(HistoryRecordInputStream stream, char c) {
			open = stream.nextDouble(c);
		}
		private void parseHigh(HistoryRecordInputStream stream, char c) {
			high = stream.nextDouble(c);
		}
		private void parseLow(HistoryRecordInputStream stream, char c) {
			low = stream.nextDouble(c);
		}
		private void parseClose(HistoryRecordInputStream stream, char c) {
			close = stream.nextDouble(c);
		}

		public static boolean sameHour(HistoryRecord curr, HistoryRecord prev) {
			if (prev == null || curr == null) {
				return false;
			}
			return curr.hour == prev.hour;
		}


		@Override
		public String toString() {
			return "HistoryRecord [volume=" + volume + ", day=" + day + ", month=" + month + ", year=" + year + ", hour="
					+ hour + ", minute=" + minute + ", timestamp=" + timestamp + ", close=" + close + ", open=" + open
					+ ", high=" + high + ", low=" + low + "]";
		}

		public static void main(String[] args) {
			System.out.println(new HistoryRecordBuilder("o,h,l,c,v dd/mm/yyyy HH:MM").
					newRecord("1,2,3,4,5 17/11/2016 21:22"));
		}
	}
}
