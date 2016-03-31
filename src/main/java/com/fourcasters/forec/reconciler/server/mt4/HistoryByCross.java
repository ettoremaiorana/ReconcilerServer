package com.fourcasters.forec.reconciler.server.mt4;

import static com.fourcasters.forec.reconciler.server.mt4.HistoryUtils.Frame;


public class HistoryByCross {

	private FrameHistory[] frames = new FrameHistory[Frame.values().length];

	HistoryByCross() {
		for (int i = 0; i < frames.length; i++) {
			frames[i] = new FrameHistory(Frame.values()[i].millis);
		}
	}

	public void clear(String frame) {
		final int index = Frame.valueOf(frame).index;
		frames[index].clear();
	}

	public void put(String frame, HistoryDataPoint[] dataPoints) {
		final int index = Frame.valueOf(frame).index;
		frames[index].put(dataPoints);
	}

}
