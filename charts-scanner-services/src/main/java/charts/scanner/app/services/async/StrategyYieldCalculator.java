package charts.scanner.app.services.async;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import charts.scanner.app.components.TickerQuoteResponse;
import charts.scanner.app.models.PriceActionRecord;
import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.ScannedRecord;
import charts.scanner.app.models.StrategyYieldResult;
import charts.scanner.app.models.StrategyYieldResult.StrategyYieldResultBuilder;
import charts.scanner.app.utils.HelperUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for calculating the yield for a given strategy. Invoked by Strategy YeildScheduler
 * 
 * @author vkommaraju
 *
 */
@Service
@Slf4j
public class StrategyYieldCalculator {

	@Autowired
	private HelperUtils utils;
	
	@Autowired
	private PriceLookupService priceService;
	
	private final String apiKey = "1DTKOY2QKA7MQNN1";
	private final double MIN_YIELD = 5.0;
	
	@Async
	public CompletableFuture<StrategyYieldResult> calculate(ScanStrategy strategy) {
		
		StrategyYieldResultBuilder resultBuilder = StrategyYieldResult.builder().strategy(strategy);
		
		try {
			List<ScannedRecord> records = utils.getRecordsForStrategySinceLastWeek(strategy);
			if(records != null && records.size() > 0) {
				TickerQuoteResponse response = getPricesForRecords(records);
				PriorityQueue<PriceActionRecord> queue = utils.getPriorityQueueWithYield(records, response, MIN_YIELD);
				resultBuilder.queue(queue).foundRecords(records != null && records.size() > 0);				
			}
		} catch (Exception e) {
			log.info("Failed to calculate yield for : " + strategy);
			e.printStackTrace();
		}
		
		return CompletableFuture.completedFuture(resultBuilder.build());
		
	}
	
	private TickerQuoteResponse getPricesForRecords(List<ScannedRecord> records)
			throws InterruptedException, ExecutionException {
		Set<String> tickers = records.stream().map(record -> record.getTicker()).collect(Collectors.toSet());
		TickerQuoteResponse response = priceService.lookup(tickers, apiKey).get();
		return response;
	}

	
	
}
