package com.fourcasters.forec.reconciler.server;

import java.util.concurrent.Future;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class TradeEventCapturer implements MessageHandler {

	private final static String PASSWORD = System.getProperty("mail.password");
	private final static Logger LOG = LogManager.getLogger(TradeEventCapturer.class);
	private final static byte[] OPEN_IN_BYTES = "OPEN".getBytes();
	private final ApplicationInterface application;
	private final Context ctx;
	private final Socket socket;;

	public TradeEventCapturer(ApplicationInterface application) {
		this.application = application;
		ctx = application.context();
		socket  = ctx.socket(ZMQ.PUB);
		socket.connect("tcp://localhost:51125");
	}

	@Override
	public void enqueue(String topic, String data) {
		final String newTopic = "RECONC@ACTIVTRADES" + topic.substring(topic.indexOf("@"));

		final Future<?> future = application.executor().submit(new Runnable() {
			@Override
			public void run() {

				//example: buffer = status + "," + type + "," + price + "," + ticket;
				final long ticket = Long.parseLong(data.split(",")[3].trim());
				//example: topic = topic_name + "@" + cross + "@" + algo_id
				final int algoId = Integer.parseInt(topic.split("@")[2].trim());
				sendEmail(algoId, ticket, data);
				
				//TODO final String message = "ticket="+ticket;
				final String message = "SINGLE="+ticket;
				LOG.info("Sending '" + message + "' on topic " + newTopic);
				socket.send(newTopic.getBytes(), ZMQ.SNDMORE);
				socket.send(message.getBytes(), 0);
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
		});
		application.futureTasks().add(future);

		final Future<?> openTradesFuture = application.executor().submit(new Runnable() {
			@Override
			public void run() {

				//TODO final String message = "ticket="+ticket;
				final String message = "OPEN";
				LOG.info("Sending '" + message + "' on topic " + newTopic);
				socket.send(newTopic.getBytes(), ZMQ.SNDMORE);
				socket.send(OPEN_IN_BYTES, 0);
			}

		});
		application.futureTasks().add(openTradesFuture);
	}

}
