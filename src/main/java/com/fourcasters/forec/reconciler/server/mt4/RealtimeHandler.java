package com.fourcasters.forec.reconciler.server.mt4;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import com.fourcasters.forec.reconciler.server.MessageHandler;

public class RealtimeHandler implements MessageHandler {

	final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
	final GregorianCalendar gc = new GregorianCalendar();

	@Override
	public void enqueue(String topic, String data) {
		try {
			//EURUSD@11214,11214,11214,11214,5,03/22/2016 17:38
			final String[] tokens = data.split("@");
			final String cross = tokens[0];
			final String quote = tokens[1];
			final String[] values = quote.split(",");
			final int open = Integer.parseInt(values[0]);
			final int high = Integer.parseInt(values[1]);
			final int low = Integer.parseInt(values[2]);
			final int close = Integer.parseInt(values[3]);
			final int vol = Integer.parseInt(values[4]);
			final Date d = sdf.parse(values[5]);
			Realtime.onNewValue(cross, new HistoryDataPoint(open, high, low, close, vol, d.getTime()));
		}
		catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

}
