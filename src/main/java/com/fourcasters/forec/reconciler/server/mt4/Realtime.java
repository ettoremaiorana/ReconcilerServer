package com.fourcasters.forec.reconciler.server.mt4;

import static com.fourcasters.forec.reconciler.server.mt4.HistoryUtils.Cross;

public class Realtime {

	private SingleRealtime[] realTimes = new SingleRealtime[Cross.values().length];

	private Realtime() {
		for (int i = 0; i < realTimes.length; i++) {
			realTimes[i] = new SingleRealtime(History.get(i));
		}
	}
	
	private static final Realtime realtime = new Realtime();
	
	public static void onNewValue(String cross, HistoryDataPoint historyDataPoint) {
		final int index = Cross.valueOf(cross).index;
		realtime.realTimes[index].update(historyDataPoint);
	}

}
