package com.ning.arecibo.alert.email;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.google.inject.Inject;
import com.ning.arecibo.alert.guice.FromEmailAddress;
import com.ning.arecibo.alert.guice.SMTPHost;
import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.mail.EmailException;

import com.ning.arecibo.util.Logger;


public class EmailManager
{
    private final static Logger log = Logger.getLogger(EmailManager.class);

    public final static int MAX_CHARS_FOR_SMS = 140;
 
    private final String smtpHost;
    private final String fromEmailAddress;
    private final String sendingHostSignature;
    
    @Inject
    public EmailManager(@SMTPHost String smtpHost,
                        @FromEmailAddress String fromEmailAddress) {
        
        this.smtpHost = smtpHost;
        this.fromEmailAddress = fromEmailAddress;
        
        String hostName;
        String hostIp;
        
		try {
			InetAddress localHost = InetAddress.getLocalHost();
			hostName = localHost.getHostName();
			hostIp = localHost.getHostAddress();
		}
		catch(UnknownHostException uhEx) {
			log.warn("UnknownHostException: won't be able to append sending host info to outgoing email");
        	log.info(uhEx);
        	
        	hostName = "";
        	hostIp = "";
		}
		
		sendingHostSignature = "\n\nSent by Arecibo Alert Service: " + hostName;
        
    }
    
    public boolean sendEmail(String to,String from,String subject,String message) {
    	return sendEmail(to,from,subject,message,false);
    }
    
    public boolean sendEmail(String to,String from,String subject,String message,boolean appendSendingHostInfo) {
    	
    	if(appendSendingHostInfo) {
    		message += sendingHostSignature;
    	}
        
        try {
            SimpleEmail email = new SimpleEmail();
            email.setHostName(smtpHost);
        	email.addTo(to);
        	email.setFrom(from);
        	email.setSubject(subject);
        	email.setMsg(message);
        	
        	log.debug("Sending email to: %s, subject = '%s'",to,subject);
        
        	email.send();

            return true;
        }
        catch(EmailException mEx) {
            log.warn("EmailException: could not send email to:'%s' from:'%s' subject:'%s'",to,from,subject);
            log.info(mEx);

            return false;
        }
    }
    
    public String getFrom() {
        return fromEmailAddress;
    }
}
