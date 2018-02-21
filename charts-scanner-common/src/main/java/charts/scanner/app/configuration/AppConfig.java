package charts.scanner.app.configuration;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration parameters common to the app
 * 
 * @author vkommaraju
 *
 */
@Configuration
@Getter @Setter
public class AppConfig {

	@Value("${system.sleep.short}") 
	private long shortSleep;
		
	@Value("${system.sleep.medium}") 
	private long mediumSleep;
	
	@Value("${system.sleep.long}") 
	private long longSleep;
}
