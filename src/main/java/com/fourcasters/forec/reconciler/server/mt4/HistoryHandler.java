package com.fourcasters.forec.reconciler.server.mt4;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import com.fourcasters.forec.reconciler.server.MessageHandler;

public class HistoryHandler implements MessageHandler {

	final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
	final GregorianCalendar gc = new GregorianCalendar();
	@Override
	public void enqueue(String topic, String data) {
		try {
			//EURUSD@m1@11252,11252,11252,11252,2,03/22/2016 00:25$11252,
			final String[] tokens = data.split("@");
			final String cross = tokens[0];
			final String frame = tokens[1];
			final String quotes = tokens[2];
			final String[] quotesAsStringArray = quotes.split("\\$");
			final HistoryDataPoint[] dataPoints = new HistoryDataPoint[quotesAsStringArray.length];
			for (int i = quotesAsStringArray.length - 1; i >= 0; i--) {
				final String[] values = quotesAsStringArray[i].split(",");
				final int open = Integer.parseInt(values[0]);
				final int high = Integer.parseInt(values[1]);
				final int low = Integer.parseInt(values[2]);
				final int close = Integer.parseInt(values[3]);
				final int vol = Integer.parseInt(values[4]);
				final Date d = sdf.parse(values[5]);
				final HistoryDataPoint dp = new HistoryDataPoint(open, high, low, close, vol, d.getTime());
				dataPoints[i] = dp;
			}
			History.clear(cross, frame);
			History.put(cross, frame, dataPoints);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
