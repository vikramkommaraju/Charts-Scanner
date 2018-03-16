package charts.scanner.app.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchTerm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import charts.scanner.app.configuration.MailServerConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Service API to read emails from inbox
 * 
 * @author vkommaraju
 *
 */
@Service
@Slf4j
public class MailReader {

	@Autowired
	private MailServerConfig config;

	public void read() throws Exception {
		Store store = getStore();;
		Folder inbox = store.getFolder("Inbox");
		try {
			log.info("Unread messages count: " + inbox.getUnreadMessageCount());
			inbox.open(Folder.READ_WRITE);
			Message[] messages = inbox.getMessages();
			processMessages(messages);
			Arrays.asList(messages).stream().forEach(msg -> {
				try {
					msg.setFlag(Flags.Flag.DELETED, true);
				} catch (MessagingException e) {
					log.info("Delete failed");
				}
			});
		} finally {
			inbox.close(false);
			store.close();
		}
	}

	private void processMessages(Message[] messages) throws MessagingException, IOException {
		System.out.println("messages.length---" + messages.length);

		for (int i = 0, n = messages.length; i < n; i++) {
			Message message = messages[i];
			System.out.println("---------------------------------");
			System.out.println("Email Number " + (i + 1));
			System.out.println("Subject: " + message.getSubject());
			System.out.println("From: " + message.getFrom()[0]);
			System.out.println("Text: " + message.getContent().toString());
		}
	}

	private Store getStore()
			throws NoSuchProviderException, MessagingException {
		Properties properties = getProperties();
		Session emailSession = Session.getDefaultInstance(properties);
		Store store = emailSession.getStore(config.getPop3ServerStore());
		store.connect(config.getPop3Host(), config.getUsername(), config.getPassword());
		return store;
	}

	private Properties getProperties() {
		Properties properties = new Properties();

		properties.put("mail.pop3.host", config.getPop3Host());
		properties.put("mail.pop3.port", config.getPop3Port());
		properties.put("mail.pop3.starttls.enable", config.getPop3Tls());
		return properties;
	}
}
