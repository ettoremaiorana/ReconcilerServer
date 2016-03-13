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

public class TradeAppender implements MessageHandler {

	private final static String PASSWORD = System.getProperty("mail.password");
	private final static Logger LOG = LogManager.getLogger(TradeAppender.class);
	final static Context ctx = Application.context;
	final static Socket socket = ctx.socket(ZMQ.PUB);

	static {
		socket.connect("tcp://localhost:51127");
	}
	@Override
	public void enqueue(String topic, String data) {
		final Future<?> future = Application.executor.submit(new Runnable() {
			@Override
			public void run() {

				final String newTopic = "RECONCILER" + topic.substring(topic.indexOf("@"));

				//TODO final String message = "ticket="+ticket;
				final String message = "full";
				LOG.info("Sending '" + message + "' on topic " + newTopic);
				socket.send(newTopic.getBytes(), ZMQ.SNDMORE);
				socket.send(message.getBytes(), 0);

				//example: buffer = status + "," + type + "," + price + "," + ticket;
				final long ticket = Long.parseLong(data.split(",")[3].trim());
				//example: topic = topic_name + "@" + cross + "@" + algo_id
				final int algoId = Integer.parseInt(topic.split("@")[2].trim());
				sendEmail(algoId, ticket, data);
				
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
		Application.tasks.add(future);
	}

}
