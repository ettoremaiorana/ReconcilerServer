package com.fourcasters.forec.reconciler.server.trades;

import com.fourcasters.forec.reconciler.EmailSender;
import com.fourcasters.forec.reconciler.server.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.server.PerformanceCalc.PerformanceCalcTask;;

public class TradeEventCapturer implements MessageHandler {

	private final static Logger LOG = LogManager.getLogger(TradeEventCapturer.class);

	private final ApplicationInterface application;
	private final TradeReconcilerMessageSender messageSender;
	private final StrategiesTracker strategiesTracker;
	private final EmailSender emailSender;


	public TradeEventCapturer(ApplicationInterface application, TradeReconcilerMessageSender reconcMessageSender,
                              StrategiesTracker strategiesTracker, EmailSender emailSender) {
		this.application = application;
		this.messageSender = reconcMessageSender;
		this.strategiesTracker = strategiesTracker;
		this.emailSender = emailSender;
	}

	@Override
	public void enqueue(String topic, String data) {
		final String newTopic = "RECONC@ACTIVTRADES" + topic.substring(topic.indexOf("@"));
		//example: buffer = status + "," + type + "," + price + "," + ticket;
		final String[] tokens = data.split(",");
		final long ticket = Long.parseLong(tokens[3].trim());
		final int status = Integer.parseInt(tokens[0].trim());
		//example: topic = topic_name + "@" + cross + "@" + algo_id
		final int algoId = Integer.parseInt(topic.split("@")[2].trim());

		if (status > 0) { //success

			application.submit(
					() -> {
						emailSender.sendEmail(algoId, ticket, data);
						final boolean result = messageSender.askForClosedTrades(String.valueOf(ticket), newTopic);
						LOG.info("closedTradesFuture result = " + result);
					}
				);

			application.submit(
					() -> {
						final boolean result = messageSender.askForOpenTrades(newTopic);
						LOG.info("openTradesFuture result = " + result);
					}
				);

			application.submit(new PerformanceCalcTask(topic, application));

			application.submit(new StrategiesCaptureTask(algoId, strategiesTracker, application));

		}
	}
}
