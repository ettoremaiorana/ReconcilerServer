package com.fourcasters.forec.reconciler.server;

public class StrategiesCaptureTask implements Runnable {

	private final StrategiesTracker strategiesTracker;
	private final int algoId;

	public StrategiesCaptureTask(int algoId, StrategiesTracker strategiesTracker, ApplicationInterface application) {
		this.algoId = algoId;
		this.strategiesTracker = strategiesTracker;
	}

	@Override
	public void run() {
		strategiesTracker.add(algoId);
	}

}
