package com.fourcasters.forec.reconciler.server.mt4;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class FrameHistory {

	private final long frameInMillis;
	private long nextUpdate;

	private Deque<HistoryDataPoint> dataPoints = new ArrayDeque<>();

	public FrameHistory(long frameInMillis) {
		this.frameInMillis = frameInMillis;
	}

	public void clear() {
		dataPoints.clear();
	}

	public void put(HistoryDataPoint[] dataPointsArray) {
		dataPoints.addAll(Arrays.asList(dataPointsArray));
	}

	public void onNewPoint(HistoryDataPoint dataPoint) {
		final HistoryDataPoint lastDataPoint = dataPoints.getLast();
		lastDataPoint.high(Math.max(dataPoints.getLast().high(), dataPoint.high()));
		lastDataPoint.low(Math.min(dataPoints.getLast().high(), dataPoint.high()));
		lastDataPoint.close(dataPoint.close());
		lastDataPoint.vol(lastDataPoint.vol() + dataPoint.vol());
		long time = dataPoint.date();
		if (time >= nextUpdate) {
			//send
			//TODO
			//reset
			nextUpdate += frameInMillis;

		}
	}
}
