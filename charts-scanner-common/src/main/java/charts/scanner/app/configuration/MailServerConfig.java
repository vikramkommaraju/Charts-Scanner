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
	

}
