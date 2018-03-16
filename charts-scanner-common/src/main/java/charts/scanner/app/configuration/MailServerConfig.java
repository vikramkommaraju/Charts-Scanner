package charts.scanner.app.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration parameters used by the Email client
 * 
 * @author vkommaraju
 *
 */
@Configuration
@Getter @Setter
public class MailServerConfig {
	
	@Value("${spring.mail.host}") 
	private String host;
	
	@Value("${spring.mail.port}") 
	private int port;

	@Value("${spring.mail.username}") 
	private String username;

	@Value("${spring.mail.password}") 
	private String password;
	
	@Value("${spring.mail.properties.mail.smtp.auth}") 
	private boolean auth;
	
	@Value("${spring.mail.properties.mail.smtp.starttls.enable}") 
	private boolean tls;
	
	@Value("${pop3.host}")
	private String pop3Host;
	
	@Value("${pop3.store}")
	private String pop3Store;
	
	@Value("${pop3.serverStore}")
	private String pop3ServerStore;
	
	@Value("${pop3.port}")
	private String pop3Port;
	
	@Value("${pop3.tls}")
	private String pop3Tls;
	
	
	
	
	

}
