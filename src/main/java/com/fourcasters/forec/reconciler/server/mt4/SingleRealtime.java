package com.fourcasters.forec.reconciler.server.mt4;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class SingleRealtime {

	private final HistoryByCross history;

	
	public SingleRealtime(HistoryByCross singleHistory) {
		this.history = singleHistory;
	}

	public void update(HistoryDataPoint historyDataPoint) {
		
	}
	
}
