package com.fourcasters.forec.reconciler.server;

import java.util.concurrent.Future;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.server.cli.PerformanceCalc.PerformanceCalcTask;;

public class TradeEventCapturer implements MessageHandler {

	private final static String PASSWORD = System.getProperty("mail.password");
	private final static Logger LOG = LogManager.getLogger(TradeEventCapturer.class);

	private final ApplicationInterface application;
	private final ReconcilerMessageSender messageSender;


	public TradeEventCapturer(ApplicationInterface application, ReconcilerMessageSender reconcMessageSender) {
		this.application = application;
		this.messageSender = reconcMessageSender;
	}

	@Override
	public void enqueue(String topic, String data) {
		final String newTopic = "RECONC@ACTIVTRADES" + topic.substring(topic.indexOf("@"));
		//example: buffer = status + "," + type + "," + price + "," + ticket;
		final long ticket = Long.parseLong(data.split(",")[3].trim());
		//example: topic = topic_name + "@" + cross + "@" + algo_id
		final int algoId = Integer.parseInt(topic.split("@")[2].trim());

		final Future<?> future = application.executor().submit(
			() -> {
				sendEmail(algoId, ticket, data);
				new ReconcilerMessageSender(application).askForClosedTrades(String.valueOf(ticket), newTopic);
			}
		);
		application.futureTasks().add(future);

		final Future<?> openTradesFuture = application.executor().submit(
			() -> {messageSender.askForOpenTrades(newTopic);}
		);
		application.futureTasks().add(openTradesFuture);

		final Future<?> perfCalcFuture = application.executor().submit(new PerformanceCalcTask(topic, application));
		application.futureTasks().add(perfCalcFuture);

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
