package charts.scanner.app.services.async;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import charts.scanner.app.components.TickerQuoteResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the {@link PriceLookupService} API
 * 
 * @author vkommaraju
 */
@Service
@Slf4j
public class PriceLookupService {

	private final RestTemplate restTemplate;
	private final float MAX_BATCH_SIZE = 100;
	
	public PriceLookupService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }
	
	@Async
	public CompletableFuture<TickerQuoteResponse> lookup(Set<String> tickers, String apikey) {
		
		if(tickers.size() > MAX_BATCH_SIZE) {
			return handleChunking(tickers, apikey);
		} else {
			return CompletableFuture.completedFuture(runForBatch(tickers, apikey));
		}
    }

	private CompletableFuture<TickerQuoteResponse> handleChunking(Set<String> tickers, String apikey) {
		List<TickerQuoteResponse> responses = getResponsesForBatches(tickers, apikey);
		TickerQuoteResponse combinedResponse = combineResponses(responses);
		return CompletableFuture.completedFuture(combinedResponse);
	}

	private TickerQuoteResponse combineResponses(List<TickerQuoteResponse> responses) {
		TickerQuoteResponse combinedResponse = TickerQuoteResponse.builder().metaData(Maps.newHashMap()).stockQuotes(Lists.newArrayList()).build();
		for(TickerQuoteResponse res : responses) {
			if(res.getMetaData() != null) {
				combinedResponse.getMetaData().putAll(res.getMetaData());				
			}
			if(res.getStockQuotes() != null) {
				combinedResponse.getStockQuotes().addAll(res.getStockQuotes());				
			}
		}
		return combinedResponse;
	}

	private List<TickerQuoteResponse> getResponsesForBatches(Set<String> tickers, String apikey) {
		List<TickerQuoteResponse> responses = Lists.newArrayList();
		List<Set<String>> batches = partitionIntoChunks(tickers);
		
		for(Set<String> batch : batches) {
			TickerQuoteResponse response = runForBatch(batch, apikey);
			responses.add(response);
		}
		return responses;
	}

	private List<Set<String>> partitionIntoChunks(Set<String> tickers) {
		int partitionCount = getPartitionCount(tickers.size(), MAX_BATCH_SIZE);
		List<Set<String>> batches = splitIntoBatches(tickers, partitionCount);
		return batches;
	}

	private TickerQuoteResponse runForBatch(Set<String> tickers, String apikey) {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return restTemplate.getForObject(getUrlForBatchRequest(tickers, apikey), TickerQuoteResponse.class);
	}

	private int getPartitionCount(float size, float maxBatchSize) {
		float partition = ((float) size/(float) maxBatchSize);
		return ((int) (Math.ceil(partition)));
	}

	private List<Set<String>> splitIntoBatches(Set<String> tickers, int batchSize) {
		List<Set<String>> batches = new ArrayList<Set<String>>(batchSize);
		for (int i = 0; i <batchSize; i++) {
			batches.add(new HashSet<String>());
		}
		
		int index = 0;
		for (String ticker : tickers) {
		    batches.get(index++ % batchSize).add(ticker);
		}
		return batches;
		
	}

	private String getUrlForBatchRequest(Set<String> tickers, String apiKey) {
		String baseUrl = getBaseUrlForBatchRequest();
		String paramString = StringUtils.join(tickers, ',');		
		String url=baseUrl+paramString+
				"&apikey="+apiKey;
		return url;
	}

	private String getBaseUrlForBatchRequest() {
		return "https://www.alphavantage.co/query?function=BATCH_STOCK_QUOTES&symbols=";
	}
	
}
