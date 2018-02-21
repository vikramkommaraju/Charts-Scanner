package charts.scanner.app.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration parameters used to log on to StockCharts
 * 
 * @author vkommaraju
 *
 */
@Configuration
@Getter @Setter
public class StockChartsConfig {

	@Value("${stockCharts.loginUrl}") 
	private String loginUrl;
		
	@Value("${stockCharts.username}") 
	private String userName;
	
	@Value("${stockCharts.password}") 
	private String password;	
}
