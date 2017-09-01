package com.fourcasters.forec.reconciler.server;

import java.util.concurrent.Future;

import com.fourcasters.forec.reconciler.EmailSender;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.server.PerformanceCalc.PerformanceCalcTask;;

public class TradeEventCapturer implements MessageHandler {

	private final static Logger LOG = LogManager.getLogger(TradeEventCapturer.class);

	private final ApplicationInterface application;
	private final ReconcilerMessageSender messageSender;
	private final StrategiesTracker strategiesTracker;
	private final EmailSender emailSender;


	public TradeEventCapturer(ApplicationInterface application, ReconcilerMessageSender reconcMessageSender,
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

			final Future<?> closedTradesfuture = application.executor().submit(
					() -> {
						emailSender.sendEmail(algoId, ticket, data);
						final boolean result = messageSender.askForClosedTrades(String.valueOf(ticket), newTopic);
						LOG.info("closedTradesFuture result = " + result);
					}
				);
			application.futureTasks().add(closedTradesfuture);

			final Future<?> openTradesFuture = application.executor().submit(
					() -> {
						final boolean result = messageSender.askForOpenTrades(newTopic);
						LOG.info("openTradesFuture result = " + result);
					}
				);
			application.futureTasks().add(openTradesFuture);

			final Future<?> perfCalcFuture = application.executor().submit(new PerformanceCalcTask(topic, application));
			application.futureTasks().add(perfCalcFuture);

			final Future<?> strategiesCaptureFuture = application.executor().submit(new StrategiesCaptureTask(algoId, strategiesTracker, application));
			application.futureTasks().add(strategiesCaptureFuture);

		}
	}
}
