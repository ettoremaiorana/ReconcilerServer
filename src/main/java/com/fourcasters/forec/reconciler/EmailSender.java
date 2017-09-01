package com.fourcasters.forec.reconciler;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by ivan on 21/07/17.
 */
public class EmailSender {

    private final static Logger LOG = LogManager.getLogger(EmailSender.class);
    private final static String PASSWORD = System.getProperty("mail.password");
    public void sendEmail(int algoId, long ticket, String data) {
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
