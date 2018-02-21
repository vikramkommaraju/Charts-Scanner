package charts.scanner.app.services.async;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import charts.scanner.app.components.TickerQuoteResponse;
import charts.scanner.app.configuration.AlphaVantageConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the {@link PriceLookupService} API
 * 
 * @author vkommaraju
 */
@Service
@Slf4j
public class PriceLookupService {

	@Autowired
	AlphaVantageConfig config;
	
	private final RestTemplate restTemplate;
	
	public PriceLookupService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }
	
	@Async
	public CompletableFuture<TickerQuoteResponse> lookup(Set<String> tickers) {
        TickerQuoteResponse response = restTemplate.getForObject(getUrlForBatchRequest(tickers), TickerQuoteResponse.class);
        return CompletableFuture.completedFuture(response);
    }

	private String getUrlForBatchRequest(Set<String> tickers) {
		String baseUrl = getBaseUrlForBatchRequest();
		String paramString = StringUtils.join(tickers, ',');		
		String url=baseUrl+paramString+
				"&apikey="+config.getApiKey();
		return url;
	}

	private String getBaseUrlForBatchRequest() {
		return "https://www.alphavantage.co/query?function=BATCH_STOCK_QUOTES&symbols=";
	}	
	
}
