package com.fourcasters.forec.reconciler.server;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PerformanceCalc {


	public static class PerformanceCalcTask implements Runnable {

		private static final Logger LOG = LogManager.getLogger(PerformanceCalc.class);
		private static final boolean exeExist = new File("performancecalc.exe").exists();
		private static final boolean isWindows = System.getProperty("os.name").contains("Windows");
		private Process proc;
		private final ProcessBuilder pb;
		private final ApplicationInterface application;
		int backoffTime = 50;

		public PerformanceCalcTask (String topic, ApplicationInterface application) {
			//example: topic = topic_name + "@" + cross + "@" + algo_id
			final String cross = topic.split("@")[1].trim();
			final int algoId = Integer.parseInt(topic.split("@")[2].trim());
			final int timeFrame = 30; //TODO FIXME this value should not be hard-coded but should rather come from algo configuration
			pb = new ProcessBuilder("performancecalc.exe", String.valueOf(algoId), String.valueOf(timeFrame), cross);
			this.application = application;
		}

		@Override
		public void run() {
			if (proc == null) {
				if (!isWindows) {
					LOG.error("This feature is available on windows only");
					return;
				}
				if (!exeExist) {
					LOG.error("The file performance.exe could not be found");
					return;
				}
				try {
					proc = pb.start();
				} catch(Exception ex) {
					throw new RuntimeException("Unable to start up performance calc process", ex);
				}
			}
			LOG.info("Checking for matlab to calculate performance");
			try {
				if (!proc.waitFor(backoffTime, TimeUnit.MILLISECONDS)) {
					LOG.info("Performance calculation is not yet completed");
					application.selectorTasks().add(new SelectorTask() {
						@Override
						public void run() {
							if (PerformanceCalcTask.this.backoffTime < 3200) {
								PerformanceCalcTask.this.backoffTime *= 2;
								application.futureTasks().add(
										application.executor().submit(PerformanceCalcTask.this)
								);
							}
							else {
								proc.destroy();
							}
						}
					});
				}
				else {
					LOG.info("Performance calculation is finished");
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String toString() {
			return "PerformanceCalcTask [proc=" + proc + ", pb=" + pb + ", application=" + application
					+ ", backoffTime=" + backoffTime + "]";
		}
	}

}
