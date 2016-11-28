package com.fourcasters.forec.reconciler.server;

import java.util.concurrent.Future;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.server.PerformanceCalc.PerformanceCalcTask;;

public class TradeEventCapturer implements MessageHandler {

	private final static String PASSWORD = System.getProperty("mail.password");
	private final static Logger LOG = LogManager.getLogger(TradeEventCapturer.class);

	private final ApplicationInterface application;
	private final ReconcilerMessageSender messageSender;
	private final StrategiesTracker strategiesTracker;


	public TradeEventCapturer(ApplicationInterface application, ReconcilerMessageSender reconcMessageSender, StrategiesTracker strategiesTracker) {
		this.application = application;
		this.messageSender = reconcMessageSender;
		this.strategiesTracker = strategiesTracker;
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
						sendEmail(algoId, ticket, data);
						final boolean result = messageSender.askForClosedTrades(String.valueOf(ticket), newTopic);
						System.out.println("closedTradesFuture result = " + result);
					}
				);
			application.futureTasks().add(closedTradesfuture);

			final Future<?> openTradesFuture = application.executor().submit(
					() -> {
						final boolean result = messageSender.askForOpenTrades(newTopic);
						System.out.println("openTradesFuture result = " + result);
					}
				);
			application.futureTasks().add(openTradesFuture);

			final Future<?> perfCalcFuture = application.executor().submit(new PerformanceCalcTask(topic, application));
			application.futureTasks().add(perfCalcFuture);

			final Future<?> strategiesCaptureFuture = application.executor().submit(new StrategiesCaptureTask(algoId, strategiesTracker, application));
			application.futureTasks().add(strategiesCaptureFuture);

		}

	}

	private void sendEmail(int algoId, long ticket, String data) {
		try {
			Email email = new SimpleEmail();
			email.setHostName("smtp.gmail.com");
			email.setSmtpPort(465);
			email.setAuthenticator(new DefaultAuthenticator("ivan.valeriani", PASSWORD));
			email.setSSL(true);
			email.setFrom("ivan.valeriani@gmail.com");
			email.setSubject("Automatic trading");
			email.setMsg(new StringBuffer().append(algoId).append(": ").append(data).toString());
			email.addTo("push_it-30@googlegroups.com");
			email.addTo("phd.alessandro.ricci@gmail.com");
			email.addTo("cwicwi2@gmail.com");
			email.addTo("simone.allemanini@gmail.com");
			email.addTo("ivan.valeriani@gmail.com");
			email.send();
		}
		catch (EmailException e) {
			LOG.error("Unable to send email.", e);
			e.printStackTrace();
		}
	}
}
