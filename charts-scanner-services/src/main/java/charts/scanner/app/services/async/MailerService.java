package charts.scanner.app.services.async;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service API for sending email alerts
 * 
 * @author vkommaraju
 *
 */
@Service
@Slf4j
public class MailerService {
  
    @Autowired
    private JavaMailSender mailSender;
    
    private boolean isDev = false;
    
    public void send(String subject, String content) throws Exception {
    		MimeMessage message = newMessage(getSendTo(), subject, content);
		mailSender.send(message);
    }

	private Address[] getSendTo() throws AddressException {
		if(isDev) {
			return new InternetAddress[] {new InternetAddress("vikthered@gmail.com")};
		} else {
			return new InternetAddress[] {
								new InternetAddress("vikthered@gmail.com"),
								new InternetAddress("sunilmvn@gmail.com"),
								new InternetAddress("venky.kv@gmail.com")
					};
		}
	}

	private MimeMessage newMessage(Address[] addresses, String subject, String content) throws MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		message.setContent(content, "text/html; charset=utf-8");
		message.setRecipients(RecipientType.TO, addresses);
		message.setSubject(subject);
		return message;
	}
}
