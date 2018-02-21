package charts.scanner.app.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration parameters for AlphaVantage API
 * 
 * @author vkommaraju
 *
 */
@Configuration
@Data
@Getter @Setter
public class AlphaVantageConfig {

	@Value("${alphaVantage.apiKey}") 
	private String apiKey;
	
	@Value("${alphaVantage.timeout}") 
	private int timeout;
		
}
