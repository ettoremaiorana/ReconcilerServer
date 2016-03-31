package com.fourcasters.forec.reconciler.server.mt4;

import static com.fourcasters.forec.reconciler.server.mt4.HistoryUtils.Cross;

public class History {

	private HistoryByCross[] historyByCross = new HistoryByCross[Cross.values().length];
	
	private History() {
		for (int i = 0; i < historyByCross.length; i++) {
			historyByCross[i] = new HistoryByCross();
		}
	}
	
	private static final History history = new History();

	public static void clear(String cross, String frame) {

		final int index = Cross.valueOf(cross).index;
		history.historyByCross[index].clear(frame); 
	}

	public static void put(String cross, String frame, HistoryDataPoint[] dataPoints) {
		final int index = Cross.valueOf(cross).index;
		history.historyByCross[index].put(frame, dataPoints); 
	}

	public static HistoryByCross get(int index) {
		return history.historyByCross[index];
	}

}
