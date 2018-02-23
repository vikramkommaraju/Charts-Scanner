package charts.scanner.app.configuration;

import java.util.List;

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

	@Value("${alphaVantage.apiKeys}") 
	private List<String> apiKeys;
	
	@Value("${alphaVantage.timeout}") 
	private int timeout;
		
}
